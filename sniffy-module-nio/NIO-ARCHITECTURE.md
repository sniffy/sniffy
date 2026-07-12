# Sniffy NIO architecture

This document is a maintenance guide to Java NIO and Sniffy's wrapper implementation. It describes the invariants
that production code and tests must preserve. Global-state test isolation is documented in
[NIO-TESTING.md](NIO-TESTING.md); future work is tracked in [NIO-TODO.md](NIO-TODO.md).

## A. Ordinary Java NIO

### Why selectors exist

A blocking socket normally dedicates a thread to waiting for one connection. A nonblocking channel instead reports
that an operation cannot currently make progress. A selector lets one thread wait for readiness changes on many
nonblocking channels, which is useful for servers and event loops with many mostly idle connections.

`SocketChannel` is the byte-stream TCP channel. It can connect, read, and write. `ServerSocketChannel` listens and
accepts new `SocketChannel` instances. In blocking mode these operations can wait. In nonblocking mode they return
immediately: for example, `read` may return zero and `accept` may return `null`.

`Selector` is the readiness multiplexer. A nonblocking channel is registered with it together with an interest mask.
Registration produces a `SelectionKey`, which is the durable relationship among one channel, one selector, and the
operations of interest.

```text
SocketChannel ---- registration ----> Selector
       \                                 |
        +--------- SelectionKey <--------+
```

The key's `interestOps` are operations the application wants to hear about, such as connect, accept, read, or write.
Its `readyOps` are the subset the JDK found ready during the latest selection. Changing `interestOps` changes future
selection work; it does not manufacture readiness.

`selector.keys()` is the live set of all registrations still owned by the selector. `selector.selectedKeys()` is the
live set of keys selected as ready. The application removes processed keys from `selectedKeys()`. The JDK deliberately
does not clear this set on every selection because retaining an unprocessed ready key is useful and avoids needless set
churn.

An optional application attachment can be stored on a key. Event loops commonly attach a connection state or handler
and retrieve it when the key becomes ready.

### Selection, wakeup, and cancellation

- `select()` waits indefinitely until readiness, wakeup, closure, or another permitted return condition.
- `select(timeout)` waits for at most the requested time.
- `selectNow()` performs the readiness update without waiting.
- `wakeup()` causes a blocked selection to return.

Wakeup behaves like one sticky permit, not an accumulating counter. A wakeup before the next selection makes that
selection return promptly. Several wakeups before one selection may collapse into one permit. Once consumed, a later
blocking selection needs a new readiness event, wakeup, or close. Tests must therefore establish that the intended
selection has entered before using wakeup as a handoff.

`SelectionKey.cancel()` invalidates the key logically at once, but physical deregistration is deferred. The JDK puts
the key in an internal cancelled-key collection and drains that collection at defined points in selection. Deferral
avoids mutating the selector's registration structures from arbitrary threads while selection is traversing them.
Cancellation and deregistration are therefore different moments:

```text
valid key -- cancel() --> logically invalid -- selector drain --> physically deregistered
```

Closing a channel cancels all its keys. Closing a selector cancels its registrations. In both cases the JDK may still
need a selection/cleanup pass to finish physical removal.

### Small event loop

```java
try (Selector selector = Selector.open();
     ServerSocketChannel server = ServerSocketChannel.open()) {
    server.configureBlocking(false);
    server.register(selector, SelectionKey.OP_ACCEPT);

    while (server.isOpen()) {
        selector.select();
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();              // application consumed this readiness
            if (!key.isValid()) continue;
            if (key.isAcceptable()) { /* accept and register a SocketChannel */ }
            if (key.isReadable())   { /* read without blocking */ }
        }
    }
}
```

## B. Sniffy's wrapping model

Applications must interact with Sniffy objects so connection policy, accounting, proxy parsing, and traffic capture
cannot be bypassed. JDK selector implementations must still operate on their real provider-specific objects. Sniffy
therefore maintains pairs:

```text
SniffySocketChannel -> delegate SocketChannel
SniffySelector      -> delegate Selector
SniffySelectionKey  -> delegate SelectionKey
```

Application registration starts with the wrapper channel and selector, but the delegate channel is what can be
registered with the delegate selector. The resulting delegate key is wrapped before it is returned to application
code. `SelectionKeyLink` is the internal backlink that joins the selector, wrapper channel, delegate key, canonical
wrapper key, and registration state.

The delegate key attachment is reserved for this link. The wrapper key independently stores the application's
attachment. This prevents `attach()` by application code from destroying Sniffy's ownership metadata while preserving
the public attachment contract.

One logical registration consequently exists in several physical places:

```text
SniffySelector.activeLinks
wrapper channel's JDK key array
delegate selector's key collections
delegate channel's JDK key array
SelectionKeyLink and its wrapper/delegate keys
```

Cancellation and close must reconcile all of them. A copied `keys()` or `selectedKeys()` set would go stale and would
have the wrong public monitor, so Sniffy exposes stable live views that translate delegate keys to canonical wrapper
keys. Selection consumer callbacks run only after delegate selection has returned and released its private selector
locks; application code is then called under the public wrapper monitors required by `Selector`.

`ChannelRegistrationSupport` was removed because it was a second passive strong-reference registry with ambiguous
ownership. `SniffySelector.activeLinks` owns the Sniffy lifecycle, while the wrapper channel's ordinary JDK key array
owns channel registration. Cleanup removes selector ownership only after wrapper-key removal succeeds.

The socket returned by `SniffySocketChannel.socket()` is one stable view of the same connection. Its streams route NIO
physical I/O, ordering, endpoint policy, close, and accounting back through the channel. Classic socket monitoring does
not use this NIO-specific bridge.

## C. Lock and lifecycle invariants

### Public selection order

Every selection operation that populates selected keys uses:

```text
SniffySelector
  -> exact public selectedKeys view
    -> delegate selection internals
```

The delegate's private monitors are not substitutes for the two public monitors.

### Selector close

```text
registrationLifecycle: OPEN -> CLOSING, then release
  -> delegate wakeup
  -> SniffySelector monitor
  -> public selectedKeys monitor
  -> delegate close
  -> wait for admitted registrations
  -> wrapper-key/link cleanup
  -> registrationLifecycle: CLOSED
```

Wakeup precedes the public monitors because a blocked selection owns those monitors. `registrationLifecycle` is never
held while acquiring public monitors, invoking delegate close, or acquiring a wrapper channel lock.

### Registration and ownership

```text
REGISTERING -> ACTIVE -> CLEANING -> REMOVED
```

Admission under `registrationLifecycle` increments `registrationsInFlight` exactly once. A `finally` decrements it
exactly once. The operation either returns a fully active wrapper key or rolls back the delegate key, wrapper-channel
key, and link. Close prevents new admission and waits for already admitted operations. `activeLinks` retains ownership
until wrapper-channel key removal succeeds; failed cleanup stays owned for reporting or retry.

Never hold a delegate selector or delegate channel lock while acquiring a wrapper channel lock. In particular, snapshot
links under `registrationLifecycle`, release it, and only then touch the channel key array.

### Connection-registry ownership

`ConnectionsRegistry` keeps weak registrations under both hostname and IP aliases so policy changes can update a live
connection. Registration is idempotent for the same connection identity and endpoint. A monitored connection's close
path explicitly unregisters that identity from every alias bucket after attempting physical close; repeated removal is
safe and empty buckets are removed. The reference-queue housekeeper is a fallback for abandoned connections and removes
the enqueued weak-reference object directly, because its referent is already unavailable at that point.

### Application and instrumentation code

Application callbacks never run while private delegate selector locks or internal provider-construction scopes are
held. Instrumentation failure must not prevent physical close, shutdown, cancellation, wakeup, or required cleanup.
When several close steps fail, preserve the primary failure and attach later failures as suppressed.

For connected socket I/O, `connectionReadLock` serializes each physical read and its immutable accounting/capture
event. `connectionWriteLock` does the same for writes, including incremental proxy parsing. Channel operations and
socket streams use the same locks. Close performs the physical delegate close before waiting for an I/O ordering lock,
so it can release a thread blocked in native I/O.

`bufferAccountingLock` protects only the cross-direction reader/writer IDs, buffered-byte counters, direction-switch
resets, and delay-cycle calculation. Physical I/O, policy resolution, sleeping, traffic publication, proxy parsing, and
TLS correlation never run under it, so reads and writes remain physically full duplex.

Each operation reads one immutable effective-endpoint policy snapshot. HTTP CONNECT parsing atomically replaces the
physical policy with a target policy after the successful physical write. The new policy starts with the next operation;
the already transmitted CONNECT write is accounted using its starting policy.

## D. Intentionally hacky JDK integration

Sniffy globally replaces the JVM's `SelectorProvider` to intercept future channel and selector construction. This is a
JDK-wide integration point, so installation is opt-in and must fail clearly when capabilities are unavailable.

Provider selectors require selectable channels implementing the internal `SelChImpl` contract. Its signatures change
between JDK releases, so adapters isolate compatibility calls. `JdkNioAccess` obtains trusted lookup/module access and
invokes private `AbstractSelectableChannel.removeKey` to reconcile the wrapper channel's key array. On some JDKs that
private method completes the necessary array mutation and then performs a provider-specific cast; a controlled trailing
`ClassCastException` can therefore mean removal already succeeded. The compatibility layer verifies the postcondition
before treating that exception as success.

These techniques are intentionally narrow, capability-probed, and backed by the OS/JDK matrix. A green test on one JDK
does not prove another provider's key arrays, locks, module boundaries, or native close/wakeup behavior. Keep probes,
conditional platform tests, diagnostic dumps, and matrix coverage when changing this layer.

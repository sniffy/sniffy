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

These rules are correctness constraints, not implementation preferences. Each entry records the required order or
owner, why it exists, the failure it prevents, and the deterministic tests that must remain green when the area changes.

### Stable public selection monitors

**Rule.** `keys()` and `selectedKeys()` each return one stable live translating view for the selector's lifetime. Every
selection operation that can populate selected keys acquires monitors in this exact order:

```text
SniffySelector
  -> the exact object returned by selectedKeys()
    -> delegate selection internals
```

Consumer callbacks run later under the same two public monitors, after delegate selection has returned. A copied set,
a newly allocated translating wrapper, or a delegate-private collection is not an equivalent lock.

**Why.** Java applications are allowed to synchronize on the public selector and selected-key set. Stable identity is
what makes their exclusion coordinate with Sniffy's selection and translation. The delegate's monitors protect a
different object graph and cannot satisfy the public contract.

**Failure if violated.** Locking a replacement view lets the application iterate the real public set while selection
mutates it, producing lost removals or concurrent traversal failures. Reversing the two public locks creates a cycle
with application code that follows the documented selector-then-set order. Calling a consumer while delegate locks are
held lets reentrant interest changes, cancellation, channel close, or selector close deadlock in provider code.

**Protected by.** `SniffySelectorSynchronizationTest.selectionUsesSelectorThenPublicSelectedKeyMonitor`,
`eachPublicMonitorPreventsSelectionFromEnteringDelegate`,
`selectedKeySetCanBeIteratedWhileItsPublicMonitorBlocksSelection`, and
`consumerRunsOnlyUnderPublicMonitorsAfterDelegateMonitorsAreReleased`; stable wrapper identity and live-set behavior
are also covered by `SniffySelectionKeyContractTest` and `SniffySelectorLifecycleTest.consumerSelectVariantsExposeWrappersAndProcessCancellation`.

### Selector close and wakeup order

**Rule.** Close performs this sequence:

```text
registrationLifecycle: OPEN -> CLOSING, then release registrationLifecycle
  -> delegate wakeup
  -> SniffySelector monitor
  -> exact public selectedKeys monitor
  -> delegate close
  -> wait for admitted registrations
  -> wrapper-key/link cleanup
  -> registrationLifecycle: CLOSED
```

No public monitor, delegate close, wrapper-channel key lock, or wait for in-flight registration is entered while
`registrationLifecycle` is held. Repeated close calls join or observe the same lifecycle instead of closing the
delegate twice.

**Why.** A thread blocked inside `select()` already owns both public monitors while it waits in delegate code. The
delegate wakeup must happen before close waits for either monitor so that selection can return and release them.
Releasing `registrationLifecycle` before public locks and delegate close also lets an admitted registration finish its
publication or rollback and decrement the in-flight count.

**Failure if violated.** Waiting for the selector monitor before wakeup deadlocks close against an indefinitely blocked
select. Holding `registrationLifecycle` while waiting for an admitted registration deadlocks against that
registration's `finally`. Closing the delegate before changing admission state lets a new registration enter a selector
whose native resources are already disappearing.

**Protected by.** `SniffySelectorLifecycleTest.closeWakesIndefinitelyBlockedSelectWithoutExternalWakeup`,
`closeFromSelectionConsumerDoesNotDeadlock`, and `concurrentCloseCallsRemainIdempotent`;
`SniffySelectorSynchronizationTest.closeUsesSelectorThenPublicSelectedKeyMonitor`;
`SniffySelectorRegistrationRaceTest.registrationRacingWithBlockedSelectAndCloseCannotBecomeActive`; and
`SniffySelectorFailureLifecycleTest.selectorCloseClosesDelegateExactlyOnce`.

### Registration admission, state, and ownership

**Rule.** Admission under `registrationLifecycle` increments `registrationsInFlight` once, and one `finally` decrements
it once. A link follows only:

```text
REGISTERING -> ACTIVE -> CLEANING -> REMOVED
```

Registration returns only after delegate key creation, canonical wrapper-key publication, wrapper-channel key-array
publication, and `activeLinks` activation are all complete. Every failure path rolls back every structure it published.
Close rejects new admission immediately and waits for already admitted attempts. During cleanup, `activeLinks` remains
the owner until removal from the wrapper channel's JDK key array succeeds; only then may the link become `REMOVED` and
leave `activeLinks`.

**Why.** `activeLinks` is the selector's only authoritative list of Sniffy registrations that may still require work.
Wrapper-key removal is the last step that proves application-visible channel registration is gone. Retaining the link
until that postcondition succeeds keeps a failed cleanup discoverable for suppression, reporting, and retry.

**Failure if violated.** Removing a link first can orphan a wrapper key in the channel: `keyFor(selector)` may keep
returning it, `configureBlocking(true)` may fail forever, and no owner remains to retry cleanup. Missing or double
in-flight accounting lets close either return while a key can still become active or wait forever. Partial publication
without rollback leaks delegate keys, attachments, or strong wrapper references.

**Protected by.** all pause points in `SniffySelectorRegistrationRaceTest`, especially
`closeWaitsForFinalizedRegistrationPublicationAndRemovesIt` and
`repeatedRegisterCloseRacesRetainNeitherLinksNorWrapperKeys`;
`SniffySelectorFailureLifecycleTest.checkedRegistrationFailureDoesNotRetainOwnership` and
`cleanupFailureIsSuppressedOntoPrimaryFailureAndRetried`; and the cancellation/close cycles in
`SniffySelectorLifecycleTest`.

### Delegate-to-wrapper lock boundary

**Rule.** Never acquire a wrapper channel's key-array lock while holding a delegate selector lock, delegate channel
lock, or `registrationLifecycle`. Cleanup snapshots links under `registrationLifecycle`, releases it, and only then
touches wrapper channels. Application callbacks and provider-facing delegate construction also stay outside the other
side's locks/scopes.

**Why.** Delegate registration and deregistration use provider-private selector/channel locks in an order Sniffy does
not control, while wrapper registration uses `AbstractSelectableChannel`'s key locks. Treating the delegate call as a
hard boundary prevents a wrapper-to-delegate path from meeting a delegate-to-wrapper cleanup path.

**Failure if violated.** Registration, cancellation, channel close, and selector close can form a lock cycle that is
provider- and timing-dependent, often appearing only on one OS/JDK selector implementation.

**Protected by.** `SniffySelectorRegistrationRaceTest`, the blocked-selection cancellation/close cases in
`SniffySelectorLifecycleTest`, `SniffySelectorSynchronizationTest`, and the supported OS/JDK provider matrix.

### Application callback boundary

**Rule.** Delegate selection and key translation finish before application consumers run. Consumers receive canonical
`SniffySelectionKey` instances under the two public wrapper monitors, never under private delegate selector locks and
never while `SniffySelectorProvider`'s delegate-construction scope is active.

**Why.** A consumer is unrestricted application code: it may change interest operations, cancel, register, close a
channel, close the selector, or open another selector/channel. Those operations must see normal wrapper behavior and
must not inherit Sniffy's internal provider bypass.

**Failure if violated.** Reentrant application work can deadlock provider internals, receive delegate objects that
bypass monitoring, or construct unwrapped channels because an internal construction scope leaked across the callback.

**Protected by.** `SniffySelectorSynchronizationTest.consumerRunsOnlyUnderPublicMonitorsAfterDelegateMonitorsAreReleased`
and `SniffySelectorLifecycleTest.consumerSelectDoesNotLeakDelegateConstructionScopeToApplicationConsumer`,
`consumerMayChangeInterestOpsAndCloseItsChannel`, `closeFromSelectionConsumerDoesNotDeadlock`, and
`cleanupRunsWhenSelectionConsumerThrows`.

### Connection-registry ownership

**Rule.** `ConnectionsRegistry` weakly registers one connection identity under both hostname and IP aliases.
Registration is idempotent for the same identity and endpoint. After attempting physical close, the connection close
path identity-removes itself from every alias; repeated removal is safe and empty buckets disappear. The reference-queue
housekeeper is only a fallback and removes the enqueued weak-reference object itself, not its already-cleared referent.

**Why.** Registry callbacks must reach a live connection's current policy without making the registry a lifetime owner.
Explicit close gives prompt removal, while weak references cover abandoned connections.

**Failure if violated.** Duplicate registrations deliver repeated callbacks, strong or stale aliases retain closed
connections, and referent-based queue cleanup cannot find an object after the JVM has cleared it.

**Protected by.** `ConnectionsRegistryTest` identity/queue cases and `NioTestIsolationTest.connectionRegistrationsDoNotLeakAcrossFunctionalTests`,
`socketChannelCloseUnregistersNetworkConnectionAliases`, and
`wildcardUpdateDoesNotReachConnectionFromCompletedTestScope`.

### Connected I/O ordering and close

**Rule.** `SniffySocketChannel` visibly owns `connectionReadLock` and `connectionWriteLock` and performs physical
`read`/`write` itself. Each direction lock covers one physical operation, the exact immutable byte copy, accounting,
raw traffic publication, and, for writes, incremental CONNECT parsing and policy publication. The stable socket view and
its streams call back into the same channel and use the same locks. Reads and writes use different locks. Close attempts
the physical delegate close before waiting for either direction's post-I/O work.

The extracted `SocketChannelEndpointPolicy`, `SocketChannelFaultDelayState`, `SocketChannelOutboundTraffic`, and
`SocketChannelTrafficPublisher` collaborators own state transitions only; they do not perform physical channel I/O or
own the direction locks.

**Why.** The capture stream and proxy parser must describe wire order across mixed channel/stream calls. Separate
direction locks preserve full duplex. Closing the delegate first releases a thread blocked in native I/O so close can
later acquire its direction lock and finalize pending telemetry.

**Failure if violated.** Concurrent writers can publish bytes in a different order than the peer received them, which
can corrupt CONNECT detection and TLS correlation. Separate channel and stream locks can reorder the two APIs. Taking a
direction lock before physical close can deadlock forever behind a native blocking read or write.

**Protected by.** all `SniffySocketChannelOrderingTest` cases,
`SniffySocketChannelBufferTest.gatheringWriteAndScatteringReadCaptureExactBytes`, mixed API tests in
`SniffySocketChannelBufferTest`, and close/stream lifecycle cases in `SniffySocketChannelPolicyLifecycleTest`.

### Fault-delay accounting remains a short full-duplex section

**Rule.** `bufferAccountingLock` protects only reader/writer thread IDs, buffered-byte counters, direction-switch
resets, and delay-cycle calculation. Physical I/O, policy resolution, sleeping, logging, traffic callbacks, proxy
parsing, and TLS correlation happen after it is released.

**Why.** The counters are cross-direction state and need one consistent update, but the network itself is full duplex
and instrumentation can reenter application or registry code.

**Failure if violated.** Unsynchronized counters lose updates or calculate the wrong delay; widening the lock serializes
independent reads and writes and can deadlock on a reentrant callback or a sleep controlled by another direction.

**Protected by.** `SniffySocketChannelAccountingTest.concurrentReadAndWriteDoNotLoseBufferAccountingUpdates`,
`directionSwitchAccountingUsesOneConsistentState`, and
`accountingLockDoesNotSerializePhysicalFullDuplexIO`.

### Atomic endpoint policy and post-write CONNECT publication

**Rule.** One immutable `EffectiveEndpointPolicy` contains address, status, generation, and proxied state. An operation
captures one object before physical I/O and uses it for permission, delay, and accounting. A successful write is sent and
accounted under its starting policy; only afterward may CONNECT parsing atomically replace the physical endpoint policy
with a target policy. That target starts with the next operation. Endpoint-aware registry callbacks update only the
matching active endpoint.

**Why.** Address and status describe one policy generation. Post-write detection cannot decide the target until enough
bytes have physically succeeded, and a completed I/O result must not be retroactively denied by the policy it revealed.

**Failure if violated.** Readers can observe a target address with a physical-proxy status, a physical registry callback
can overwrite the tunneled target, or a CONNECT write can throw after its bytes have already reached the peer.

**Protected by.** `SniffySocketChannelPolicyLifecycleTest.operationNeverSeesNewAddressWithOldStatus`,
`physicalProxyStatusUpdateDoesNotOverwriteTargetPolicy`, the stream/channel/gathering
`*WriteCompletingDeniedConnect*` cases, and target/wildcard/thread-local update cases in that class.

### Failure-safe instrumentation and cleanup

**Rule.** Monitoring failure never skips required physical close, output shutdown, selector wakeup/close, cancellation,
registry removal, or key/link cleanup. Multi-step cleanup keeps the first failure as primary and adds later failures as
suppressed. Idempotent lifecycle calls do not repeat a completed physical action.

**Why.** Instrumentation is secondary to application resource safety, but silently discarding a monitoring or cleanup
failure would make state divergence impossible to diagnose.

**Failure if violated.** A telemetry callback can leak a socket or selector, a secondary exception can hide the original
cause, or a retry can close provider resources twice.

**Protected by.** `SniffySocketChannelPolicyLifecycleTest.telemetryFailureCannotPreventPhysicalClose` and
`telemetryFailureCannotPreventPhysicalShutdownOutput`; `SniffySelectorFailureLifecycleTest.closeAttemptsDelegateAndCleanupAfterWakeupAndCloseFailures`,
`cleanupRunsWhenDelegateSelectThrows`, and `selectorCloseClosesDelegateExactlyOnce`; and repeated close cases in
`SniffySelectorLifecycleTest`.

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

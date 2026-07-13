# NIO module agent instructions

Before modifying this module, read `NIO-ARCHITECTURE.md`, `NIO-TESTING.md`, and `NIO-TODO.md` completely. They define
the object model, test isolation boundary, non-goals, and the rationale behind the following non-negotiable rules.

## Operational invariants

- Selection locks `SniffySelector`, then the exact stable public `selectedKeys()` view, then enters delegate selection.
  Consumer callbacks run only after delegate locks are released and never inherit provider-construction scope.
- Selector close changes admission from `OPEN` to `CLOSING`, releases the lifecycle lock, wakes the delegate before
  waiting for public monitors, closes the delegate, waits for admitted registrations, cleans keys/links, then publishes
  `CLOSED`.
- Every admitted registration increments `registrationsInFlight` once and decrements it once in `finally`.
  Registration either publishes a fully active canonical wrapper key or rolls back every delegate/wrapper structure.
- `activeLinks` owns a registration until wrapper-channel key removal succeeds. Never drop selector ownership first;
  failed cleanup must remain discoverable for suppression, reporting, or retry.
- Never acquire a wrapper-channel key lock while holding a delegate selector/channel lock or `registrationLifecycle`.
  Snapshot under lifecycle ownership, release it, then touch wrapper channels.
- `SniffySocketChannel` retains physical `read`/`write` and the per-direction ordering locks. Channel calls and the stable
  socket/stream view share those locks, endpoint policy, counters, capture state, and connection identity. Reads and
  writes remain full duplex.
- One immutable endpoint-policy snapshot governs each operation. HTTP CONNECT detection is post-write: the successful
  write uses its starting policy, and a detected target policy begins with the next operation.
- Physical close/shutdown happens even when proxy finalization, traffic publication, registry removal, or other
  instrumentation fails. Close the physical channel before waiting for an I/O ordering lock; suppress later failures
  onto the primary failure.
- Functional tests never replace or uninstall the JVM-global `SelectorProvider`. Provider mutation belongs only in the
  isolated, non-reused provider-lifecycle fork; raw fixtures use `NioFunctionalTestEnvironment.originalProvider()` and
  `openRawSelector()`.

Do not intentionally redesign an invariant unless the task explicitly requests that redesign. When it does, update
`NIO-ARCHITECTURE.md` and deterministic tests in the same change so the new rule, rationale, failure mode, and coverage
remain reviewable.

## Test discipline

- Use latches, barriers, controlled hooks, and explicit state observations. Do not use timing sleeps or retries to make
  races pass, and do not weaken a concurrency assertion or timeout.
- Snapshot and restore locally mutable global state with `NioTestStateScope`/`NioTestStateRule`. Cleanup runs in reverse
  order, continues after failure, and suppresses secondary failures.
- Run `mvn -pl sniffy-module-nio -am clean test` on Java 8 and the current development JDK. For ordering/isolation work,
  also run the functional suite with `-Dnio.functional.runOrder=reversealphabetical`. Run `git diff --check` before
  committing.

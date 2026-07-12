# NIO follow-up work

## Protocol coverage

- Implement real NIO2/AIO monitoring.
- Design monitored UNIX-domain socket addresses, traffic identity, and fault rules.
- Define UDP/datagram endpoint identity, per-datagram traffic capture, and fault injection.
- Add SOCKS proxy detection and interception.
- Add true pre-send HTTP CONNECT interception; current bounded incremental detection is post-write.

## API and JDK parity

- Audit broader `ServerSocket`, socket-view, and option behavior across supported JDKs.
- Verify `FileChannel.transferTo` and other zero-copy paths across JDK implementations.
- Evaluate whether the public `getDelegate()` escape hatch should remain available or be constrained.
- Define a recovery/reporting strategy for cleanup failures discovered after selector close.

## Confidence and rollout

- Add longer-running GC, weak-reference retention, registration, cancellation, and selector concurrency soaks.
- Enable NIO monitoring by default only after the supported OS/JDK matrix passes protocol, retention, fault-injection,
  proxy, TLS-correlation, and concurrency suites without delegate-state divergence.

## Planned stress harnesses

### Nightly deterministic stress

- Run JDK 8 and the current LTS JDK on Linux, plus a rotating second OS.
- Record a fixed random seed for every run and print it with every failure; never retry a failure to green.
- Execute 10,000 to 100,000 seeded registration/select/cancel/close cycles, including concurrent selector close,
  concurrent channel close, and wrapper/delegate cancellation.
- Mix channel and socket-stream reads and writes, fragmented proxy prefixes, and close/shutdown during I/O.
- Check `ThreadMXBean` for deadlocks, assert `activeLinks` returns to zero, and retain weak references for forced-GC
  retention checks.
- Bound the workflow and each worker. Always upload the seed, Surefire dumps, explicit thread dumps, heap summaries,
  and JVM crash files on success or failure.

### Weekly multi-hour stress

- Run the supported OS/JDK matrix, an alternate JVM such as OpenJ9, and selected QEMU architectures.
- Repeatedly install and uninstall the provider; exercise both long-lived selectors and thousands of short-lived
  selectors.
- Force GC and verify weak-reference cleanup, registration retention, active-link ownership, and channel key arrays.
- Fragment HTTP CONNECT and mixed reads/writes at seeded boundaries while racing native close and wakeup.
- Track file descriptors or handles, native resources, and heap-growth trends over the complete run.
- Save seeds, thread/heap dumps, crash logs, resource samples, and trend data. A failure remains failed until its seed is
  reproduced and diagnosed.

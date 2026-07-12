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

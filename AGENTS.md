# Repository agent instructions

These rules apply to every change in this repository. More specific `AGENTS.md` files may add constraints for their
subtrees.

## Compatibility and scope

- Preserve Java 8 source, bytecode, and runtime compatibility unless a task explicitly changes the supported baseline.
  Do not use post-Java-8 language features or APIs in shared production or test sources. JDK-specific integrations must
  stay behind the existing capability probes and Maven profiles.
- Keep changes inside the requested behavior. Preserve public contracts and existing user changes; do not turn a
  focused fix into an unrelated redesign.

## Concurrency and lifecycle

- Treat deterministic concurrency tests as executable contracts. Do not weaken assertions, add retries, retry a failed
  run to green, replace latches/hooks with timing sleeps, or increase timeouts to hide an ordering defect.
- Instrumentation is secondary to application resource safety. Monitoring, proxy parsing, logging, registry, or
  callback failure must not prevent required physical close, shutdown, cancellation, wakeup, or cleanup. Preserve the
  first failure and suppress later cleanup failures when multiple steps fail.
- Keep application callbacks out of private JDK/provider locks and internal construction scopes. Preserve documented
  lock order and ownership rather than relying on a particular OS/JDK implementation.

## Verification

- Run focused tests for the changed behavior first.
- For NIO changes, run `mvn -pl sniffy-module-nio -am clean test` on Java 8 and the current development JDK.
- For TLS changes, run `mvn -pl sniffy-module-tls -am clean test` on Java 8 and the current development JDK.
- For cross-module or release-facing changes, run
  `mvn -T 1C -B clean verify --file pom.xml -U -P ci -Dgpg.skip=true -Dmaven.wagon.http.retryHandler.count=3`.
- Run `git diff --check` before committing. A failing or skipped required check must be reported with its exact reason;
  never describe a partial or ignored run as passing.

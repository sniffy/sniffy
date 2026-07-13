# Repository agent instructions

These rules apply to every change in this repository. More specific `AGENTS.md` or `AGENTS.override.md` files may add
constraints for their subtrees.

## Task ownership and autonomy

- Treat the linked GitHub issue and its acceptance criteria as the source of truth. Read the complete issue, relevant
  comments, existing pull requests, and current code before editing.
- For tasks explicitly marked ready for autonomous execution, do not stop for routine design choices or ask for
  confirmation. Inspect the repository, choose the smallest maintainable solution consistent with the issue, and record
  material decisions in the pull request. Stop only for a real blocker such as missing credentials, unavailable external
  infrastructure, destructive ambiguity, or mutually incompatible acceptance criteria.
- Complete the task end to end when permissions allow: implement, add or update tests, run applicable checks, review the
  diff, commit, push, and update or open a draft pull request. Never merge a pull request unless the task explicitly says
  to do so.
- Preserve unrelated user changes. Do not reset, clean, force-push, rebase shared branches, or rewrite history unless the
  issue explicitly requires it. Prefer a new `agent/<short-description>` branch from `develop` for new work.

## Compatibility and scope

- Preserve Java 8 source, bytecode, and runtime compatibility unless a task explicitly changes the supported baseline.
  Do not use post-Java-8 language features or APIs in shared production or test sources. JDK-specific integrations must
  stay behind capability probes, isolated modules, or Maven profiles.
- Keep changes inside the requested behavior. Preserve public contracts and existing user changes; do not turn a
  focused fix into an unrelated redesign.
- Prefer the smallest compatible dependency update. Do not perform broad dependency modernization as part of an
  unrelated task. Document security-motivated upgrades and compatibility trade-offs in the pull request.

## Concurrency and lifecycle

- Treat deterministic concurrency tests as executable contracts. Do not weaken assertions, add retries, retry a failed
  test run to green, replace latches/hooks with timing sleeps, or increase timeouts to hide an ordering defect.
- Instrumentation is secondary to application resource safety. Monitoring, proxy parsing, logging, registry, or
  callback failure must not prevent required physical close, shutdown, cancellation, wakeup, or cleanup. Preserve the
  first failure and suppress later cleanup failures when multiple steps fail.
- Keep application callbacks out of private JDK/provider locks and internal construction scopes. Preserve documented
  lock order and ownership rather than relying on a particular OS/JDK implementation.

## Build and verification

- The Codex Cloud environment is described in `docs/codex-workflow.md`. Use `source .codex/cloud/use-jdk.sh <version>`
  to switch among the installed JDK 8, 11, 17, 21, and 25 toolchains.
- Run focused tests for the changed behavior first.
- For NIO changes, run `mvn -pl sniffy-module-nio -am clean test` on Java 8 and the current development JDK.
- For TLS changes, run `mvn -pl sniffy-module-tls -am clean test` on Java 8 and the current development JDK.
- For cross-module or release-facing changes, run
  `mvn -T 1C -B clean verify --file pom.xml -U -P ci -Dgpg.skip=true -Dmaven.wagon.http.retryHandler.count=3`.
- Run `git diff --check` before committing. A failing or skipped required check must be reported with its exact reason;
  never describe a partial, retried, or ignored run as passing.

## Pull requests

- Keep pull requests draft until the implementation and locally applicable verification are complete.
- The pull request description must explain the problem, design, compatibility impact, tests executed, checks not run,
  dependency changes, and remaining risks. Link the authoritative issue.
- Do not hide limitations. If the environment cannot run a platform-, JDK-, or credential-dependent check, say exactly
  what is missing and leave CI to perform that check.

## Review guidelines

- Prioritize correctness, compatibility, resource safety, concurrency, public API stability, and test integrity over
  formatting preferences.
- Flag tests that were weakened, made timing-dependent, skipped, or retried to conceal a failure.
- Flag accidental Java baseline increases, use of newer JDK APIs in Java 8 artifacts, and unintentional dependency or
  public API changes.
- Flag resource leaks or cleanup paths where Sniffy instrumentation can prevent application resources from closing.

# Codex Cloud environment troubleshooting

## Maven cache warm-up failures

Codex Cloud setup and maintenance run with internet access, but outbound traffic still passes through the Cloud network proxy. A temporary Maven Central failure can therefore occur even when the JDK and Maven downloads succeeded.

The Sniffy environment uses `.codex/cloud/warm-maven-cache.sh` to:

- probe Maven Central directly before each Maven attempt;
- let Maven retry individual HTTP transfers five times;
- retry the complete dependency-resolution command six times;
- wait progressively longer between attempts;
- fail with an explicit diagnostic rather than immediately aborting after two identical attempts.

The cache warm-up remains mandatory because normal agent-phase internet is disabled. Allowing setup to succeed with an incomplete `~/.m2` cache would only move the same failure into the task itself.

If setup still fails after all retries:

1. Check whether the final output reports a failed Maven Central probe or a Maven resolution error.
2. Ensure the environment uses the latest `develop` branch.
3. Select **Reset cache** on the `sniffy` environment page. Codex also invalidates the cache automatically when setup or maintenance scripts change.
4. Start the environment validation task again.
5. For diagnosis, increase the `SNIFFY_MAVEN_WARMUP_ATTEMPTS` environment variable. Do not reduce it to zero or bypass cache warm-up while agent-phase internet is disabled.

The setup script installs the toolchain before warming dependencies, so repeated runs reuse already installed JDKs and Maven from the cached container whenever the cache is available.

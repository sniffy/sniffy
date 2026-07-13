# Codex Cloud environment troubleshooting

## Maven cache warm-up failures

Codex Cloud runs setup and maintenance behind its platform HTTP/HTTPS proxy. The base `codex-universal` image already provides Maven and JDK 11, 17, 21, and 25. These platform runtimes must remain in use because their network and TLS behavior is part of the Cloud environment.

Sniffy only installs Temurin JDK 8, which the universal image does not provide. Dependency warm-up always runs with the Codex-provided Maven and JDK 25; Java 8 is used later for compatibility builds after dependencies are cached.

### Why the earlier setup failed

The first setup downloaded its own Maven and replacement Temurin JDKs. `curl` could download archives through the Cloud proxy, but Maven running on the replacement JDK repeatedly failed during HTTPS artifact resolution. Adding retries and synthesizing `~/.m2/settings.xml` did not fix the failure because the replacement Java runtime did not inherit the platform's complete proxy/TLS integration.

The telltale log pattern was:

- setup successfully downloaded JDK and Maven archives with `curl`;
- Maven proxy configuration was present;
- every Maven attempt failed on the first HTTPS artifact descriptor;
- the failure was deterministic rather than intermittent.

The correct fix is to stop replacing platform tools, not to add more proxy retries.

### Expected setup log

A fresh setup should now show:

```text
Installing latest Temurin JDK 8 GA release...
Using Codex-provided Maven: ...
Warming dependencies with Maven ... and JAVA_HOME=...
```

It must not print either of these old messages:

```text
Installing Apache Maven 3.9.11...
Configured Maven proxy ...
```

### After changing environment scripts

1. Ensure the environment uses the latest `develop` branch.
2. Select **Reset cache** on the `sniffy` environment page.
3. Start the environment validation task again.
4. Confirm `mvn -version` reports the Maven installed by the Codex image, not `/root/.local/share/sniffy-codex/apache-maven-*`.
5. Confirm Java 25 resolves to a `mise` installation while Java 8 resolves to `$HOME/.jdks/temurin-8`.
6. If dependency warm-up still fails, use the final Maven `-X` transport exception. Do not reinstall Maven, replace the platform JDK, or add proxy credentials as a first response.

The cache warm-up remains mandatory while agent-phase internet is disabled. Allowing setup to succeed with an incomplete Maven cache would only move the same resolution failure into the task itself.

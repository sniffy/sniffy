# Codex Cloud environment troubleshooting

## Maven cache warm-up failures

Codex Cloud setup and maintenance run with internet access, but outbound traffic is exposed through standard proxy environment variables such as `HTTPS_PROXY`, `HTTP_PROXY`, and `NO_PROXY`.

Command-line tools such as `curl` read those variables automatically. Maven does not reliably translate them into its own proxy configuration. Maven's supported proxy configuration lives in `${user.home}/.m2/settings.xml`.

This distinction matters when logs show both of the following:

- JDK and Maven archives download successfully with `curl`;
- Maven repeatedly fails on the first artifact descriptor, for example `maven-bundle-plugin`, while the direct Maven Central probe succeeds.

That pattern is a proxy-configuration failure, not a transient repository timeout. Repeating the same Maven command cannot fix it.

The Sniffy environment now uses `.codex/cloud/configure-maven-proxy.sh` before dependency resolution. The script:

- reads upper- and lower-case `HTTPS_PROXY` / `HTTP_PROXY` variables;
- parses optional proxy credentials without printing them;
- converts `NO_PROXY` into Maven `nonProxyHosts` syntax;
- creates or updates the `codex-cloud-env-proxy` entry in `~/.m2/settings.xml`;
- preserves unrelated Maven settings and makes the generated file readable only by the current user;
- runs during both initial setup and cached-environment maintenance.

`.codex/cloud/warm-maven-cache.sh` then:

- verifies that Maven proxy settings exist when Cloud proxy variables are present;
- probes Maven Central separately for diagnosis;
- retries Maven dependency resolution three times for genuine transient failures;
- enables Maven stack traces on the final attempt;
- distinguishes a curl/proxy mismatch from complete network failure in its final diagnostic.

The cache warm-up remains mandatory because normal agent-phase internet is disabled. Allowing setup to succeed with an incomplete `~/.m2` cache would only move the same failure into the task itself.

After merging a setup-script change:

1. Ensure the environment uses the latest `develop` branch.
2. Select **Reset cache** on the `sniffy` environment page.
3. Start the environment validation task again.
4. Confirm that setup prints `Configured Maven proxy HOST:PORT from the Cloud proxy environment.` before Maven dependency resolution.
5. If Maven still fails, use the stack trace from the final attempt. Do not increase retry counts until the reported exception has been understood.

For local script validation without exposing real credentials, use a temporary `HOME` and a synthetic proxy URL:

```bash
HOME="$(mktemp -d)" \
HTTPS_PROXY='http://user%40example:password@proxy.example:3128' \
NO_PROXY='localhost,127.0.0.1,.example.org' \
bash .codex/cloud/configure-maven-proxy.sh
```

Inspect the generated `~/.m2/settings.xml` only in the temporary directory. Never print a real Cloud proxy URL because it may contain credentials.

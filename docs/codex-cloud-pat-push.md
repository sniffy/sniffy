# Experimental Codex Cloud push access with a fine-grained PAT

## Status

Experimental and opt-in. Merging this document and the bootstrap script does not enable GitHub write access by itself. Push access is enabled only when the Codex Cloud environment contains a secret named `SNIFFY_GITHUB_PAT`.

The goal is to test whether Codex Cloud can commit and push implementation work directly to an existing non-default pull-request branch, avoiding the manual **Open pull request / Update pull request** UI step.

## Why this is an experiment

Codex Cloud normally keeps secrets available only during the setup script and removes them before the agent phase. Persisting a Git credential from setup for later agent use intentionally crosses that security boundary. The agent, repository code, build tools, and any process running as the same container user can potentially read or use the credential.

Use this only with a short-lived fine-grained PAT whose blast radius is limited to `sniffy/sniffy`. Do not use a classic PAT, a non-expiring token, a token covering multiple repositories, or a token with organization/administration/secrets/workflows permissions.

Official references:

- Codex Cloud environments and secret lifetime: <https://learn.chatgpt.com/docs/environments/cloud-environment>
- GitHub fine-grained PAT creation and limitations: <https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens>
- GitHub branch protection: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- GitHub rulesets: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets>

## Benefits

- Cloud tasks can remain unattended through commit and push.
- Existing draft pull requests can receive implementation commits without a manual publication click.
- The token can be restricted to one repository and a short expiration.
- Omitting Pull requests write means the credential cannot merge, close, retarget, or otherwise manage pull requests through the GitHub API.
- Omitting Workflows write prevents the credential from updating files under `.github/workflows` through GitHub's protected workflow-file authorization path.
- Omitting Administration, Actions, Secrets, Webhooks, Deployments, Packages, and organization permissions prevents access to those management surfaces.

## Risks and limitations

- `Contents: write` is still a broad repository-content permission. It can create commits and refs, update ordinary branches, and delete files in commits.
- A fine-grained PAT cannot be restricted to only `codex-cloud/*` branches or only fast-forward pushes.
- Because the PAT belongs to Dmitry's GitHub identity, it inherits that identity's repository access, subject to token permissions and branch/ruleset enforcement.
- A local `pre-push` hook is defense in depth only. An agent or process can remove it or invoke `git push --no-verify`.
- GitHub-side branch protection or rulesets are therefore mandatory for protecting `develop` and other important branches.
- The persisted credential is readable by processes running as the same container user. Malicious repository instructions, compromised build plugins, or dependencies with execution hooks could exfiltrate it when agent internet access is enabled.
- Codex caches environment state. Revoke the token and reset the environment cache after the experiment.
- The PAT changes Git publication only. It does not change Codex Cloud task history: each task launched from a GitHub mention still creates a `GitHub Mention` entry in the Codex UI.

## Identity and provenance

Git identity, GitHub push identity, and the agent identity are different concepts.

The setup script configures commits as:

```text
Sniffy Codex Cloud <codex-cloud@sniffy.invalid>
```

The `.invalid` address is intentionally not linked to a GitHub account. This prevents GitHub from presenting the commit as authored by Dmitry or by an unrelated registered account.

A `commit-msg` hook also appends:

```text
Agent-Executor: Codex Cloud
Execution-Environment: OpenAI Codex Cloud
Publication-Credential: bedrin fine-grained PAT
```

This makes `git log --format=full` and the GitHub commit page self-describing.

However, GitHub's ref-update actor will still be `bedrin`, because the PAT belongs to Dmitry. Changing `git config user.name` or `user.email` cannot change the authenticated push actor. A dedicated GitHub App or machine account is required for a genuinely separate GitHub actor.

Use branch prefixes to distinguish execution routes:

- `codex-cloud/*` for Codex Cloud;
- `codex-cli/*` for unattended local Codex CLI;
- `chatgpt/*` for repository-management changes performed by ChatGPT through Dmitry's connector;
- `human/*` only when a dedicated prefix is useful for manual work.

For the initial experiment, existing `codex/*` branches may remain unchanged, but all new Cloud implementation branches should use `codex-cloud/*`.

## Codex task-list behavior

A successful direct push does not remove or transform the originating Cloud task. `GitHub Mention` cards are task-history records, not unpublished-change queues. Therefore:

- one `@codex ...` GitHub mention should be treated as one durable Cloud task entry;
- avoid creating a second mention merely to publish the same result;
- prefer one implementation task per issue/PR branch;
- use follow-up messages in the existing task when practical instead of starting duplicate GitHub-mentioned tasks;
- do not interpret an old task card as an additional open pull request or unpushed branch.

The PAT experiment is intended to remove the publication click, not to clean up the Codex task list. No automatic task-history cleanup is assumed.

## Required GitHub protections before creating the token

Create or verify an active branch ruleset for the default branch `develop` with all of the following:

1. Target `develop` explicitly or use GitHub's default-branch target.
2. Require changes through a pull request.
3. Block force pushes.
4. Block branch deletion.
5. Do not add Dmitry, repository administrators, or the PAT owner to the bypass list.
6. Require status checks and reviews as appropriate for normal Sniffy delivery.

GitHub branch protection blocks force pushes and deletion by default, but administrators may otherwise bypass protections depending on configuration. The ruleset must have no applicable bypass actor for the PAT owner.

Recommended additional ruleset for agent branches:

- target `codex-cloud/**/*`, `codex-cli/**/*`, and the transitional `codex/**/*` pattern;
- block force pushes;
- block branch deletion during the experiment;
- leave ordinary fast-forward pushes allowed.

This additional rule is not required to protect `develop`, but it limits accidental history destruction on active agent PR branches.

## Fine-grained PAT configuration

Create a **fine-grained personal access token**, not a classic token.

- Token name: `sniffy-codex-cloud-push`
- Resource owner: the owner of `sniffy/sniffy`
- Expiration: 7 days or less for the initial experiment
- Repository access: **Only select repositories**
- Selected repository: `sniffy/sniffy` only

Repository permissions:

| Permission | Access | Reason |
| --- | --- | --- |
| Contents | Read and write | Required for Git commit/ref push over HTTPS |
| Metadata | Read-only | Added automatically by GitHub |
| Pull requests | No access | The agent only needs to push an existing PR head branch |
| Workflows | No access | Prevent workflow-file modification authorization |
| Administration | No access | Prevent repository settings/rules changes |
| Actions | No access | Prevent workflow dispatch/cancel/rerun operations |
| Secrets | No access | Prevent reading metadata or changing repository secrets |
| Webhooks | No access | Prevent webhook management |
| All organization/account permissions | No access | Not needed |

Do not grant `Pull requests: write` for the first experiment. ChatGPT's GitHub connector can continue creating PRs and comments separately; the Cloud container credential should only provide Git transport access.

## Codex Cloud environment configuration

1. In the `sniffy` Codex Cloud environment, add a **secret** named `SNIFFY_GITHUB_PAT` containing the fine-grained PAT.
2. Keep the existing setup command:

   ```bash
   bash .codex/cloud/setup.sh
   ```

3. Enable agent-phase internet access only to `github.com` if limited-domain access is available. Do not enable unrestricted internet solely for pushing.
4. Reset the environment cache after adding or rotating the secret.

`.codex/cloud/setup.sh` calls `.codex/cloud/configure-github-push.sh`. When the secret is absent, the script exits without changing Git authentication or identity. When present, it:

- stores the token in a mode-`0600` Git credential file;
- keeps the `origin` URL free of credentials;
- sets the non-human Codex Cloud author/committer identity;
- appends provenance trailers to commits;
- installs a global `pre-push` hook that rejects direct pushes to `develop`, `main`, and `master`, branch deletion, and non-fast-forward updates;
- verifies authenticated repository access with `git ls-remote`;
- unsets the setup-phase environment variable.

The hooks do not replace GitHub-side rules because they can be bypassed locally.

## Initial validation

Use a disposable `codex-cloud/*` branch and draft PR. Do not test against `develop` directly.

Suggested prompt:

```text
Validate guarded GitHub push access only.

Read AGENTS.md and docs/codex-cloud-pat-push.md. Confirm that:

- origin is the credential-free HTTPS URL;
- the current branch is the existing pull-request head branch;
- `git config user.name` is `Sniffy Codex Cloud`;
- `git config user.email` is `codex-cloud@sniffy.invalid`.

Create `.codex/push-test.txt` with the current UTC timestamp, commit it normally, inspect the commit author and provenance trailers, and push with a normal fast-forward command:

  git push origin HEAD:<existing-pr-head-branch>

Do not use --force, --force-with-lease, --no-verify, ref deletion syntax, or a default-branch destination. Do not create or merge another pull request. Report complete stderr on failure. The test succeeds only when the commit is visible on GitHub with the Codex Cloud commit identity and expected trailers.
```

After the commit appears, run a second task that deletes `.codex/push-test.txt`, commits the deletion, and performs another normal fast-forward push. This validates ordinary branch updates without exercising destructive ref operations.

## Validation of protections

Before delegating real implementation, verify from a trusted local shell that the server rejects dangerous operations. Use a disposable local clone and do not rewrite any real work:

1. Confirm a direct push to `develop` is rejected.
2. Confirm a force push to the protected disposable agent branch is rejected if the recommended agent-branch ruleset is enabled.
3. Confirm deletion of `develop` is rejected.
4. Confirm the PAT cannot update `.github/workflows/*` because Workflows permission is absent.
5. Confirm the PAT cannot create, merge, close, or retarget pull requests because Pull requests permission is absent.

Do not attempt destructive tests against branches containing unique work. Create a dedicated disposable branch for ruleset validation.

## Rollback

Immediately revoke the PAT if:

- it appears in logs, task output, a remote URL, process output, or committed files;
- Codex pushes to an unexpected branch;
- a ruleset does not block a tested dangerous operation;
- the task executes untrusted build or repository instructions;
- unrestricted agent internet is required.

After the experiment:

1. Revoke the PAT in GitHub.
2. Remove `SNIFFY_GITHUB_PAT` from the Codex environment.
3. Reset the Codex environment cache.
4. Confirm the credential no longer authenticates.
5. Keep or revert the optional bootstrap depending on the experiment result.

## Decision after the experiment

Adopt this mechanism only if all of the following are true:

- direct fast-forward push to an existing PR branch succeeds;
- default-branch push, force push, and deletion protections are enforced by GitHub;
- no token material appears in task output;
- GitHub shows the expected Codex Cloud commit identity and provenance trailers;
- Codex reliably reports the actual remote commit SHA;
- the operational benefit outweighs storing a user credential in the Cloud container.

For long-lived or multi-repository automation, replace the PAT with a dedicated GitHub App issuing short-lived installation tokens. A PAT is acceptable only as a bounded experiment or small personal workflow.

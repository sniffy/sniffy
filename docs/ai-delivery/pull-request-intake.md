# Pull-request-first intake

Not every delivery item starts as an issue. Dependabot, an external contributor, or other automation may open a pull request that
already contains an implementation. Add that pull request itself to the GitHub Project; do not create a shadow issue unless
follow-up work needs a new independently owned implementation.

## Lifecycle entry

A newly discovered external pull request normally enters as:

```text
Status: Review
Execution: Ready
Implementer: empty
Verifier: <chosen during intake>
Executor: ChatGPT
Assignee: bedrin-gpt
Worker reference: <PR URL and exact head>
```

The PR is the authoritative source for its author and implementation provenance. Intake must not copy the author login, bot name,
or a newly observed provider into `Implementer`. Empty means that Sniffy has not deliberately selected an executor for a future
Implementation turn; Review and later lifecycle statuses do not require one.

This skips Draft, Planning, and Implementation only when the proposed scope is understandable and no product, compatibility,
security, or policy decision is missing. Ambiguous, breaking, or policy-sensitive changes move to `Planning / Ready` before
review continues.

The exact head, body, commits, CI, labels, and linked alerts replace the branch/PR publication proof normally produced by a
Sniffy-routed Implementer.

## GitHub Project intake for Sniffy

Use GitHub Projects' built-in **Auto-add to project** workflow rather than a new repository workflow:

1. Open organization Project 2 and choose **Workflows -> Auto-add to project**.
2. Select repository `sniffy/sniffy`.
3. Use filter `is:pr is:open label:dependencies`.
4. Save and enable the workflow.

GitHub's auto-add workflow supports `is` and `label` filters but not an author filter. The `dependencies` label is therefore the
discovery signal; intake must still verify the actual PR author for trust, review independence, Dependabot commands, and policy
classification. Authorship is not a Project routing decision and must not be mirrored automatically into `Implementer`.

Project 2 currently contains a `Dependabot` option in the `Implementer` single-select. It is retained for manual or historical
classification, but automated external-PR intake deliberately leaves `Implementer` empty. The same rule applies to human external
contributors, bots, and future automation providers that are not part of Sniffy's configured implementation executor pool.

Auto-add is not retroactive. Existing matching PRs must be added manually or backfilled by an idempotent event-loop scan. The
backfill key is repository plus PR number, and an existing Project item or intake marker prevents duplication.

The current `.github/dependabot.yml` schedules weekly GitHub Actions and npm updates and applies `dependencies` to both. Open
Dependabot PRs may include browser tooling, GitHub Actions, framework major versions, cryptography, and other update classes, so
the queue must not apply one blanket approval policy.

Enabling the Project workflow is a manual repository-management action outside documentation/code changes. Until it is enabled,
the stateless event loop provides discovery and backfill; after it is enabled, the loop remains responsible for field
initialization, author verification, missed-item reconciliation, and review.

## Event-loop intake adapter

Before selecting ordinary `Execution = Ready` work, a tick may scan for eligible open pull requests that are not yet represented
in the Project. Intake should:

1. verify repository, open state, bot/external author, labels, target branch, draft state, and exact head;
2. add or locate the PR Project item idempotently;
3. classify security versus routine update and assign priority;
4. choose a Verifier from the actual compatibility and environment risk;
5. initialize `Review / Ready / Executor = ChatGPT / Assignee = bedrin-gpt` without setting `Implementer`;
6. preserve an existing `Implementer` value if one was deliberately set, but never require it for intake or Review;
7. record the PR URL, exact head, dependency ecosystem, old/new version, author, and linked security alert when available.

A guarded command should omit `Implementer` from both `set` and `clear`; omitted fields remain unchanged. Do not use a synthetic
`Unknown` option and do not infer a select value from the author.

Security updates receive higher priority, but trusted automation is not approval. A tick claims and reviews at most the amount
of work it can finish or safely hand off.

## Dependabot review paths

GitHub documents Dependabot pull requests, management commands, and Actions integration here:

- [Dependabot pull requests](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-pull-requests)
- [Managing pull requests for dependency updates](https://docs.github.com/en/code-security/dependabot/working-with-dependabot/managing-pull-requests-for-dependency-updates)
- [Dependabot pull request comment commands](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-pull-request-comment-commands)
- [Automating Dependabot with GitHub Actions](https://docs.github.com/en/code-security/tutorials/secure-your-dependencies/automate-dependabot-with-actions)
- [GitHub Projects auto-add](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/adding-items-automatically)

Route the reviewed exact head as follows:

- **Acceptable low-risk update:** inspect dependency/lockfile scope, release or security information, compatibility, tests,
  generated output, and exact-head CI. Submit an honest formal `APPROVE`, then advance to Verification when representative
  outcome proof is still needed or directly to `Approval / Ready / Human` when existing evidence is sufficient.
- **Risky runtime, framework, build-tool, browser, action, or security update:** choose a capable Verifier and exercise the
  affected integration rather than treating green generic CI as complete proof.
- **Stale base or merge conflict:** use `@dependabot rebase` when appropriate, keep the item in Review, and let ChatGPT own the
  wait through `Execution = In progress`, `Executor = ChatGPT`, and a concrete Worker reference. Dependabot authorship does not
  need to be represented in `Implementer` for this monitoring flow. Restart exact-head review only after a new head appears.
- **Implementation incompatibility:** set the source PR to `Review / Blocked / Human / bedrin` and ask Dmitry to authorize a
  linked compatibility issue or replacement update. The linked task goes through Planning, deliberately chooses an Implementer,
  and produces an agent-owned replacement PR. Keep the source PR in Review with a Worker reference to that task until the
  replacement supersedes it; then close the source PR with rationale.
- **Requirement, major-version, or policy ambiguity:** move the PR item to `Planning / Ready` for ChatGPT or Dmitry. If the outcome
  requires new code, Planning must choose a concrete Implementer before entering Implementation.
- **Intentional rejection or ignore:** record the reason. Prefer visible repository configuration in `.github/dependabot.yml`
  over a centrally stored one-off ignore when the policy should be shared by maintainers.

Do not normally push repository-specific fixes onto a Dependabot branch. A replacement agent-owned PR keeps branch ownership,
review identity, and future Dependabot rebase behavior easier to reason about.

## Verification examples by update class

- **GitHub Action update:** inspect action ownership/release notes, changed permissions and inputs, and the exact workflow jobs
  executing the new action version.
- **Browser/test tooling:** run the complete affected browser matrix and inspect artifacts, screenshots, traces, and generated
  output rather than relying only on typecheck/unit tests.
- **Runtime framework/library:** run the affected integration or representative application path, including startup, request,
  shutdown, compatibility, and failure behavior.
- **Cryptography/security provider:** require the real provider/runtime path and supported-JDK compatibility evidence; a compile
  or unrelated test suite is insufficient.
- **Security update:** confirm the linked advisory is addressed without introducing a broader unsupported upgrade or silently
  retaining the vulnerable path.

## Volume control

If routine dependency PR volume becomes costly, refine `.github/dependabot.yml` in a separate reviewed change. Prefer grouping
compatible minor/patch development-tool updates while keeping major, runtime, cryptography, security, and GitHub Action updates
separately reviewable. Do not add blind auto-approval or auto-merge as a volume-control mechanism.

## Review identity and merge boundary

A Dependabot-authored PR is independent from `bedrin-gpt`, so ChatGPT may submit an honest formal `APPROVE` or
`REQUEST_CHANGES` after complete review. Verification remains outcome- and compatibility-oriented; green CI alone is not
sufficient for a risky update.

GitHub supports auto-merge after required reviews and checks, but this workflow does not enable it. Dependabot PRs, like every
other PR, move to `Approval / Ready` and are merged only after Dmitry's explicit instruction.

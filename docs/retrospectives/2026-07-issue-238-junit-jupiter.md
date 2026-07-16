# Retrospective: issue #238 JUnit Jupiter delivery

Date: 2026-07-16

Related work:

- issue [#238](https://github.com/sniffy/sniffy/issues/238);
- pull request [#633](https://github.com/sniffy/sniffy/pull/633);
- follow-up [#646](https://github.com/sniffy/sniffy/issues/646) for a framework-neutral reversible socket-blocking scope.

This is an intermediate process retrospective. PR #633 was still in its review/fix cycle when this document was
written. The goal is to improve future autonomous delivery, not to assign blame for an unfinished implementation.

## What happened

The work passed through three distinct phases:

1. **Delivery infrastructure failures.** Early Codex Cloud tasks produced local commits and completion summaries but no
   resolvable GitHub branch or pull request. Missing GitHub CLI setup, credential persistence, token approval, agent
   network write methods, and PR publication made implementation results effectively ephemeral. PRs #638 and #639 and
   the reversible remote-ref probe addressed this class of failure.
2. **Architecture and product-scope churn.** Once a real PR existed, the issue simultaneously required
   `ConnectionsRegistry` restoration, JUnit 4 compatibility, resource safety, and no broad public-API redesign. The
   phrase "restore ConnectionsRegistry" did not define whether it meant clearing the JUnit-owned wildcard or
   reconstructing unrelated application state. Codex chose a generic snapshot/replay implementation. Review then found
   persistence corruption, incomplete best-effort restoration, and resurrection of application-closed connections.
   The product decision was eventually corrected: whole-registry snapshotting is outside the Jupiter feature and is
   tracked separately in #646.
3. **Incomplete proof of an otherwise converging implementation.** Later heads compiled and passed CI, but review found
   missing combinations, unused launcher fixtures, and a JUnit 6 consumer placed in a servlet-specific profile. Green CI
   proved the tests that ran; it did not prove that every acceptance criterion had an executing test.

These phases should not be counted as one undifferentiated "Codex needed many tries." They have different remedies.

## What went well

- The GitHub-visible branch and PR eventually became the only accepted delivery evidence.
- The issue was expanded into an authoritative specification with compatibility, lifecycle, failure, documentation, and
  publication requirements.
- Review correctly prioritized application resource safety over instrumentation convenience.
- Java 8 production compatibility and real JUnit 6 consumption of the same JUnit-5-built artifact became explicit and
  observable in CI.
- When the snapshot design was recognized as a separate product concern, it was removed rather than polished further
  inside the wrong feature.
- Monitoring checked remote SHA, PR state, complete diff, and CI logs rather than trusting bot summaries.

## Why the review/fix loop was long

### 1. The task was broad in risk axes, not merely large in lines of code

The feature combined a new published artifact, Jupiter lifecycle ordering, global connectivity state, partial setup and
cleanup failures, primary/suppressed exceptions, legacy annotations, Java 8 bytecode, JUnit 5 and JUnit 6, reactor
placement, documentation, and GitHub delivery. A normal prose issue can mention all of these without showing their
interactions.

### 2. The initial specification contained a material ambiguity

"Restore ConnectionsRegistry" was interpreted as preserving the entire registry. That interpretation conflicted with
the smaller JUnit 4 behavior and with the non-goal of a broad core redesign. The delivery lead should have resolved this
before coding. A longer prompt repeating both statements would not have helped; the contradiction needed a product
decision.

### 3. Architecture ownership was decided too late

The production Jupiter artifact belongs in `sniffy-test`, while the JUnit 6 consumer is test-only infrastructure and
belongs in `sniffy-integration-tests`. The original task did not make that distinction explicit, so the first placement
was reasonable but wrong for the desired reactor structure.

### 4. Acceptance criteria were not converted into a proof matrix

The issue had a long test list, but there was no table mapping each criterion and failure combination to a named test and
CI job. This allowed dead fixtures and untested combinations to coexist with a green build.

### 5. Review was adversarial but incremental

Each review found a real problem, but several independent coverage gaps could have been reported together if the first
review had audited a single acceptance-to-proof matrix. Serial comments increased latency and gave each cold-start
follow-up a growing, partially superseded thread to interpret.

### 6. GitHub comments became a second specification

Some older comments required increasingly sophisticated snapshot restoration; the later authoritative issue excluded
that design. Even when the issue says it is authoritative, a new Cloud task reads the thread and must reconcile
conflicting history. Scope changes should update the issue first and explicitly mark older guidance as superseded.

### 7. PR ownership weakened formal review state

The human-owned PR prevented the same authenticated human account from submitting a formal Request Changes review.
Blocking comments worked by convention, but future agent work should normally create the PR using the dedicated agent
identity so human review state is represented by GitHub.

## Would micro-subtasks have helped?

Not as the default. A PR for every annotation, callback, or test would add merge and integration overhead without
removing the hardest interactions.

The better shape is:

- a short **read-only design/proof preflight** before coding;
- one cohesive draft PR;
- staged checkpoints inside that PR for module skeleton, lifecycle behavior, failure matrix, cross-version consumer,
  and documentation;
- separate issues/PRs only for independently mergeable architecture, such as the framework-neutral socket-blocking
  scope now tracked in #646.

JUnit 6 compatibility could have been a separate PR, but keeping it in the same PR is reasonable once the production
artifact boundary and same-artifact proof are explicit.

## Cloud Codex versus local Codex

Cloud Codex was not intrinsically unable to implement the feature. After credentials and agent-network policy were
fixed, it repeatedly pushed working commits and ran useful focused tests.

Cloud amplified two problems:

- each GitHub `@codex` follow-up was effectively a cold-start task whose durable context was the issue, PR, and comments;
- long full-reactor runs exceeded the task window, making persistent local artifacts less convenient.

Unattended local Codex in an isolated VM would likely reduce cold-start and long-build friction, but it would still have
implemented an ambiguous registry-restoration requirement. The primary fix is better refinement; routing is secondary.

For future work, re-route to persistent local Codex after two substantive review/fix rounds or immediately after a major
architecture reversal.

## Process changes adopted

1. Add a mandatory complex-task preflight for high-risk combinations.
2. Require an acceptance-to-proof matrix with named tests/jobs and verify actual test execution.
3. Resolve public/core API and global-state decisions before implementation.
4. Distinguish production modules from test-only compatibility consumers during refinement.
5. Keep cohesive work in one PR with internal checkpoints; avoid arbitrary micro-PRs.
6. Re-baseline after two substantive fix rounds instead of stacking another narrow prompt.
7. Update the authoritative issue before dispatch when scope changes, and mark conflicting older guidance as superseded.
8. Prefer agent-authored PRs so human formal review remains available.
9. Require one comprehensive first review against the same proof matrix.
10. Treat remote branch/PR SHA and actual GitHub state—not local commits, metadata, or summaries—as delivery evidence.

The durable rules from this retrospective live in `AGENTS.md`, `docs/codex-workflow.md`, and the autonomous task issue
template. This document preserves why those rules exist.

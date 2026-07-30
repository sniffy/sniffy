'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

test('ChatGPT dispatcher scans and canonicalizes every develop pull request', () => {
  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  assert.match(prompt, /Scan every open pull request in sniffy\/sniffy targeting develop/i);
  assert.match(prompt, /Exactly one formal closing issue: that issue is the canonical lifecycle item/i);
  assert.match(prompt, /No formal closing issue: the non-draft PR itself is the canonical lifecycle item/i);
  assert.match(prompt, /Multiple formal closing issues: the non-draft PR is the canonical coordination item/i);
  assert.match(prompt, /Draft PR: do not start Review/i);
  assert.match(prompt, /regardless of author, label, bot\/provider, branch creator/i);
  assert.doesNotMatch(prompt, /Find eligible open sniffy\/sniffy dependency PRs missing/i);
});

test('profile makes universal intake and existing-PR routing explicit', () => {
  const profile = read('docs/ai-delivery/profile.yml');
  assert.match(profile, /pullRequests:\s*\n\s+enabled:\s*true/);
  assert.match(profile, /baseBranch:\s*develop/);
  assert.match(profile, /includeDraftsInReview:\s*false/);
  assert.match(profile, /setImplementerFromAuthor:\s*false/);
  assert.match(profile, /exactlyOneClosingIssue:\s*issue/);
  assert.match(profile, /noClosingIssue:\s*pullRequest/);
  assert.match(profile, /multipleClosingIssues:\s*pullRequestPlanning/);
  assert.match(profile, /existingPullRequestContinuationExecutor:\s*Local Codex/);
});

test('Local Codex can adopt an explicitly routed same-repository PR without duplication', () => {
  const worker = read('.codex/local/worker-task-prompt.md');
  const dispatcher = read('.codex/local/scheduled-task-prompt.md');
  assert.match(worker, /<WORK_ITEM_TYPE>/);
  assert.match(worker, /adopted-continuation/);
  assert.match(worker, /reuse the exact same-repository open PR branch/i);
  assert.match(worker, /never adopt a fork or Dependabot branch/i);
  assert.match(worker, /Do\s+not create a fresh branch or duplicate PR/i);
  assert.match(dispatcher, /existing same-repository PR may be an adopted continuation/i);
  assert.match(dispatcher, /Reuse the exact branch and PR/i);
  assert.match(dispatcher, /Do not adopt fork or Dependabot branches/i);
});

test('canonical intake policy treats issues and PRs as distinct linked artifacts', () => {
  const intake = read('docs/ai-delivery/pull-request-intake.md');
  const lifecycle = read('docs/ai-delivery/lifecycle.md');
  assert.match(intake, /Every open PR targeting\s+`develop` is therefore a discovery source/);
  assert.match(intake, /exactly one Project item is canonical for lifecycle routing/i);
  assert.match(intake, /## Canonical work-item selection/);
  assert.match(intake, /### Exactly one formal closing issue[\s\S]*The issue is the canonical lifecycle item/i);
  assert.match(intake, /### No formal closing issue[\s\S]*The PR itself is the canonical lifecycle item/i);
  assert.match(intake, /### Multiple formal closing issues[\s\S]*The PR is the canonical coordination item/i);
  assert.match(lifecycle, /The canonical item owns Status, Execution, Executor, Assignee, and Worker reference/);
});

test('Cloud does not turn arbitrary existing PRs into duplicate fresh work', () => {
  const cloud = read('docs/ai-delivery/executors/codex-cloud.md');
  assert.match(cloud, /Do not route an arbitrary existing PR/i);
  assert.match(cloud, /Cloud must not open a duplicate branch\/PR/i);
  assert.match(cloud, /existing-PR continuation normally goes to Local Codex/i);
});

test('every implementation executor publishes a non-draft exact-head PR before Review', () => {
  const files = [
    'AGENTS.md',
    '.chatgpt/scheduled-task-prompt.md',
    '.codex/local/worker-task-prompt.md',
    'docs/ai-delivery/executors/chatgpt.md',
    'docs/ai-delivery/executors/codex-cloud.md',
    'docs/ai-delivery/executors/codex-local.md',
    'docs/ai-delivery/executors/ide-agent.md'
  ];

  for (const file of files) {
    const content = read(file);
    assert.match(content, /ready for review/i, `${file} must require ready-for-review publication`);
    assert.match(content, /non-draft|draft\s*=\s*false|draft=false/i, `${file} must require non-draft state`);
    assert.match(content, /exact (published |remote )?head|exact-head/i, `${file} must verify the exact PR head`);
    assert.match(content, /Status\s*=\s*Review|Status=Review|Review \/ Ready/i, `${file} must connect PR state to Review handoff`);
  }

  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  const worker = read('.codex/local/worker-task-prompt.md');
  const control = read('docs/ai-delivery/control-plane.md');
  const workflow = read('.github/workflows/delivery-control.yml');

  assert.match(prompt, /reviewPullRequest\.number.*reviewPullRequest\.head/is);
  assert.match(worker, /reviewPullRequest\.number.*reviewPullRequest\.head/is);
  assert.match(control, /reviewPullRequest/);
  assert.match(control, /marks? (?:a )?draft PR ready/i);
  assert.match(control, /before writing Project Review/i);
  assert.match(workflow, /pull-requests:\s*write/);
  assert.match(workflow, /REPOSITORY_TOKEN:\s*\$\{\{ github\.token \}\}/);
});
'use strict';

const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
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
  assert.match(profile, /Local Codex:[\s\S]*maxConcurrentWorkers:\s*1/);
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

test('delivery executors require readable control comments with authoritative marked JSON', () => {
  const commandPrompts = [
    '.chatgpt/scheduled-task-prompt.md',
    '.codex/local/scheduled-task-prompt.md',
    '.codex/local/worker-task-prompt.md'
  ];
  const executorDocs = [
    'docs/ai-delivery/executors/chatgpt.md',
    'docs/ai-delivery/executors/codex-cloud.md',
    'docs/ai-delivery/executors/codex-local.md'
  ];

  for (const file of [...commandPrompts, ...executorDocs]) {
    const content = read(file);
    assert.match(content, /human-readable Markdown wrapper/i, `${file} must require the readable wrapper`);
    assert.match(content, /exact (?:start\/end )?markers|exact markers/i, `${file} must preserve protocol markers`);
    assert.match(content, /lowercase\s+`json` fence/i, `${file} must preserve the lowercase JSON fence`);
    assert.match(content, /marked JSON.*authoritative|authoritative marked JSON/is, `${file} must make JSON authoritative`);
  }
  for (const file of commandPrompts) {
    assert.match(read(file), /never\s+emit bare JSON/i, `${file} must reject bare autonomous output`);
  }

  const control = read('docs/ai-delivery/control-plane.md');
  assert.equal((control.match(/<!-- delivery-control-command:start -->/g) || []).length, 1);
  assert.equal((control.match(/<!-- delivery-control-command:end -->/g) || []).length, 1);
  assert.match(control, /display-only[\s\S]*sole authoritative `delivery-control\/v1` command/i);
  assert.match(control, /Legacy comments containing only a valid JSON object remain accepted/i);
  assert.match(control, /workflow_dispatch` continues to accept the raw JSON object/i);
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
    assert.match(content, /non-draft|not\s+draft|draft\s*=\s*false|draft=false/i, `${file} must require non-draft state`);
    assert.match(content, /exact (published |remote )?head|exact-head/i, `${file} must verify the exact PR head`);
    assert.match(content, /Review/i, `${file} must connect PR state to Review handoff`);
  }

  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  const worker = read('.codex/local/worker-task-prompt.md');
  const control = read('docs/ai-delivery/control-plane.md');
  const workflow = read('.github/workflows/delivery-control.yml');

  assert.match(prompt, /reviewPullRequest\.number.*reviewPullRequest\.head/is);
  assert.match(worker, /reviewPullRequest\.number.*reviewPullRequest\.head/is);
  assert.match(control, /reviewPullRequest/);
  assert.match(control, /marks? (?:a )?draft PR ready/i);
  assert.match(control, /re-reads it before writing (?:the )?Project (?:status|Review)/i);
  assert.match(control, /re-reads the (?:review )?PR again after the Project mutation/i);
  assert.match(workflow, /pull-requests:\s*write/);
  assert.match(workflow, /REPOSITORY_TOKEN:\s*\$\{\{ github\.token \}\}/);
});

test('blocking Review always publishes feedback and completes a same-tick durable route', () => {
  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  const supervision = read('docs/ai-delivery/supervision.md');
  const chatgpt = read('docs/ai-delivery/executors/chatgpt.md');
  const reviewReturn = read('docs/ai-delivery/review-return.md');

  assert.match(prompt, /reviewer is independent[\s\S]*comprehensive REQUEST_CHANGES/i);
  assert.match(prompt, /reviewer is the PR author[\s\S]*ordinary\s+PR\s+comment[\s\S]*identity limitation/i);
  assert.match(prompt, /In either case, route the canonical item durably in this same tick/i);
  assert.match(prompt, /never leave the reviewed exact head in Review \/ Ready \/ ChatGPT/i);
  assert.match(prompt, /do not wait for the formal-review return adapter/i);
  assert.match(prompt, /technically acceptable[\s\S]*Verification or Approval \/ Ready \/ Human/i);

  for (const [file, content] of [
    ['docs/ai-delivery/supervision.md', supervision],
    ['docs/ai-delivery/executors/chatgpt.md', chatgpt]
  ]) {
    assert.match(content, /technical Review outcome|technical outcome/i, `${file} must separate technical outcome from identity`);
    assert.match(content, /same tick/i, `${file} must require immediate durable routing`);
    assert.match(content, /ordinary\s+PR\s+comment/i, `${file} must define the self-review blocker path`);
    assert.match(content, /Implementation \/ Ready/i, `${file} must define implementation-defect routing`);
    assert.match(content, /Planning \/ Ready \/ ChatGPT/i, `${file} must define planning-defect routing`);
    assert.match(content, /Blocked \/ Human/i, `${file} must preserve human-blocker semantics`);
  }

  assert.match(reviewReturn, /recovery path for independently submitted formal reviews/i);
  assert.match(reviewReturn, /not the primary or only correction\s+route/i);
  assert.match(reviewReturn, /ordinary blocker comment[\s\S]*no\s+`pull_request_review` event/i);
});

test('Local Codex uses one bounded normalized Project snapshot per tick', () => {
  const dispatcher = read('.codex/local/scheduled-task-prompt.md');
  const helper = read('.codex/local/project-queue-snapshot.sh');

  assert.match(dispatcher, /project-queue-snapshot\.sh` exactly once/i);
  assert.match(dispatcher, /only normal full-Project query/i);
  assert.match(dispatcher, /Do not run `gh project item-list`/i);
  assert.match(dispatcher, /Do not switch to another\s+connector, combine stale snapshots/i);
  assert.match(dispatcher, /Compute available capacity from `executors\.Local Codex\.maxConcurrentWorkers`[\s\S]*`ownedInProgress`/i);
  assert.match(dispatcher, /Do not call `List projects`[\s\S]*provider-wide inventory on\s+the normal `Ready` claim path/i);
  assert.match(dispatcher, /inventory is allowed only to recover one selected `In progress` item[\s\S]*claim token and generation/i);
  assert.match(dispatcher, /Do not read the complete discussion, proof matrix, review submissions,[\s\S]*the lifecycle worker owns those reads/i);
  assert.equal((helper.match(/^\s*if ! gh project item-list\b/gm) || []).length, 1);
  assert.match(helper, /exit 75/);

  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'sniffy-project-snapshot-'));
  const fixture = path.join(directory, 'project.json');
  fs.writeFileSync(fixture, JSON.stringify({
    totalCount: 3,
    items: [
      {
        id: 'READY',
        title: 'Ready implementation',
        content: {type: 'Issue', number: 762, url: 'https://github.com/sniffy/sniffy/issues/762', repository: 'sniffy/sniffy'},
        status: 'Implementation',
        execution: 'Ready',
        executor: 'Local Codex',
        implementer: 'Local Codex',
        assignees: [],
        priority: 'High'
      },
      {
        id: 'OWNED',
        title: 'Owned verification',
        content: {type: 'PullRequest', number: 700, url: 'https://github.com/sniffy/sniffy/pull/700', repository: {nameWithOwner: 'sniffy/sniffy'}},
        status: 'Verification',
        execution: 'In progress',
        executor: 'Local Codex',
        assignees: [{login: 'bedrin-codex-local'}],
        'worker reference': 'worker-700'
      },
      {
        id: 'OTHER',
        title: 'ChatGPT work',
        content: {type: 'Issue', number: 701, url: 'https://github.com/sniffy/sniffy/issues/701', repository: 'sniffy/sniffy'},
        status: 'Review',
        execution: 'Ready',
        executor: 'ChatGPT'
      }
    ]
  }));

  try {
    const output = execFileSync('bash', [path.join(root, '.codex/local/project-queue-snapshot.sh'), '--input', fixture], {
      encoding: 'utf8'
    });
    const snapshot = JSON.parse(output);
    assert.equal(snapshot.totalCount, 3);
    assert.equal(snapshot.repositoryItemCount, 3);
    assert.deepEqual(snapshot.ownedInProgress.map(item => item.number), [700]);
    assert.deepEqual(snapshot.readyCandidates.map(item => item.number), [762]);
    assert.deepEqual(snapshot.ownedInProgress[0].assignees, ['bedrin-codex-local']);
  } finally {
    fs.rmSync(directory, {recursive: true, force: true});
  }
});

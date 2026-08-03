'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {buildDispatch, canonicalIdentity, exactHeads} = require('./delivery-dispatch-view');

function projectItem(number, fields = {}, type = 'Issue') {
  return {
    projectItemId: `${type}-${number}`,
    content: {
      type,
      number,
      repository: 'sniffy/sniffy',
      title: `${type} ${number}`,
      url: `https://github.com/sniffy/sniffy/${type === 'Issue' ? 'issues' : 'pull'}/${number}`
    },
    fields: {
      Status: null,
      Execution: null,
      Executor: null,
      Implementer: null,
      Verifier: null,
      Priority: null,
      'Ready timestamp': null,
      'Worker reference': null,
      ...fields
    }
  };
}

function pullRequest(number, closingIssueNumbers = [], overrides = {}) {
  return {
    number,
    url: `https://github.com/sniffy/sniffy/pull/${number}`,
    author: 'contributor',
    repositoryOwnership: 'same-repository',
    isDraft: false,
    baseRefName: 'develop',
    headRefName: `agent/pr-${number}`,
    headRefOid: String(number).padStart(40, 'a').slice(-40),
    mergeable: 'MERGEABLE',
    mergeStateStatus: 'CLEAN',
    closingIssueNumbers,
    ...overrides
  };
}

function rawItem(item, assignees = []) {
  return {id: item.projectItemId, assignees: assignees.map(login => ({login}))};
}

function snapshot(items, pullRequests = []) {
  return {
    generatedAt: '2026-08-03T00:00:00Z',
    items,
    pullRequests
  };
}

test('canonicalizes PRs without asking the dispatcher model', () => {
  assert.deepEqual(canonicalIdentity(pullRequest(10, [9])), {
    type: 'Issue', number: 9, expectedInitialStatus: 'Review'
  });
  assert.deepEqual(canonicalIdentity(pullRequest(10)), {
    type: 'PullRequest', number: 10, expectedInitialStatus: 'Review'
  });
  assert.deepEqual(canonicalIdentity(pullRequest(10, [8, 9])), {
    type: 'PullRequest', number: 10, expectedInitialStatus: 'Planning'
  });
});

test('groups ready and in-progress work and detects route mismatch', () => {
  const ready = projectItem(2, {
    Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT', Priority: 'High'
  });
  const owned = projectItem(3, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Codex Cloud',
    'Worker reference': 'task cloud-3; nextObservationAt=2026-08-02T23:30:00Z'
  });
  const result = buildDispatch(
    snapshot([ready, owned]),
    {items: [rawItem(ready, ['bedrin-codex-local']), rawItem(owned, ['bedrin-codex-cloud'])]},
    [],
    {executorAssignees: {ChatGPT: 'bedrin-gpt', 'Codex Cloud': 'bedrin-codex-cloud'}}
  );

  assert.deepEqual(result.readyByExecutor.ChatGPT.map(item => item.number), [2]);
  assert.deepEqual(result.dueInProgressByExecutor['Codex Cloud'].map(item => item.number), [3]);
  assert.deepEqual(result.routeMismatches.map(item => item.number), [2]);
  assert.equal(result.orderedCandidates[0].kind, 'route-mismatch');
});

test('does not select an in-progress observation before its durable due time', () => {
  const owned = projectItem(3, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Codex Cloud',
    'Worker reference': 'nextObservationAt=2026-08-03T00:15:00Z'
  });
  const result = buildDispatch(snapshot([owned]), {items: [rawItem(owned)]}, [], {});
  assert.equal(result.inProgressByExecutor['Codex Cloud'].length, 1);
  assert.equal(result.dueInProgressByExecutor['Codex Cloud'], undefined);
  assert.equal(result.orderedCandidates.length, 0);
});

test('projects PR intake, duplicate representation, and conflicts', () => {
  const issue = projectItem(9, {Status: 'Draft'}, 'Issue');
  const duplicatePr = projectItem(10, {Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT'}, 'PullRequest');
  const pr = pullRequest(10, [9], {mergeable: 'CONFLICTING', mergeStateStatus: 'DIRTY'});
  const result = buildDispatch(
    snapshot([issue, duplicatePr], [pr]),
    {items: [rawItem(issue), rawItem(duplicatePr)]},
    [{number: 10, labels: []}],
    {}
  );

  assert.equal(result.pullRequests.conflicting.length, 1);
  assert.equal(result.pullRequests.conflicting[0].canonical.type, 'Issue');
  assert.equal(result.pullRequests.conflicting[0].duplicateProjectItems[0].number, 10);
  assert.equal(result.orderedCandidates[0].kind, 'conflicting-pr');
});

test('keeps fork and Dependabot PRs out of direct conflict continuation', () => {
  const fork = pullRequest(11, [], {repositoryOwnership: 'fork', mergeable: 'CONFLICTING'});
  const dependabot = pullRequest(12, [], {author: 'dependabot[bot]', mergeable: 'CONFLICTING'});
  const result = buildDispatch(
    snapshot([], [fork, dependabot]),
    {items: []},
    [{number: 11, labels: []}, {number: 12, labels: [{name: 'dependencies'}]}],
    {}
  );
  assert.equal(result.pullRequests.conflicting.length, 0);
  assert.deepEqual(result.pullRequests.intake.map(value => value.number), [11, 12]);
});

test('does not treat a human dependency PR as Dependabot', () => {
  const human = pullRequest(13, [], {author: 'human', mergeable: 'CONFLICTING'});
  const result = buildDispatch(
    snapshot([], [human]),
    {items: []},
    [{number: 13, labels: [{name: 'dependencies'}]}],
    {}
  );
  assert.equal(result.pullRequests.conflicting.length, 1);
  assert.equal(result.pullRequests.conflicting[0].isDependabot, false);
});

test('does not steal an explicit Local Codex correction route as PR intake', () => {
  const canonicalPr = projectItem(14, {
    Status: 'Implementation', Execution: 'Ready', Executor: 'Local Codex', Implementer: 'Local Codex'
  }, 'PullRequest');
  const pr = pullRequest(14);
  const result = buildDispatch(
    snapshot([canonicalPr], [pr]),
    {items: [rawItem(canonicalPr)]},
    [{number: 14, labels: []}],
    {}
  );
  assert.equal(result.pullRequests.intake.length, 0);
  assert.equal(result.orderedCandidates.length, 0);
});

test('detects downstream lifecycle evidence pinned to an older exact head', () => {
  const oldHead = '1'.repeat(40);
  const issue = projectItem(9, {
    Status: 'Approval', Execution: 'Ready', Executor: 'Human',
    'Worker reference': `PR #10 exact head ${oldHead}`
  }, 'Issue');
  const pr = pullRequest(10, [9], {headRefOid: '2'.repeat(40)});
  const result = buildDispatch(
    snapshot([issue], [pr]),
    {items: [rawItem(issue)]},
    [{number: 10, labels: []}],
    {}
  );
  assert.equal(result.staleExactHead.length, 1);
  assert.deepEqual(result.staleExactHead[0].recordedHeads, [oldHead]);
  assert.ok(exactHeads(issue.fields['Worker reference']).includes(oldHead));
});

test('sorts ready work by priority and ready timestamp', () => {
  const low = projectItem(1, {
    Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT', Priority: 'Low',
    'Ready timestamp': '2026-08-01T00:00:00Z'
  });
  const highLate = projectItem(3, {
    Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT', Priority: 'High',
    'Ready timestamp': '2026-08-02T00:00:00Z'
  });
  const highEarly = projectItem(2, {
    Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT', Priority: 'High',
    'Ready timestamp': '2026-08-01T00:00:00Z'
  });
  const result = buildDispatch(
    snapshot([low, highLate, highEarly]),
    {items: [rawItem(low), rawItem(highLate), rawItem(highEarly)]},
    [],
    {}
  );
  assert.deepEqual(result.readyByExecutor.ChatGPT.map(item => item.number), [2, 3, 1]);
});

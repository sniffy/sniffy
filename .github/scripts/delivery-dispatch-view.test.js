'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  buildDispatch,
  canonicalIdentity,
  exactHeads,
  leaseUntilFrom,
  pullRequestNumberFromUrl,
  routeSuggestion,
  staleOwnershipReason
} = require('./delivery-dispatch-view');

function projectItem(number, fields = {}, type = 'Issue', content = {}) {
  return {
    projectItemId: `${type}-${number}`,
    content: {
      type,
      number,
      repository: 'sniffy/sniffy',
      title: `${type} ${number}`,
      url: `https://github.com/sniffy/sniffy/${type === 'Issue' ? 'issues' : 'pull'}/${number}`,
      state: 'OPEN',
      stateReason: '',
      isDraft: false,
      mergedAt: null,
      closedAt: null,
      labels: [],
      ...content
    },
    fields: {
      Status: null,
      Execution: null,
      Executor: null,
      Implementer: null,
      Verifier: null,
      Priority: null,
      'Ready timestamp': null,
      'Linked pull requests': null,
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

function rawPullRequest(number, overrides = {}) {
  return {
    number,
    url: `https://github.com/sniffy/sniffy/pull/${number}`,
    baseRefName: 'develop',
    headRefName: `agent/pr-${number}`,
    headRefOid: String(number).padStart(40, 'b').slice(-40),
    mergedAt: null,
    labels: [],
    ...overrides
  };
}

function rawItem(item, assignees = []) {
  return {id: item.projectItemId, assignees: assignees.map(login => ({login}))};
}

function snapshot(items, pullRequests = [], generatedAt = '2026-08-03T00:00:00Z') {
  return {generatedAt, items, pullRequests};
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

test('groups active and stale in-progress work and detects route mismatch', () => {
  const ready = projectItem(2, {
    Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT', Priority: 'High'
  });
  const stale = projectItem(3, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Codex Cloud',
    'Worker reference': 'task=cloud-3; claimToken=g3; leaseUntil=2026-08-02T23:30:00Z'
  });
  const active = projectItem(4, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Codex Cloud',
    'Worker reference': 'task=cloud-4; claimToken=g4; leaseUntil=2026-08-03T01:00:00Z'
  });
  const result = buildDispatch(
    snapshot([ready, stale, active]),
    {items: [
      rawItem(ready, ['bedrin-codex-local']),
      rawItem(stale, ['bedrin-codex-cloud']),
      rawItem(active, ['bedrin-codex-cloud'])
    ]},
    [],
    {executorAssignees: {ChatGPT: 'bedrin-gpt', 'Codex Cloud': 'bedrin-codex-cloud'}}
  );

  assert.deepEqual(result.readyByExecutor.ChatGPT.map(item => item.number), [2]);
  assert.deepEqual(result.activeInProgressByExecutor['Codex Cloud'].map(item => item.number), [4]);
  assert.deepEqual(result.staleInProgressByExecutor['Codex Cloud'].map(item => item.number), [3]);
  assert.equal(result.staleInProgressByExecutor['Codex Cloud'][0].staleReason, 'lease-expired');
  assert.deepEqual(result.routeMismatches.map(item => item.number), [2]);
  assert.equal(result.orderedCandidates[0].kind, 'route-mismatch');
  assert.equal(result.orderedCandidates[1].kind, 'stale-owned-recovery');
});

test('treats multiple assignees as a route mismatch even when one is correct', () => {
  const ready = projectItem(2, {Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT'});
  const result = buildDispatch(
    snapshot([ready]),
    {items: [rawItem(ready, ['bedrin-gpt', 'bedrin'])]},
    [],
    {executorAssignees: {ChatGPT: 'bedrin-gpt', Human: 'bedrin'}}
  );
  assert.deepEqual(result.routeMismatches.map(item => item.number), [2]);
});

test('ignores normal in-progress ownership until its lease expires', () => {
  const owned = projectItem(3, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Codex Cloud',
    'Worker reference': 'worker=cloud-3; leaseUntil=2026-08-03T00:15:00Z'
  });
  const result = buildDispatch(snapshot([owned]), {items: [rawItem(owned)]}, [], {});
  assert.equal(result.inProgressByExecutor['Codex Cloud'].length, 1);
  assert.equal(result.activeInProgressByExecutor['Codex Cloud'].length, 1);
  assert.equal(result.staleInProgressByExecutor['Codex Cloud'], undefined);
  assert.equal(result.orderedCandidates.length, 0);
});

test('queues expired Local Codex claims for ChatGPT-supervised stale recovery', () => {
  const expired = projectItem(803, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Local Codex',
    'Worker reference': 'owner=bedrin-codex-local; task=client-new-thread:b59adedf-c70f-4a0a-b5cb-90675d99c37d; claimToken=implementation-803-ready-20260804T065005Z; leaseUntil=2026-08-04T10:50:05Z'
  });
  const active = projectItem(804, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'Local Codex',
    'Worker reference': 'owner=bedrin-codex-local; task=client-new-thread:active; claimToken=implementation-804-ready; leaseUntil=2026-08-04T11:30:00Z'
  });
  const result = buildDispatch(
    snapshot([expired, active], [], '2026-08-04T10:59:29Z'),
    {items: [rawItem(expired), rawItem(active)]},
    [],
    {}
  );

  assert.deepEqual(result.staleInProgressByExecutor['Local Codex'].map(item => item.number), [803]);
  assert.equal(result.staleInProgressByExecutor['Local Codex'][0].staleReason, 'lease-expired');
  assert.deepEqual(result.activeInProgressByExecutor['Local Codex'].map(item => item.number), [804]);
  assert.deepEqual(result.orderedCandidates.map(value => ({kind: value.kind, number: value.item.number})), [
    {kind: 'stale-owned-recovery', number: 803}
  ]);
});

test('treats missing or invalid lease evidence as stale recovery', () => {
  const missingReference = projectItem(5, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'ChatGPT'
  });
  const missingLease = projectItem(6, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'ChatGPT',
    'Worker reference': 'worker=chat-6; claimToken=g6'
  });
  const invalidLease = projectItem(7, {
    Status: 'Implementation', Execution: 'In progress', Executor: 'ChatGPT',
    'Worker reference': 'worker=chat-7; leaseUntil=tomorrowish'
  });
  const result = buildDispatch(
    snapshot([missingReference, missingLease, invalidLease]),
    {items: [rawItem(missingReference), rawItem(missingLease), rawItem(invalidLease)]},
    [],
    {}
  );
  assert.deepEqual(
    result.staleInProgressByExecutor.ChatGPT.map(item => item.staleReason),
    ['missing-worker-reference', 'missing-lease', 'invalid-lease']
  );
  assert.deepEqual(result.orderedCandidates.map(value => value.kind), [
    'stale-owned-recovery', 'stale-owned-recovery', 'stale-owned-recovery'
  ]);
  assert.equal(leaseUntilFrom('{"leaseUntil":"2026-08-03T02:00:00Z"}'), '2026-08-03T02:00:00Z');
  assert.equal(staleOwnershipReason(result.staleInProgressByExecutor.ChatGPT[1], Date.parse('2026-08-03T00:00:00Z')), 'missing-lease');
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

test('keeps fork and Dependabot conflicts visible without direct branch adoption', () => {
  const fork = pullRequest(11, [], {repositoryOwnership: 'fork', mergeable: 'CONFLICTING'});
  const dependabot = pullRequest(12, [], {author: 'dependabot[bot]', mergeable: 'CONFLICTING'});
  const result = buildDispatch(
    snapshot([], [fork, dependabot]),
    {items: []},
    [{number: 11, labels: []}, {number: 12, labels: [{name: 'dependencies'}]}],
    {}
  );
  assert.equal(result.pullRequests.conflicting.length, 0);
  assert.deepEqual(result.pullRequests.managedConflicts.map(value => value.number), [11, 12]);
  assert.deepEqual(result.pullRequests.managedConflicts.map(value => value.conflictMode), [
    'contributor-feedback', 'dependabot-operation'
  ]);
  assert.equal(result.pullRequests.intake.length, 0);
  assert.deepEqual(result.orderedCandidates.map(value => value.kind), ['managed-pr-conflict', 'managed-pr-conflict']);
});

test('recognizes GitHub App Dependabot login', () => {
  const dependabot = pullRequest(12, [], {author: 'app/dependabot', mergeable: 'CONFLICTING'});
  const result = buildDispatch(
    snapshot([], [dependabot]),
    {items: []},
    [{number: 12, labels: [{name: 'dependencies'}]}],
    {}
  );
  assert.equal(result.pullRequests.managedConflicts[0].isDependabot, true);
  assert.equal(result.pullRequests.managedConflicts[0].conflictMode, 'dependabot-operation');
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

test('does not steal an explicit Local Codex correction route as intake or conflict handling', () => {
  const canonicalPr = projectItem(14, {
    Status: 'Implementation', Execution: 'Ready', Executor: 'Local Codex', Implementer: 'Local Codex'
  }, 'PullRequest');
  const pr = pullRequest(14, [], {mergeable: 'CONFLICTING', mergeStateStatus: 'DIRTY'});
  const result = buildDispatch(
    snapshot([canonicalPr], [pr]),
    {items: [rawItem(canonicalPr)]},
    [{number: 14, labels: []}],
    {}
  );
  assert.equal(result.pullRequests.intake.length, 0);
  assert.equal(result.pullRequests.conflicting.length, 0);
  assert.equal(result.pullRequests.managedConflicts.length, 0);
  assert.equal(result.orderedCandidates.length, 0);
});

test('does not requeue an explicitly suppressed PR item', () => {
  const suppressedPr = projectItem(761, {
    Status: 'Draft',
    'Worker reference': 'Suppressed duplicate lifecycle item; canonical replacement issue https://github.com/sniffy/sniffy/issues/772 owns the lifecycle'
  }, 'PullRequest');
  const pr = pullRequest(761, [], {
    author: 'app/dependabot',
    mergeable: 'CONFLICTING',
    mergeStateStatus: 'DIRTY'
  });
  const result = buildDispatch(
    snapshot([suppressedPr], [pr]),
    {items: [rawItem(suppressedPr, ['bedrin-gpt'])]},
    [{number: 761, labels: [{name: 'dependencies'}]}],
    {}
  );
  assert.equal(result.pullRequests.intake.length, 0);
  assert.equal(result.pullRequests.conflicting.length, 0);
  assert.equal(result.pullRequests.managedConflicts.length, 0);
  assert.equal(result.orderedCandidates.length, 0);
});

test('does not requeue an already suppressed duplicate representation', () => {
  const issue = projectItem(9, {Status: 'Review', Execution: 'Ready', Executor: 'Human'}, 'Issue');
  const duplicatePr = projectItem(10, {
    Status: 'Draft',
    'Worker reference': 'Suppressed duplicate lifecycle item; canonical issue https://github.com/sniffy/sniffy/issues/9'
  }, 'PullRequest');
  const pr = pullRequest(10, [9]);
  const result = buildDispatch(
    snapshot([issue, duplicatePr], [pr]),
    {items: [rawItem(issue), rawItem(duplicatePr)]},
    [{number: 10, labels: []}],
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

test('projects merged completion drift for a canonical issue and standalone PR', () => {
  const issue = projectItem(755, {
    Status: 'Approval', Execution: 'Ready', Executor: 'Human',
    'Linked pull requests': ['https://github.com/sniffy/sniffy/pull/764']
  }, 'Issue', {state: 'CLOSED', stateReason: 'COMPLETED', closedAt: '2026-07-30T19:09:52Z'});
  const standalone = projectItem(900, {
    Status: 'Approval', Execution: 'Ready', Executor: 'Human'
  }, 'PullRequest', {state: 'CLOSED'});
  const result = buildDispatch(
    snapshot([issue, standalone]),
    {items: [rawItem(issue, ['bedrin']), rawItem(standalone, ['bedrin'])]},
    [
      rawPullRequest(764, {mergedAt: '2026-07-30T19:09:51Z'}),
      rawPullRequest(900, {mergedAt: '2026-08-03T10:00:00Z'})
    ],
    {executorAssignees: {Human: 'bedrin'}}
  );
  assert.deepEqual(result.completionDrift.map(value => value.canonical), [
    {type: 'Issue', number: 755},
    {type: 'PullRequest', number: 900}
  ]);
  assert.deepEqual(result.orderedCandidates.map(value => value.kind), ['completion-drift', 'completion-drift']);
});

test('repairs Done items only when active routing or assignment still lingers', () => {
  const clean = projectItem(754, {Status: 'Done'}, 'PullRequest', {state: 'CLOSED'});
  const routed = projectItem(755, {Status: 'Done', Execution: 'Ready', Executor: 'Human'}, 'PullRequest', {state: 'CLOSED'});
  const assigned = projectItem(756, {Status: 'Done'}, 'PullRequest', {state: 'CLOSED'});
  const result = buildDispatch(
    snapshot([clean, routed, assigned]),
    {items: [rawItem(clean), rawItem(routed), rawItem(assigned, ['bedrin'])]},
    [
      rawPullRequest(754, {mergedAt: '2026-08-03T10:00:00Z'}),
      rawPullRequest(755, {mergedAt: '2026-08-03T10:00:00Z'}),
      rawPullRequest(756, {mergedAt: '2026-08-03T10:00:00Z'})
    ],
    {}
  );
  assert.deepEqual(result.completionDrift.map(value => value.item.number), [755, 756]);
});

test('does not complete a closed unmerged or explicitly suppressed pull request', () => {
  const closed = projectItem(757, {Status: 'Draft'}, 'PullRequest', {state: 'CLOSED'});
  const suppressed = projectItem(761, {
    Status: 'Draft',
    'Worker reference': 'Suppressed duplicate lifecycle item; canonical replacement issue #772'
  }, 'PullRequest', {state: 'CLOSED'});
  const result = buildDispatch(
    snapshot([closed, suppressed]),
    {items: [rawItem(closed), rawItem(suppressed)]},
    [rawPullRequest(757), rawPullRequest(761, {mergedAt: '2026-08-03T10:00:00Z'})],
    {}
  );
  assert.equal(result.completionDrift.length, 0);
  assert.equal(result.orderedCandidates.length, 0);
});

test('routes uninitialized and unassigned Planning issues without whole-Project live reads', () => {
  const planning = projectItem(727, {Status: 'Planning', Execution: 'Ready'});
  const uninitialized = projectItem(803);
  const result = buildDispatch(
    snapshot([planning, uninitialized]),
    {items: [rawItem(planning), rawItem(uninitialized, ['bedrin-gpt'])]},
    [],
    {executorAssignees: {ChatGPT: 'bedrin-gpt', Human: 'bedrin'}}
  );
  assert.deepEqual(result.routingReconciliations.map(value => ({kind: value.kind, number: value.item.number, executor: value.suggestedExecutor})), [
    {kind: 'default-planning-route', number: 727, executor: 'ChatGPT'},
    {kind: 'uninitialized-item', number: 803, executor: 'ChatGPT'}
  ]);
  assert.deepEqual(result.orderedCandidates.map(value => value.kind), ['default-planning-route', 'uninitialized-item']);
});

test('does not duplicate an uninitialized canonical issue already selected for PR intake', () => {
  const issue = projectItem(806, {'Linked pull requests': ['https://github.com/sniffy/sniffy/pull/807']});
  const pr = pullRequest(807, [806]);
  const result = buildDispatch(
    snapshot([issue], [pr]),
    {items: [rawItem(issue, ['bedrin-gpt'])]},
    [rawPullRequest(807)],
    {executorAssignees: {ChatGPT: 'bedrin-gpt'}}
  );
  assert.deepEqual(result.orderedCandidates.map(value => value.kind), ['pr-intake']);
  assert.equal(result.routingReconciliations.length, 0);
});

test('does not guess a route from unknown or conflicting assignees', () => {
  const unknown = projectItem(803);
  const multiple = projectItem(804);
  const result = buildDispatch(
    snapshot([unknown, multiple]),
    {items: [rawItem(unknown, ['someone']), rawItem(multiple, ['bedrin-gpt', 'bedrin'])]},
    [],
    {executorAssignees: {ChatGPT: 'bedrin-gpt', Human: 'bedrin'}}
  );
  assert.deepEqual(result.routingReconciliations.map(value => value.kind), ['route-ambiguity', 'route-ambiguity']);
  assert.equal(routeSuggestion({executor: null, assignees: []}, {ChatGPT: 'bedrin-gpt'}).executor, 'ChatGPT');
  assert.equal(pullRequestNumberFromUrl('https://github.com/sniffy/sniffy/pull/764'), 764);
});

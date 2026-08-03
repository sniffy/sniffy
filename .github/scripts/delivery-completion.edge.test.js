'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  clearAssigneesAndVerify,
  commandFor,
  prepareCompletion,
  verifyMergedPullRequest
} = require('./delivery-completion');

function fieldValues(fields) {
  return Object.entries(fields).map(([name, value]) => ({
    __typename: ['Status', 'Execution', 'Executor'].includes(name)
      ? 'ProjectV2ItemFieldSingleSelectValue'
      : 'ProjectV2ItemFieldTextValue',
    ...(['Status', 'Execution', 'Executor'].includes(name) ? {name: value} : {text: value}),
    field: {name}
  }));
}

function item(fields = {}) {
  return {
    project: {id: 'project'},
    fieldValues: {nodes: fieldValues({
      Status: 'Approval', Execution: 'Ready', Executor: 'Human', 'Worker reference': null, ...fields
    })}
  };
}

function mergedPullRequest({projectItems = [item()], closingIssues = []} = {}) {
  return {
    number: 12,
    state: 'MERGED',
    mergedAt: '2026-08-03T12:00:00Z',
    mergeCommit: {oid: 'c'.repeat(40)},
    headRefOid: 'a'.repeat(40),
    headRefName: 'agent/example',
    baseRefName: 'develop',
    projectItems: {nodes: projectItems},
    closingIssuesReferences: {nodes: closingIssues}
  };
}

function coreStub() {
  const outputs = new Map();
  const summary = {addHeading() { return this; }, addRaw() { return this; }, addCodeBlock() { return this; }};
  return {outputs, summary, setOutput(name, value) { outputs.set(name, value); }};
}

function context() {
  return {
    eventName: 'pull_request_target',
    payload: {action: 'closed', pull_request: {number: 12, merged: true, base: {ref: 'develop'}}}
  };
}

test('skips explicit suppressed canonical representation', async () => {
  const result = await prepareCompletion({
    github: {graphql: async () => ({
      organization: {projectV2: {id: 'project'}},
      repository: {pullRequest: mergedPullRequest({projectItems: [item({
        Status: 'Draft',
        'Worker reference': 'Suppressed duplicate lifecycle item; canonical issue #9'
      })]})}
    })},
    context: context(),
    core: coreStub()
  });
  assert.equal(result.shouldTransition, false);
  assert.match(result.reason, /suppressed duplicate/);
});

test('rejects a current PR that is no longer merged or on develop', async () => {
  const wrongState = await prepareCompletion({
    github: {graphql: async () => ({
      organization: {projectV2: {id: 'project'}},
      repository: {pullRequest: {...mergedPullRequest(), state: 'CLOSED', mergedAt: null}}
    })},
    context: context(),
    core: coreStub()
  });
  assert.equal(wrongState.shouldTransition, false);

  const wrongBase = await prepareCompletion({
    github: {graphql: async () => ({
      organization: {projectV2: {id: 'project'}},
      repository: {pullRequest: {...mergedPullRequest(), baseRefName: 'main'}}
    })},
    context: context(),
    core: coreStub()
  });
  assert.equal(wrongBase.shouldTransition, false);
});

test('recognizes an idempotent already-completed command', () => {
  const target = {
    type: 'PullRequest',
    number: 12,
    pullRequest: {
      number: 12,
      head: 'a'.repeat(40),
      mergeCommit: 'c'.repeat(40),
      mergedAt: '2026-08-03T12:00:00Z'
    },
    item: item({
      Status: 'Done',
      Execution: null,
      Executor: null,
      'Worker reference': `Merged PR #12 head ${'a'.repeat(40)} as ${'c'.repeat(40)} at 2026-08-03T12:00:00Z`
    })
  };
  assert.equal(commandFor(target).alreadyCompleted, true);
});

test('fails exact live verification on stale head or merge commit', async () => {
  const github = {rest: {pulls: {get: async () => ({data: {
    merged_at: '2026-08-03T12:00:00Z',
    base: {ref: 'develop'},
    head: {sha: 'a'.repeat(40)},
    merge_commit_sha: 'c'.repeat(40)
  }})}}};

  await assert.rejects(
    verifyMergedPullRequest({github, pullRequestNumber: 12, expectedHead: 'b'.repeat(40), expectedMergeCommit: 'c'.repeat(40)}),
    /head is/
  );
  await assert.rejects(
    verifyMergedPullRequest({github, pullRequestNumber: 12, expectedHead: 'a'.repeat(40), expectedMergeCommit: 'd'.repeat(40)}),
    /merge commit is/
  );
});

test('clears and verifies canonical assignment', async () => {
  let updated = false;
  const github = {rest: {issues: {
    update: async ({assignees}) => { updated = true; assert.deepEqual(assignees, []); },
    get: async () => ({data: {assignees: []}})
  }}};
  await clearAssigneesAndVerify({github, targetNumber: 9});
  assert.equal(updated, true);
});

test('fails assignment cleanup when an assignee remains', async () => {
  const github = {rest: {issues: {
    update: async () => {},
    get: async () => ({data: {assignees: [{login: 'bedrin'}]}})
  }}};
  await assert.rejects(clearAssigneesAndVerify({github, targetNumber: 9}), /still assigned/);
});

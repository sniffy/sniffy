'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  commandFor,
  isSuppressed,
  prepareCompletion,
  selectCanonicalTarget,
  verifyMergedPullRequest
} = require('./delivery-completion');

function fieldValues(fields = {}) {
  return Object.entries(fields).map(([name, value]) => ({
    __typename: ['Status', 'Execution', 'Executor'].includes(name)
      ? 'ProjectV2ItemFieldSingleSelectValue'
      : 'ProjectV2ItemFieldTextValue',
    ...(['Status', 'Execution', 'Executor'].includes(name) ? {name: value} : {text: value}),
    field: {name}
  }));
}

function item(fields = {}, project = 'project') {
  return {
    project: {id: project},
    fieldValues: {nodes: fieldValues({
      Status: 'Approval', Execution: 'Ready', Executor: 'Human', 'Worker reference': null, ...fields
    })}
  };
}

function issue(number, items = [item()]) {
  return {number, projectItems: {nodes: items}};
}

function pr({closing = [], items = [item()]} = {}) {
  return {
    number: 12,
    state: 'MERGED',
    mergedAt: '2026-08-03T12:00:00Z',
    mergeCommit: {oid: 'c'.repeat(40)},
    headRefOid: 'a'.repeat(40),
    headRefName: 'agent/example',
    baseRefName: 'develop',
    projectItems: {nodes: items},
    closingIssuesReferences: {nodes: closing}
  };
}

function core() {
  const outputs = new Map();
  const summary = {addHeading() { return this; }, addRaw() { return this; }, addCodeBlock() { return this; }};
  return {outputs, summary, setOutput(name, value) { outputs.set(name, value); }};
}

test('resolves issue, standalone PR, and multi-issue PR canonical targets', () => {
  assert.equal(selectCanonicalTarget(pr({closing: [issue(9)]}), 'project').target.type, 'Issue');
  assert.equal(selectCanonicalTarget(pr(), 'project').target.type, 'PullRequest');
  assert.equal(selectCanonicalTarget(pr({closing: [issue(8), issue(9)]}), 'project').target.type, 'PullRequest');
});

test('fails closed for missing or duplicate canonical Project items', () => {
  assert.match(selectCanonicalTarget(pr({closing: [issue(9, [])]}), 'project').reason, /not represented/);
  assert.match(selectCanonicalTarget(pr({closing: [issue(9, [item(), item()])]}), 'project').reason, /2 Project 2/);
});

test('builds guarded Done transition with compact merge evidence', () => {
  const target = selectCanonicalTarget(pr({closing: [issue(9)]}), 'project').target;
  const prepared = commandFor(target);
  assert.equal(prepared.command.set.Status, 'Done');
  assert.deepEqual(prepared.command.clear, ['Execution', 'Executor']);
  assert.equal(prepared.command.expected.fields.Status, 'Approval');
  assert.match(prepared.reference, /^Merged PR #12 head a{40} as c{40}/);
});

test('recognizes explicit suppressed duplicate marker', () => {
  assert.equal(isSuppressed(item({
    Status: 'Draft',
    'Worker reference': 'Suppressed duplicate lifecycle item; canonical replacement issue #9'
  })), true);
});

test('prepares only a trusted merged completion event', async () => {
  const output = core();
  const result = await prepareCompletion({
    github: {graphql: async () => ({
      organization: {projectV2: {id: 'project'}},
      repository: {pullRequest: pr({closing: [issue(9)]})}
    })},
    context: {
      eventName: 'pull_request_target',
      payload: {action: 'closed', pull_request: {number: 12, merged: true, base: {ref: 'develop'}}}
    },
    core: output
  });
  assert.equal(result.shouldTransition, true);
  assert.equal(output.outputs.get('target_number'), '9');
  assert.ok(output.outputs.get('payload_base64'));

  const skipped = await prepareCompletion({
    github: {},
    context: {eventName: 'pull_request_target', payload: {action: 'closed', pull_request: {merged: false}}},
    core: core()
  });
  assert.equal(skipped.shouldTransition, false);
});

test('verifies exact merged head and merge commit', async () => {
  const result = await verifyMergedPullRequest({
    github: {rest: {pulls: {get: async () => ({data: {
      merged_at: '2026-08-03T12:00:00Z',
      base: {ref: 'develop'},
      head: {sha: 'a'.repeat(40)},
      merge_commit_sha: 'c'.repeat(40)
    }})}}},
    pullRequestNumber: 12,
    expectedHead: 'a'.repeat(40),
    expectedMergeCommit: 'c'.repeat(40)
  });
  assert.equal(result.mergeCommit, 'c'.repeat(40));
});

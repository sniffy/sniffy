'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {desired, mismatch, parseCommand} = require('./delivery-control');

const claim = {
  command: 'delivery-control/v1',
  target: {repository: 'sniffy/sniffy', number: 651},
  expected: {
    type: 'PullRequest',
    head: '46c47e02e8a5236cf1e7348fc6202da11ea1bbd3',
    fields: {Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT'}
  },
  set: {
    Execution: 'In progress',
    Executor: 'ChatGPT',
    'Worker reference': 'tick-00; token x; lease 2026-07-30T10:00:00Z'
  }
};

test('parses a fully guarded claim', () => {
  assert.deepEqual(parseCommand(JSON.stringify(claim)), {...claim, clear: [], addIfMissing: false});
});

test('rejects unguarded transitions', () => {
  assert.throws(() => parseCommand({
    command: 'delivery-control/v1',
    target: {repository: 'sniffy/sniffy', number: 1},
    set: {Status: 'Review'}
  }), /Unguarded mutations/);
});

test('requires claim ownership guards', () => {
  const command = structuredClone(claim);
  delete command.expected.fields.Executor;
  assert.throws(() => parseCommand(command), /guard Status and Executor/);
});

test('rejects set and clear overlap', () => {
  const command = structuredClone(claim);
  command.clear = ['Executor'];
  assert.throws(() => parseCommand(command), /both set and clear/);
});

test('detects stale fields and changed head', () => {
  const command = parseCommand(claim);
  const current = new Map([['Status', 'Review'], ['Execution', 'In progress'], ['Executor', 'ChatGPT']]);
  assert.deepEqual(mismatch(command, {__typename: 'PullRequest', headRefOid: 'deadbeef'}, current), [
    ['head', claim.expected.head, 'deadbeef'],
    ['Execution', 'Ready', 'In progress']
  ]);
});

test('recognizes an idempotent repeated command', () => {
  const command = parseCommand(claim);
  const current = new Map(Object.entries(command.set));
  assert.equal(desired(command, current), true);
});

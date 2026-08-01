'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const {enrichSnapshot} = require('./delivery-status-mergeability');

test('adds merge conflict indicators to pull request items only', () => {
  const snapshot = {
    items: [
      {content: {type: 'PullRequest', number: 780}},
      {content: {type: 'Issue', number: 782}}
    ],
    semantics: {}
  };
  const result = enrichSnapshot(snapshot, [
    {number: 780, mergeable: 'CONFLICTING', mergeStateStatus: 'DIRTY'}
  ]);

  assert.equal(result.items[0].content.mergeable, 'CONFLICTING');
  assert.equal(result.items[0].content.mergeStateStatus, 'DIRTY');
  assert.equal(result.items[1].content.mergeable, undefined);
  assert.match(result.semantics.mergeability, /re-read the live PR/);
});

test('preserves unknown mergeability explicitly', () => {
  const snapshot = {items: [{content: {type: 'PullRequest', number: 780}}]};
  const result = enrichSnapshot(snapshot, [{number: 780}]);
  assert.equal(result.items[0].content.mergeable, null);
  assert.equal(result.items[0].content.mergeStateStatus, null);
});

test('workflow exports and applies mergeability before artifact publication', () => {
  const workflow = fs.readFileSync(path.join(__dirname, '../workflows/delivery-status.yml'), 'utf8');
  assert.match(workflow, /--json[^\n]*mergeable,mergeStateStatus/);
  assert.match(workflow, /delivery-status-mergeability\.js[\s\\]*status-artifact\/project-2-status\.json/);
});

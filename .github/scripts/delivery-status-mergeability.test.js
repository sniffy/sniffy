'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const {enrichSnapshot} = require('./delivery-status-mergeability');

function closingIssues(number, issues = []) {
  return {
    number,
    closingIssuesReferences: {
      totalCount: issues.length,
      nodes: issues.map(issueNumber => ({number: issueNumber}))
    }
  };
}

test('publishes open PR observations independently of canonical Project item type', () => {
  const snapshot = {
    items: [
      {content: {type: 'Issue', number: 782}}
    ],
    semantics: {},
    counts: {activeItemCount: 1}
  };
  const result = enrichSnapshot(snapshot, [
    {
      number: 783,
      state: 'OPEN',
      url: 'https://github.com/sniffy/sniffy/pull/783',
      author: {login: 'bedrin-gpt'},
      isCrossRepository: false,
      isDraft: false,
      baseRefName: 'develop',
      headRefName: 'agent/merge-conflict-snapshot',
      headRefOid: '42594d311f6077d5557653c95f1942b5b224a5ae',
      mergeable: 'CONFLICTING',
      mergeStateStatus: 'DIRTY'
    }
  ], [closingIssues(783, [782])]);

  assert.equal(result.items[0].content.mergeable, undefined);
  assert.deepEqual(result.pullRequests, [{
    number: 783,
    url: 'https://github.com/sniffy/sniffy/pull/783',
    author: 'bedrin-gpt',
    repositoryOwnership: 'same-repository',
    isCrossRepository: false,
    isDraft: false,
    baseRefName: 'develop',
    headRefName: 'agent/merge-conflict-snapshot',
    headRefOid: '42594d311f6077d5557653c95f1942b5b224a5ae',
    mergeable: 'CONFLICTING',
    mergeStateStatus: 'DIRTY',
    closingIssueNumbers: [782]
  }]);
  assert.equal(result.counts.openPullRequestCount, 1);
  assert.match(result.semantics.mergeability, /re-read the live PR/);
  assert.match(result.semantics.pullRequests, /independent of canonical Project item/);
});

test('also enriches represented pull request items', () => {
  const snapshot = {items: [{content: {type: 'PullRequest', number: 780}}]};
  const result = enrichSnapshot(snapshot, [
    {number: 780, state: 'OPEN', mergeable: 'CONFLICTING', mergeStateStatus: 'DIRTY'}
  ], [closingIssues(780)]);

  assert.equal(result.items[0].content.mergeable, 'CONFLICTING');
  assert.equal(result.items[0].content.mergeStateStatus, 'DIRTY');
});

test('preserves unknown mergeability explicitly', () => {
  const snapshot = {items: [{content: {type: 'PullRequest', number: 780}}]};
  const result = enrichSnapshot(snapshot, [
    {number: 780, state: 'OPEN', isCrossRepository: true}
  ], [closingIssues(780)]);
  assert.equal(result.items[0].content.mergeable, null);
  assert.equal(result.items[0].content.mergeStateStatus, null);
  assert.equal(result.pullRequests[0].mergeable, null);
  assert.equal(result.pullRequests[0].mergeStateStatus, null);
  assert.equal(result.pullRequests[0].repositoryOwnership, 'fork');
});

test('omits closed PRs and fails closed when formal-link observations are missing', () => {
  const closed = {number: 779, state: 'CLOSED'};
  assert.deepEqual(enrichSnapshot({items: []}, [closed], []).pullRequests, []);
  assert.throws(
    () => enrichSnapshot({items: []}, [{number: 783, state: 'OPEN'}], []),
    /Missing formal closing-issue export for open PR #783/
  );
  assert.throws(
    () => enrichSnapshot({items: []}, [{number: 783, state: 'OPEN'}], [{
      number: 783,
      closingIssuesReferences: {totalCount: 2, nodes: [{number: 782}]}
    }]),
    /Closing-issue export for PR #783 is incomplete/
  );
});

test('workflow exports and applies mergeability before artifact publication', () => {
  const workflow = fs.readFileSync(path.join(__dirname, '../workflows/delivery-status.yml'), 'utf8');
  assert.match(workflow, /--json[^\n]*mergeable,mergeStateStatus/);
  assert.match(workflow, /closingIssuesReferences\(first:\s*100\)/);
  assert.match(workflow, /delivery-status-mergeability\.js[\s\\]*status-artifact\/project-2-status\.json/);
});

test('dispatcher scans independent PR observations without changing fork or Dependabot policy', () => {
  const instructions = fs.readFileSync(path.join(__dirname, '../../.chatgpt/status-snapshot-instructions.md'), 'utf8');
  assert.match(instructions, /top-level `pullRequests` observations/);
  assert.match(instructions, /use `closingIssueNumbers` to resolve the\s+canonical issue-versus-PR identity/);
  assert.match(instructions, /Fork PRs wait for their contributor/);
  assert.match(instructions, /Dependabot PRs use `@dependabot rebase`/);
  assert.match(instructions, /never adopt or rewrite those\s+branches through this rule/);
});

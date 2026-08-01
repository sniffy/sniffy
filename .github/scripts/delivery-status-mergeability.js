'use strict';

const fs = require('node:fs');

function indexPullRequests(values) {
  return new Map((Array.isArray(values) ? values : []).map(value => [Number(value.number), value]));
}

function indexClosingIssues(values) {
  const result = new Map();
  for (const value of Array.isArray(values) ? values : []) {
    const references = value?.closingIssuesReferences ?? {};
    const nodes = Array.isArray(references.nodes) ? references.nodes : [];
    if (Number.isInteger(references.totalCount) && references.totalCount > nodes.length) {
      throw new Error(`Closing-issue export for PR #${value.number} is incomplete`);
    }
    result.set(
      Number(value.number),
      nodes.map(node => Number(node.number)).filter(Number.isSafeInteger).sort((left, right) => left - right)
    );
  }
  return result;
}

function authorLogin(author) {
  if (typeof author === 'string') return author;
  return author?.login ?? author?.name ?? null;
}

function normalizeOpenPullRequests(values, closingIssuesByNumber) {
  return (Array.isArray(values) ? values : [])
    .filter(value => String(value?.state ?? '').toUpperCase() === 'OPEN')
    .map(value => {
      const number = Number(value.number);
      if (!closingIssuesByNumber.has(number)) {
        throw new Error(`Missing formal closing-issue export for open PR #${value.number}`);
      }
      return {
        number,
        url: value.url ?? null,
        author: authorLogin(value.author),
        repositoryOwnership: value.isCrossRepository === true
          ? 'fork'
          : value.isCrossRepository === false ? 'same-repository' : null,
        isCrossRepository: value.isCrossRepository ?? null,
        isDraft: value.isDraft ?? false,
        baseRefName: value.baseRefName ?? null,
        headRefName: value.headRefName ?? null,
        headRefOid: value.headRefOid ?? null,
        mergeable: value.mergeable ?? null,
        mergeStateStatus: value.mergeStateStatus ?? null,
        closingIssueNumbers: closingIssuesByNumber.get(number)
      };
    })
    .sort((left, right) => left.number - right.number);
}

function enrichSnapshot(snapshot, pullRequests, closingIssues = []) {
  const byNumber = indexPullRequests(pullRequests);
  for (const item of snapshot.items || []) {
    const type = String(item?.content?.type || '').toUpperCase();
    if (type !== 'PULLREQUEST' && type !== 'PULL_REQUEST') continue;
    const source = byNumber.get(Number(item.content.number));
    if (!source) continue;
    item.content.mergeable = source.mergeable ?? null;
    item.content.mergeStateStatus = source.mergeStateStatus ?? null;
  }
  snapshot.pullRequests = normalizeOpenPullRequests(pullRequests, indexClosingIssues(closingIssues));
  snapshot.semantics = {
    ...(snapshot.semantics || {}),
    mergeability: 'snapshot hint only; re-read the live PR before dispatch or mutation',
    pullRequests: 'all open repository PRs, independent of canonical Project item representation'
  };
  snapshot.counts = {
    ...(snapshot.counts || {}),
    openPullRequestCount: snapshot.pullRequests.length
  };
  return snapshot;
}

function main(argv) {
  const [snapshotPath, pullRequestsPath, closingIssuesPath] = argv;
  if (!snapshotPath || !pullRequestsPath || !closingIssuesPath) {
    throw new Error('Usage: delivery-status-mergeability.js <snapshot.json> <pull-requests.json> <closing-issues.json>');
  }
  const snapshot = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
  const pullRequests = JSON.parse(fs.readFileSync(pullRequestsPath, 'utf8'));
  const closingIssues = JSON.parse(fs.readFileSync(closingIssuesPath, 'utf8'));
  enrichSnapshot(snapshot, pullRequests, closingIssues);
  fs.writeFileSync(snapshotPath, `${JSON.stringify(snapshot, null, 2)}\n`);
}

if (require.main === module) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.stack || error.message}\n`);
    process.exitCode = 1;
  }
}

module.exports = {enrichSnapshot, normalizeOpenPullRequests};

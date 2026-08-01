'use strict';

const fs = require('node:fs');

function indexPullRequests(values) {
  return new Map((Array.isArray(values) ? values : []).map(value => [Number(value.number), value]));
}

function enrichSnapshot(snapshot, pullRequests) {
  const byNumber = indexPullRequests(pullRequests);
  for (const item of snapshot.items || []) {
    const type = String(item?.content?.type || '').toUpperCase();
    if (type !== 'PULLREQUEST' && type !== 'PULL_REQUEST') continue;
    const source = byNumber.get(Number(item.content.number));
    if (!source) continue;
    item.content.mergeable = source.mergeable ?? null;
    item.content.mergeStateStatus = source.mergeStateStatus ?? null;
  }
  snapshot.semantics = {
    ...(snapshot.semantics || {}),
    mergeability: 'snapshot hint only; re-read the live PR before dispatch or mutation'
  };
  return snapshot;
}

function main(argv) {
  const [snapshotPath, pullRequestsPath] = argv;
  if (!snapshotPath || !pullRequestsPath) {
    throw new Error('Usage: delivery-status-mergeability.js <snapshot.json> <pull-requests.json>');
  }
  const snapshot = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
  const pullRequests = JSON.parse(fs.readFileSync(pullRequestsPath, 'utf8'));
  enrichSnapshot(snapshot, pullRequests);
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

module.exports = {enrichSnapshot};

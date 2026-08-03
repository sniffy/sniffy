'use strict';

const fs = require('node:fs');

const DOWNSTREAM_HEAD_STATUSES = new Set(['Review', 'Verification', 'Approval']);
const PRIORITY_RANK = new Map([['Urgent', 0], ['High', 1], ['Medium', 2], ['Low', 3]]);
const SHA_PATTERN = /\b[0-9a-f]{40}\b/gi;
const NEXT_OBSERVATION_PATTERN = /nextObservationAt\s*[=:]\s*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:.+-]+Z?)/i;

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function field(item, name) {
  const fields = item?.fields || {};
  const key = Object.keys(fields).find(candidate => candidate.toLowerCase() === name.toLowerCase());
  return key === undefined ? null : fields[key] ?? null;
}

function assigneeLogins(rawItem) {
  return asArray(rawItem?.assignees).flatMap(value => {
    if (typeof value === 'string') return [value];
    if (value && typeof value === 'object') return [value.login ?? value.name].filter(Boolean);
    return [];
  }).sort();
}

function labelNames(rawPullRequest) {
  return asArray(rawPullRequest?.labels).flatMap(value => {
    if (typeof value === 'string') return [value];
    if (value && typeof value === 'object') return [value.name].filter(Boolean);
    return [];
  }).sort();
}

function normalizedType(value) {
  const type = String(value ?? '').toUpperCase();
  if (type === 'PULLREQUEST' || type === 'PULL_REQUEST') return 'PullRequest';
  if (type === 'ISSUE') return 'Issue';
  return value ?? null;
}

function priorityRank(value) {
  return PRIORITY_RANK.get(value) ?? 4;
}

function compareCandidates(left, right) {
  const keys = candidate => [
    priorityRank(candidate.priority),
    candidate.readyTimestamp ?? '9999-12-31T23:59:59Z',
    candidate.repository ?? '',
    Number.isFinite(Number(candidate.number)) ? Number(candidate.number) : Number.MAX_SAFE_INTEGER,
    candidate.type ?? '',
    candidate.projectItemId ?? ''
  ];
  const a = keys(left);
  const b = keys(right);
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] < b[index]) return -1;
    if (a[index] > b[index]) return 1;
  }
  return 0;
}

function compactItem(item, rawById) {
  const raw = rawById.get(item.projectItemId) || {};
  const workerReference = field(item, 'Worker reference');
  const nextMatch = typeof workerReference === 'string' ? workerReference.match(NEXT_OBSERVATION_PATTERN) : null;
  const nextObservationAt = nextMatch?.[1] ?? null;
  return {
    projectItemId: item.projectItemId,
    repository: item.content?.repository ?? null,
    type: normalizedType(item.content?.type),
    number: item.content?.number ?? null,
    title: item.content?.title ?? null,
    url: item.content?.url ?? null,
    state: item.content?.state ?? null,
    isDraft: item.content?.isDraft ?? false,
    baseRefName: item.content?.baseRefName ?? null,
    headRefName: item.content?.headRefName ?? null,
    headRefOid: item.content?.headRefOid ?? null,
    status: field(item, 'Status'),
    execution: field(item, 'Execution'),
    executor: field(item, 'Executor'),
    implementer: field(item, 'Implementer'),
    verifier: field(item, 'Verifier'),
    priority: field(item, 'Priority'),
    readyTimestamp: field(item, 'Ready timestamp'),
    workerReference,
    nextObservationAt,
    assignees: assigneeLogins(raw)
  };
}

function groupByExecutor(items) {
  const grouped = {};
  for (const item of [...items].sort(compareCandidates)) {
    const executor = item.executor ?? 'Unassigned';
    (grouped[executor] ||= []).push(item);
  }
  return grouped;
}

function itemKey(type, number) {
  return `${normalizedType(type)}#${Number(number)}`;
}

function canonicalIdentity(pullRequest) {
  const closing = asArray(pullRequest.closingIssueNumbers);
  if (closing.length === 1) return {type: 'Issue', number: closing[0], expectedInitialStatus: 'Review'};
  if (closing.length === 0) return {type: 'PullRequest', number: pullRequest.number, expectedInitialStatus: 'Review'};
  return {type: 'PullRequest', number: pullRequest.number, expectedInitialStatus: 'Planning'};
}

function projectIdentity(item) {
  return itemKey(item.type, item.number);
}

function exactHeads(workerReference) {
  if (typeof workerReference !== 'string') return [];
  return [...new Set(workerReference.match(SHA_PATTERN) || [])].map(value => value.toLowerCase());
}

function projectItemsForPullRequest(pullRequest, itemsByIdentity) {
  const identities = [{type: 'PullRequest', number: pullRequest.number}];
  for (const issueNumber of asArray(pullRequest.closingIssueNumbers)) {
    identities.push({type: 'Issue', number: issueNumber});
  }
  return identities.flatMap(identity => itemsByIdentity.get(itemKey(identity.type, identity.number)) || []);
}

function pullRequestProjection(pullRequest, rawPullRequestsByNumber, itemsByIdentity, baseBranch) {
  const canonical = canonicalIdentity(pullRequest);
  const canonicalItems = itemsByIdentity.get(itemKey(canonical.type, canonical.number)) || [];
  const relatedItems = projectItemsForPullRequest(pullRequest, itemsByIdentity);
  const duplicateItems = relatedItems.filter(item => projectIdentity(item) !== itemKey(canonical.type, canonical.number));
  const canonicalItem = canonicalItems[0] ?? null;
  const raw = rawPullRequestsByNumber.get(Number(pullRequest.number)) || {};
  const labels = labelNames(raw);
  const dependabot = String(pullRequest.author ?? '').toLowerCase().startsWith('dependabot');
  const onBase = pullRequest.baseRefName === baseBranch;
  const reviewable = onBase && pullRequest.isDraft === false;
  const statusNeedsIntake = canonicalItem === null || canonicalItem.status === null || canonicalItem.status === 'Draft';
  return {
    number: pullRequest.number,
    url: pullRequest.url,
    author: pullRequest.author,
    labels,
    repositoryOwnership: pullRequest.repositoryOwnership,
    isDraft: pullRequest.isDraft,
    baseRefName: pullRequest.baseRefName,
    headRefName: pullRequest.headRefName,
    headRefOid: pullRequest.headRefOid,
    mergeable: pullRequest.mergeable,
    mergeStateStatus: pullRequest.mergeStateStatus,
    closingIssueNumbers: asArray(pullRequest.closingIssueNumbers),
    canonical,
    canonicalProjectItem: canonicalItem,
    duplicateProjectItems: duplicateItems,
    isDependabot: dependabot,
    needsIntake: reviewable && (statusNeedsIntake || duplicateItems.length > 0),
    conflictCandidate: reviewable && pullRequest.repositoryOwnership === 'same-repository' && !dependabot &&
      (pullRequest.mergeable === 'CONFLICTING' || pullRequest.mergeStateStatus === 'DIRTY')
  };
}

function staleExactHeadCandidate(projection) {
  const item = projection.canonicalProjectItem;
  const currentHead = String(projection.headRefOid ?? '').toLowerCase();
  if (!item || !currentHead || !DOWNSTREAM_HEAD_STATUSES.has(item.status)) return null;
  const recordedHeads = exactHeads(item.workerReference);
  if (recordedHeads.length === 0 || recordedHeads.includes(currentHead)) return null;
  return {
    kind: 'stale-exact-head',
    canonical: projection.canonical,
    pullRequest: projection,
    recordedHeads
  };
}

function buildDispatch(snapshot, rawItems, rawPullRequests, options = {}) {
  const baseBranch = options.baseBranch ?? 'develop';
  const executorAssignees = options.executorAssignees || {};
  const rawById = new Map(asArray(rawItems?.items).map(item => [item.id, item]));
  const rawPullRequestsByNumber = new Map(asArray(rawPullRequests).map(item => [Number(item.number), item]));
  const compactItems = asArray(snapshot.items).map(item => compactItem(item, rawById));
  const itemsByIdentity = new Map();
  for (const item of compactItems) {
    const key = projectIdentity(item);
    if (!itemsByIdentity.has(key)) itemsByIdentity.set(key, []);
    itemsByIdentity.get(key).push(item);
  }

  const ready = compactItems.filter(item => item.execution === 'Ready');
  const inProgress = compactItems.filter(item => item.execution === 'In progress');
  const generatedAt = Date.parse(snapshot.generatedAt);
  const dueInProgress = inProgress.filter(item => {
    if (!item.nextObservationAt) return true;
    const dueAt = Date.parse(item.nextObservationAt);
    return !Number.isFinite(dueAt) || !Number.isFinite(generatedAt) || dueAt <= generatedAt;
  });

  const routeMismatches = ready.filter(item => {
    const expected = executorAssignees[item.executor];
    return expected && item.assignees.length > 0 && !item.assignees.includes(expected);
  }).sort(compareCandidates);

  const pullRequests = asArray(snapshot.pullRequests)
    .filter(value => value.baseRefName === baseBranch)
    .map(value => pullRequestProjection(value, rawPullRequestsByNumber, itemsByIdentity, baseBranch));
  const conflicting = pullRequests.filter(value => value.conflictCandidate);
  const intake = pullRequests.filter(value => value.needsIntake && !value.conflictCandidate);
  const drafts = pullRequests.filter(value => value.isDraft);
  const staleExactHead = pullRequests.map(staleExactHeadCandidate).filter(Boolean);

  const orderedCandidates = [
    ...conflicting.map(value => ({kind: 'conflicting-pr', canonical: value.canonical, pullRequest: value})),
    ...intake.map(value => ({kind: 'pr-intake', canonical: value.canonical, pullRequest: value})),
    ...staleExactHead,
    ...routeMismatches.map(value => ({kind: 'route-mismatch', item: value})),
    ...dueInProgress
      .filter(value => value.executor === 'ChatGPT' || value.executor === 'Codex Cloud')
      .sort(compareCandidates)
      .map(value => ({kind: 'due-owned-observation', item: value})),
    ...ready
      .filter(value => value.executor === 'Codex Cloud')
      .sort(compareCandidates)
      .map(value => ({kind: 'supervised-ready', item: value})),
    ...ready
      .filter(value => value.executor === 'ChatGPT')
      .sort(compareCandidates)
      .map(value => ({kind: 'chatgpt-ready', item: value}))
  ];

  return {
    generatedAt: snapshot.generatedAt,
    baseBranch,
    readyByExecutor: groupByExecutor(ready),
    inProgressByExecutor: groupByExecutor(inProgress),
    dueInProgressByExecutor: groupByExecutor(dueInProgress),
    routeMismatches,
    pullRequests: {conflicting, intake, drafts},
    staleExactHead,
    orderedCandidates,
    counts: {
      ready: ready.length,
      inProgress: inProgress.length,
      dueInProgress: dueInProgress.length,
      routeMismatches: routeMismatches.length,
      conflictingPullRequests: conflicting.length,
      intakePullRequests: intake.length,
      staleExactHead: staleExactHead.length,
      orderedCandidates: orderedCandidates.length
    }
  };
}

function parseArguments(argv) {
  const [snapshotPath, rawItemsPath, rawPullRequestsPath, ...rest] = argv;
  if (!snapshotPath || !rawItemsPath || !rawPullRequestsPath) {
    throw new Error('Usage: delivery-dispatch-view.js <snapshot.json> <raw-items.json> <raw-pull-requests.json> [--base-branch NAME] [--executor-assignees JSON]');
  }
  const options = {};
  for (let index = 0; index < rest.length; index += 2) {
    const key = rest[index];
    const value = rest[index + 1];
    if (!key?.startsWith('--') || value === undefined) throw new Error(`Invalid argument sequence near ${key ?? '<end>'}`);
    options[key.slice(2)] = value;
  }
  return {snapshotPath, rawItemsPath, rawPullRequestsPath, options};
}

function main(argv) {
  const {snapshotPath, rawItemsPath, rawPullRequestsPath, options} = parseArguments(argv);
  const snapshot = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
  const rawItems = JSON.parse(fs.readFileSync(rawItemsPath, 'utf8'));
  const rawPullRequests = JSON.parse(fs.readFileSync(rawPullRequestsPath, 'utf8'));
  snapshot.dispatch = buildDispatch(snapshot, rawItems, rawPullRequests, {
    baseBranch: options['base-branch'] ?? 'develop',
    executorAssignees: options['executor-assignees'] ? JSON.parse(options['executor-assignees']) : {}
  });
  snapshot.semantics = {
    ...(snapshot.semantics || {}),
    dispatch: 'deterministic selection projection; live-read only the selected candidate before a guarded mutation'
  };
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

module.exports = {buildDispatch, canonicalIdentity, compareCandidates, exactHeads};

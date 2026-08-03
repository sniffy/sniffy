'use strict';

const fs = require('node:fs');

const DOWNSTREAM_HEAD_STATUSES = new Set(['Review', 'Verification', 'Approval']);
const PRIORITY_RANK = new Map([['Urgent', 0], ['High', 1], ['Medium', 2], ['Low', 3]]);
const SHA_PATTERN = /\b[0-9a-f]{40}\b/gi;
const LEASE_UNTIL_PATTERN = /\b"?leaseUntil"?\s*[=:]\s*"?([^"\s;,}]+)/i;
const SUPPRESSED_DUPLICATE_PATTERN = /^Suppressed duplicate lifecycle item\b/i;
const TECHNICAL_LABELS = new Set(['ai-delivery-control', 'ai-delivery-status']);

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

function isDependabotAuthor(author) {
  const login = String(author ?? '').trim().toLowerCase();
  return login === 'dependabot' || login === 'dependabot[bot]' || login === 'app/dependabot';
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

function leaseUntilFrom(workerReference) {
  if (typeof workerReference !== 'string') return null;
  return workerReference.match(LEASE_UNTIL_PATTERN)?.[1] ?? null;
}

function compactItem(item, rawById) {
  const raw = rawById.get(item.projectItemId) || {};
  const workerReference = field(item, 'Worker reference');
  return {
    projectItemId: item.projectItemId,
    repository: item.content?.repository ?? null,
    type: normalizedType(item.content?.type),
    number: item.content?.number ?? null,
    title: item.content?.title ?? null,
    url: item.content?.url ?? null,
    state: item.content?.state ?? null,
    stateReason: item.content?.stateReason ?? null,
    isDraft: item.content?.isDraft ?? false,
    mergedAt: item.content?.mergedAt ?? null,
    closedAt: item.content?.closedAt ?? null,
    baseRefName: item.content?.baseRefName ?? null,
    headRefName: item.content?.headRefName ?? null,
    headRefOid: item.content?.headRefOid ?? null,
    labels: asArray(item.content?.labels),
    linkedPullRequests: asArray(field(item, 'Linked pull requests')),
    status: field(item, 'Status'),
    execution: field(item, 'Execution'),
    executor: field(item, 'Executor'),
    implementer: field(item, 'Implementer'),
    verifier: field(item, 'Verifier'),
    priority: field(item, 'Priority'),
    readyTimestamp: field(item, 'Ready timestamp'),
    workerReference,
    leaseUntil: leaseUntilFrom(workerReference),
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

function isSuppressedDuplicateItem(item) {
  return item?.status === 'Draft' &&
    typeof item.workerReference === 'string' &&
    SUPPRESSED_DUPLICATE_PATTERN.test(item.workerReference.trim());
}

function projectItemsForPullRequest(pullRequest, itemsByIdentity) {
  const identities = [{type: 'PullRequest', number: pullRequest.number}];
  for (const issueNumber of asArray(pullRequest.closingIssueNumbers)) {
    identities.push({type: 'Issue', number: issueNumber});
  }
  return identities.flatMap(identity => itemsByIdentity.get(itemKey(identity.type, identity.number)) || []);
}

function conflictModeFor({pullRequest, canonicalItem, dependabot, reviewable, suppressed}) {
  const hasConflict = reviewable &&
    (pullRequest.mergeable === 'CONFLICTING' || pullRequest.mergeStateStatus === 'DIRTY');
  const routedImplementation = canonicalItem?.status === 'Implementation' &&
    ['Ready', 'In progress'].includes(canonicalItem.execution) && canonicalItem.executor !== null;
  if (suppressed || !hasConflict || routedImplementation) return null;
  if (dependabot) return 'dependabot-operation';
  if (pullRequest.repositoryOwnership === 'fork') return 'contributor-feedback';
  if (pullRequest.repositoryOwnership === 'same-repository') return 'same-repository-continuation';
  return 'inspect-ownership';
}

function pullRequestProjection(pullRequest, rawPullRequestsByNumber, itemsByIdentity, baseBranch) {
  const canonical = canonicalIdentity(pullRequest);
  const canonicalItems = itemsByIdentity.get(itemKey(canonical.type, canonical.number)) || [];
  const relatedItems = projectItemsForPullRequest(pullRequest, itemsByIdentity);
  const duplicateItems = relatedItems.filter(item => projectIdentity(item) !== itemKey(canonical.type, canonical.number));
  const suppressedDuplicateItems = duplicateItems.filter(isSuppressedDuplicateItem);
  const actionableDuplicateItems = duplicateItems.filter(item => !isSuppressedDuplicateItem(item));
  const canonicalItem = canonicalItems[0] ?? null;
  const suppressed = isSuppressedDuplicateItem(canonicalItem);
  const raw = rawPullRequestsByNumber.get(Number(pullRequest.number)) || {};
  const labels = labelNames(raw);
  const dependabot = isDependabotAuthor(pullRequest.author);
  const onBase = pullRequest.baseRefName === baseBranch;
  const reviewable = onBase && pullRequest.isDraft === false;
  const statusNeedsIntake = canonicalItem === null || canonicalItem.status === null || canonicalItem.status === 'Draft';
  const conflictMode = conflictModeFor({pullRequest, canonicalItem, dependabot, reviewable, suppressed});
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
    suppressedDuplicateProjectItems: suppressedDuplicateItems,
    suppressed,
    isDependabot: dependabot,
    needsIntake: reviewable && !suppressed && (statusNeedsIntake || actionableDuplicateItems.length > 0),
    conflictMode,
    conflictCandidate: conflictMode !== null
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

function staleOwnershipReason(item, generatedAt) {
  if (typeof item.workerReference !== 'string' || item.workerReference.trim() === '') {
    return 'missing-worker-reference';
  }
  if (!item.leaseUntil) return 'missing-lease';
  const leaseUntil = Date.parse(item.leaseUntil);
  if (!Number.isFinite(leaseUntil)) return 'invalid-lease';
  if (!Number.isFinite(generatedAt)) return 'invalid-snapshot-time';
  return leaseUntil <= generatedAt ? 'lease-expired' : null;
}

function pullRequestNumberFromUrl(url) {
  const match = String(url ?? '').match(/\/pull\/(\d+)(?:$|[/?#])/);
  return match ? Number(match[1]) : null;
}

function mergedPullRequestSummary(raw) {
  if (!raw?.mergedAt) return null;
  return {
    number: Number(raw.number),
    url: raw.url ?? null,
    baseRefName: raw.baseRefName ?? null,
    headRefName: raw.headRefName ?? null,
    headRefOid: raw.headRefOid ?? null,
    mergedAt: raw.mergedAt
  };
}

function completionDriftCandidate(item, rawPullRequestsByNumber, baseBranch) {
  const awaitingMerge = item.status === 'Approval' && item.execution === 'Ready' && item.executor === 'Human';
  const terminalRoutingDrift = item.status === 'Done' &&
    (item.execution !== null || item.executor !== null || item.assignees.length > 0);
  if ((!awaitingMerge && !terminalRoutingDrift) || isSuppressedDuplicateItem(item)) return null;

  if (item.type === 'PullRequest') {
    const pullRequest = mergedPullRequestSummary(rawPullRequestsByNumber.get(Number(item.number)));
    if (!pullRequest || pullRequest.baseRefName !== baseBranch) return null;
    return {kind: 'completion-drift', canonical: {type: 'PullRequest', number: item.number}, item, pullRequest};
  }

  if (item.type !== 'Issue' || item.state !== 'CLOSED' || item.stateReason !== 'COMPLETED') return null;
  const linkedNumbers = [...new Set(item.linkedPullRequests.map(pullRequestNumberFromUrl).filter(Number.isInteger))];
  if (linkedNumbers.length !== 1) return null;
  const pullRequest = mergedPullRequestSummary(rawPullRequestsByNumber.get(linkedNumbers[0]));
  if (!pullRequest || pullRequest.baseRefName !== baseBranch) return null;
  return {kind: 'completion-drift', canonical: {type: 'Issue', number: item.number}, item, pullRequest};
}

function executorByAssignee(executorAssignees) {
  return new Map(Object.entries(executorAssignees).map(([executor, assignee]) => [assignee, executor]));
}

function routeSuggestion(item, executorAssignees, defaultExecutor = 'ChatGPT') {
  if (item.executor) return {executor: item.executor, reason: 'existing-executor'};
  if (item.assignees.length === 0) return {executor: defaultExecutor, reason: 'default-unassigned'};
  if (item.assignees.length !== 1) return {executor: null, reason: 'multiple-assignees'};
  const executor = executorByAssignee(executorAssignees).get(item.assignees[0]) ?? null;
  return executor
    ? {executor, reason: 'assignee-route'}
    : {executor: null, reason: 'unknown-assignee'};
}

function isTechnicalItem(item) {
  return item.labels.some(label => TECHNICAL_LABELS.has(label));
}

function routingReconciliationCandidates(compactItems, executorAssignees, excludedIdentities = new Set()) {
  const candidates = [];
  for (const item of compactItems) {
    if (item.type !== 'Issue' || item.state !== 'OPEN' || isTechnicalItem(item) ||
        excludedIdentities.has(projectIdentity(item))) continue;

    if (item.status === null) {
      const suggestion = routeSuggestion(item, executorAssignees);
      candidates.push(suggestion.executor
        ? {kind: 'uninitialized-item', item, suggestedStatus: 'Planning', suggestedExecution: 'Ready', suggestedExecutor: suggestion.executor, routeReason: suggestion.reason}
        : {kind: 'route-ambiguity', item, routeReason: suggestion.reason});
      continue;
    }

    if (item.status === 'Planning' && item.execution === 'Ready' && item.executor === null) {
      const suggestion = routeSuggestion(item, executorAssignees);
      candidates.push(suggestion.executor
        ? {kind: 'default-planning-route', item, suggestedExecutor: suggestion.executor, routeReason: suggestion.reason}
        : {kind: 'route-ambiguity', item, routeReason: suggestion.reason});
    }
  }
  return candidates.sort((left, right) => compareCandidates(left.item, right.item));
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
  const staleInProgress = inProgress
    .map(item => ({...item, staleReason: staleOwnershipReason(item, generatedAt)}))
    .filter(item => item.staleReason !== null);
  const staleIds = new Set(staleInProgress.map(item => item.projectItemId));
  const activeInProgress = inProgress.filter(item => !staleIds.has(item.projectItemId));

  const routeMismatches = ready.filter(item => {
    const expected = executorAssignees[item.executor];
    return expected && item.assignees.length > 0 &&
      (item.assignees.length !== 1 || item.assignees[0] !== expected);
  }).sort(compareCandidates);

  const completionDrift = compactItems
    .map(item => completionDriftCandidate(item, rawPullRequestsByNumber, baseBranch))
    .filter(Boolean)
    .sort((left, right) => compareCandidates(left.item, right.item));

  const pullRequests = asArray(snapshot.pullRequests)
    .filter(value => value.baseRefName === baseBranch)
    .map(value => pullRequestProjection(value, rawPullRequestsByNumber, itemsByIdentity, baseBranch));
  const prAttentionCanonicals = new Set(pullRequests
    .filter(value => value.needsIntake || value.conflictMode !== null)
    .map(value => itemKey(value.canonical.type, value.canonical.number)));
  const routingReconciliations = routingReconciliationCandidates(
    compactItems,
    executorAssignees,
    prAttentionCanonicals
  );
  const conflicting = pullRequests.filter(value => value.conflictMode === 'same-repository-continuation');
  const managedConflicts = pullRequests.filter(value =>
    value.conflictMode !== null && value.conflictMode !== 'same-repository-continuation');
  const intake = pullRequests.filter(value => value.needsIntake && value.conflictMode === null);
  const drafts = pullRequests.filter(value => value.isDraft);
  const staleExactHead = pullRequests.map(staleExactHeadCandidate).filter(Boolean);

  const orderedCandidates = [
    ...conflicting.map(value => ({kind: 'conflicting-pr', canonical: value.canonical, pullRequest: value})),
    ...managedConflicts.map(value => ({kind: 'managed-pr-conflict', canonical: value.canonical, pullRequest: value})),
    ...intake.map(value => ({kind: 'pr-intake', canonical: value.canonical, pullRequest: value})),
    ...completionDrift,
    ...staleExactHead,
    ...routingReconciliations,
    ...routeMismatches.map(value => ({kind: 'route-mismatch', item: value})),
    ...staleInProgress
      .filter(value => value.executor === 'ChatGPT' || value.executor === 'Codex Cloud')
      .sort(compareCandidates)
      .map(value => ({kind: 'stale-owned-recovery', item: value})),
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
    activeInProgressByExecutor: groupByExecutor(activeInProgress),
    staleInProgressByExecutor: groupByExecutor(staleInProgress),
    completionDrift,
    routingReconciliations,
    routeMismatches,
    pullRequests: {conflicting, managedConflicts, intake, drafts},
    staleExactHead,
    orderedCandidates,
    counts: {
      ready: ready.length,
      inProgress: inProgress.length,
      activeInProgress: activeInProgress.length,
      staleInProgress: staleInProgress.length,
      completionDrift: completionDrift.length,
      routingReconciliations: routingReconciliations.length,
      routeMismatches: routeMismatches.length,
      conflictingPullRequests: conflicting.length,
      managedConflictPullRequests: managedConflicts.length,
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
    dispatch: 'deterministic selection projection; live-read only the selected candidate before a guarded mutation',
    inProgress: 'active ownership is ignored until lease expiry; only missing, invalid, or expired leases enter stale recovery'
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

module.exports = {
  buildDispatch,
  canonicalIdentity,
  compareCandidates,
  completionDriftCandidate,
  exactHeads,
  leaseUntilFrom,
  pullRequestNumberFromUrl,
  routeSuggestion,
  routingReconciliationCandidates,
  staleOwnershipReason
};

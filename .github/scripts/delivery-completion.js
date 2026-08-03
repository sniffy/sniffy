'use strict';

const REPOSITORY = 'sniffy/sniffy';
const ORGANIZATION = 'sniffy';
const PROJECT_NUMBER = 2;
const BASE_BRANCH = 'develop';
const SUPPRESSED_DUPLICATE_PATTERN = /^Suppressed duplicate lifecycle item\b/i;
const GUARDED_FIELDS = ['Status', 'Execution', 'Executor', 'Worker reference'];

function values(nodes) {
  const result = new Map();
  for (const value of nodes || []) {
    if (!value.field?.name) continue;
    if (value.__typename === 'ProjectV2ItemFieldSingleSelectValue') {
      result.set(value.field.name, value.name ?? null);
    }
    if (value.__typename === 'ProjectV2ItemFieldTextValue') {
      result.set(value.field.name, value.text ?? null);
    }
  }
  return result;
}

function projectItems(content, projectId) {
  return (content?.projectItems?.nodes || []).filter(item => item.project?.id === projectId);
}

function pullRequestReference(pullRequest) {
  return {
    number: pullRequest.number,
    branch: pullRequest.headRefName,
    head: String(pullRequest.headRefOid || '').toLowerCase(),
    mergeCommit: String(pullRequest.mergeCommit?.oid || '').toLowerCase(),
    mergedAt: pullRequest.mergedAt
  };
}

function selectProjectItem(content, projectId, description) {
  const items = projectItems(content, projectId);
  if (items.length === 0) return {item: null, reason: `${description} is not represented in Project 2.`};
  if (items.length > 1) return {item: null, reason: `${description} has ${items.length} Project 2 representations; completion requires explicit reconciliation.`};
  return {item: items[0], reason: null};
}

function selectCanonicalTarget(pullRequest, projectId) {
  const closingIssues = pullRequest.closingIssuesReferences?.nodes || [];
  const pullRequestRef = pullRequestReference(pullRequest);

  if (closingIssues.length === 1) {
    const issue = closingIssues[0];
    const selected = selectProjectItem(issue, projectId, `Canonical closing issue #${issue.number}`);
    if (!selected.item) return {target: null, reason: selected.reason};
    return {
      target: {type: 'Issue', number: issue.number, item: selected.item, pullRequest: pullRequestRef},
      reason: null
    };
  }

  const selected = selectProjectItem(
    pullRequest,
    projectId,
    closingIssues.length === 0
      ? `Canonical standalone pull request #${pullRequest.number}`
      : `Canonical multi-issue coordination pull request #${pullRequest.number}`
  );
  if (!selected.item) return {target: null, reason: selected.reason};
  return {
    target: {type: 'PullRequest', number: pullRequest.number, item: selected.item, pullRequest: pullRequestRef},
    reason: null
  };
}

function completionReference(target) {
  const pullRequest = target.pullRequest;
  return `Merged PR #${pullRequest.number} head ${pullRequest.head} as ${pullRequest.mergeCommit} at ${pullRequest.mergedAt}`;
}

function isSuppressed(item) {
  const current = values(item.fieldValues?.nodes);
  const workerReference = current.get('Worker reference') ?? null;
  return current.get('Status') === 'Draft' &&
    typeof workerReference === 'string' &&
    SUPPRESSED_DUPLICATE_PATTERN.test(workerReference.trim());
}

function commandFor(target) {
  const current = values(target.item.fieldValues?.nodes);
  const expectedFields = {};
  for (const field of GUARDED_FIELDS) expectedFields[field] = current.get(field) ?? null;
  const expected = {type: target.type, fields: expectedFields};
  if (target.type === 'PullRequest') expected.head = target.pullRequest.head;

  const reference = completionReference(target);
  const command = {
    command: 'delivery-control/v1',
    target: {repository: REPOSITORY, number: target.number},
    expected,
    set: {Status: 'Done', 'Worker reference': reference},
    clear: ['Execution', 'Executor'],
    addIfMissing: false
  };
  const alreadyCompleted = current.get('Status') === 'Done' &&
    (current.get('Execution') ?? null) === null &&
    (current.get('Executor') ?? null) === null &&
    (current.get('Worker reference') ?? null) === reference;
  return {alreadyCompleted, command, reference};
}

function skip(core, reason) {
  core.setOutput('should_transition', 'false');
  core.setOutput('reason', reason);
  core.summary.addHeading('AI delivery completion skipped').addRaw(`${reason}\n`);
  return {shouldTransition: false, reason};
}

async function prepareCompletion({github, context, core}) {
  const payload = context.payload || {};
  const eventPullRequest = payload.pull_request;
  if (context.eventName !== 'pull_request_target' || payload.action !== 'closed') {
    return skip(core, 'The event is not a closed pull_request_target event.');
  }
  if (!eventPullRequest?.merged) return skip(core, 'The pull request was closed without merging.');
  if (eventPullRequest.base?.ref !== BASE_BRANCH) return skip(core, `The pull request does not target ${BASE_BRANCH}.`);

  const [owner, name] = REPOSITORY.split('/');
  const result = await github.graphql(`query($org: String!, $project: Int!, $owner: String!, $name: String!, $number: Int!) {
    organization(login: $org) { projectV2(number: $project) { id } }
    repository(owner: $owner, name: $name) { pullRequest(number: $number) {
      id
      number
      state
      mergedAt
      mergeCommit { oid }
      headRefOid
      headRefName
      baseRefName
      projectItems(first: 50) { nodes { ...ProjectItem } }
      closingIssuesReferences(first: 50) { nodes {
        id
        number
        projectItems(first: 50) { nodes { ...ProjectItem } }
      } }
    } }
  }
  fragment ProjectItem on ProjectV2Item {
    id
    project { id }
    fieldValues(first: 100) { nodes {
      __typename
      ... on ProjectV2ItemFieldSingleSelectValue {
        name
        field { ... on ProjectV2SingleSelectField { name } }
      }
      ... on ProjectV2ItemFieldTextValue {
        text
        field { ... on ProjectV2Field { name } }
      }
    } }
  }`, {org: ORGANIZATION, project: PROJECT_NUMBER, owner, name, number: eventPullRequest.number});

  const project = result.organization?.projectV2;
  const pullRequest = result.repository?.pullRequest;
  if (!project || !pullRequest) throw new Error('Project 2 or the merged pull request was not found.');
  if (pullRequest.state !== 'MERGED' || !pullRequest.mergedAt || !pullRequest.mergeCommit?.oid) {
    return skip(core, 'The current pull request is not verified as merged.');
  }
  if (pullRequest.baseRefName !== BASE_BRANCH) return skip(core, `The current pull request no longer targets ${BASE_BRANCH}.`);

  const selection = selectCanonicalTarget(pullRequest, project.id);
  if (!selection.target) return skip(core, selection.reason);
  if (isSuppressed(selection.target.item)) {
    return skip(core, 'The selected Project item is an explicitly suppressed duplicate; completion must reconcile its canonical replacement instead.');
  }

  const target = selection.target;
  const prepared = commandFor(target);
  core.setOutput('should_transition', 'true');
  core.setOutput('payload_base64', Buffer.from(JSON.stringify(prepared.command)).toString('base64'));
  core.setOutput('target_repository', REPOSITORY.replace('/', '-'));
  core.setOutput('target_number', String(target.number));
  core.setOutput('target_type', target.type);
  core.setOutput('pull_request_number', String(target.pullRequest.number));
  core.setOutput('pull_request_head', target.pullRequest.head);
  core.setOutput('merge_commit', target.pullRequest.mergeCommit);
  core.summary.addHeading('AI delivery completion prepared')
    .addRaw(`Target: \`${target.type} ${REPOSITORY}#${target.number}\`\n\n`)
    .addRaw(`Merged pull request: \`#${target.pullRequest.number}\` at \`${target.pullRequest.head}\`\n\n`)
    .addCodeBlock(JSON.stringify(prepared.command, null, 2), 'json');
  if (prepared.alreadyCompleted) {
    core.summary.addRaw('Project completion is already recorded; assignee cleanup will still be verified.\n');
  }
  return {shouldTransition: true, target, ...prepared};
}

async function verifyMergedPullRequest({github, pullRequestNumber, expectedHead, expectedMergeCommit}) {
  const [owner, repo] = REPOSITORY.split('/');
  const response = await github.rest.pulls.get({owner, repo, pull_number: Number(pullRequestNumber)});
  const pullRequest = response.data || {};
  const actualHead = String(pullRequest.head?.sha || '').toLowerCase();
  const actualMergeCommit = String(pullRequest.merge_commit_sha || '').toLowerCase();
  if (!pullRequest.merged_at) throw new Error(`Pull request #${pullRequestNumber} is not merged.`);
  if (pullRequest.base?.ref !== BASE_BRANCH) throw new Error(`Pull request #${pullRequestNumber} does not target ${BASE_BRANCH}.`);
  if (actualHead !== String(expectedHead || '').toLowerCase()) {
    throw new Error(`Pull request #${pullRequestNumber} head is ${actualHead || '(missing)'}, not ${expectedHead || '(missing)'}.`);
  }
  if (actualMergeCommit !== String(expectedMergeCommit || '').toLowerCase()) {
    throw new Error(`Pull request #${pullRequestNumber} merge commit is ${actualMergeCommit || '(missing)'}, not ${expectedMergeCommit || '(missing)'}.`);
  }
  return {head: actualHead, mergeCommit: actualMergeCommit, mergedAt: pullRequest.merged_at};
}

async function clearAssigneesAndVerify({github, targetNumber}) {
  const [owner, repo] = REPOSITORY.split('/');
  const issueNumber = Number(targetNumber);
  await github.rest.issues.update({owner, repo, issue_number: issueNumber, assignees: []});
  const response = await github.rest.issues.get({owner, repo, issue_number: issueNumber});
  const remaining = (response.data?.assignees || []).map(assignee => assignee.login);
  if (remaining.length > 0) {
    throw new Error(`Failed to clear assignees from #${issueNumber}; still assigned: ${remaining.join(', ')}.`);
  }
}

module.exports = {
  clearAssigneesAndVerify,
  commandFor,
  completionReference,
  isSuppressed,
  prepareCompletion,
  projectItems,
  selectCanonicalTarget,
  values,
  verifyMergedPullRequest
};

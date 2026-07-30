'use strict';

const REPOSITORY = 'sniffy/sniffy';
const ORGANIZATION = 'sniffy';
const PROJECT_NUMBER = 2;
const BASE_BRANCH = 'develop';
const AGENT_EXECUTORS = new Set(['ChatGPT', 'Codex Cloud', 'Local Codex']);
const RETURNABLE_STATUSES = new Set(['Review', 'Approval']);
const GUARDED_FIELDS = ['Status', 'Execution', 'Implementer', 'Executor', 'Worker reference'];

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

function projectItem(content, projectId) {
  return content?.projectItems?.nodes?.find(item => item.project?.id === projectId) || null;
}

function representedTargets(pullRequest, projectId) {
  const targets = [];
  const pullRequestItem = projectItem(pullRequest, projectId);
  if (pullRequestItem) {
    targets.push({
      type: 'PullRequest',
      number: pullRequest.number,
      head: String(pullRequest.headRefOid || '').toLowerCase(),
      item: pullRequestItem
    });
  }
  for (const issue of pullRequest.closingIssuesReferences?.nodes || []) {
    const issueItem = projectItem(issue, projectId);
    if (issueItem) targets.push({type: 'Issue', number: issue.number, head: null, item: issueItem});
  }
  return targets;
}

function commandFor(target) {
  const current = values(target.item.fieldValues?.nodes);
  const status = current.get('Status') ?? null;
  const execution = current.get('Execution') ?? null;
  const implementer = current.get('Implementer') ?? null;
  const executor = current.get('Executor') ?? null;

  if (status === 'Implementation' && execution === 'Ready' && executor === implementer && AGENT_EXECUTORS.has(implementer)) {
    return {alreadyReturned: true, implementer, command: null};
  }
  if (!RETURNABLE_STATUSES.has(status)) {
    throw new Error(`Project item is ${status || '(clear)'}, not Review or Approval.`);
  }
  if (!AGENT_EXECUTORS.has(implementer)) {
    throw new Error(`Implementer "${implementer || '(clear)'}" is not an agent executor eligible for automatic continuation.`);
  }
  if (execution === 'Blocked') {
    throw new Error('Blocked work requires a maintainer decision and cannot be returned automatically.');
  }

  const expectedFields = {};
  for (const field of GUARDED_FIELDS) expectedFields[field] = current.get(field) ?? null;
  const expected = {type: target.type, fields: expectedFields};
  if (target.type === 'PullRequest') expected.head = target.head;

  return {
    alreadyReturned: false,
    implementer,
    command: {
      command: 'delivery-control/v1',
      target: {repository: REPOSITORY, number: target.number},
      expected,
      set: {
        Status: 'Implementation',
        Execution: 'Ready',
        Executor: implementer
      },
      clear: ['Worker reference'],
      addIfMissing: false
    }
  };
}

function skip(core, reason) {
  core.setOutput('should_transition', 'false');
  core.setOutput('reason', reason);
  core.summary.addHeading('Review return skipped').addRaw(`${reason}\n`);
  return {shouldTransition: false, reason};
}

async function prepareReviewReturn({github, context, core, authorizedActors}) {
  const payload = context.payload || {};
  const reviewer = payload.review?.user?.login || context.actor;
  if (!authorizedActors?.has(reviewer)) return skip(core, `Reviewer ${reviewer || '(unknown)'} is not authorized.`);
  if (payload.action !== 'submitted' || payload.review?.state !== 'changes_requested') {
    return skip(core, 'The event is not a submitted changes-requested review.');
  }

  const eventPullRequest = payload.pull_request;
  if (!eventPullRequest?.number) throw new Error('The review event does not contain a pull request number.');
  if (eventPullRequest.base?.ref !== BASE_BRANCH) return skip(core, `The pull request does not target ${BASE_BRANCH}.`);
  if (eventPullRequest.head?.repo?.full_name !== REPOSITORY) {
    return skip(core, 'Fork pull requests are excluded because correction dispatch must not use privileged secrets on untrusted heads.');
  }

  const [owner, name] = REPOSITORY.split('/');
  const result = await github.graphql(`query($org: String!, $project: Int!, $owner: String!, $name: String!, $number: Int!) {
    organization(login: $org) { projectV2(number: $project) { id } }
    repository(owner: $owner, name: $name) { pullRequest(number: $number) {
      id
      number
      headRefOid
      baseRefName
      isCrossRepository
      reviewDecision
      projectItems(first: 50) { nodes { ...ProjectItem } }
      closingIssuesReferences(first: 20) { nodes {
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
  if (!project || !pullRequest) throw new Error('Project 2 or the reviewed pull request was not found.');
  if (pullRequest.baseRefName !== BASE_BRANCH || pullRequest.isCrossRepository) {
    return skip(core, 'The current pull request is no longer an eligible same-repository develop PR.');
  }
  if (pullRequest.reviewDecision !== 'CHANGES_REQUESTED') {
    return skip(core, `The current aggregate review decision is ${pullRequest.reviewDecision || '(none)'}, not CHANGES_REQUESTED.`);
  }

  const targets = representedTargets(pullRequest, project.id);
  if (targets.length === 0) {
    return skip(core, 'Neither the pull request nor a formally linked closing issue is represented in Project 2.');
  }
  if (targets.length > 1) {
    throw new Error(`Found ${targets.length} represented work items. Keep exactly one Project work item for a pull request correction.`);
  }

  const target = targets[0];
  const prepared = commandFor(target);
  core.setOutput('target_repository', REPOSITORY.replace('/', '-'));
  core.setOutput('target_number', String(target.number));
  core.setOutput('target_type', target.type);
  core.setOutput('implementer', prepared.implementer);

  if (prepared.alreadyReturned) {
    return skip(core, `The ${target.type} #${target.number} is already Implementation / Ready for ${prepared.implementer}.`);
  }

  core.setOutput('should_transition', 'true');
  core.setOutput('payload_base64', Buffer.from(JSON.stringify(prepared.command)).toString('base64'));
  core.summary.addHeading('Review return prepared')
    .addRaw(`Reviewer: \`${reviewer}\`\n\nTarget: \`${target.type} ${REPOSITORY}#${target.number}\`\n\n`)
    .addRaw(`Return route: \`Implementation / Ready / ${prepared.implementer}\`\n\n`)
    .addCodeBlock(JSON.stringify(prepared.command, null, 2), 'json');
  return {shouldTransition: true, target, ...prepared};
}

module.exports = {AGENT_EXECUTORS, commandFor, prepareReviewReturn, representedTargets, values};

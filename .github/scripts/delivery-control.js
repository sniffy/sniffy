'use strict';

const VERSION = 'delivery-control/v1';
const CONTROL_LABEL = 'ai-delivery-control';
const REPOSITORY = 'sniffy/sniffy';
const ORGANIZATION = 'sniffy';
const PROJECT_NUMBER = 2;
const BASE_BRANCH = 'develop';
const DEFAULT_ACTORS = ['bedrin', 'bedrin-gpt', 'bedrin-codex-cloud', 'bedrin-codex-local'];
const COMMIT_ID = /^[0-9a-f]{40}([0-9a-f]{24})?$/;

function object(value, name) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${name} must be an object.`);
  }
  return value;
}

function fields(value, name, allowNull) {
  if (value === undefined) return {};
  object(value, name);
  const result = {};
  for (const [field, raw] of Object.entries(value)) {
    if (!field.trim()) throw new Error(`${name} contains an empty field name.`);
    if (raw === null && allowNull) result[field.trim()] = null;
    else if (typeof raw === 'string' && raw.trim()) result[field.trim()] = raw.trim();
    else throw new Error(`${name}.${field} must be a non-empty string${allowNull ? ' or null' : ''}.`);
  }
  return result;
}

function pullRequestReference(value, name) {
  if (value === undefined) return null;
  const reference = object(value, name);
  if (!Number.isInteger(reference.number) || reference.number < 1) {
    throw new Error(`${name}.number must be a positive integer.`);
  }
  const head = reference.head ? String(reference.head).toLowerCase() : '';
  if (!COMMIT_ID.test(head)) throw new Error(`${name}.head must be a 40- or 64-character hexadecimal commit ID.`);
  return {number: reference.number, head};
}

function parseCommand(input, repository = REPOSITORY) {
  let raw;
  try {
    raw = typeof input === 'string' ? JSON.parse(input) : input;
  } catch (error) {
    throw new Error(`Command must be valid JSON: ${error.message}`);
  }
  object(raw, 'command');
  if (raw.command !== VERSION) throw new Error(`command must equal "${VERSION}".`);
  const target = object(raw.target, 'target');
  if (target.repository !== repository) throw new Error(`target.repository must equal "${repository}".`);
  if (!Number.isInteger(target.number) || target.number < 1) throw new Error('target.number must be a positive integer.');

  const expectedRaw = raw.expected === undefined ? {} : object(raw.expected, 'expected');
  const expected = {
    type: expectedRaw.type ?? null,
    head: expectedRaw.head ? String(expectedRaw.head).toLowerCase() : null,
    fields: fields(expectedRaw.fields, 'expected.fields', true)
  };
  if (expected.type && !['Issue', 'PullRequest'].includes(expected.type)) {
    throw new Error('expected.type must be Issue or PullRequest.');
  }
  if (expected.head && !COMMIT_ID.test(expected.head)) {
    throw new Error('expected.head must be a 40- or 64-character hexadecimal commit ID.');
  }

  const reviewPullRequest = pullRequestReference(raw.reviewPullRequest, 'reviewPullRequest');
  const set = fields(raw.set, 'set', false);
  const clear = raw.clear === undefined ? [] : raw.clear;
  if (!Array.isArray(clear) || clear.some(value => typeof value !== 'string' || !value.trim())) {
    throw new Error('clear must be an array of non-empty field names.');
  }
  const normalizedClear = [...new Set(clear.map(value => value.trim()))];
  for (const field of normalizedClear) {
    if (Object.hasOwn(set, field)) throw new Error(`Field "${field}" cannot appear in both set and clear.`);
  }

  const addIfMissing = raw.addIfMissing === true;
  if (!Object.keys(set).length && !normalizedClear.length && !addIfMissing) {
    throw new Error('Command must set or clear a field, or set addIfMissing=true.');
  }
  if (!expected.type && !expected.head && !Object.keys(expected.fields).length) {
    throw new Error('Unguarded mutations are not allowed.');
  }
  if (set.Execution === 'In progress') {
    if (expected.fields.Execution !== 'Ready') throw new Error('A claim must guard Execution=Ready.');
    if (!expected.fields.Status || !expected.fields.Executor) {
      throw new Error('A claim must guard Status and Executor.');
    }
    if (!set['Worker reference']) throw new Error('A claim must set a unique Worker reference.');
    if (!expected.type) throw new Error('A claim must guard target type.');
    if (expected.type === 'PullRequest' && !expected.head) throw new Error('A pull-request claim must guard head.');
  }
  if (set.Status === 'Review') {
    if (!expected.type) throw new Error('A Review transition must guard target type.');
    if (expected.type === 'PullRequest') {
      if (!expected.head) throw new Error('A pull-request Review transition must guard head.');
      if (reviewPullRequest &&
          (reviewPullRequest.number !== target.number || reviewPullRequest.head !== expected.head)) {
        throw new Error('reviewPullRequest must match the target pull request and expected head.');
      }
    } else if (!reviewPullRequest) {
      throw new Error('An issue Review transition must identify reviewPullRequest.number and reviewPullRequest.head.');
    }
  } else if (reviewPullRequest) {
    throw new Error('reviewPullRequest is allowed only when setting Status=Review.');
  }

  return {
    command: VERSION,
    target: {repository: target.repository, number: target.number},
    expected,
    ...(reviewPullRequest ? {reviewPullRequest} : {}),
    set,
    clear: normalizedClear,
    addIfMissing
  };
}

function actorSet(environment = {}) {
  const actors = new Set(DEFAULT_ACTORS);
  for (const value of String(environment.AI_DELIVERY_COMMAND_ACTORS || '').split(',')) {
    if (value.trim()) actors.add(value.trim());
  }
  if (String(environment.PRODUCT_MANAGER_LOGIN || '').trim()) {
    actors.add(environment.PRODUCT_MANAGER_LOGIN.trim());
  }
  return actors;
}

function parseInvocation({context, core, dispatchCommand, environment = {}}) {
  const actor = context.actor || context.payload?.sender?.login;
  if (!actorSet(environment).has(actor)) throw new Error(`Actor "${actor || 'unknown'}" is not authorized.`);

  let input;
  let commentId = '';
  if (context.eventName === 'workflow_dispatch') input = dispatchCommand;
  else if (context.eventName === 'issue_comment') {
    const issue = context.payload?.issue;
    if (!issue || !context.payload?.comment) throw new Error('Incomplete issue_comment payload.');
    const labels = (issue.labels || []).map(label => typeof label === 'string' ? label : label.name);
    if (issue.pull_request || issue.state !== 'open' || !labels.includes(CONTROL_LABEL)) {
      throw new Error(`Commands must be posted to an open issue labeled "${CONTROL_LABEL}".`);
    }
    input = context.payload.comment.body;
    commentId = String(context.payload.comment.id);
  } else throw new Error(`Unsupported event "${context.eventName}".`);

  const command = parseCommand(input, `${context.repo.owner}/${context.repo.repo}`);
  core.setOutput('payload_base64', Buffer.from(JSON.stringify(command)).toString('base64'));
  core.setOutput('target_repository', command.target.repository.replace('/', '-'));
  core.setOutput('target_number', String(command.target.number));
  core.setOutput('control_comment_id', commentId);
  core.summary.addHeading('AI delivery control command')
    .addRaw(`Actor: \`${actor}\`\n\nTarget: \`${command.target.repository}#${command.target.number}\`\n\n`)
    .addCodeBlock(JSON.stringify(command, null, 2), 'json');
}

function definitions(nodes) {
  const map = new Map();
  for (const field of nodes || []) {
    if (field.__typename === 'ProjectV2SingleSelectField') {
      map.set(field.name, {id: field.id, type: 'select', options: new Map(field.options.map(o => [o.name, o.id]))});
    } else if (field.__typename === 'ProjectV2Field' && field.dataType === 'TEXT') {
      map.set(field.name, {id: field.id, type: 'text'});
    }
  }
  return map;
}

function values(nodes) {
  const map = new Map();
  for (const value of nodes || []) {
    if (!value.field?.name) continue;
    if (value.__typename === 'ProjectV2ItemFieldSingleSelectValue') map.set(value.field.name, value.name ?? null);
    if (value.__typename === 'ProjectV2ItemFieldTextValue') map.set(value.field.name, value.text ?? null);
  }
  return map;
}

function mismatch(command, target, current) {
  const result = [];
  if (command.expected.type && command.expected.type !== target.__typename) {
    result.push(['type', command.expected.type, target.__typename]);
  }
  const head = target.__typename === 'PullRequest' ? String(target.headRefOid || '').toLowerCase() : null;
  if (command.expected.head && command.expected.head !== head) result.push(['head', command.expected.head, head]);
  for (const [field, expected] of Object.entries(command.expected.fields)) {
    const actual = current.has(field) ? current.get(field) : null;
    if (actual !== expected) result.push([field, expected, actual]);
  }
  return result;
}

function desired(command, current) {
  for (const [field, expected] of Object.entries(command.set)) {
    if ((current.has(field) ? current.get(field) : null) !== expected) return false;
  }
  for (const field of command.clear) {
    if ((current.has(field) ? current.get(field) : null) !== null) return false;
  }
  return true;
}

function idempotentRepeat(command, mismatches, current) {
  if (!desired(command, current)) return false;
  const changedFields = new Set([...Object.keys(command.set), ...command.clear]);
  return mismatches.every(([field]) => changedFields.has(field));
}

async function projectItem(github, targetId, projectId) {
  const result = await github.graphql(`query($id: ID!) {
    node(id: $id) {
      ... on Issue { projectItems(first: 50) { nodes { ...ProjectItem } } }
      ... on PullRequest { projectItems(first: 50) { nodes { ...ProjectItem } } }
    }
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
  }`, {id: targetId});
  return result.node?.projectItems?.nodes?.find(item => item.project?.id === projectId) || null;
}

async function readPullRequest(github, owner, name, number) {
  const result = await github.graphql(`query($owner: String!, $name: String!, $number: Int!) {
    repository(owner: $owner, name: $name) { pullRequest(number: $number) {
      id
      number
      state
      isDraft
      baseRefName
      headRefOid
      closingIssuesReferences(first: 50) { nodes { number } }
    } }
  }`, {owner, name, number});
  return result.repository?.pullRequest || null;
}

function reviewReference(command) {
  if (command.set.Status !== 'Review') return null;
  return command.expected.type === 'PullRequest'
    ? {number: command.target.number, head: command.expected.head, canonicalIssue: null}
    : {...command.reviewPullRequest, canonicalIssue: command.target.number};
}

function reviewIdentityMismatches(reference, pullRequest) {
  if (!pullRequest) return [['reviewPullRequest', `#${reference.number}@${reference.head}`, '(not found)']];
  const rows = [];
  const actualHead = String(pullRequest.headRefOid || '').toLowerCase();
  if (pullRequest.state !== 'OPEN') rows.push(['reviewPullRequest.state', 'OPEN', pullRequest.state || '(unknown)']);
  if (pullRequest.baseRefName !== BASE_BRANCH) {
    rows.push(['reviewPullRequest.base', BASE_BRANCH, pullRequest.baseRefName || '(unknown)']);
  }
  if (actualHead !== reference.head) rows.push(['reviewPullRequest.head', reference.head, actualHead]);
  if (reference.canonicalIssue !== null) {
    const closing = new Set((pullRequest.closingIssuesReferences?.nodes || []).map(issue => issue.number));
    if (!closing.has(reference.canonicalIssue)) {
      rows.push(['reviewPullRequest.closes', `#${reference.canonicalIssue}`, '(not linked)']);
    }
  }
  return rows;
}

async function ensureReviewPullRequestReady({github, repositoryGithub = github, command, owner, name}) {
  const reference = reviewReference(command);
  if (!reference) return {changed: false, reference: null, pullRequest: null, conflicts: []};
  let pullRequest = await readPullRequest(github, owner, name, reference.number);
  const conflicts = reviewIdentityMismatches(reference, pullRequest);
  if (conflicts.length) return {changed: false, reference, pullRequest, conflicts};

  let changed = false;
  if (pullRequest.isDraft) {
    const result = await repositoryGithub.graphql(`mutation($id: ID!) {
      markPullRequestReadyForReview(input: {pullRequestId: $id}) {
        pullRequest { id number state isDraft baseRefName headRefOid }
      }
    }`, {id: pullRequest.id});
    const ready = result.markPullRequestReadyForReview?.pullRequest;
    if (!ready || ready.isDraft) throw new Error(`Pull request #${reference.number} remained draft after ready-for-review mutation.`);
    if (String(ready.headRefOid || '').toLowerCase() !== reference.head) {
      throw new Error(`Pull request #${reference.number} head changed while marking it ready for review.`);
    }
    changed = true;
  }

  pullRequest = await readPullRequest(github, owner, name, reference.number);
  const verified = reviewIdentityMismatches(reference, pullRequest);
  if (verified.length || pullRequest?.isDraft) {
    throw new Error(`Pull request #${reference.number} was not verified as open, non-draft, exact-head Review input.`);
  }
  return {changed, reference, pullRequest, conflicts: []};
}

async function conflict(core, rows, reason) {
  core.setOutput('outcome', 'conflict');
  core.setOutput('reason', reason);
  core.summary.addHeading('AI delivery control conflict');
  if (rows.length) core.summary.addTable([
    [{data: 'Key', header: true}, {data: 'Expected', header: true}, {data: 'Actual', header: true}],
    ...rows.map(([key, expected, actual]) => [key, String(expected ?? '(clear)'), String(actual ?? '(clear)')])
  ]);
  else core.summary.addRaw(`${reason}\n`);
  await core.summary.write();
  return {outcome: 'conflict'};
}

async function executeTransition({github, repositoryGithub = github, core, payloadBase64}) {
  const command = parseCommand(Buffer.from(payloadBase64, 'base64').toString('utf8'));
  const [owner, name] = command.target.repository.split('/');
  const result = await github.graphql(`query($org: String!, $project: Int!, $owner: String!, $name: String!, $number: Int!) {
    organization(login: $org) { projectV2(number: $project) {
      id
      fields(first: 100) { nodes {
        __typename
        ... on ProjectV2Field { id name dataType }
        ... on ProjectV2SingleSelectField { id name options { id name } }
      } }
    } }
    repository(owner: $owner, name: $name) { issueOrPullRequest(number: $number) {
      __typename
      ... on Issue { id }
      ... on PullRequest { id headRefOid }
    } }
  }`, {org: ORGANIZATION, project: PROJECT_NUMBER, owner, name, number: command.target.number});

  const project = result.organization?.projectV2;
  const target = result.repository?.issueOrPullRequest;
  if (!project || !target) throw new Error('Project or target was not found.');
  const targetOnly = mismatch(command, target, new Map()).filter(([key]) => key === 'type' || key === 'head');
  if (targetOnly.length) return conflict(core, targetOnly, 'Target identity changed.');

  const defs = definitions(project.fields.nodes);
  const operations = [];
  for (const [field, value] of Object.entries(command.set)) {
    const def = defs.get(field);
    if (!def) throw new Error(`Unsupported or missing Project field "${field}".`);
    if (def.type === 'select' && !def.options.has(value)) throw new Error(`Unknown option "${value}" for ${field}.`);
    operations.push({field, value, ...def});
  }
  for (const field of command.clear) {
    const def = defs.get(field);
    if (!def) throw new Error(`Unsupported or missing Project field "${field}".`);
    operations.push({field, value: null, ...def});
  }

  let item = await projectItem(github, target.id, project.id);
  if (!item && command.addIfMissing) {
    await github.graphql(`mutation($project: ID!, $target: ID!) {
      addProjectV2ItemById(input: {projectId: $project, contentId: $target}) { item { id } }
    }`, {project: project.id, target: target.id});
    item = await projectItem(github, target.id, project.id);
  }
  if (!item) return conflict(core, [], 'Target is not in Project 2 and addIfMissing is false.');

  const before = values(item.fieldValues.nodes);
  const fieldMismatch = mismatch(command, target, before).filter(([key]) => key !== 'type' && key !== 'head');
  if (fieldMismatch.length && !idempotentRepeat(command, fieldMismatch, before)) {
    return conflict(core, fieldMismatch, 'Expected field state changed.');
  }

  const review = await ensureReviewPullRequestReady({github, repositoryGithub, command, owner, name});
  if (review.conflicts.length) return conflict(core, review.conflicts, 'Review pull request identity changed.');

  for (const op of operations) {
    const current = before.has(op.field) ? before.get(op.field) : null;
    if (current === op.value) continue;
    if (op.value === null) {
      await github.graphql(`mutation($project: ID!, $item: ID!, $field: ID!) {
        clearProjectV2ItemFieldValue(input: {projectId: $project, itemId: $item, fieldId: $field}) {
          projectV2Item { id }
        }
      }`, {project: project.id, item: item.id, field: op.id});
    } else {
      const value = op.type === 'select' ? {singleSelectOptionId: op.options.get(op.value)} : {text: op.value};
      await github.graphql(`mutation($project: ID!, $item: ID!, $field: ID!, $value: ProjectV2FieldValue!) {
        updateProjectV2ItemFieldValue(input: {projectId: $project, itemId: $item, fieldId: $field, value: $value}) {
          projectV2Item { id }
        }
      }`, {project: project.id, item: item.id, field: op.id, value});
    }
  }

  const afterItem = await projectItem(github, target.id, project.id);
  const after = values(afterItem?.fieldValues?.nodes);
  if (!afterItem || !desired(command, after)) throw new Error('Post-mutation Project state did not match the command.');

  let finalReviewPullRequest = review.pullRequest;
  if (review.reference) {
    finalReviewPullRequest = await readPullRequest(github, owner, name, review.reference.number);
    const finalReviewMismatch = reviewIdentityMismatches(review.reference, finalReviewPullRequest);
    if (finalReviewMismatch.length || finalReviewPullRequest?.isDraft) {
      throw new Error(`Post-mutation pull request #${review.reference.number} was not open, non-draft, and at the guarded Review head.`);
    }
  }

  const projectChanged = !desired(command, before);
  core.setOutput('outcome', 'success');
  core.setOutput('reason', projectChanged || review.changed
    ? 'Transition applied and verified.'
    : 'Already in requested state.');
  core.summary.addHeading('AI delivery control success')
    .addRaw(`Target: \`${command.target.repository}#${command.target.number}\`\n\n`);
  if (finalReviewPullRequest) {
    core.summary.addRaw(`Review pull request: \`#${finalReviewPullRequest.number}\` at \`${String(finalReviewPullRequest.headRefOid).toLowerCase()}\`, non-draft${review.changed ? ' (marked ready by control plane)' : ''}.\n\n`);
  }
  core.summary.addTable([
    [{data: 'Field', header: true}, {data: 'Before', header: true}, {data: 'After', header: true}],
    ...operations.map(op => [op.field, String(before.get(op.field) ?? '(clear)'), String(after.get(op.field) ?? '(clear)')])
  ]);
  await core.summary.write();
  return {outcome: 'success'};
}

module.exports = {
  VERSION,
  CONTROL_LABEL,
  actorSet,
  desired,
  ensureReviewPullRequestReady,
  executeTransition,
  idempotentRepeat,
  mismatch,
  parseCommand,
  parseInvocation,
  reviewIdentityMismatches
};
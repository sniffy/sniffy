'use strict';

const fs = require('node:fs');

const SCHEMA_VERSION = 1;
const STATUS_KIND = 'ai-delivery-status/v1';
const STATUS_LABEL = 'ai-delivery-status';
const STATUS_TITLE = 'AI Delivery Status — Project 2';
const TECHNICAL_LABELS = new Set(['ai-delivery-control', STATUS_LABEL]);
const TERMINAL_PROJECT_STATUSES = new Set(['Done']);

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function lowerKeyMap(object) {
  return new Map(Object.entries(object || {}).map(([key, value]) => [key.toLowerCase(), value]));
}

function fieldValue(item, fieldName) {
  const values = lowerKeyMap(item);
  if (!values.has(String(fieldName).toLowerCase())) return null;
  const value = values.get(String(fieldName).toLowerCase());
  return value === undefined ? null : value;
}

function repositoryName(item) {
  const repository = item?.content?.repository ?? item?.repository ?? null;
  if (typeof repository === 'string') return repository;
  if (!repository || typeof repository !== 'object') return null;
  return repository.nameWithOwner ?? repository.fullName ?? repository.name ?? null;
}

function labelNames(item) {
  return asArray(item?.source?.labels ?? item?.labels ?? item?.content?.labels).flatMap(label => {
    if (typeof label === 'string') return [label];
    if (label && typeof label === 'object' && typeof label.name === 'string') return [label.name];
    return [];
  });
}

function contentType(item) {
  return String(item?.content?.type ?? item?.type ?? '').toUpperCase();
}

function isDraftIssueType(type) {
  const normalized = String(type ?? '').toUpperCase();
  return normalized === 'DRAFT_ISSUE' || normalized === 'DRAFTISSUE';
}

function contentState(item) {
  return String(item?.source?.state ?? item?.content?.state ?? item?.state ?? '').toUpperCase();
}

function isTechnicalItem(item) {
  return labelNames(item).some(label => TECHNICAL_LABELS.has(label));
}

function isActiveItem(item) {
  if (isTechnicalItem(item)) return false;

  const type = contentType(item);
  const state = contentState(item);
  const status = fieldValue(item, 'Status');

  if (isDraftIssueType(type)) return true;
  if (state === 'OPEN') return true;
  if (!state) return true;
  return !TERMINAL_PROJECT_STATUSES.has(status);
}

function normalizeField(field) {
  return {
    id: field?.id ?? null,
    name: field?.name ?? null,
    dataType: field?.type ?? field?.dataType ?? null,
    options: field?.options ?? null,
    configuration: field?.configuration ?? null
  };
}

function normalizeContent(item) {
  const content = item?.content ?? {};
  const source = item?.source ?? {};
  const type = content.type ?? item?.type ?? source.__typename ?? null;
  return {
    type,
    repository: repositoryName(item),
    number: content.number ?? item?.number ?? null,
    url: content.url ?? item?.url ?? null,
    title: source.title ?? content.title ?? item?.title ?? null,
    state: source.state ?? content.state ?? item?.state ?? null,
    stateReason: source.stateReason ?? null,
    isDraft: source.isDraft ?? content.isDraft ?? content.draft ?? item?.isDraft ?? item?.draft ?? false,
    mergedAt: source.mergedAt ?? null,
    closedAt: source.closedAt ?? null,
    headRefName: source.headRefName ?? null,
    headRefOid: source.headRefOid ?? null,
    baseRefName: source.baseRefName ?? null,
    isCrossRepository: source.isCrossRepository ?? null,
    author: source.author ?? content.author ?? item?.author ?? null,
    createdAt: source.createdAt ?? content.createdAt ?? item?.createdAt ?? null,
    updatedAt: source.updatedAt ?? content.updatedAt ?? item?.updatedAt ?? null,
    labels: labelNames(item)
  };
}

function normalizeItem(item, fields) {
  const normalizedFields = fields.map(field => ({
    fieldId: field.id,
    name: field.name,
    dataType: field.dataType,
    value: fieldValue(item, field.name)
  }));
  return {
    projectItemId: item?.id ?? null,
    content: normalizeContent(item),
    fields: Object.fromEntries(normalizedFields.map(field => [field.name, field.value])),
    fieldValues: normalizedFields
  };
}

function itemSortKey(item) {
  const content = item.content;
  return [
    content.repository ?? '',
    Number.isFinite(Number(content.number)) ? Number(content.number) : Number.MAX_SAFE_INTEGER,
    String(content.type ?? ''),
    content.title ?? '',
    item.projectItemId ?? ''
  ];
}

function compareKeys(left, right) {
  const a = itemSortKey(left);
  const b = itemSortKey(right);
  for (let index = 0; index < a.length; index += 1) {
    if (a[index] < b[index]) return -1;
    if (a[index] > b[index]) return 1;
  }
  return 0;
}

function verifyComplete(raw, collectionName) {
  const values = asArray(raw?.[collectionName]);
  const totalCount = raw?.totalCount;
  if (Number.isInteger(totalCount) && totalCount > values.length) {
    throw new Error(`${collectionName} export is incomplete: returned ${values.length} of ${totalCount}`);
  }
  return values;
}

function sourceIndex(values) {
  return new Map(asArray(values).map(value => [Number(value.number), value]));
}

function attachSource(item, repository, issuesByNumber, pullRequestsByNumber) {
  if (repositoryName(item) !== repository) return item;
  const type = contentType(item);
  const number = Number(item?.content?.number ?? item?.number);
  if (isDraftIssueType(type)) return item;
  const source = type === 'PULLREQUEST' || type === 'PULL_REQUEST'
    ? pullRequestsByNumber.get(number)
    : issuesByNumber.get(number);
  if (!source) {
    throw new Error(`Missing live repository state for ${type || 'item'} #${number}`);
  }
  return {...item, source};
}

function buildSnapshot({fieldsRaw, itemsRaw, issuesRaw = [], pullRequestsRaw = [], generatedAt, repository, projectOwner, projectNumber, workflowRun}) {
  const fields = verifyComplete(fieldsRaw, 'fields').map(normalizeField);
  const allItems = verifyComplete(itemsRaw, 'items');
  const issuesByNumber = sourceIndex(issuesRaw);
  const pullRequestsByNumber = sourceIndex(pullRequestsRaw);
  const enrichedItems = allItems.map(item => attachSource(item, repository, issuesByNumber, pullRequestsByNumber));
  const activeItems = enrichedItems
    .filter(isActiveItem)
    .map(item => normalizeItem(item, fields))
    .filter(item => !repository || item.content.repository === repository || isDraftIssueType(item.content.type))
    .sort(compareKeys);

  return {
    schemaVersion: SCHEMA_VERSION,
    kind: STATUS_KIND,
    generatedAt,
    source: {
      repository,
      project: {owner: projectOwner, number: Number(projectNumber)},
      workflowRun
    },
    semantics: {
      active: 'open source item, Project draft issue, or non-terminal closed/merged source item',
      terminalProjectStatuses: [...TERMINAL_PROJECT_STATUSES],
      excludedTechnicalLabels: [...TECHNICAL_LABELS]
    },
    counts: {
      fieldCount: fields.length,
      exportedItemCount: allItems.length,
      activeItemCount: activeItems.length
    },
    fields,
    items: activeItems
  };
}

function pointerPayload({generatedAt, repository, projectOwner, projectNumber, artifact, workflowRun, counts}) {
  const artifactId = Number(artifact.id);
  if (!Number.isSafeInteger(artifactId) || artifactId <= 0) {
    throw new Error(`Invalid artifact id: ${artifact.id ?? '<missing>'}`);
  }
  return {
    schemaVersion: SCHEMA_VERSION,
    kind: STATUS_KIND,
    generatedAt,
    source: {
      repository,
      project: {owner: projectOwner, number: Number(projectNumber)},
      workflowRun
    },
    artifact: {
      id: artifactId,
      name: artifact.name,
      url: artifact.url,
      digest: artifact.digest,
      snapshotFile: artifact.snapshotFile,
      rawFieldsFile: artifact.rawFieldsFile,
      rawItemsFile: artifact.rawItemsFile,
      rawIssuesFile: artifact.rawIssuesFile,
      rawPullRequestsFile: artifact.rawPullRequestsFile,
      retentionDays: Number(artifact.retentionDays)
    },
    counts
  };
}

async function ensureLabel({github, owner, repo, label = STATUS_LABEL}) {
  try {
    await github.rest.issues.getLabel({owner, repo, name: label});
  } catch (error) {
    if (error?.status !== 404) throw error;
    await github.rest.issues.createLabel({
      owner,
      repo,
      name: label,
      color: '1d76db',
      description: 'Machine-readable pointer to the latest AI delivery Project status artifact'
    });
  }
}

async function findOrCreateStatusIssue({github, owner, repo, label = STATUS_LABEL, title = STATUS_TITLE}) {
  await ensureLabel({github, owner, repo, label});
  const issues = await github.paginate(github.rest.issues.listForRepo, {
    owner,
    repo,
    labels: label,
    state: 'open',
    per_page: 100
  });
  const candidates = issues
    .filter(issue => !issue.pull_request)
    .sort((left, right) => right.number - left.number);
  if (candidates.length > 0) {
    return {issue: candidates[0], duplicateCount: candidates.length - 1, created: false};
  }
  const created = await github.rest.issues.create({
    owner,
    repo,
    title,
    labels: [label],
    body: JSON.stringify({schemaVersion: SCHEMA_VERSION, kind: STATUS_KIND, state: 'initializing'}, null, 2)
  });
  return {issue: created.data, duplicateCount: 0, created: true};
}

async function publishStatusPointer({github, context, core, payload, label = STATUS_LABEL, title = STATUS_TITLE}) {
  const {owner, repo} = context.repo;
  const selected = await findOrCreateStatusIssue({github, owner, repo, label, title});
  const body = `${JSON.stringify(payload, null, 2)}\n`;
  const updated = await github.rest.issues.update({
    owner,
    repo,
    issue_number: selected.issue.number,
    title,
    body,
    labels: [label]
  });

  core.setOutput('status_issue_number', String(updated.data.number));
  core.setOutput('status_issue_url', updated.data.html_url);
  core.summary
    .addHeading('AI delivery status pointer')
    .addRaw(`Published artifact ${payload.artifact.id} to issue #${updated.data.number}.\n`);
  if (selected.duplicateCount > 0) {
    core.warning(`Found ${selected.duplicateCount + 1} open issues labeled ${label}; updated newest #${updated.data.number}.`);
    core.summary.addRaw(`Warning: ${selected.duplicateCount} older open labeled issue(s) remain.\n`);
  }
  await core.summary.write();
  return {issue: updated.data, ...selected};
}

function parseArguments(argv) {
  const result = {};
  for (let index = 0; index < argv.length; index += 2) {
    const key = argv[index];
    const value = argv[index + 1];
    if (!key?.startsWith('--') || value === undefined) throw new Error(`Invalid argument sequence near ${key ?? '<end>'}`);
    result[key.slice(2)] = value;
  }
  return result;
}

function normalizeCommand(argv) {
  const options = parseArguments(argv);
  for (const required of ['fields', 'items', 'issues', 'pull-requests', 'output', 'generated-at', 'repository', 'project-owner', 'project-number', 'workflow-run-id']) {
    if (!options[required]) throw new Error(`Missing --${required}`);
  }
  const fieldsRaw = JSON.parse(fs.readFileSync(options.fields, 'utf8'));
  const itemsRaw = JSON.parse(fs.readFileSync(options.items, 'utf8'));
  const issuesRaw = JSON.parse(fs.readFileSync(options.issues, 'utf8'));
  const pullRequestsRaw = JSON.parse(fs.readFileSync(options['pull-requests'], 'utf8'));
  const snapshot = buildSnapshot({
    fieldsRaw,
    itemsRaw,
    issuesRaw,
    pullRequestsRaw,
    generatedAt: options['generated-at'],
    repository: options.repository,
    projectOwner: options['project-owner'],
    projectNumber: options['project-number'],
    workflowRun: {
      id: Number(options['workflow-run-id']),
      attempt: Number(options['workflow-run-attempt'] ?? 1),
      event: options.event ?? null,
      upstreamRunId: options['upstream-run-id'] ? Number(options['upstream-run-id']) : null,
      headSha: options['head-sha'] ?? null
    }
  });
  fs.writeFileSync(options.output, `${JSON.stringify(snapshot, null, 2)}\n`);
  process.stdout.write(`${JSON.stringify(snapshot.counts)}\n`);
}

if (require.main === module) {
  try {
    const [command, ...argv] = process.argv.slice(2);
    if (command !== 'normalize') throw new Error(`Unknown command: ${command ?? '<missing>'}`);
    normalizeCommand(argv);
  } catch (error) {
    process.stderr.write(`${error.stack || error.message}\n`);
    process.exitCode = 1;
  }
}

module.exports = {
  SCHEMA_VERSION,
  STATUS_KIND,
  STATUS_LABEL,
  STATUS_TITLE,
  buildSnapshot,
  ensureLabel,
  fieldValue,
  findOrCreateStatusIssue,
  isActiveItem,
  normalizeItem,
  pointerPayload,
  publishStatusPointer
};

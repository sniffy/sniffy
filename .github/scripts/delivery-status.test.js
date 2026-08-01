'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  buildSnapshot,
  findOrCreateStatusIssue,
  pointerPayload,
  publishStatusPointer
} = require('./delivery-status');

const fieldsRaw = {
  totalCount: 4,
  fields: [
    {id: 'TITLE', name: 'Title', type: 'TITLE'},
    {id: 'STATUS', name: 'Status', type: 'SINGLE_SELECT', options: [{id: 'DONE', name: 'Done'}]},
    {id: 'EXECUTION', name: 'Execution', type: 'SINGLE_SELECT'},
    {id: 'WORKER', name: 'Worker reference', type: 'TEXT'}
  ]
};

function item(number, {state = 'OPEN', status = 'Planning', type = 'Issue', labels = [], execution, worker} = {}) {
  return {
    id: `ITEM-${number}`,
    title: `Item ${number}`,
    status,
    execution,
    'worker reference': worker,
    labels: labels.map(name => ({name})),
    content: {
      type,
      number,
      state,
      repository: 'sniffy/sniffy',
      url: `https://github.com/sniffy/sniffy/issues/${number}`,
      title: `Item ${number}`
    }
  };
}

function snapshot(items) {
  const issuesRaw = [];
  const pullRequestsRaw = [];
  const projectItems = items.map(value => {
    const copy = structuredClone(value);
    const source = {
      number: copy.content.number,
      state: copy.content.state,
      title: copy.content.title,
      url: copy.content.url,
      labels: copy.labels
    };
    delete copy.content.state;
    if (String(copy.content.type).toLowerCase() === 'pullrequest') {
      source.isDraft = copy.content.isDraft ?? false;
      source.headRefName = 'agent/example';
      source.headRefOid = '0123456789abcdef0123456789abcdef01234567';
      source.baseRefName = 'develop';
      source.isCrossRepository = false;
      pullRequestsRaw.push(source);
    } else if (String(copy.content.type).toLowerCase() !== 'draftissue') {
      issuesRaw.push(source);
    }
    return copy;
  });
  return buildSnapshot({
    fieldsRaw,
    itemsRaw: {totalCount: projectItems.length, items: projectItems},
    issuesRaw,
    pullRequestsRaw,
    generatedAt: '2026-08-01T00:00:00Z',
    repository: 'sniffy/sniffy',
    projectOwner: 'sniffy',
    projectNumber: 2,
    workflowRun: {id: 100, attempt: 1, event: 'schedule'}
  });
}

test('normalizes every Project field and uses null for absent values', () => {
  const result = snapshot([item(770, {execution: 'Ready'})]);
  assert.deepEqual(result.items[0].fields, {
    Title: 'Item 770',
    Status: 'Planning',
    Execution: 'Ready',
    'Worker reference': null
  });
  assert.equal(result.items[0].fieldValues[1].fieldId, 'STATUS');
});

test('uses live repository metadata for pull-request state and exact head', () => {
  const result = snapshot([item(11, {type: 'PullRequest'})]);
  assert.equal(result.items[0].content.state, 'OPEN');
  assert.equal(result.items[0].content.isDraft, false);
  assert.equal(result.items[0].content.number, 11);
  assert.equal(result.items[0].content.headRefName, 'agent/example');
  assert.equal(result.items[0].content.headRefOid, '0123456789abcdef0123456789abcdef01234567');
  assert.equal(result.items[0].content.baseRefName, 'develop');
});

test('keeps source and lifecycle drift while dropping only Done closed items', () => {
  const result = snapshot([
    item(1, {state: 'OPEN', status: 'Done'}),
    item(2, {state: 'CLOSED', status: 'Review'}),
    item(3, {state: 'CLOSED', status: 'Done'}),
    item(4, {state: 'CLOSED', status: 'Draft'}),
    item(5, {state: '', status: null, type: 'DraftIssue'})
  ]);
  assert.deepEqual(result.items.map(value => value.content.number), [1, 2, 4, 5]);
  assert.deepEqual(result.semantics.terminalProjectStatuses, ['Done']);
});

test('excludes control and status issues from active work', () => {
  const result = snapshot([
    item(10, {labels: ['ai-delivery-control']}),
    item(11, {labels: ['ai-delivery-status']}),
    item(12)
  ]);
  assert.deepEqual(result.items.map(value => value.content.number), [12]);
});

test('orders items deterministically by repository, number, type and title', () => {
  const result = snapshot([item(20), item(3), item(11, {type: 'PullRequest'})]);
  assert.deepEqual(result.items.map(value => value.content.number), [3, 11, 20]);
});

test('fails closed when gh pagination output is incomplete', () => {
  assert.throws(() => buildSnapshot({
    fieldsRaw,
    itemsRaw: {totalCount: 2, items: [item(1)]},
    generatedAt: '2026-08-01T00:00:00Z',
    repository: 'sniffy/sniffy',
    projectOwner: 'sniffy',
    projectNumber: 2,
    workflowRun: {id: 100}
  }), /items export is incomplete/);
});

test('fails closed when a represented issue has no live repository state', () => {
  assert.throws(() => buildSnapshot({
    fieldsRaw,
    itemsRaw: {totalCount: 1, items: [item(1)]},
    issuesRaw: [],
    pullRequestsRaw: [],
    generatedAt: '2026-08-01T00:00:00Z',
    repository: 'sniffy/sniffy',
    projectOwner: 'sniffy',
    projectNumber: 2,
    workflowRun: {id: 100}
  }), /Missing live repository state for ISSUE #1/);
});

test('builds a machine-readable artifact pointer', () => {
  const payload = pointerPayload({
    generatedAt: '2026-08-01T00:00:00Z',
    repository: 'sniffy/sniffy',
    projectOwner: 'sniffy',
    projectNumber: 2,
    workflowRun: {id: 101, attempt: 1, event: 'schedule'},
    artifact: {
      id: '55',
      name: 'ai-delivery-status-project-2',
      url: 'https://github.com/example/artifacts/55',
      digest: 'sha256:abc',
      snapshotFile: 'project-2-status.json',
      rawFieldsFile: 'project-fields.raw.json',
      rawItemsFile: 'project-items.raw.json',
      rawIssuesFile: 'repository-issues.raw.json',
      rawPullRequestsFile: 'repository-pull-requests.raw.json',
      retentionDays: '2'
    },
    counts: {activeItemCount: 1}
  });
  assert.equal(payload.artifact.id, 55);
  assert.equal(payload.source.project.number, 2);
  assert.equal(payload.artifact.snapshotFile, 'project-2-status.json');
  assert.equal(payload.artifact.rawIssuesFile, 'repository-issues.raw.json');
  assert.equal(payload.artifact.rawPullRequestsFile, 'repository-pull-requests.raw.json');
});

test('rejects a missing or nonnumeric artifact id before publishing a pointer', () => {
  const common = {
    generatedAt: '2026-08-01T00:00:00Z',
    repository: 'sniffy/sniffy',
    projectOwner: 'sniffy',
    projectNumber: 2,
    workflowRun: {id: 101},
    counts: {}
  };
  assert.throws(() => pointerPayload({...common, artifact: {id: ''}}), /Invalid artifact id/);
  assert.throws(() => pointerPayload({...common, artifact: {id: 'not-a-number'}}), /Invalid artifact id/);
});

function githubDouble({issues = [], labelExists = true} = {}) {
  const calls = [];
  const issueApi = {
    async getLabel() {
      calls.push(['getLabel']);
      if (!labelExists) { const error = new Error('missing'); error.status = 404; throw error; }
      return {data: {name: 'ai-delivery-status'}};
    },
    async createLabel(input) { calls.push(['createLabel', input]); return {data: input}; },
    async listForRepo() { throw new Error('paginate supplies results'); },
    async create(input) { calls.push(['create', input]); return {data: {number: 99, html_url: 'https://example/99', ...input}}; },
    async update(input) { calls.push(['update', input]); return {data: {number: input.issue_number, html_url: `https://example/${input.issue_number}`}}; }
  };
  return {
    calls,
    github: {
      rest: {issues: issueApi},
      async paginate() { return issues; }
    }
  };
}

test('creates the label and status issue when none exists', async () => {
  const {github, calls} = githubDouble({labelExists: false});
  const result = await findOrCreateStatusIssue({github, owner: 'sniffy', repo: 'sniffy'});
  assert.equal(result.created, true);
  assert.equal(result.issue.number, 99);
  assert.ok(calls.some(([name]) => name === 'createLabel'));
  assert.ok(calls.some(([name]) => name === 'create'));
});

test('selects the newest open labeled issue and reports duplicates', async () => {
  const {github} = githubDouble({issues: [{number: 7}, {number: 9}, {number: 8, pull_request: {}}]});
  const result = await findOrCreateStatusIssue({github, owner: 'sniffy', repo: 'sniffy'});
  assert.equal(result.issue.number, 9);
  assert.equal(result.duplicateCount, 1);
});

test('publishes pure JSON to the selected issue body', async () => {
  const {github, calls} = githubDouble({issues: [{number: 12}]});
  const outputs = {};
  const core = {
    summary: {addHeading() { return this; }, addRaw() { return this; }, async write() {}},
    setOutput(name, value) { outputs[name] = value; },
    warning() {}
  };
  const payload = {kind: 'ai-delivery-status/v1', artifact: {id: 55}};
  await publishStatusPointer({github, context: {repo: {owner: 'sniffy', repo: 'sniffy'}}, core, payload});
  const update = calls.find(([name]) => name === 'update')[1];
  assert.deepEqual(JSON.parse(update.body), payload);
  assert.deepEqual(update.labels, ['ai-delivery-status']);
  assert.equal(outputs.status_issue_number, '12');
});

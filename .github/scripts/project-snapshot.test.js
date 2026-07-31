'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  LIFECYCLE_FIELDS,
  SNAPSHOT_SCHEMA,
  parseItemNumber,
  parseRepository,
  readProjectSnapshot,
  revisionFor,
  stableStringify
} = require('./project-snapshot');

function value(name, selected) {
  return {
    __typename: 'ProjectV2ItemFieldSingleSelectValue',
    name: selected,
    field: {name}
  };
}

function text(name, content) {
  return {
    __typename: 'ProjectV2ItemFieldTextValue',
    text: content,
    field: {name}
  };
}

function fixture({type = 'Issue', item = true, target = true, project = true} = {}) {
  const targetNode = type === 'PullRequest'
    ? {
        __typename: 'PullRequest',
        id: 'PR',
        number: 770,
        title: 'Snapshot PR',
        url: 'https://github.com/sniffy/sniffy/pull/770',
        state: 'OPEN',
        isDraft: false,
        baseRefName: 'develop',
        headRefOid: '0123456789012345678901234567890123456789'
      }
    : {
        __typename: 'Issue',
        id: 'ISSUE',
        number: 770,
        title: 'Snapshot issue',
        url: 'https://github.com/sniffy/sniffy/issues/770',
        state: 'OPEN'
      };

  if (targetNode && item) {
    targetNode.projectItems = {
      nodes: [{
        id: 'ITEM',
        project: {id: 'PROJECT'},
        fieldValues: {
          nodes: [
            value('Status', 'Planning'),
            value('Execution', 'Ready'),
            value('Verifier', 'ChatGPT'),
            text('Worker reference', 'PR #759; exact head abc')
          ]
        }
      }]
    };
  } else if (targetNode) {
    targetNode.projectItems = {nodes: []};
  }

  const calls = [];
  return {
    calls,
    github: {
      async graphql(query, variables) {
        calls.push({query, variables});
        return {
          organization: {projectV2: project ? {id: 'PROJECT', number: 2, title: 'AI Delivery'} : null},
          repository: {issueOrPullRequest: target ? targetNode : null}
        };
      }
    }
  };
}

test('validates repository and item number inputs', () => {
  assert.deepEqual(parseRepository('sniffy/sniffy'), {
    repository: 'sniffy/sniffy',
    owner: 'sniffy',
    name: 'sniffy'
  });
  assert.equal(parseItemNumber('770'), 770);
  assert.throws(() => parseRepository('sniffy'), /owner\/name/);
  assert.throws(() => parseItemNumber('0'), /positive integer/);
});

test('stable serialization and revision ignore object insertion order', () => {
  assert.equal(stableStringify({b: 2, a: {d: 4, c: 3}}), stableStringify({a: {c: 3, d: 4}, b: 2}));
  assert.equal(revisionFor({b: 2, a: 1}), revisionFor({a: 1, b: 2}));
});

test('reads an issue snapshot with explicit null lifecycle fields', async () => {
  const source = fixture();
  const snapshot = await readProjectSnapshot({
    github: source.github,
    repository: 'sniffy/sniffy',
    number: 770,
    observedAt: '2026-07-31T22:00:00.000Z'
  });

  assert.equal(snapshot.schema, SNAPSHOT_SCHEMA);
  assert.equal(snapshot.project.owner, 'sniffy');
  assert.equal(snapshot.project.number, 2);
  assert.equal(snapshot.target.type, 'Issue');
  assert.equal(snapshot.target.number, 770);
  assert.equal(snapshot.fields.Status, 'Planning');
  assert.equal(snapshot.fields.Execution, 'Ready');
  assert.equal(snapshot.fields.Executor, null);
  assert.equal(snapshot.fields.Implementer, null);
  assert.equal(snapshot.fields.Verifier, 'ChatGPT');
  assert.equal(snapshot.fields['Worker reference'], 'PR #759; exact head abc');
  assert.deepEqual(Object.keys(snapshot.fields), [...LIFECYCLE_FIELDS]);
  assert.match(snapshot.revision, /^sha256:[0-9a-f]{64}$/);
  assert.deepEqual(source.calls[0].variables, {
    organization: 'sniffy',
    projectNumber: 2,
    owner: 'sniffy',
    name: 'sniffy',
    number: 770
  });
});

test('includes exact pull-request publication identity', async () => {
  const snapshot = await readProjectSnapshot({
    github: fixture({type: 'PullRequest'}).github,
    repository: 'sniffy/sniffy',
    number: 770,
    observedAt: '2026-07-31T22:00:00.000Z'
  });
  assert.equal(snapshot.target.type, 'PullRequest');
  assert.equal(snapshot.target.head, '0123456789012345678901234567890123456789');
  assert.equal(snapshot.target.base, 'develop');
  assert.equal(snapshot.target.draft, false);
});

test('revision is stable across observation times', async () => {
  const first = await readProjectSnapshot({
    github: fixture().github,
    repository: 'sniffy/sniffy',
    number: 770,
    observedAt: '2026-07-31T22:00:00.000Z'
  });
  const second = await readProjectSnapshot({
    github: fixture().github,
    repository: 'sniffy/sniffy',
    number: 770,
    observedAt: '2026-07-31T22:01:00.000Z'
  });
  assert.equal(first.revision, second.revision);
  assert.notEqual(first.observedAt, second.observedAt);
});

test('fails rather than publishing an inaccessible or partial snapshot', async () => {
  await assert.rejects(() => readProjectSnapshot({
    github: fixture({project: false}).github,
    repository: 'sniffy/sniffy',
    number: 770
  }), /Project sniffy\/2/);
  await assert.rejects(() => readProjectSnapshot({
    github: fixture({target: false}).github,
    repository: 'sniffy/sniffy',
    number: 770
  }), /#770 was not found/);
  await assert.rejects(() => readProjectSnapshot({
    github: fixture({item: false}).github,
    repository: 'sniffy/sniffy',
    number: 770
  }), /not represented/);
});

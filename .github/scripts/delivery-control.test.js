'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const {
  CONTROL_LABEL,
  desired,
  executeTransition,
  idempotentRepeat,
  mismatch,
  parseCommand,
  parseInvocation
} = require('./delivery-control');

const head = '46c47e02e8a5236cf1e7348fc6202da11ea1bbd3';
const claim = {
  command: 'delivery-control/v1',
  target: {repository: 'sniffy/sniffy', number: 651},
  expected: {
    type: 'PullRequest',
    head,
    fields: {Status: 'Review', Execution: 'Ready', Executor: 'ChatGPT'}
  },
  set: {
    Execution: 'In progress',
    Executor: 'ChatGPT',
    'Worker reference': 'tick-00; token x; lease 2026-07-30T10:00:00Z'
  }
};

function coreDouble() {
  const outputs = {};
  const summary = {
    addHeading() { return this; },
    addRaw() { return this; },
    addCodeBlock() { return this; },
    addTable() { return this; },
    async write() {}
  };
  return {
    outputs,
    summary,
    setOutput(name, value) { outputs[name] = String(value); }
  };
}

function projectDouble({initial, freezeWrites = false}) {
  const current = new Map(Object.entries(initial));
  const mutations = [];
  const fields = [
    selectField('STATUS', 'Status', ['Planning', 'Implementation', 'Review', 'Verification', 'Approval']),
    selectField('EXECUTION', 'Execution', ['Ready', 'In progress', 'Blocked']),
    selectField('EXECUTOR', 'Executor', ['ChatGPT', 'Codex Cloud', 'Local Codex', 'Human']),
    textField('WORKER', 'Worker reference'),
    textField('CLAIM', 'Claim token')
  ];
  const definitions = new Map(fields.map(field => [field.id, field]));
  const optionValues = new Map();
  for (const field of fields) {
    for (const option of field.options || []) optionValues.set(option.id, option.name);
  }

  function item() {
    return {
      id: 'ITEM',
      project: {id: 'PROJECT'},
      fieldValues: {nodes: [...current.entries()].flatMap(([name, value]) => {
        if (value === null || value === undefined) return [];
        const field = fields.find(candidate => candidate.name === name);
        if (!field) throw new Error(`Missing fixture field ${name}`);
        return field.__typename === 'ProjectV2SingleSelectField'
          ? [{__typename: 'ProjectV2ItemFieldSingleSelectValue', name: value, field: {name}}]
          : [{__typename: 'ProjectV2ItemFieldTextValue', text: value, field: {name}}];
      })}
    };
  }

  const github = {
    async graphql(query, variables) {
      if (query.includes('organization(login:')) {
        return {
          organization: {projectV2: {id: 'PROJECT', fields: {nodes: fields}}},
          repository: {issueOrPullRequest: {__typename: 'PullRequest', id: 'TARGET', headRefOid: head}}
        };
      }
      if (query.includes('projectItems(first: 50)')) {
        return {node: {projectItems: {nodes: [item()]}}};
      }
      if (query.includes('updateProjectV2ItemFieldValue')) {
        const field = definitions.get(variables.field);
        const value = variables.value.text ?? optionValues.get(variables.value.singleSelectOptionId);
        mutations.push(['set', field.name, value]);
        if (!freezeWrites) current.set(field.name, value);
        return {updateProjectV2ItemFieldValue: {projectV2Item: {id: 'ITEM'}}};
      }
      if (query.includes('clearProjectV2ItemFieldValue')) {
        const field = definitions.get(variables.field);
        mutations.push(['clear', field.name, null]);
        if (!freezeWrites) current.delete(field.name);
        return {clearProjectV2ItemFieldValue: {projectV2Item: {id: 'ITEM'}}};
      }
      throw new Error(`Unexpected GraphQL operation: ${query.slice(0, 80)}`);
    }
  };
  return {current, github, mutations};
}

function selectField(id, name, names) {
  return {
    __typename: 'ProjectV2SingleSelectField',
    id,
    name,
    options: names.map((option, index) => ({id: `${id}-${index}`, name: option}))
  };
}

function textField(id, name) {
  return {__typename: 'ProjectV2Field', dataType: 'TEXT', id, name};
}

function encoded(command) {
  return Buffer.from(JSON.stringify(command)).toString('base64');
}

function labeledIssue() {
  return {state: 'open', labels: [{name: CONTROL_LABEL}]};
}

test('parses a fully guarded claim', () => {
  assert.deepEqual(parseCommand(JSON.stringify(claim)), {...claim, clear: [], addIfMissing: false});
});

test('rejects unguarded transitions', () => {
  assert.throws(() => parseCommand({
    command: 'delivery-control/v1',
    target: {repository: 'sniffy/sniffy', number: 1},
    set: {Status: 'Review'}
  }), /Unguarded mutations/);
});

test('requires claim ownership guards', () => {
  const command = structuredClone(claim);
  delete command.expected.fields.Executor;
  assert.throws(() => parseCommand(command), /guard Status and Executor/);
});

test('rejects set and clear overlap', () => {
  const command = structuredClone(claim);
  command.clear = ['Executor'];
  assert.throws(() => parseCommand(command), /both set and clear/);
});

test('detects stale fields and changed head', () => {
  const command = parseCommand(claim);
  const current = new Map([['Status', 'Review'], ['Execution', 'In progress'], ['Executor', 'ChatGPT']]);
  assert.deepEqual(mismatch(command, {__typename: 'PullRequest', headRefOid: 'deadbeef'}, current), [
    ['head', claim.expected.head, 'deadbeef'],
    ['Execution', 'Ready', 'In progress']
  ]);
});

test('recognizes an idempotent repeated command', () => {
  const command = parseCommand(claim);
  const current = new Map(Object.entries(command.set));
  assert.equal(desired(command, current), true);
});

test('accepts an exact idempotent repeat when only changed guards moved', () => {
  const command = parseCommand(claim);
  const current = new Map(Object.entries(command.set));
  const mismatches = [
    ['Execution', 'Ready', 'In progress'],
    ['Worker reference', null, command.set['Worker reference']]
  ];
  assert.equal(idempotentRepeat(command, mismatches, current), true);
});

test('rejects idempotency when an unchanged guard moved', () => {
  const command = parseCommand(claim);
  const current = new Map([
    ...Object.entries(command.set),
    ['Status', 'Planning']
  ]);
  assert.equal(idempotentRepeat(command, [['Status', 'Review', 'Planning']], current), false);
});

test('authorizes a labeled control-issue command and emits routing outputs', () => {
  const core = coreDouble();
  parseInvocation({
    context: {
      actor: 'bedrin-gpt',
      eventName: 'issue_comment',
      repo: {owner: 'sniffy', repo: 'sniffy'},
      payload: {
        issue: labeledIssue(),
        comment: {id: 123, body: JSON.stringify(claim)}
      }
    },
    core
  });
  assert.equal(core.outputs.target_repository, 'sniffy-sniffy');
  assert.equal(core.outputs.target_number, '651');
  assert.equal(core.outputs.control_comment_id, '123');
  assert.deepEqual(JSON.parse(Buffer.from(core.outputs.payload_base64, 'base64')), parseCommand(claim));
});

test('rejects an unlabeled control issue', () => {
  const context = {
    actor: 'bedrin-gpt',
    eventName: 'issue_comment',
    repo: {owner: 'sniffy', repo: 'sniffy'},
    payload: {issue: {state: 'open', labels: []}, comment: {id: 123, body: JSON.stringify(claim)}}
  };
  assert.throws(() => parseInvocation({context, core: coreDouble()}), /labeled "ai-delivery-control"/);
});

test('rejects unauthorized and malformed control commands before transition', () => {
  const context = {
    actor: 'external-user',
    eventName: 'issue_comment',
    repo: {owner: 'sniffy', repo: 'sniffy'},
    payload: {issue: labeledIssue(), comment: {id: 123, body: JSON.stringify(claim)}}
  };
  assert.throws(() => parseInvocation({context, core: coreDouble()}), /not authorized/);
  context.actor = 'bedrin-gpt';
  context.payload.comment.body = '{';
  assert.throws(() => parseInvocation({context, core: coreDouble()}), /valid JSON/);
});

test('applies and verifies one complete multi-field transition', async () => {
  const transition = {
    command: 'delivery-control/v1',
    target: {repository: 'sniffy/sniffy', number: 748},
    expected: {
      type: 'PullRequest',
      head,
      fields: {
        Status: 'Implementation',
        Execution: 'In progress',
        Executor: 'ChatGPT',
        'Worker reference': 'active worker',
        'Claim token': 'claim-1'
      }
    },
    set: {
      Status: 'Review',
      Execution: 'Ready',
      Executor: 'ChatGPT',
      'Worker reference': 'published head and CI'
    },
    clear: ['Claim token']
  };
  const fixture = projectDouble({initial: transition.expected.fields});
  const core = coreDouble();
  const result = await executeTransition({github: fixture.github, core, payloadBase64: encoded(transition)});
  assert.equal(result.outcome, 'success');
  assert.equal(core.outputs.outcome, 'success');
  assert.equal(fixture.current.get('Status'), 'Review');
  assert.equal(fixture.current.get('Execution'), 'Ready');
  assert.equal(fixture.current.get('Worker reference'), 'published head and CI');
  assert.equal(fixture.current.has('Claim token'), false);
  assert.deepEqual(fixture.mutations, [
    ['set', 'Status', 'Review'],
    ['set', 'Execution', 'Ready'],
    ['set', 'Worker reference', 'published head and CI'],
    ['clear', 'Claim token', null]
  ]);
});

test('returns a non-mutating conflict for a losing claim', async () => {
  const fixture = projectDouble({
    initial: {
      Status: 'Review',
      Execution: 'In progress',
      Executor: 'ChatGPT',
      'Worker reference': 'winning claim'
    }
  });
  const core = coreDouble();
  const result = await executeTransition({github: fixture.github, core, payloadBase64: encoded(claim)});
  assert.equal(result.outcome, 'conflict');
  assert.equal(core.outputs.outcome, 'conflict');
  assert.deepEqual(fixture.mutations, []);
  assert.equal(fixture.current.get('Worker reference'), 'winning claim');
});

test('fails when the post-write Project state cannot be verified', async () => {
  const transition = {
    command: 'delivery-control/v1',
    target: {repository: 'sniffy/sniffy', number: 748},
    expected: {type: 'PullRequest', head, fields: {Status: 'Implementation'}},
    set: {Status: 'Review'}
  };
  const fixture = projectDouble({initial: transition.expected.fields, freezeWrites: true});
  await assert.rejects(
    executeTransition({github: fixture.github, core: coreDouble(), payloadBase64: encoded(transition)}),
    /Post-mutation Project state did not match/
  );
  assert.deepEqual(fixture.mutations, [['set', 'Status', 'Review']]);
});

test('workflow has one write-bearing job and deterministic claim serialization', () => {
  const workflow = fs.readFileSync(path.join(__dirname, '../workflows/delivery-control.yml'), 'utf8');
  assert.equal((workflow.match(/issues:\s*write/g) || []).length, 1);
  assert.doesNotMatch(workflow, /^\s{2}acknowledge-parse-failure:/m);
  assert.match(workflow, /Reject invalid control command[\s\S]*needs\.parse\.result == 'failure'/);
  assert.match(workflow, /contains\(github\.event\.issue\.labels\.\*\.name, 'ai-delivery-control'\)/);
  assert.match(workflow, /ai-delivery-\$\{\{ needs\.parse\.outputs\.target_repository/);
  assert.match(workflow, /cancel-in-progress:\s*false/);
});

test('rotation thresholds are explicit and legacy target-comment workflows are retired', () => {
  const profile = fs.readFileSync(path.join(__dirname, '../../docs/ai-delivery/profile.yml'), 'utf8');
  assert.match(profile, /controlIssueLabel:\s*"ai-delivery-control"/);
  assert.match(profile, /rotateAfterCommands:\s*[1-9][0-9]*/);
  assert.match(profile, /rotateAfterDays:\s*[1-9][0-9]*/);
  assert.equal(fs.existsSync(path.join(__dirname, '../workflows/project-field.yml')), false);
  assert.equal(fs.existsSync(path.join(__dirname, '../workflows/project-status.yml')), false);
});

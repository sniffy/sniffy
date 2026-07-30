'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {commandFor, prepareReviewReturn, representedTargets} = require('./review-return');

const head = '46c47e02e8a5236cf1e7348fc6202da11ea1bbd3';

function selectValue(field, name) {
  return {__typename: 'ProjectV2ItemFieldSingleSelectValue', name, field: {name: field}};
}

function textValue(field, text) {
  return {__typename: 'ProjectV2ItemFieldTextValue', text, field: {name: field}};
}

function item(project = 'PROJECT', overrides = {}) {
  const fields = {
    Status: 'Review',
    Execution: 'In progress',
    Implementer: 'Codex Cloud',
    Executor: 'ChatGPT',
    'Worker reference': 'review-claim',
    ...overrides
  };
  return {
    id: 'ITEM',
    project: {id: project},
    fieldValues: {nodes: Object.entries(fields).flatMap(([field, value]) => {
      if (value === null) return [];
      return field === 'Worker reference' ? [textValue(field, value)] : [selectValue(field, value)];
    })}
  };
}

function pullRequest({prItem = null, issues = []} = {}) {
  return {
    number: 751,
    headRefOid: head,
    baseRefName: 'develop',
    isCrossRepository: false,
    reviewDecision: 'CHANGES_REQUESTED',
    author: {login: 'bedrin-codex-cloud'},
    projectItems: {nodes: prItem ? [prItem] : []},
    closingIssuesReferences: {nodes: issues}
  };
}

function issue(number, issueItem = item()) {
  return {id: `ISSUE-${number}`, number, projectItems: {nodes: [issueItem]}};
}

function coreDouble() {
  const outputs = {};
  const summary = {
    addHeading() { return this; },
    addRaw() { return this; },
    addCodeBlock() { return this; }
  };
  return {outputs, summary, setOutput(name, value) { outputs[name] = String(value); }};
}

function context(prNumber = 751) {
  return {
    actor: 'bedrin',
    payload: {
      action: 'submitted',
      review: {state: 'changes_requested', user: {login: 'bedrin'}},
      pull_request: {
        number: prNumber,
        base: {ref: 'develop'},
        head: {repo: {full_name: 'sniffy/sniffy'}}
      }
    }
  };
}

test('selects the PR when it is the only represented work item', () => {
  const targets = representedTargets(pullRequest({prItem: item()}), 'PROJECT');
  assert.deepEqual(targets.map(({type, number}) => ({type, number})), [{type: 'PullRequest', number: 751}]);
});

test('selects one formally linked issue when the PR is not represented', () => {
  const targets = representedTargets(pullRequest({issues: [issue(748)]}), 'PROJECT');
  assert.deepEqual(targets.map(({type, number}) => ({type, number})), [{type: 'Issue', number: 748}]);
});

test('builds a guarded return to the original agent implementer', () => {
  const target = representedTargets(pullRequest({issues: [issue(748)]}), 'PROJECT')[0];
  const prepared = commandFor(target);
  assert.equal(prepared.implementer, 'Codex Cloud');
  assert.deepEqual(prepared.command.set, {
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: 'Codex Cloud'
  });
  assert.deepEqual(prepared.command.clear, ['Worker reference']);
  assert.equal(prepared.command.expected.type, 'Issue');
  assert.equal(prepared.command.expected.fields.Status, 'Review');
  assert.equal(prepared.command.expected.fields.Execution, 'In progress');
  assert.equal(prepared.command.expected.fields.Executor, 'ChatGPT');
  assert.equal(prepared.command.expected.fields['Worker reference'], 'review-claim');
});

test('guards the exact head when the PR itself is the work item', () => {
  const target = representedTargets(pullRequest({prItem: item()}), 'PROJECT')[0];
  assert.equal(commandFor(target).command.expected.head, head);
});

test('rejects non-agent implementers and blocked work', () => {
  assert.throws(() => commandFor({type: 'Issue', number: 1, item: item('PROJECT', {Implementer: 'Human'})}), /not an agent executor/);
  assert.throws(() => commandFor({type: 'Issue', number: 1, item: item('PROJECT', {Execution: 'Blocked'})}), /Blocked work/);
});

test('prepares exactly one linked issue transition from a current request-changes review', async () => {
  const core = coreDouble();
  const github = {async graphql() {
    return {
      organization: {projectV2: {id: 'PROJECT'}},
      repository: {pullRequest: pullRequest({issues: [issue(748)]})}
    };
  }};
  const result = await prepareReviewReturn({
    github,
    context: context(),
    core,
    authorizedActors: new Set(['bedrin'])
  });
  assert.equal(result.shouldTransition, true);
  assert.equal(core.outputs.should_transition, 'true');
  assert.equal(core.outputs.target_number, '748');
  assert.equal(core.outputs.target_type, 'Issue');
  const command = JSON.parse(Buffer.from(core.outputs.payload_base64, 'base64').toString('utf8'));
  assert.equal(command.set.Executor, 'Codex Cloud');
});

test('skips a self-review even when the aggregate decision requests changes', async () => {
  const core = coreDouble();
  await prepareReviewReturn({
    github: {async graphql() {
      return {
        organization: {projectV2: {id: 'PROJECT'}},
        repository: {pullRequest: {...pullRequest({issues: [issue(748)]}), author: {login: 'bedrin'}}}
      };
    }},
    context: context(),
    core,
    authorizedActors: new Set(['bedrin'])
  });
  assert.equal(core.outputs.should_transition, 'false');
  assert.match(core.outputs.reason, /also the pull request author/);
});

test('skips stale aggregate decisions and rejects duplicate represented work items', async () => {
  const staleCore = coreDouble();
  await prepareReviewReturn({
    github: {async graphql() {
      return {
        organization: {projectV2: {id: 'PROJECT'}},
        repository: {pullRequest: {...pullRequest({issues: [issue(748)]}), reviewDecision: 'APPROVED'}}
      };
    }},
    context: context(),
    core: staleCore,
    authorizedActors: new Set(['bedrin'])
  });
  assert.equal(staleCore.outputs.should_transition, 'false');

  await assert.rejects(
    prepareReviewReturn({
      github: {async graphql() {
        return {
          organization: {projectV2: {id: 'PROJECT'}},
          repository: {pullRequest: pullRequest({prItem: item(), issues: [issue(748)]})}
        };
      }},
      context: context(),
      core: coreDouble(),
      authorizedActors: new Set(['bedrin'])
    }),
    /exactly one Project work item/
  );
});

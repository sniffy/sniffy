'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  clearAssigneesAndVerify,
  commandFor,
  prepareReviewReturn,
  selectCanonicalTarget,
  verifyPullRequestHead
} = require('./review-return');

const head = '46c47e02e8a5236cf1e7348fc6202da11ea1bbd3';
const branch = 'agent/example-correction';

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
    headRefName: branch,
    baseRefName: 'develop',
    isCrossRepository: false,
    reviewDecision: 'CHANGES_REQUESTED',
    author: {login: 'bedrin-codex-cloud'},
    projectItems: {nodes: prItem ? [prItem] : []},
    closingIssuesReferences: {nodes: issues}
  };
}

function issue(number, issueItem = item()) {
  return {id: `ISSUE-${number}`, number, projectItems: {nodes: issueItem ? [issueItem] : []}};
}

function issueTarget(overrides = {}) {
  return selectCanonicalTarget(
    pullRequest({issues: [issue(748, item('PROJECT', overrides))]}),
    'PROJECT'
  ).target;
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

test('selects the represented PR only when no issue is formally closed', () => {
  const selection = selectCanonicalTarget(pullRequest({prItem: item()}), 'PROJECT');
  assert.deepEqual(
    {type: selection.target.type, number: selection.target.number},
    {type: 'PullRequest', number: 751}
  );
});

test('selects one formal closing issue even when the PR is also represented', () => {
  const selection = selectCanonicalTarget(
    pullRequest({prItem: item(), issues: [issue(748)]}),
    'PROJECT'
  );
  assert.deepEqual(
    {type: selection.target.type, number: selection.target.number},
    {type: 'Issue', number: 748}
  );
});

test('does not substitute a represented PR for a missing canonical issue', () => {
  const selection = selectCanonicalTarget(
    pullRequest({prItem: item(), issues: [issue(748, null)]}),
    'PROJECT'
  );
  assert.equal(selection.target, null);
  assert.match(selection.reason, /Closing issue #748 is canonical but is not represented/);
});

test('keeps a multi-issue pull request in Planning instead of routing one linked issue', () => {
  const selection = selectCanonicalTarget(
    pullRequest({prItem: item(), issues: [issue(748), issue(749)]}),
    'PROJECT'
  );
  assert.equal(selection.target, null);
  assert.match(selection.reason, /closes 2 issues.*multi-issue pull request in Planning/);
});

test('builds a guarded issue return with a durable continuation reference', () => {
  const target = issueTarget();
  const prepared = commandFor(target);
  assert.equal(prepared.implementer, 'Codex Cloud');
  assert.deepEqual(prepared.command.set, {
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: 'Codex Cloud',
    'Worker reference': `Continuation PR https://github.com/sniffy/sniffy/pull/751 (#751); branch ${branch}; exact head ${head}`
  });
  assert.deepEqual(prepared.command.clear, []);
  assert.equal(prepared.command.expected.type, 'Issue');
  assert.equal(prepared.command.expected.head, undefined);
  assert.equal(prepared.command.expected.fields.Status, 'Review');
  assert.equal(prepared.command.expected.fields.Execution, 'In progress');
  assert.equal(prepared.command.expected.fields.Executor, 'ChatGPT');
  assert.equal(prepared.command.expected.fields['Worker reference'], 'review-claim');
});

test('guards the exact head when the PR itself is the work item', () => {
  const target = selectCanonicalTarget(pullRequest({prItem: item()}), 'PROJECT').target;
  assert.equal(commandFor(target).command.expected.head, head);
  assert.deepEqual(commandFor(target).command.clear, ['Worker reference']);
});

test('repairs stale ownership on an already-routed issue before treating it as returned', () => {
  const target = issueTarget({
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: 'Codex Cloud',
    'Worker reference': 'stale review claim'
  });
  const prepared = commandFor(target);
  assert.equal(prepared.alreadyReturned, false);
  assert.match(prepared.command.set['Worker reference'], /PR https:\/\/github.com\/sniffy\/sniffy\/pull\/751/);
});

test('keeps an idempotent command so failed assignment release can be retried', () => {
  const desiredReference = commandFor(issueTarget()).command.set['Worker reference'];
  const prepared = commandFor(issueTarget({
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: 'Codex Cloud',
    'Worker reference': desiredReference
  }));
  assert.equal(prepared.alreadyReturned, true);
  assert.ok(prepared.command);
  assert.equal(prepared.command.set['Worker reference'], desiredReference);
});

test('rejects non-agent implementers and blocked work', () => {
  assert.throws(() => commandFor(issueTarget({Implementer: 'Human'})), /not an agent executor/);
  assert.throws(() => commandFor(issueTarget({Execution: 'Blocked'})), /Blocked work/);
});

test('prepares exactly one linked issue transition from a current request-changes review', async () => {
  const core = coreDouble();
  const github = {async graphql() {
    return {
      organization: {projectV2: {id: 'PROJECT'}},
      repository: {pullRequest: pullRequest({prItem: item(), issues: [issue(748)]})}
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
  assert.equal(core.outputs.pull_request_number, '751');
  assert.equal(core.outputs.pull_request_branch, branch);
  assert.equal(core.outputs.pull_request_head, head);
  const command = JSON.parse(Buffer.from(core.outputs.payload_base64, 'base64').toString('utf8'));
  assert.equal(command.set.Executor, 'Codex Cloud');
  assert.match(command.set['Worker reference'], new RegExp(`branch ${branch}; exact head ${head}`));
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

test('skips stale aggregate decisions and non-materialized canonical targets', async () => {
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

  const missingCore = coreDouble();
  await prepareReviewReturn({
    github: {async graphql() {
      return {
        organization: {projectV2: {id: 'PROJECT'}},
        repository: {pullRequest: pullRequest({prItem: item(), issues: [issue(748, null)]})}
      };
    }},
    context: context(),
    core: missingCore,
    authorizedActors: new Set(['bedrin'])
  });
  assert.equal(missingCore.outputs.should_transition, 'false');
  assert.match(missingCore.outputs.reason, /Closing issue #748 is canonical but is not represented/);
});

test('verifies the prepared pull-request head immediately before transition', async () => {
  const github = {
    rest: {
      pulls: {
        async get() {
          return {data: {head: {sha: head}}};
        }
      }
    }
  };
  assert.equal(await verifyPullRequestHead({github, pullRequestNumber: 751, expectedHead: head}), head);
  await assert.rejects(
    verifyPullRequestHead({github, pullRequestNumber: 751, expectedHead: 'deadbeef'}),
    /not prepared head deadbeef/
  );
});

test('re-reads assignment state and fails when an assignee remains', async () => {
  let updateArguments;
  const github = {
    rest: {
      issues: {
        async update(args) {
          updateArguments = args;
        },
        async get() {
          return {data: {assignees: []}};
        }
      }
    }
  };
  await clearAssigneesAndVerify({github, targetNumber: 748});
  assert.deepEqual(updateArguments.assignees, []);

  github.rest.issues.get = async () => ({data: {assignees: [{login: 'bedrin-gpt'}]}});
  await assert.rejects(
    clearAssigneesAndVerify({github, targetNumber: 748}),
    /still assigned: bedrin-gpt/
  );
});

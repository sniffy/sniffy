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

function pullRequestReference(pullRequest) {
  return {
    number: pullRequest.number,
    branch: pullRequest.headRefName,
    head: String(pullRequest.headRefOid || '').toLowerCase()
  };
}

function selectCanonicalTarget(pullRequest, projectId) {
  const closingIssues = pullRequest.closingIssuesReferences?.nodes || [];
  const pullRequestRef = pullRequestReference(pullRequest);

  if (closingIssues.length > 1) {
    return {
      target: null,
      reason: `Pull request #${pullRequest.number} closes ${closingIssues.length} issues; current policy keeps a multi-issue pull request in Planning rather than returning one linked item automatically.`
    };
  }

  if (closingIssues.length === 1) {
    const issue = closingIssues[0];
    const issueItem = projectItem(issue, projectId);
    if (!issueItem) {
      return {
        target: null,
        reason: `Closing issue #${issue.number} is canonical but is not represented in Project 2; wait for universal intake to materialize it.`
      };
    }
    return {
      target: {
        type: 'Issue',
        number: issue.number,
        item: issueItem,
        pullRequest: pullRequestRef
      },
      reason: null
    };
  }

  const pullRequestItem = projectItem(pullRequest, projectId);
  if (!pullRequestItem) {
    return {
      target: null,
      reason: `Standalone pull request #${pullRequest.number} is canonical but is not represented in Project 2.`
    };
  }
  return {
    target: {
      type: 'PullRequest',
      number: pullRequest.number,
      item: pullRequestItem,
      pullRequest: pullRequestRef
    },
    reason: null
  };
}

function continuationReference(target) {
  const pullRequest = target.pullRequest;
  return `Continuation PR https://github.com/${REPOSITORY}/pull/${pullRequest.number} (#${pullRequest.number}); branch ${pullRequest.branch}; exact head ${pullRequest.head}`;
}

function commandFor(target) {
  const current = values(target.item.fieldValues?.nodes);
  const status = current.get('Status') ?? null;
  const execution = current.get('Execution') ?? null;
  const implementer = current.get('Implementer') ?? null;
  const executor = current.get('Executor') ?? null;
  const workerReference = current.get('Worker reference') ?? null;
  const desiredWorkerReference = target.type === 'Issue' ? continuationReference(target) : null;

  if (!AGENT_EXECUTORS.has(implementer)) {
    throw new Error(`Implementer "${implementer || '(clear)'}" is not an agent executor eligible for automatic continuation.`);
  }
  if (execution === 'Blocked') {
    throw new Error('Blocked work requires a maintainer decision and cannot be returned automatically.');
  }

  const alreadyRouted = status === 'Implementation' && execution === 'Ready' && executor === implementer;
  const alreadyReturned = alreadyRouted && workerReference === desiredWorkerReference;
  if (!alreadyRouted && !RETURNABLE_STATUSES.has(status)) {
    throw new Error(`Project item is ${status || '(clear)'}, not Review or Approval.`);
  }

  const expectedFields = {};
  for (const field of GUARDED_FIELDS) expectedFields[field] = current.get(field) ?? null;
  const expected = {type: target.type, fields: expectedFields};
  if (target.type === 'PullRequest') expected.head = target.pullRequest.head;

  const set = {
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: implementer
  };
  const clear = [];
  if (desiredWorkerReference === null) {
    clear.push('Worker reference');
  } else {
    set['Worker reference'] = desiredWorkerReference;
  }

  return {
    alreadyReturned,
    implementer,
    command: {
      command: 'delivery-control/v1',
      target: {repository: REPOSITORY, number: target.number},
      expected,
      set,
      clear,
      addIfMissing: false
    }
  };
}

async function verifyPullRequestHead({github, pullRequestNumber, expectedHead}) {
  const [owner, repo] = REPOSITORY.split('/');
  const response = await github.rest.pulls.get({
    owner,
    repo,
    pull_number: Number(pullRequestNumber)
  });
  const actualHead = String(response.data?.head?.sha || '').toLowerCase();
  const preparedHead = String(expectedHead || '').toLowerCase();
  if (!preparedHead || actualHead !== preparedHead) {
    throw new Error(`Pull request #${pullRequestNumber} head is ${actualHead || '(missing)'}, not prepared head ${preparedHead || '(missing)'}.`);
  }
  return actualHead;
}

async function clearAssigneesAndVerify({github, targetNumber}) {
  const [owner, repo] = REPOSITORY.split('/');
  const issueNumber = Number(targetNumber);
  await github.rest.issues.update({
    owner,
    repo,
    issue_number: issueNumber,
    assignees: []
  });
  const response = await github.rest.issues.get({
    owner,
    repo,
    issue_number: issueNumber
  });
  const remaining = (response.data?.assignees || []).map(assignee => assignee.login);
  if (remaining.length > 0) {
    throw new Error(`Failed to clear assignees from #${issueNumber}; still assigned: ${remaining.join(', ')}.`);
  }
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
      headRefName
      baseRefName
      isCrossRepository
      reviewDecision
      author { login }
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
  if (pullRequest.author?.login === reviewer) {
    return skip(core, 'The reviewer is also the pull request author; correction routing requires an independent review.');
  }
  if (pullRequest.reviewDecision !== 'CHANGES_REQUESTED') {
    return skip(core, `The current aggregate review decision is ${pullRequest.reviewDecision || '(none)'}, not CHANGES_REQUESTED.`);
  }

  const selection = selectCanonicalTarget(pullRequest, project.id);
  if (!selection.target) return skip(core, selection.reason);

  const target = selection.target;
  const prepared = commandFor(target);
  core.setOutput('target_repository', REPOSITORY.replace('/', '-'));
  core.setOutput('target_number', String(target.number));
  core.setOutput('target_type', target.type);
  core.setOutput('implementer', prepared.implementer);
  core.setOutput('pull_request_number', String(target.pullRequest.number));
  core.setOutput('pull_request_branch', target.pullRequest.branch);
  core.setOutput('pull_request_head', target.pullRequest.head);

  core.setOutput('should_transition', 'true');
  core.setOutput('payload_base64', Buffer.from(JSON.stringify(prepared.command)).toString('base64'));
  core.summary.addHeading('Review return prepared')
    .addRaw(`Reviewer: \`${reviewer}\`\n\nTarget: \`${target.type} ${REPOSITORY}#${target.number}\`\n\n`)
    .addRaw(`Return route: \`Implementation / Ready / ${prepared.implementer}\`\n\n`);
  if (prepared.alreadyReturned) {
    core.summary.addRaw('Project routing is already in the requested state; assignment release will still be re-run and verified.\n\n');
  }
  core.summary
    .addCodeBlock(JSON.stringify(prepared.command, null, 2), 'json');
  return {shouldTransition: true, target, ...prepared};
}

module.exports = {
  AGENT_EXECUTORS,
  clearAssigneesAndVerify,
  commandFor,
  prepareReviewReturn,
  selectCanonicalTarget,
  values,
  verifyPullRequestHead
};

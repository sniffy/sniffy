'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {executeTransition} = require('./delivery-control');

const head = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
const movedHead = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';

function coreDouble() {
  const summary = {
    addHeading() { return this; },
    addRaw() { return this; },
    addTable() { return this; },
    async write() {}
  };
  return {
    summary,
    setOutput() {}
  };
}

function fixture() {
  let status = 'Implementation';
  let projectWritten = false;
  let pullReads = 0;

  const statusField = {
    __typename: 'ProjectV2SingleSelectField',
    id: 'STATUS',
    name: 'Status',
    options: [
      {id: 'STATUS-IMPLEMENTATION', name: 'Implementation'},
      {id: 'STATUS-REVIEW', name: 'Review'}
    ]
  };

  function projectItem() {
    return {
      id: 'ITEM',
      project: {id: 'PROJECT'},
      fieldValues: {
        nodes: [{
          __typename: 'ProjectV2ItemFieldSingleSelectValue',
          name: status,
          field: {name: 'Status'}
        }]
      }
    };
  }

  const github = {
    async graphql(query, variables) {
      if (query.includes('organization(login:')) {
        return {
          organization: {projectV2: {id: 'PROJECT', fields: {nodes: [statusField]}}},
          repository: {
            issueOrPullRequest: {
              __typename: 'PullRequest',
              id: 'TARGET',
              headRefOid: head
            }
          }
        };
      }

      if (query.includes('repository(owner:') && query.includes('pullRequest(number:')) {
        pullReads += 1;
        return {
          repository: {
            pullRequest: {
              id: 'REVIEW',
              number: variables.number,
              state: 'OPEN',
              isDraft: false,
              baseRefName: 'develop',
              headRefOid: projectWritten ? movedHead : head,
              closingIssuesReferences: {nodes: []}
            }
          }
        };
      }

      if (query.includes('projectItems(first: 50)')) {
        return {node: {projectItems: {nodes: [projectItem()]}}};
      }

      if (query.includes('updateProjectV2ItemFieldValue')) {
        assert.equal(variables.field, 'STATUS');
        assert.deepEqual(variables.value, {singleSelectOptionId: 'STATUS-REVIEW'});
        status = 'Review';
        projectWritten = true;
        return {updateProjectV2ItemFieldValue: {projectV2Item: {id: 'ITEM'}}};
      }

      throw new Error(`Unexpected GraphQL operation: ${query.slice(0, 80)}`);
    }
  };

  return {
    github,
    get projectWritten() { return projectWritten; },
    get pullReads() { return pullReads; }
  };
}

test('rejects a Review transition when the PR head moves after the Project mutation', async () => {
  const command = {
    command: 'delivery-control/v1',
    target: {repository: 'sniffy/sniffy', number: 748},
    expected: {
      type: 'PullRequest',
      head,
      fields: {Status: 'Implementation'}
    },
    set: {Status: 'Review'}
  };
  const state = fixture();

  await assert.rejects(
    executeTransition({
      github: state.github,
      core: coreDouble(),
      payloadBase64: Buffer.from(JSON.stringify(command)).toString('base64')
    }),
    /Post-mutation pull request #748 was not open, non-draft, and at the guarded Review head/
  );

  assert.equal(state.projectWritten, true);
  assert.equal(state.pullReads, 3);
});

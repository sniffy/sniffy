'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {commandFor} = require('./review-return');

function selectValue(field, name) {
  return {__typename: 'ProjectV2ItemFieldSingleSelectValue', name, field: {name: field}};
}

function textValue(field, text) {
  return {__typename: 'ProjectV2ItemFieldTextValue', text, field: {name: field}};
}

test('returns a verified agent implementation to correction after Request Changes', () => {
  const target = {
    type: 'Issue',
    number: 753,
    pullRequest: {
      number: 754,
      branch: 'agent/playwright-1.62-coherent',
      head: '9ff68a34f8c1dbdb3368ed5a0f4e1568ff7ebe49'
    },
    item: {
      fieldValues: {
        nodes: [
          selectValue('Status', 'Verification'),
          selectValue('Execution', 'Ready'),
          selectValue('Implementer', 'Codex Cloud'),
          selectValue('Executor', 'Local Codex'),
          textValue('Worker reference', 'verification claim')
        ]
      }
    }
  };

  const prepared = commandFor(target);
  assert.equal(prepared.implementer, 'Codex Cloud');
  assert.equal(prepared.command.expected.fields.Status, 'Verification');
  assert.equal(prepared.command.expected.fields.Execution, 'Ready');
  assert.equal(prepared.command.expected.fields.Executor, 'Local Codex');
  assert.deepEqual(prepared.command.set, {
    Status: 'Implementation',
    Execution: 'Ready',
    Executor: 'Codex Cloud',
    'Worker reference': 'Continuation PR https://github.com/sniffy/sniffy/pull/754 (#754); branch agent/playwright-1.62-coherent; exact head 9ff68a34f8c1dbdb3368ed5a0f4e1568ff7ebe49'
  });
});

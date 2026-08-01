'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');
const workflow = fs.readFileSync(path.join(root, '.github/workflows/delivery-status.yml'), 'utf8');

function jobBlock(jobName) {
  const match = workflow.match(new RegExp(`\\n  ${jobName}:[\\s\\S]*?(?=\\n  [a-zA-Z0-9_-]+:\\n|$)`));
  assert.ok(match, `missing ${jobName} job`);
  return match[0];
}

function sparsePaths(jobName) {
  const match = jobBlock(jobName).match(/sparse-checkout:[ \t]*\|\n((?: {12}[^\n]+\n?)+)/);
  assert.ok(match, `missing sparse checkout for ${jobName} job`);
  return match[1].trim().split('\n').map(line => line.trim());
}

test('validation checkout materializes the exact focused inputs without credentials', () => {
  const validate = jobBlock('validate');
  assert.doesNotMatch(validate, /filter:/);
  assert.match(validate, /sparse-checkout-cone-mode:\s*false/);
  assert.match(validate, /persist-credentials:\s*false/);
  assert.deepEqual(sparsePaths('validate'), [
    '.chatgpt/scheduled-task-prompt.md',
    '.chatgpt/status-snapshot-instructions.md',
    '.github/scripts/delivery-status.js',
    '.github/scripts/delivery-status.test.js',
    '.github/scripts/delivery-status-policy.test.js',
    '.github/scripts/delivery-status-checkout-policy.test.js',
    '.github/scripts/delivery-status-runner-policy.test.js',
    '.github/scripts/delivery-status-mergeability.js',
    '.github/scripts/delivery-status-mergeability.test.js',
    '.github/workflows/delivery-status.yml',
    'docs/ai-delivery/README.md',
    'docs/ai-delivery/profile.yml',
    'docs/ai-delivery/status-snapshot.md',
    'docs/ai-delivery/status-workflow-optimization.md'
  ]);
});

test('trusted publication checks out only its two scripts from develop without credentials', () => {
  const publish = jobBlock('publish');
  assert.match(publish, /ref:\s*develop/);
  assert.doesNotMatch(publish, /filter:/);
  assert.match(publish, /sparse-checkout-cone-mode:\s*false/);
  assert.match(publish, /persist-credentials:\s*false/);
  assert.deepEqual(sparsePaths('publish'), [
    '.github/scripts/delivery-status.js',
    '.github/scripts/delivery-status-mergeability.js'
  ]);
});

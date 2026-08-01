'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const workflow = fs.readFileSync(path.join(__dirname, '../workflows/delivery-status.yml'), 'utf8');

test('validation stays on the full runner while publication uses ubuntu-slim', () => {
  assert.match(workflow, /validate:[\s\S]*?runs-on:\s*ubuntu-24\.04/);
  assert.match(workflow, /publish:[\s\S]*?runs-on:\s*ubuntu-slim/);
  assert.equal((workflow.match(/runs-on:\s*ubuntu-slim/g) || []).length, 1);
});

test('validation retains Ruby YAML parsing and actionlint', () => {
  assert.match(workflow, /ruby -e/);
  assert.match(workflow, /rhysd\/actionlint:1\.7\.12/);
});

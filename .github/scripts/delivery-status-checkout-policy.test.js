'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');
const workflow = fs.readFileSync(path.join(root, '.github/workflows/delivery-status.yml'), 'utf8');

test('validation checkout is partial, sparse and credential-free', () => {
  assert.match(workflow, /filter:\s*blob:none/);
  assert.match(workflow, /sparse-checkout-cone-mode:\s*false/);
  assert.match(workflow, /persist-credentials:\s*false/);
  assert.match(workflow, /\.github\/scripts\/delivery-status\.test\.js/);
  assert.match(workflow, /docs\/ai-delivery\/profile\.yml/);
});

test('trusted publication keeps develop and checks out only the normalizer', () => {
  assert.match(
    workflow,
    /publish:[\s\S]*?ref:\s*develop[\s\S]*?sparse-checkout:\s*\.github\/scripts\/delivery-status\.js[\s\S]*?persist-credentials:\s*false/
  );
});

test('checkout optimization does not migrate either job to ubuntu-slim', () => {
  assert.doesNotMatch(workflow, /runs-on:\s*ubuntu-slim/);
  assert.equal((workflow.match(/runs-on:\s*ubuntu-24\.04/g) || []).length, 2);
});

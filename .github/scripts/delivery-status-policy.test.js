'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

test('ChatGPT scheduled ticks require the labeled status artifact fallback', () => {
  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  assert.match(prompt, /\.chatgpt\/status-snapshot-instructions\.md/);
  assert.match(prompt, /newest open non-PR issue labeled ai-delivery-status/i);
  assert.match(prompt, /download the artifact by\s+numeric artifact ID/is);
  assert.match(prompt, /read project-2-status\.json/i);
  assert.match(prompt, /missing, expired, inaccessible, malformed, mismatched, or stale snapshot/i);
  assert.match(prompt, /\/ai-delivery-status refresh/);
  assert.match(prompt, /terminal \+1 reaction/i);
  assert.match(prompt, /refreshRequest\.commentId/i);
});

test('snapshot-backed commands retain live compare-and-set semantics', () => {
  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  assert.match(prompt, /snapshot is an eventually consistent selection aid, not a mutation ledger or lease/i);
  assert.match(prompt, /Every Project mutation still goes through delivery-control\/v1/i);
  assert.match(prompt, /confused reaction means the snapshot lost a race/i);
  assert.match(prompt, /snapshot generated after the preceding successful command/i);
});

test('profile fixes the status protocol, label, artifact and freshness contract', () => {
  const profile = read('docs/ai-delivery/profile.yml');
  assert.match(profile, /statusSnapshot:/);
  assert.match(profile, /protocol:\s*ai-delivery-status\/v1/);
  assert.match(profile, /issueLabel:\s*"ai-delivery-status"/);
  assert.match(profile, /artifactName:\s*"ai-delivery-status-project-2"/);
  assert.match(profile, /snapshotFile:\s*"project-2-status\.json"/);
  assert.match(profile, /maxAgeMinutes:\s*30/);
  assert.match(profile, /refreshCommand:\s*"\/ai-delivery-status refresh"/);
  assert.match(profile, /refreshTimeoutSeconds:\s*120/);
  assert.match(profile, /refreshActors:\s*\[bedrin, bedrin-gpt\]/);
});

test('status documentation preserves the read-only mutation boundary', () => {
  const documentation = read('docs/ai-delivery/status-snapshot.md');
  assert.match(documentation, /read-only materialized view/i);
  assert.match(documentation, /never\s+replaces.*guarded mutation protocol/is);
  assert.match(documentation, /No client may mutate Project fields directly from the snapshot/i);
  assert.match(documentation, /newest open issue labeled ai-delivery-status/i);
});

test('workflow isolates validation and trusted status publication', () => {
  const workflow = read('.github/workflows/delivery-status.yml');
  assert.match(workflow, /permissions:\s*\{\}/);
  assert.match(workflow, /cron:\s*'5 \* \* \* \*'/);
  assert.match(workflow, /workflows:\s*\[AI delivery control\]/);
  assert.match(workflow, /issue_comment:\s*\n\s+types:\s*\[created\]/);
  assert.match(workflow, /github\.event\.comment\.body == '\/ai-delivery-status refresh'/);
  assert.match(workflow, /contains\(github\.event\.issue\.labels\.\*\.name, 'ai-delivery-status'\)/);
  assert.match(workflow, /fromJSON\('\["bedrin","bedrin-gpt"\]'\)/);
  assert.match(workflow, /content:\s*'\+1'/);
  assert.match(workflow, /content:\s*'-1'/);
  assert.match(workflow, /refreshRequest:/);
  assert.match(workflow, /ref:\s*develop/);
  assert.match(workflow, /issues:\s*write/);
  assert.match(workflow, /pull-requests:\s*read/);
  assert.doesNotMatch(workflow, /contents:\s*write/);
});

test('status publication preserves every pending refresh request', () => {
  const workflow = read('.github/workflows/delivery-status.yml');
  assert.match(workflow, /concurrency:\s*\n\s+group:\s*ai-delivery-status-\$\{\{ github\.repository \}\}\s*\n\s+queue:\s*max\s*\n\s+cancel-in-progress:\s*false/);
  assert.match(workflow, /-ignore 'unexpected key "queue" for "concurrency" section'/);
});

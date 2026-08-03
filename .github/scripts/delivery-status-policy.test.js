'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function chatRuntime() {
  return [
    read('.chatgpt/scheduled-task-prompt.md'),
    read('.chatgpt/status-snapshot-instructions.md'),
    read('docs/ai-delivery/runtime-contract.md')
  ].join('\n');
}

test('ChatGPT scheduled ticks require the labeled status artifact fallback', () => {
  const policy = chatRuntime();
  assert.match(policy, /\.chatgpt\/status-snapshot-instructions\.md/);
  assert.match(policy, /newest open non-PR issue.*labelled `ai-delivery-status`/is);
  assert.match(policy, /download that artifact by numeric ID/is);
  assert.match(policy, /project-2-status\.json/i);
  assert.match(policy, /malformed, inconsistent, incomplete, stale, or inaccessible/i);
  assert.match(policy, /\/ai-delivery-status refresh/);
  assert.match(policy, /terminal reaction/i);
  assert.match(policy, /refresh comment provenance/i);
});

test('snapshot-backed commands retain live compare-and-set semantics', () => {
  const policy = chatRuntime();
  assert.match(policy, /materialized selection view/i);
  assert.match(policy, /Every Project claim and\s+handoff uses the guarded `delivery-control\/v1` protocol/i);
  assert.match(policy, /A conflict means another actor\s+won or state changed/i);
  assert.match(policy, /snapshot generated after the preceding successful command/i);
  assert.match(policy, /live-read only the selected candidate/i);
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
  assert.match(profile, /dispatchProjection:\s*true/);
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
  assert.doesNotMatch(workflow, /workflow_run:/);
  assert.doesNotMatch(workflow, /github\.event\.workflow_run/);
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

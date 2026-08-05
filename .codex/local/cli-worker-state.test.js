'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const {
  resolveBranch,
  resolveWorkerProfile,
  spawnAcknowledgement,
  turnOutcome,
  validateManifest,
  workerReference
} = require('./cli-worker-state');

function manifest(overrides = {}) {
  return {
    generation: 'implementation-808-20260803t180000z',
    token: 'token-808-0123456789',
    type: 'Issue',
    number: 808,
    status: 'Implementation',
    mode: 'fresh',
    branch: 'agent/demo-history-import',
    baseSha: 'a'.repeat(40),
    workerProfile: 'balanced',
    model: 'gpt-5.6-terra',
    reasoning: 'medium',
    spawningReference: 'v=2;state=spawning',
    ...overrides
  };
}

test('requires both thread.started and turn.started for strong spawn acknowledgement', () => {
  assert.equal(spawnAcknowledgement([{type: 'thread.started', thread_id: 'thread-1'}]), null);
  assert.equal(spawnAcknowledgement([{type: 'turn.started'}]), null);
  assert.deepEqual(spawnAcknowledgement([
    {type: 'thread.started', thread_id: 'thread-1'},
    {type: 'turn.started'}
  ]), {threadId: 'thread-1'});
});

test('reports the terminal Codex turn event without confusing it with lifecycle handoff', () => {
  assert.deepEqual(turnOutcome([{type: 'turn.completed'}]), {state: 'completed', event: 'turn.completed'});
  assert.deepEqual(turnOutcome([{type: 'turn.failed'}]), {state: 'failed', event: 'turn.failed'});
  assert.deepEqual(turnOutcome([{type: 'thread.started', thread_id: 'thread-1'}]), null);
});

test('authoritative issue branch overrides fallback generation', () => {
  assert.deepEqual(resolveBranch({
    body: '- **Branch:** `agent/demo-history-import`',
    fallbackBranch: 'agent/issue-808-import-curated-legacy-demo-history'
  }), {branch: 'agent/demo-history-import', source: 'authoritative-issue'});
});

test('continuation branch has highest authority', () => {
  assert.deepEqual(resolveBranch({
    body: '- **Branch:** `agent/ignored`',
    continuationBranch: 'agent/existing-pr',
    fallbackBranch: 'agent/issue-1'
  }), {branch: 'agent/existing-pr', source: 'continuation-pr'});
});

test('conflicting explicit branches fail closed', () => {
  assert.throws(() => resolveBranch({
    body: '**Branch:** `agent/one`\nBranch: `agent/two`',
    fallbackBranch: 'agent/fallback'
  }), /Conflicting explicit branches/);
});

test('worker profile is selected per authoritative item with a configured fallback', () => {
  assert.deepEqual(resolveWorkerProfile({
    body: '- **Codex worker profile:** `economy`',
    defaultProfile: 'balanced'
  }), {profile: 'economy', source: 'authoritative-item'});
  assert.deepEqual(resolveWorkerProfile({body: '', defaultProfile: 'frontier'}), {
    profile: 'frontier',
    source: 'configured-default'
  });
});

test('conflicting or unknown worker profiles fail closed', () => {
  assert.throws(() => resolveWorkerProfile({
    body: '**Codex worker profile:** `economy`\nCodex worker profile: frontier'
  }), /Conflicting explicit Codex worker profiles/);
  assert.throws(() => resolveWorkerProfile({body: '', defaultProfile: 'fastest'}), /Invalid default worker profile/);
});

test('worker reference is compact and excludes local paths and pid', () => {
  const reference = workerReference(manifest(), 'running', '2026-08-03T20:00:00Z', 'thread-1');
  assert.match(reference, /state=running/);
  assert.match(reference, /thread=thread-1/);
  assert.match(reference, /profile=balanced/);
  assert.match(reference, /model=gpt-5\.6-terra/);
  assert.doesNotMatch(reference, /worktree|pid|\/home\//);
});

test('manifest rejects unsupported or unsafe identity', () => {
  assert.throws(() => validateManifest(manifest({branch: '../bad'})), /Invalid branch/);
  assert.throws(() => validateManifest(manifest({generation: 'bad'})), /Invalid generation/);
  assert.throws(() => validateManifest(manifest({branch: 'agent/bad;leaseUntil=forever'})), /Invalid branch/);
  assert.throws(() => validateManifest(manifest({token: 'short'})), /Invalid token/);
  assert.throws(() => validateManifest(manifest({workerProfile: 'fastest'})), /Invalid worker profile/);
  assert.throws(() => validateManifest(manifest({model: 'bad model'})), /Invalid model/);
});

test('repository scripts and units preserve strong acknowledgement contract', () => {
  const root = path.join(__dirname, '../..');
  const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
  const dispatcher = read('.codex/local/cli-dispatch.sh');
  const launcher = read('.codex/local/cli-launch-worker.sh');
  const recovery = read('.codex/local/cli-recover-worker.sh');
  const service = read('.codex/local/systemd/sniffy-local-worker@.service');
  const profile = read('docs/ai-delivery/profile.yml');

  assert.match(dispatcher, /project-queue-snapshot\.sh/);
  assert.match(dispatcher, /flock/);
  assert.match(dispatcher, /state=spawning/);
  assert.match(dispatcher, /SPAWN_UNCERTAIN/);
  assert.doesNotMatch(dispatcher, /client-new-thread/);
  assert.match(dispatcher, /owned_cli_generations/);
  assert.match(dispatcher, /cli-worker-state\.js" profile/);

  assert.match(launcher, /exec --json --cd/);
  assert.match(launcher, /model="\$\(jq -r \.model "\$manifest_file"\)"/);
  assert.doesNotMatch(launcher, /CODEX_MODEL/);
  assert.match(launcher, /cli-worker-state\.js" ack/);
  assert.match(launcher, /thread\.started/);
  assert.match(launcher, /turn\.started/);
  assert.match(launcher, /cli-worker-state\.js" terminal/);
  assert.match(launcher, /git worktree add/);

  assert.match(recovery, /RELEASE_REQUIRED/);
  assert.match(recovery, /same generation/i);
  assert.match(service, /cli-launch-worker\.sh %i/);
  assert.match(profile, /cliDispatcherModel: none/);
  assert.match(profile, /cliDefaultWorkerProfile: balanced/);
  assert.match(profile, /economy:[\s\S]*model: gpt-5\.6-luna[\s\S]*reasoning: low/);
  assert.match(profile, /balanced:[\s\S]*model: gpt-5\.6-terra[\s\S]*reasoning: medium/);
  assert.match(profile, /frontier:[\s\S]*model: gpt-5\.6-sol[\s\S]*reasoning: high/);
});

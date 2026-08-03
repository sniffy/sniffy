'use strict';

const fs = require('node:fs');

const GENERATION = /^[a-z0-9][a-z0-9._-]{7,159}$/;
const BRANCH = /^(?!\/)(?!.*(?:^|\/)\.\.?($|\/))(?!.*\/\/)(?!.*@\{)(?!.*[~^:?*\[\\\s;=])[A-Za-z0-9._/-]+(?<![/.])$/;

function readJsonLines(file) {
  if (!fs.existsSync(file)) return [];
  return fs.readFileSync(file, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line, index) => {
      try {
        return JSON.parse(line);
      } catch (error) {
        throw new Error(`Invalid JSONL at line ${index + 1}: ${error.message}`);
      }
    });
}

function spawnAcknowledgement(events) {
  let threadId = null;
  let threadStarted = false;
  let turnStarted = false;
  for (const event of events) {
    const type = event.type || event.event || null;
    if (type === 'thread.started') {
      threadStarted = true;
      threadId = event.thread_id || event.threadId || event.thread?.id || null;
    }
    if (type === 'turn.started') turnStarted = true;
  }
  if (!threadStarted || !threadId || !turnStarted) return null;
  return {threadId};
}

function turnOutcome(events) {
  let outcome = null;
  for (const event of events) {
    const type = event.type || event.event || null;
    if (type === 'turn.completed') outcome = {state: 'completed', event: type};
    if (type === 'turn.failed' || type === 'error') outcome = {state: 'failed', event: type};
  }
  return outcome;
}

function explicitBranches(body) {
  const text = String(body || '');
  const values = new Set();
  const patterns = [
    /(?:^|\n)\s*[-*]?\s*\*\*Branch:\*\*\s*`([^`]+)`/gi,
    /(?:^|\n)\s*Branch:\s*`?([A-Za-z0-9._/-]+)`?\s*$/gim
  ];
  for (const pattern of patterns) {
    for (const match of text.matchAll(pattern)) values.add(match[1].trim());
  }
  return [...values];
}

function resolveBranch({body, continuationBranch = null, fallbackBranch}) {
  if (continuationBranch) {
    validateBranch(continuationBranch);
    return {branch: continuationBranch, source: 'continuation-pr'};
  }
  const explicit = explicitBranches(body);
  if (explicit.length > 1) {
    throw new Error(`Conflicting explicit branches: ${explicit.join(', ')}`);
  }
  if (explicit.length === 1) {
    validateBranch(explicit[0]);
    return {branch: explicit[0], source: 'authoritative-issue'};
  }
  validateBranch(fallbackBranch);
  return {branch: fallbackBranch, source: 'deterministic-fallback'};
}

function validateBranch(branch) {
  if (!BRANCH.test(String(branch || ''))) throw new Error(`Invalid branch: ${branch || '(empty)'}`);
}

function validateManifest(manifest) {
  if (!manifest || typeof manifest !== 'object' || Array.isArray(manifest)) {
    throw new Error('Manifest must be an object.');
  }
  if (!GENERATION.test(String(manifest.generation || ''))) throw new Error('Invalid generation.');
  if (!Number.isInteger(manifest.number) || manifest.number < 1) throw new Error('Invalid work item number.');
  if (!['Issue', 'PullRequest'].includes(manifest.type)) throw new Error('Invalid work item type.');
  if (!['Implementation', 'Verification'].includes(manifest.status)) throw new Error('Invalid lifecycle status.');
  if (!['fresh', 'continuation'].includes(manifest.mode)) throw new Error('Invalid work mode.');
  validateBranch(manifest.branch);
  if (!/^[0-9a-f]{40}$/.test(String(manifest.baseSha || ''))) throw new Error('Invalid base SHA.');
  if (!/^[A-Za-z0-9._-]{16,128}$/.test(String(manifest.token || ''))) throw new Error('Invalid token.');
  if (!manifest.spawningReference) throw new Error('Manifest ownership is incomplete.');
  return manifest;
}

function workerReference(manifest, state, leaseUntil, threadId = null) {
  validateManifest(manifest);
  if (!['spawning', 'running', 'recovering'].includes(state)) throw new Error(`Invalid worker state: ${state}`);
  if (!leaseUntil) throw new Error('leaseUntil is required.');
  const parts = [
    'v=2',
    `state=${state}`,
    'adapter=codex-cli-v1',
    `generation=${manifest.generation}`,
    `token=${manifest.token}`,
    `branch=${manifest.branch}`,
    `leaseUntil=${leaseUntil}`
  ];
  if (threadId) {
    if (!/^[A-Za-z0-9._:-]{4,200}$/.test(String(threadId))) throw new Error('Invalid thread ID.');
    parts.push(`thread=${threadId}`);
  }
  return parts.join(';');
}

function command() {
  const [operation, ...args] = process.argv.slice(2);
  if (operation === 'ack') {
    const ack = spawnAcknowledgement(readJsonLines(args[0]));
    if (!ack) process.exit(3);
    process.stdout.write(`${JSON.stringify(ack)}\n`);
    return;
  }
  if (operation === 'terminal') {
    const outcome = turnOutcome(readJsonLines(args[0]));
    if (!outcome) process.exit(3);
    process.stdout.write(`${JSON.stringify(outcome)}\n`);
    return;
  }
  if (operation === 'branch') {
    const input = JSON.parse(fs.readFileSync(args[0], 'utf8'));
    process.stdout.write(`${JSON.stringify(resolveBranch(input))}\n`);
    return;
  }
  if (operation === 'reference') {
    const manifest = validateManifest(JSON.parse(fs.readFileSync(args[0], 'utf8')));
    process.stdout.write(`${workerReference(manifest, args[1], args[2], args[3] || null)}\n`);
    return;
  }
  throw new Error('Usage: cli-worker-state.js ack FILE | terminal FILE | branch JSON | reference MANIFEST STATE LEASE [THREAD]');
}

if (require.main === module) command();

module.exports = {
  explicitBranches,
  readJsonLines,
  resolveBranch,
  spawnAcknowledgement,
  turnOutcome,
  validateBranch,
  validateManifest,
  workerReference
};

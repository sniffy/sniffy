'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const root = path.join(__dirname, '../..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function bytes(relativePath) {
  return Buffer.byteLength(read(relativePath), 'utf8');
}

test('recurring prompts stay within reviewed byte budgets', () => {
  assert.ok(bytes('.chatgpt/scheduled-task-prompt.md') <= 10000, 'ChatGPT scheduler prompt exceeded 10 KB');
  assert.ok(bytes('.chatgpt/status-snapshot-instructions.md') <= 6500, 'snapshot supplement exceeded 6.5 KB');
  assert.ok(bytes('.codex/local/scheduled-task-prompt.md') <= 8500, 'Local Codex dispatcher prompt exceeded 8.5 KB');
  assert.ok(bytes('.codex/local/worker-task-prompt.md') <= 9500, 'Local Codex worker prompt exceeded 9.5 KB');
});

test('ChatGPT recurring scheduler is Chat, not Work, and reads compact policy', () => {
  const prompt = read('.chatgpt/scheduled-task-prompt.md');
  const profile = read('docs/ai-delivery/profile.yml');
  assert.match(prompt, /Create this scheduler in \*\*Chat\*\*, not \*\*Work\*\*/);
  assert.match(prompt, /runtime-contract\.md/);
  assert.match(prompt, /dispatch\.orderedCandidates/);
  assert.doesNotMatch(prompt, /docs\/ai-delivery\/\{README,profile,lifecycle,control-plane/i);
  assert.match(profile, /ChatGPT:[\s\S]*surface:\s*Chat[\s\S]*forbiddenSurface:\s*Work/);
});

test('Local Codex uses Luna dispatcher, Terra worker, and explicit Sol escalation', () => {
  const profile = read('docs/ai-delivery/profile.yml');
  const dispatcher = read('.codex/local/scheduled-task-prompt.md');
  const worker = read('.codex/local/worker-task-prompt.md');
  const runIssue = read('.codex/local/run-issue.sh');

  assert.match(profile, /dispatcherModel:\s*gpt-5\.6-luna/);
  assert.match(profile, /dispatcherReasoning:\s*low/);
  assert.match(profile, /workerModel:\s*gpt-5\.6-terra/);
  assert.match(profile, /workerReasoning:\s*medium/);
  assert.match(profile, /escalationModel:\s*gpt-5\.6-sol/);
  assert.match(profile, /escalationReasoning:\s*high/);
  assert.match(dispatcher, /gpt-5\.6-luna.*low/i);
  assert.match(dispatcher, /gpt-5\.6-terra.*medium/is);
  assert.match(worker, /gpt-5\.6-terra.*medium/is);
  assert.match(runIssue, /CODEX_MODEL:-gpt-5\.6-terra/);
  assert.match(runIssue, /CODEX_REASONING_EFFORT:-medium/);
  assert.doesNotMatch(runIssue, /CODEX_MODEL:-gpt-5\.6-sol/);
  assert.doesNotMatch(runIssue, /CODEX_REASONING_EFFORT:-xhigh/);
});

test('runtime telemetry is explicit and never fabricates token counters', () => {
  for (const file of [
    'docs/ai-delivery/runtime-contract.md',
    '.chatgpt/scheduled-task-prompt.md',
    '.codex/local/scheduled-task-prompt.md',
    '.codex/local/worker-task-prompt.md',
    '.codex/local/run-issue.sh'
  ]) {
    const content = read(file);
    assert.match(content, /startedAt/);
    assert.match(content, /durationSeconds/);
    assert.match(content, /usageSource/);
    assert.match(content, /token/i);
    assert.match(content, /null|unavailable/i);
    assert.match(content, /never (?:invent|fabricate)/i);
  }
});

test('generated status artifact includes deterministic dispatch projection', () => {
  const workflow = read('.github/workflows/delivery-status.yml');
  assert.match(workflow, /delivery-dispatch-view\.js/);
  assert.match(workflow, /--executor-assignees/);
  assert.match(workflow, /project-2-status\.json/);
});

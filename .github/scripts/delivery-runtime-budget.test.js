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

test('app-native Local Codex uses Terra medium in one thread and forbids nested spawn', () => {
  const profile = read('docs/ai-delivery/profile.yml');
  const automation = read('.codex/local/scheduled-task-prompt.md');
  const worker = read('.codex/local/worker-task-prompt.md');
  const runIssue = read('.codex/local/run-issue.sh');

  assert.match(profile, /appNativeAdapter:\s*codex-app-same-thread-v1/);
  assert.match(profile, /appNativeModel:\s*gpt-5\.6-terra/);
  assert.match(profile, /appNativeReasoning:\s*medium/);
  assert.match(profile, /appNativeSameThread:\s*true/);
  assert.doesNotMatch(profile, /dispatcherModel:\s*gpt-5\.6-luna/);
  assert.doesNotMatch(profile, /dispatcherReasoning:\s*low/);
  assert.match(profile, /workerModel:\s*gpt-5\.6-terra/);
  assert.match(profile, /workerReasoning:\s*medium/);
  assert.match(profile, /escalationModel:\s*gpt-5\.6-sol/);
  assert.match(profile, /escalationReasoning:\s*high/);

  assert.match(automation, /gpt-5\.6-terra.*medium/is);
  assert.match(automation, /adapter=codex-app-same-thread-v1/);
  assert.match(automation, /conversation=self/);
  assert.match(automation, /same conversation/i);
  assert.match(automation, /explicit branch.*overrides/is);
  assert.match(automation, /resume that one generation before considering Ready work/is);
  assert.match(automation, /Never create a child thread, child task, hidden\s+subagent, `client-new-thread:\*` reference/is);
  assert.doesNotMatch(automation, /Create exactly one standalone one-time app-owned worker task/i);
  assert.doesNotMatch(automation, /After confirmed child creation/i);

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

test('worker supervision is lease-based stale recovery rather than periodic polling', () => {
  const profile = read('docs/ai-delivery/profile.yml');
  const runtime = read('docs/ai-delivery/runtime-contract.md');
  const chat = read('.chatgpt/scheduled-task-prompt.md');
  const dispatcher = read('.codex/local/scheduled-task-prompt.md');
  const worker = read('.codex/local/worker-task-prompt.md');
  const projection = read('.github/scripts/delivery-dispatch-view.js');
  const combined = [profile, runtime, chat, dispatcher, worker, projection].join('\n');

  assert.match(profile, /mode:\s*lease-based-stale-recovery/);
  assert.match(profile, /schedulerChecksOnlyExpiredOrInvalidLease:\s*true/);
  assert.match(profile, /workerMayRenewBeforeExpiry:\s*true/);
  assert.match(profile, /leaseMinutes:[\s\S]*Codex Cloud:\s*120[\s\S]*Local Codex:\s*240/);
  assert.match(runtime, /Normal `In progress` ownership is not polled/i);
  assert.match(runtime, /missing, invalid, or expired lease/i);
  assert.match(chat, /valid future `leaseUntil` is invisible to this tick/i);
  assert.match(dispatcher, /same-thread generation.*resume/is);
  assert.match(worker, /renew the same\s+claim\/generation before expiry/i);
  assert.match(projection, /stale-owned-recovery/);
  assert.match(projection, /activeInProgressByExecutor/);
  assert.match(projection, /staleInProgressByExecutor/);

  assert.doesNotMatch(combined, /nextObservationAt/);
  assert.doesNotMatch(combined, /dueInProgress/);
  assert.doesNotMatch(combined, /due-owned-observation/);
  assert.doesNotMatch(combined, /initialObservationMinutes/);
  assert.doesNotMatch(combined, /secondObservationMinutes/);
  assert.doesNotMatch(combined, /incompleteObservationMinutes/);
  assert.doesNotMatch(combined, /15\/15\/hourly/i);
  assert.doesNotMatch(combined, /second-15-minute/i);
});

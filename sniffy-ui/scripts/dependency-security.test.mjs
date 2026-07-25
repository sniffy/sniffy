import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

import {
  assessPullRequest,
  compareAudits,
  dependencyPaths,
  dependencyInputs,
  validateExceptions,
  validateOverrides,
} from './dependency-security.mjs';
import {
  baseAudit,
  baseLockfile,
  cloneFixture,
  packageJsonWithOverride,
  validException,
  validOverride,
} from './dependency-security.fixtures.mjs';

function addAdvisory(audit, lockfile) {
  audit.vulnerabilities['serialize-javascript'] = {
    name: 'serialize-javascript',
    severity: 'high',
    isDirect: false,
    via: [
      {
        source: 1002,
        name: 'serialize-javascript',
        title: 'Fixture serialization advisory',
        url: 'https://github.com/advisories/GHSA-fixture-0002',
        severity: 'high',
        range: '<=7.0.0',
      },
    ],
    effects: [],
    range: '<=7.0.0',
    nodes: ['node_modules/serialize-javascript'],
    fixAvailable: false,
  };
  lockfile.packages['node_modules/serialize-javascript'] = {
    version: '7.0.0',
  };
}

const workflowPolicy = `name: Check Pull Request
# dependency-security-policy:start
- name: Install frontend dependencies
  run: npm ci
# dependency-security-policy:end
- name: Lint
  run: npm run lint
`;

test('trusts the mounted CI workspace before the live pull-request comparison', () => {
  const workflow = readFileSync(new URL('../../.github/workflows/pr.yml', import.meta.url), 'utf8');
  const comparisonStep = workflow.match(
    /- name: Compare dependency vulnerability risk\n([\s\S]*?)(?=\n {6}- name:)/,
  )?.[0];

  assert.ok(comparisonStep, 'dependency comparison step must remain present');
  const trustWorkspace = 'git config --global --add safe.directory "$GITHUB_WORKSPACE"';
  assert.ok(
    comparisonStep.indexOf(trustWorkspace) >= 0,
    'container HOME must trust the mounted workspace independently of actions/checkout',
  );
  assert.ok(
    comparisonStep.indexOf(trustWorkspace) <
      comparisonStep.indexOf('npm run dependency-security:pr'),
    'workspace trust must be configured before Git merge-base inspection',
  );
});

function manifest(change = {}) {
  return JSON.stringify({
    name: '@sniffy/frontend',
    version: '0.0.0',
    private: true,
    scripts: {
      'test:site': 'npm run test:site',
      ...change.scripts,
    },
    devDependencies: {
      vitest: '4.1.10',
      ...change.devDependencies,
    },
    ...change.fields,
  });
}

test('classifies dependency inputs semantically', () => {
  assert.deepEqual(
    dependencyInputs([
      { file: 'README.md' },
      {
        file: 'sniffy-ui/apps/site/package.json',
        baseContent: manifest(),
        headContent: manifest({ devDependencies: { vitest: '4.2.0' } }),
      },
      { file: 'sniffy-ui/package-lock.json' },
      { file: 'sniffy-ui/.npmrc' },
      { file: 'sniffy-ui/dependency-security-exceptions.json' },
      {
        file: '.github/workflows/pr.yml',
        baseContent: workflowPolicy,
        headContent: workflowPolicy.replace('npm ci', 'npm ci --ignore-scripts'),
      },
    ]),
    [
      '.github/workflows/pr.yml',
      'sniffy-ui/.npmrc',
      'sniffy-ui/apps/site/package.json',
      'sniffy-ui/dependency-security-exceptions.json',
      'sniffy-ui/package-lock.json',
    ],
  );
});

test('treats introducing or removing workflow policy sections as dependency-impacting', () => {
  assert.deepEqual(
    dependencyInputs([
      {
        file: '.github/workflows/pr.yml',
        baseContent: 'name: Check Pull Request\n',
        headContent: workflowPolicy,
      },
    ]),
    ['.github/workflows/pr.yml'],
  );
});

test('skips #710-style script-only manifest changes and unrelated workflow edits', () => {
  assert.deepEqual(
    dependencyInputs([
      {
        file: 'sniffy-ui/package.json',
        baseContent: manifest(),
        headContent: manifest({
          scripts: {
            'test:site': 'npm run test:site apps/site/src/traffic-capture-use-case.test.tsx',
          },
        }),
      },
      {
        file: '.github/workflows/pr.yml',
        baseContent: workflowPolicy,
        headContent: workflowPolicy.replace('npm run lint', 'npm run lint -- --quiet'),
      },
    ]),
    [],
  );
});

test('detects override and install-lifecycle manifest changes', () => {
  assert.deepEqual(
    dependencyInputs([
      {
        file: 'sniffy-ui/package.json',
        baseContent: manifest(),
        headContent: manifest({
          fields: { overrides: { minimatch: '10.2.5' } },
        }),
      },
      {
        file: 'sniffy-ui/apps/site/package.json',
        baseContent: manifest(),
        headContent: manifest({
          scripts: { postinstall: 'node scripts/postinstall.mjs' },
        }),
      },
    ]),
    ['sniffy-ui/apps/site/package.json', 'sniffy-ui/package.json'],
  );
});

test('detects audit-policy script changes while ignoring dependency key order', () => {
  assert.deepEqual(
    dependencyInputs([
      {
        file: 'sniffy-ui/package.json',
        baseContent: manifest({
          scripts: { 'dependency-security:pr': 'node scripts/dependency-security.mjs pr' },
        }),
        headContent: manifest({
          scripts: { 'dependency-security:pr': 'echo skipped' },
        }),
      },
      {
        file: 'sniffy-ui/apps/site/package.json',
        baseContent: manifest({
          devDependencies: { alpha: '1.0.0', zulu: '1.0.0' },
        }),
        headContent: manifest({
          devDependencies: { zulu: '1.0.0', alpha: '1.0.0' },
        }),
      },
    ]),
    ['sniffy-ui/package.json'],
  );
});

test('skips an unchanged dependency graph without requiring audit fixtures', () => {
  const result = assessPullRequest({
    dependencyInputFiles: [],
  });

  assert.equal(result.skipped, true);
  assert.match(result.reason, /unchanged/);
});

test('passes unchanged pre-existing findings', () => {
  const result = compareAudits({
    baseAudit,
    baseLockfile,
    headAudit: cloneFixture(baseAudit),
    headLockfile: cloneFixture(baseLockfile),
  });

  assert.deepEqual(result.regressions, []);
  assert.equal(result.baseFindingCount, result.headFindingCount);
});

test('fails a newly introduced advisory', () => {
  const headAudit = cloneFixture(baseAudit);
  const headLockfile = cloneFixture(baseLockfile);
  addAdvisory(headAudit, headLockfile);

  const result = compareAudits({
    baseAudit,
    baseLockfile,
    headAudit,
    headLockfile,
  });

  assert.ok(
    result.regressions.some(
      (regression) =>
        regression.type === 'new-advisory' && regression.advisoryId === 'GHSA-FIXTURE-0002',
    ),
  );
});

test('fails new paths and an occurrence-count increase', () => {
  const headAudit = cloneFixture(baseAudit);
  const headLockfile = cloneFixture(baseLockfile);
  headAudit.vulnerabilities['brace-expansion'].nodes.push(
    'node_modules/tool/node_modules/brace-expansion',
  );
  headLockfile.packages['node_modules/tool/node_modules/brace-expansion'] = {
    version: '1.0.0',
  };

  const result = compareAudits({
    baseAudit,
    baseLockfile,
    headAudit,
    headLockfile,
  });

  assert.ok(result.regressions.some((regression) => regression.type === 'new-vulnerable-path'));
  assert.ok(
    result.regressions.some((regression) => regression.type === 'occurrence-count-increase'),
  );
});

test('detects a new logical parent path even when npm hoists the same node', () => {
  const baseGraph = {
    packages: {
      '': {
        devDependencies: {
          parent: '1.0.0',
        },
      },
      'node_modules/parent': {
        version: '1.0.0',
        dependencies: {
          'brace-expansion': '1.0.0',
        },
      },
      'node_modules/brace-expansion': {
        version: '1.0.0',
      },
    },
  };
  const headGraph = cloneFixture(baseGraph);
  headGraph.packages[''].devDependencies['second-parent'] = '1.0.0';
  headGraph.packages['node_modules/second-parent'] = {
    version: '1.0.0',
    dependencies: {
      'brace-expansion': '1.0.0',
    },
  };

  assert.deepEqual(dependencyPaths(baseGraph, 'node_modules/brace-expansion'), [
    'node_modules/parent > brace-expansion@1.0.0 [node_modules/brace-expansion]',
    'sniffy-ui > parent@1.0.0 [node_modules/parent]',
  ]);

  const result = compareAudits({
    baseAudit,
    baseLockfile: baseGraph,
    headAudit: cloneFixture(baseAudit),
    headLockfile: headGraph,
  });
  assert.ok(
    result.regressions.some(
      (regression) =>
        regression.type === 'new-vulnerable-path' &&
        regression.finding.path.includes('second-parent'),
    ),
  );
});

test('resolves logical paths without following dependency cycles', () => {
  const graph = {
    packages: {
      '': {
        devDependencies: {
          parent: '1.0.0',
        },
      },
      'node_modules/parent': {
        version: '1.0.0',
        dependencies: {
          child: '1.0.0',
        },
      },
      'node_modules/child': {
        version: '1.0.0',
        dependencies: {
          parent: '1.0.0',
          target: '1.0.0',
        },
      },
      'node_modules/target': {
        version: '1.0.0',
      },
    },
  };

  assert.deepEqual(dependencyPaths(graph, 'node_modules/target'), [
    'node_modules/child > parent@1.0.0 [node_modules/parent]',
    'node_modules/child > target@1.0.0 [node_modules/target]',
    'node_modules/parent > child@1.0.0 [node_modules/child]',
    'sniffy-ui > parent@1.0.0 [node_modules/parent]',
  ]);
});

test('fails a severity increase', () => {
  const headAudit = cloneFixture(baseAudit);
  headAudit.vulnerabilities['brace-expansion'].severity = 'high';
  headAudit.vulnerabilities['brace-expansion'].via[0].severity = 'high';

  const result = compareAudits({
    baseAudit,
    baseLockfile,
    headAudit,
    headLockfile: cloneFixture(baseLockfile),
  });

  assert.ok(result.regressions.some((regression) => regression.type === 'severity-increase'));
});

test('requires complete, current, repository-linked exceptions', () => {
  assert.deepEqual(
    validateExceptions({ exceptions: [validException()] }, new Date('2026-07-25T00:00:00Z')),
    [],
  );

  const errors = validateExceptions(
    {
      exceptions: [
        validException({
          risk: '',
          reviewDate: '2020-01-01',
          authorizationPullRequest: 'https://example.com/pr/1',
        }),
      ],
    },
    new Date('2026-07-25T00:00:00Z'),
  );
  assert.ok(errors.some((error) => error.includes('.risk is required')));
  assert.ok(errors.some((error) => error.includes('is stale')));
  assert.ok(errors.some((error) => error.includes('must link a sniffy/sniffy pull request')));
});

test('an explicit narrow exception covers only its exact finding', () => {
  const headAudit = cloneFixture(baseAudit);
  const headLockfile = cloneFixture(baseLockfile);
  addAdvisory(headAudit, headLockfile);

  const result = compareAudits({
    baseAudit,
    baseLockfile,
    headAudit,
    headLockfile,
    exceptions: [validException()],
  });

  assert.deepEqual(result.regressions, []);
  assert.equal(result.appliedExceptions.length, 1);
});

test('rejects undocumented, stale, mismatched, and removed overrides', () => {
  assert.deepEqual(
    validateOverrides(
      packageJsonWithOverride,
      { overrides: [validOverride()] },
      new Date('2026-07-25T00:00:00Z'),
    ),
    [],
  );

  assert.ok(
    validateOverrides(
      packageJsonWithOverride,
      { overrides: [] },
      new Date('2026-07-25T00:00:00Z'),
    ).some((error) => error.includes('is undocumented')),
  );
  assert.ok(
    validateOverrides(
      { overrides: {} },
      { overrides: [validOverride()] },
      new Date('2026-07-25T00:00:00Z'),
    ).some((error) => error.includes('no longer exists')),
  );
  assert.ok(
    validateOverrides(
      packageJsonWithOverride,
      {
        overrides: [validOverride({ version: '3.0.0', reviewDate: '2020-01-01' })],
      },
      new Date('2026-07-25T00:00:00Z'),
    ).some((error) => error.includes('package.json') || error.includes('is stale')),
  );
});

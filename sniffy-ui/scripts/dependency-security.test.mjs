import assert from 'node:assert/strict';
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

test('detects manifests, lockfiles, npm configuration, and policy inputs', () => {
  assert.deepEqual(
    dependencyInputs([
      'README.md',
      'sniffy-ui/apps/site/package.json',
      'sniffy-ui/package-lock.json',
      'sniffy-ui/.npmrc',
      'sniffy-ui/dependency-security-exceptions.json',
      '.github/workflows/pr.yml',
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

test('skips an unchanged dependency graph without requiring audit fixtures', () => {
  const result = assessPullRequest({
    changedFiles: ['sniffy-ui/apps/site/src/pages/index.tsx'],
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

export const baseAudit = {
  vulnerabilities: {
    'brace-expansion': {
      name: 'brace-expansion',
      severity: 'moderate',
      isDirect: false,
      via: [
        {
          source: 1001,
          name: 'brace-expansion',
          dependency: 'brace-expansion',
          title: 'Fixture expansion advisory',
          url: 'https://github.com/advisories/GHSA-fixture-0001',
          severity: 'moderate',
          range: '<=1.0.0',
        },
      ],
      effects: ['minimatch'],
      range: '<=1.0.0',
      nodes: ['node_modules/brace-expansion'],
      fixAvailable: false,
    },
    minimatch: {
      name: 'minimatch',
      severity: 'moderate',
      isDirect: false,
      via: ['brace-expansion'],
      effects: [],
      range: '<=3.0.0',
      nodes: ['node_modules/minimatch'],
      fixAvailable: false,
    },
  },
  metadata: {
    vulnerabilities: {
      info: 0,
      low: 0,
      moderate: 2,
      high: 0,
      critical: 0,
      total: 2,
    },
  },
};

export const baseLockfile = {
  lockfileVersion: 3,
  packages: {
    '': { name: 'fixture' },
    'node_modules/brace-expansion': { version: '1.0.0' },
    'node_modules/minimatch': { version: '3.0.0' },
  },
};

export function cloneFixture(value) {
  return JSON.parse(JSON.stringify(value));
}

export function validException(overrides = {}) {
  return {
    advisoryId: 'GHSA-FIXTURE-0002',
    package: 'serialize-javascript',
    version: '7.0.0',
    path: 'node_modules/serialize-javascript',
    noSafeAlternative: 'No compatible released parent is available.',
    risk: 'Build-time serialization only; no untrusted runtime input.',
    owner: 'sniffy maintainers',
    reviewDate: '2099-01-01',
    followUpIssue: 'https://github.com/sniffy/sniffy/issues/712',
    removalCondition: 'Remove when the parent accepts a patched compatible line.',
    authorizationIssue: 'https://github.com/sniffy/sniffy/issues/711',
    authorizationPullRequest: 'https://github.com/sniffy/sniffy/pull/999',
    ...overrides,
  };
}

export const packageJsonWithOverride = {
  overrides: {
    parent: {
      child: '2.0.0',
    },
  },
};

export function validOverride(overrides = {}) {
  return {
    selector: 'parent>child',
    version: '2.0.0',
    rationale: 'The compatible parent range remains vulnerable.',
    dependencyPath: 'parent -> child',
    compatibilityProof: 'Clean install and focused parent behavior pass.',
    upstream: 'https://github.com/example/parent',
    owner: 'sniffy maintainers',
    reviewDate: '2099-01-01',
    removalCondition: 'Remove when parent accepts child 2.x.',
    authorization: 'https://github.com/sniffy/sniffy/pull/681',
    ...overrides,
  };
}

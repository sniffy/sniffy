import { spawnSync } from 'node:child_process';
import { appendFile, mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const workspaceDirectory = path.resolve(scriptDirectory, '..');
const repositoryDirectory = path.resolve(workspaceDirectory, '..');
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const severityRank = new Map([
  ['info', 0],
  ['low', 1],
  ['moderate', 2],
  ['high', 3],
  ['critical', 4],
]);

const alwaysDependencyInputFiles = new Set([
  '.github/workflows/dependency-security.yml',
  'sniffy-ui/dependency-security-exceptions.json',
  'sniffy-ui/dependency-security-overrides.json',
  'sniffy-ui/scripts/dependency-security.mjs',
]);
const manifestDependencyFields = [
  'name',
  'version',
  'workspaces',
  'packageManager',
  'engines',
  'devEngines',
  'os',
  'cpu',
  'libc',
  'bin',
  'config',
  'dependencies',
  'devDependencies',
  'optionalDependencies',
  'peerDependencies',
  'peerDependenciesMeta',
  'bundleDependencies',
  'bundledDependencies',
  'overrides',
];
const installLifecycleScripts = [
  'preinstall',
  'install',
  'postinstall',
  'prepublish',
  'preprepare',
  'prepare',
  'postprepare',
];
const dependencyPolicyScriptPattern = /^(?:dependency-security(?::|$)|audit(?::|$))/;
const workflowPolicyStart = '# dependency-security-policy:start';
const workflowPolicyEnd = '# dependency-security-policy:end';

function isDependencyInputCandidate(file) {
  const normalized = file.replaceAll('\\', '/').replace(/^\.\/+/, '');

  return (
    alwaysDependencyInputFiles.has(normalized) ||
    normalized === '.github/workflows/pr.yml' ||
    /^sniffy-ui\/(?:.+\/)?(?:package(?:-lock)?|npm-shrinkwrap)\.json$/.test(normalized) ||
    /^sniffy-ui\/(?:.+\/)?\.npmrc$/.test(normalized)
  );
}

function manifestDependencyState(content) {
  if (content === null) {
    return null;
  }

  const manifest = JSON.parse(content);
  const state = Object.fromEntries(
    manifestDependencyFields
      .filter((field) => Object.hasOwn(manifest, field))
      .map((field) => [field, canonicalJsonValue(manifest[field])]),
  );
  const dependencyScripts = Object.fromEntries(
    Object.entries(manifest.scripts ?? {})
      .filter(
        ([script]) =>
          installLifecycleScripts.includes(script) || dependencyPolicyScriptPattern.test(script),
      )
      .sort(([left], [right]) => left.localeCompare(right)),
  );
  if (Object.keys(dependencyScripts).length > 0) {
    state.dependencyScripts = dependencyScripts;
  }
  return state;
}

function canonicalJsonValue(value) {
  if (Array.isArray(value)) {
    return value.map(canonicalJsonValue);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, nested]) => [key, canonicalJsonValue(nested)]),
    );
  }
  return value;
}

function workflowDependencyPolicy(content) {
  if (content === null) {
    return null;
  }

  const sections = [];
  let active = false;
  let current = [];
  for (const line of content.split(/\r?\n/)) {
    if (line.includes(workflowPolicyStart)) {
      if (active) {
        throw new Error(`nested ${workflowPolicyStart} marker`);
      }
      active = true;
      current = [];
      continue;
    }
    if (line.includes(workflowPolicyEnd)) {
      if (!active) {
        throw new Error(`${workflowPolicyEnd} marker has no matching start`);
      }
      sections.push(current.join('\n').trim());
      active = false;
      continue;
    }
    if (active) {
      current.push(line);
    }
  }
  if (active) {
    throw new Error(`${workflowPolicyStart} marker has no matching end`);
  }
  return sections;
}

function dependencyInputChanged({ file, baseContent, headContent }) {
  const normalized = file.replaceAll('\\', '/').replace(/^\.\/+/, '');
  if (!isDependencyInputCandidate(normalized)) {
    return false;
  }
  if (alwaysDependencyInputFiles.has(normalized)) {
    return true;
  }
  if (
    /^sniffy-ui\/(?:.+\/)?(?:package-lock|npm-shrinkwrap)\.json$/.test(normalized) ||
    /^sniffy-ui\/(?:.+\/)?\.npmrc$/.test(normalized)
  ) {
    return true;
  }
  if (baseContent === undefined || headContent === undefined) {
    throw new Error(`${normalized} requires base and head content for semantic classification`);
  }

  const baseState =
    normalized === '.github/workflows/pr.yml'
      ? workflowDependencyPolicy(baseContent)
      : manifestDependencyState(baseContent);
  const headState =
    normalized === '.github/workflows/pr.yml'
      ? workflowDependencyPolicy(headContent)
      : manifestDependencyState(headContent);
  return JSON.stringify(baseState) !== JSON.stringify(headState);
}

export function dependencyInputs(changes) {
  return [
    ...new Set(
      changes
        .filter(dependencyInputChanged)
        .map(({ file }) => file.replaceAll('\\', '/').replace(/^\.\/+/, '')),
    ),
  ].sort();
}

function advisoryId(via) {
  const githubAdvisory = via.url?.match(/\/(GHSA-[a-z0-9-]+)$/i)?.[1];
  if (githubAdvisory) {
    return githubAdvisory.toUpperCase();
  }
  if (via.source !== undefined && via.source !== null) {
    return `npm:${via.source}`;
  }
  return `npm:${via.name}:${via.title}`;
}

function maxSeverity(left, right) {
  return (severityRank.get(right) ?? -1) > (severityRank.get(left) ?? -1) ? right : left;
}

function advisoriesFor(packageName, audit, visiting = new Set()) {
  if (visiting.has(packageName)) {
    return new Map();
  }

  const vulnerability = audit.vulnerabilities?.[packageName];
  if (!vulnerability) {
    return new Map();
  }

  const nextVisiting = new Set(visiting);
  nextVisiting.add(packageName);
  const advisories = new Map();

  for (const via of vulnerability.via ?? []) {
    if (typeof via === 'string') {
      for (const [id, advisory] of advisoriesFor(via, audit, nextVisiting)) {
        advisories.set(id, advisory);
      }
      continue;
    }

    const id = advisoryId(via);
    advisories.set(id, {
      id,
      title: via.title ?? via.name ?? id,
      url: via.url ?? null,
      severity: via.severity ?? vulnerability.severity ?? 'info',
    });
  }

  return advisories;
}

function resolutionParent(packagePath) {
  const nestedIndex = packagePath.lastIndexOf('/node_modules/');
  if (nestedIndex >= 0) {
    return packagePath.slice(0, nestedIndex);
  }
  if (packagePath.startsWith('node_modules/')) {
    return '';
  }
  const parent = path.posix.dirname(packagePath);
  return parent === '.' ? '' : parent;
}

function resolveDependency(packages, sourcePath, dependency) {
  let scope = sourcePath;
  while (true) {
    const candidate = scope ? `${scope}/node_modules/${dependency}` : `node_modules/${dependency}`;
    if (packages[candidate]) {
      return candidate;
    }
    if (!scope) {
      return null;
    }
    scope = resolutionParent(scope);
  }
}

export function dependencyPaths(lockfile, targetPath) {
  const packages = lockfile.packages ?? {};
  const roots = [
    '',
    ...Object.keys(packages).filter(
      (packagePath) =>
        packagePath &&
        !packagePath.startsWith('node_modules/') &&
        !packagePath.includes('/node_modules/'),
    ),
  ];
  const outgoing = new Map();
  const incoming = new Map();

  for (const [packagePath, packageEntry] of Object.entries(packages)) {
    const dependencies = {
      ...packageEntry.dependencies,
      ...packageEntry.devDependencies,
      ...packageEntry.optionalDependencies,
    };
    for (const dependency of Object.keys(dependencies).sort()) {
      const resolved = resolveDependency(packages, packagePath, dependency);
      if (!resolved) {
        continue;
      }
      const edge = {
        source: packagePath,
        target: resolved,
        dependency,
        version: packages[resolved]?.version ?? 'unknown',
      };
      outgoing.set(packagePath, [...(outgoing.get(packagePath) ?? []), edge]);
      incoming.set(resolved, [...(incoming.get(resolved) ?? []), edge]);
    }
  }

  const reachable = new Set(roots);
  const reachableQueue = [...roots];
  while (reachableQueue.length > 0) {
    const source = reachableQueue.shift();
    for (const edge of outgoing.get(source) ?? []) {
      if (!reachable.has(edge.target)) {
        reachable.add(edge.target);
        reachableQueue.push(edge.target);
      }
    }
  }

  const reverseQueue = [targetPath];
  const reverseSeen = new Set(reverseQueue);
  const discovered = new Set();

  while (reverseQueue.length > 0) {
    const current = reverseQueue.shift();
    for (const edge of incoming.get(current) ?? []) {
      if (!reachable.has(edge.source)) {
        continue;
      }
      const source = edge.source || 'sniffy-ui';
      discovered.add(`${source} > ${edge.dependency}@${edge.version} [${edge.target}]`);
      if (!reverseSeen.has(edge.source)) {
        reverseSeen.add(edge.source);
        reverseQueue.push(edge.source);
      }
    }
  }

  return discovered.size > 0 ? [...discovered].sort() : [targetPath];
}

export function auditFindings(audit, lockfile) {
  const findings = [];

  for (const [packageName, vulnerability] of Object.entries(audit.vulnerabilities ?? {})) {
    const advisories = advisoriesFor(packageName, audit);
    for (const nodePath of vulnerability.nodes ?? []) {
      const version = lockfile.packages?.[nodePath]?.version ?? 'unknown';
      for (const dependencyPath of dependencyPaths(lockfile, nodePath)) {
        for (const advisory of advisories.values()) {
          findings.push({
            advisoryId: advisory.id,
            title: advisory.title,
            url: advisory.url,
            severity: maxSeverity(advisory.severity, vulnerability.severity ?? 'info'),
            package: vulnerability.name ?? packageName,
            version,
            path: dependencyPath,
            nodePath,
          });
        }
      }
    }
  }

  return findings.sort((left, right) =>
    [left.advisoryId, left.package, left.version, left.path]
      .join('\0')
      .localeCompare([right.advisoryId, right.package, right.version, right.path].join('\0')),
  );
}

function groupFindings(findings) {
  const groups = new Map();

  for (const finding of findings) {
    const group = groups.get(finding.advisoryId) ?? {
      advisoryId: finding.advisoryId,
      title: finding.title,
      url: finding.url,
      severity: 'info',
      findings: [],
    };
    group.severity = maxSeverity(group.severity, finding.severity);
    group.findings.push(finding);
    groups.set(finding.advisoryId, group);
  }

  return groups;
}

function findingKey(finding) {
  return `${finding.package}@${finding.version}:${finding.path}`;
}

function matchesException(finding, exception) {
  return (
    finding.advisoryId === exception.advisoryId &&
    finding.package === exception.package &&
    finding.version === exception.version &&
    finding.path === exception.path
  );
}

export function compareAudits({
  baseAudit,
  baseLockfile,
  headAudit,
  headLockfile,
  exceptions = [],
}) {
  const baseGroups = groupFindings(auditFindings(baseAudit, baseLockfile));
  const headGroups = groupFindings(auditFindings(headAudit, headLockfile));
  const regressions = [];
  const appliedExceptions = [];

  for (const [id, headGroup] of headGroups) {
    const baseGroup = baseGroups.get(id);
    const unexceptedHead = headGroup.findings.filter((finding) => {
      const exception = exceptions.find((candidate) => matchesException(finding, candidate));
      if (exception) {
        appliedExceptions.push({
          advisoryId: finding.advisoryId,
          package: finding.package,
          version: finding.version,
          path: finding.path,
          followUpIssue: exception.followUpIssue,
          reviewDate: exception.reviewDate,
        });
        return false;
      }
      return true;
    });

    if (!baseGroup) {
      if (unexceptedHead.length > 0) {
        regressions.push({
          type: 'new-advisory',
          advisoryId: id,
          severity: headGroup.severity,
          findings: unexceptedHead,
        });
      }
      continue;
    }

    if (
      (severityRank.get(headGroup.severity) ?? -1) > (severityRank.get(baseGroup.severity) ?? -1) &&
      unexceptedHead.length > 0
    ) {
      regressions.push({
        type: 'severity-increase',
        advisoryId: id,
        baseSeverity: baseGroup.severity,
        headSeverity: headGroup.severity,
        findings: unexceptedHead,
      });
    }

    const baseKeys = new Set(baseGroup.findings.map(findingKey));
    const newFindings = unexceptedHead.filter((finding) => !baseKeys.has(findingKey(finding)));
    for (const finding of newFindings) {
      regressions.push({
        type: 'new-vulnerable-path',
        advisoryId: id,
        finding,
      });
    }

    if (headGroup.findings.length > baseGroup.findings.length && newFindings.length > 0) {
      regressions.push({
        type: 'occurrence-count-increase',
        advisoryId: id,
        baseCount: baseGroup.findings.length,
        headCount: headGroup.findings.length,
        findings: newFindings,
      });
    }
  }

  return {
    baseFindingCount: [...baseGroups.values()].reduce(
      (total, group) => total + group.findings.length,
      0,
    ),
    headFindingCount: [...headGroups.values()].reduce(
      (total, group) => total + group.findings.length,
      0,
    ),
    regressions,
    appliedExceptions,
  };
}

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

function validateReviewDate(value, now, label, errors) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value ?? '')) {
    errors.push(`${label} must use YYYY-MM-DD`);
    return;
  }

  const reviewDate = new Date(`${value}T23:59:59Z`);
  if (Number.isNaN(reviewDate.valueOf()) || reviewDate < now) {
    errors.push(`${label} is stale (${value})`);
  }
}

function validateRepositoryUrl(value, kind, label, errors) {
  const pattern =
    kind === 'issue'
      ? /^https:\/\/github\.com\/sniffy\/sniffy\/issues\/\d+$/
      : /^https:\/\/github\.com\/sniffy\/sniffy\/pull\/\d+$/;
  if (!pattern.test(value ?? '')) {
    errors.push(`${label} must link a sniffy/sniffy ${kind}`);
  }
}

export function validateExceptions(registry, now = new Date()) {
  const errors = [];
  if (!Array.isArray(registry.exceptions)) {
    return ['dependency-security-exceptions.json must contain exceptions[]'];
  }

  const seen = new Set();
  for (const [index, exception] of registry.exceptions.entries()) {
    const label = `exceptions[${index}]`;
    for (const field of [
      'advisoryId',
      'package',
      'version',
      'path',
      'noSafeAlternative',
      'risk',
      'owner',
      'reviewDate',
      'followUpIssue',
      'removalCondition',
      'authorizationIssue',
      'authorizationPullRequest',
    ]) {
      if (!isNonEmptyString(exception[field])) {
        errors.push(`${label}.${field} is required`);
      }
    }
    validateReviewDate(exception.reviewDate, now, `${label}.reviewDate`, errors);
    validateRepositoryUrl(exception.followUpIssue, 'issue', `${label}.followUpIssue`, errors);
    validateRepositoryUrl(
      exception.authorizationIssue,
      'issue',
      `${label}.authorizationIssue`,
      errors,
    );
    validateRepositoryUrl(
      exception.authorizationPullRequest,
      'pull request',
      `${label}.authorizationPullRequest`,
      errors,
    );

    const key = [exception.advisoryId, exception.package, exception.version, exception.path].join(
      ':',
    );
    if (seen.has(key)) {
      errors.push(`${label} duplicates ${key}`);
    }
    seen.add(key);
  }

  return errors;
}

export function overrideLeaves(overrides, parents = []) {
  const leaves = [];
  for (const [name, value] of Object.entries(overrides ?? {})) {
    const selector = [...parents, name];
    if (typeof value === 'string') {
      leaves.push({ selector: selector.join('>'), version: value });
    } else if (value && typeof value === 'object') {
      leaves.push(...overrideLeaves(value, selector));
    }
  }
  return leaves.sort((left, right) => left.selector.localeCompare(right.selector));
}

export function validateOverrides(packageJson, registry, now = new Date()) {
  const errors = [];
  if (!Array.isArray(registry.overrides)) {
    return ['dependency-security-overrides.json must contain overrides[]'];
  }

  const manifest = new Map(
    overrideLeaves(packageJson.overrides).map((entry) => [entry.selector, entry.version]),
  );
  const documented = new Map();

  for (const [index, entry] of registry.overrides.entries()) {
    const label = `overrides[${index}]`;
    for (const field of [
      'selector',
      'version',
      'rationale',
      'dependencyPath',
      'compatibilityProof',
      'upstream',
      'owner',
      'reviewDate',
      'removalCondition',
      'authorization',
    ]) {
      if (!isNonEmptyString(entry[field])) {
        errors.push(`${label}.${field} is required`);
      }
    }
    if (!/^https:\/\//.test(entry.upstream ?? '')) {
      errors.push(`${label}.upstream must be an HTTPS URL`);
    }
    if (!/^https:\/\/github\.com\/sniffy\/sniffy\/pull\/\d+$/.test(entry.authorization ?? '')) {
      errors.push(`${label}.authorization must link a sniffy/sniffy pull request`);
    }
    validateReviewDate(entry.reviewDate, now, `${label}.reviewDate`, errors);
    if (documented.has(entry.selector)) {
      errors.push(`${label} duplicates ${entry.selector}`);
    }
    documented.set(entry.selector, entry.version);
  }

  for (const [selector, version] of manifest) {
    if (!documented.has(selector)) {
      errors.push(`override ${selector}@${version} is undocumented`);
    } else if (documented.get(selector) !== version) {
      errors.push(
        `override ${selector} is ${version} in package.json but ${documented.get(
          selector,
        )} in the registry`,
      );
    }
  }
  for (const [selector] of documented) {
    if (!manifest.has(selector)) {
      errors.push(`documented override ${selector} no longer exists`);
    }
  }

  return errors;
}

export function assessPullRequest({
  dependencyInputFiles,
  baseAudit,
  baseLockfile,
  headAudit,
  headLockfile,
  exceptions = [],
}) {
  const inputs = dependencyInputFiles;
  if (inputs.length === 0) {
    return {
      skipped: true,
      reason: 'PR dependency graph is unchanged',
      dependencyInputs: [],
      regressions: [],
      appliedExceptions: [],
    };
  }

  return {
    skipped: false,
    dependencyInputs: inputs,
    ...compareAudits({
      baseAudit,
      baseLockfile,
      headAudit,
      headLockfile,
      exceptions,
    }),
  };
}

function run(command, arguments_, options = {}) {
  const result = spawnSync(command, arguments_, {
    cwd: options.cwd ?? repositoryDirectory,
    encoding: 'utf8',
    env: process.env,
  });
  if (result.error) {
    throw result.error;
  }
  if (!options.accept?.includes(result.status) && result.status !== 0) {
    throw new Error(
      `${command} ${arguments_.join(' ')} failed with ${result.status}\n${
        result.stdout
      }\n${result.stderr}`,
    );
  }
  return result;
}

function revisionFile(revision, file) {
  const result = run('git', ['show', `${revision}:${file}`], { accept: [0, 128] });
  return result.status === 0 ? result.stdout : null;
}

function dependencyChanges(changedFiles, base, head) {
  return changedFiles.filter(isDependencyInputCandidate).map((file) => {
    const normalized = file.replaceAll('\\', '/').replace(/^\.\/+/, '');
    const needsSemanticContent =
      normalized === '.github/workflows/pr.yml' ||
      /^sniffy-ui\/(?:.+\/)?package\.json$/.test(normalized);
    return {
      file,
      ...(needsSemanticContent
        ? {
            baseContent: revisionFile(base, file),
            headContent: revisionFile(head, file),
          }
        : {}),
    };
  });
}

async function readJson(file) {
  return JSON.parse(await readFile(file, 'utf8'));
}

async function loadRegistries(directory = workspaceDirectory) {
  const [packageJson, exceptions, overrides] = await Promise.all([
    readJson(path.join(directory, 'package.json')),
    readJson(path.join(directory, 'dependency-security-exceptions.json')),
    readJson(path.join(directory, 'dependency-security-overrides.json')),
  ]);
  return { packageJson, exceptions, overrides };
}

async function validateRegistries() {
  const registries = await loadRegistries();
  const errors = [
    ...validateExceptions(registries.exceptions),
    ...validateOverrides(registries.packageJson, registries.overrides),
  ];
  if (errors.length > 0) {
    throw new Error(`Dependency security policy validation failed:\n- ${errors.join('\n- ')}`);
  }
  return registries;
}

function parseAudit(result) {
  try {
    return JSON.parse(result.stdout);
  } catch {
    throw new Error(`npm audit did not return JSON:\n${result.stdout}\n${result.stderr}`);
  }
}

async function auditRevision(revision) {
  const temporaryDirectory = await mkdtemp(path.join(tmpdir(), 'sniffy-dependency-security-'));
  const checkout = path.join(temporaryDirectory, 'checkout');
  let worktreeAdded = false;

  try {
    run('git', ['worktree', 'add', '--detach', checkout, revision]);
    worktreeAdded = true;
    const directory = path.join(checkout, 'sniffy-ui');
    run(npmCommand, ['ci'], { cwd: directory });
    const audit = parseAudit(
      run(npmCommand, ['audit', '--json'], {
        cwd: directory,
        accept: [0, 1],
      }),
    );
    const lockfile = await readJson(path.join(directory, 'package-lock.json'));
    return { audit, lockfile };
  } finally {
    if (worktreeAdded) {
      run('git', ['worktree', 'remove', '--force', checkout], {
        accept: [0],
      });
    }
    await rm(temporaryDirectory, { force: true, recursive: true });
  }
}

function argumentValue(arguments_, name) {
  const index = arguments_.indexOf(name);
  if (index < 0 || !arguments_[index + 1]) {
    throw new Error(`${name} is required`);
  }
  return arguments_[index + 1];
}

function comparisonMarkdown(result, context) {
  const lines = [
    '## Dependency vulnerability comparison',
    '',
    `- Node: ${process.version}`,
    `- npm: ${context.npmVersion}`,
    `- Merge base: \`${context.mergeBase}\``,
    `- Dependency inputs changed: ${result.dependencyInputs.length}`,
    ...result.dependencyInputs.map((file) => `  - \`${file}\``),
    `- Base vulnerable findings: ${result.baseFindingCount}`,
    `- Head vulnerable findings: ${result.headFindingCount}`,
    `- Authorized exceptions applied: ${result.appliedExceptions.length}`,
    `- Regressions: ${result.regressions.length}`,
  ];

  for (const regression of result.regressions) {
    lines.push(
      '',
      `### ${regression.type}: ${regression.advisoryId}`,
      '',
      '```json',
      JSON.stringify(regression, null, 2),
      '```',
    );
  }
  return `${lines.join('\n')}\n`;
}

async function writeSummary(markdown) {
  if (process.env.GITHUB_STEP_SUMMARY) {
    await appendFile(process.env.GITHUB_STEP_SUMMARY, markdown);
  }
  process.stdout.write(markdown);
}

async function runPullRequest(arguments_) {
  const base = argumentValue(arguments_, '--base');
  const head = argumentValue(arguments_, '--head');
  const auditHead = argumentValue(arguments_, '--audit-head');
  const { exceptions } = await validateRegistries();
  const mergeBase = run('git', ['merge-base', base, head]).stdout.trim();
  const changedFiles = run('git', ['diff', '--name-only', `${mergeBase}...${head}`])
    .stdout.split(/\r?\n/)
    .filter(Boolean);
  const inputs = dependencyInputs(dependencyChanges(changedFiles, mergeBase, head));

  if (inputs.length === 0) {
    await writeSummary(
      '## Dependency vulnerability comparison\n\n' +
        'Skipped: semantic base/head classification found no dependency input changes. Repository-wide advisory drift is reported by the independent Dependency Security workflow.\n',
    );
    return;
  }

  const npmVersion = run(npmCommand, ['--version'], {
    cwd: workspaceDirectory,
  }).stdout.trim();
  const baseState = await auditRevision(mergeBase);
  const headState = await auditRevision(auditHead);
  const result = assessPullRequest({
    dependencyInputFiles: inputs,
    baseAudit: baseState.audit,
    baseLockfile: baseState.lockfile,
    headAudit: headState.audit,
    headLockfile: headState.lockfile,
    exceptions: exceptions.exceptions,
  });
  const markdown = comparisonMarkdown(result, { mergeBase, npmVersion });
  await writeSummary(markdown);
  if (result.regressions.length > 0) {
    process.exitCode = 1;
  }
}

async function runHealth() {
  await validateRegistries();
  const audit = parseAudit(
    run(npmCommand, ['audit', '--json'], {
      cwd: workspaceDirectory,
      accept: [0, 1],
    }),
  );
  const vulnerabilities = audit.metadata?.vulnerabilities ?? {};
  const total = Object.values(vulnerabilities).reduce(
    (sum, value) => sum + (typeof value === 'number' ? value : 0),
    0,
  );
  const markdown = [
    '## Current dependency health',
    '',
    `- Node: ${process.version}`,
    `- npm: ${run(npmCommand, ['--version'], { cwd: workspaceDirectory }).stdout.trim()}`,
    ...Object.entries(vulnerabilities).map(([severity, count]) => `- ${severity}: ${count}`),
    '',
    total === 0
      ? 'No current npm vulnerabilities were reported.'
      : 'Current npm vulnerabilities require the dedicated remediation issue; ordinary feature PRs must not absorb this work.',
    '',
  ].join('\n');
  await writeSummary(markdown);
  if (total > 0) {
    process.exitCode = 1;
  }
}

async function main() {
  const [command, ...arguments_] = process.argv.slice(2);
  if (command === 'validate') {
    await validateRegistries();
    process.stdout.write('Dependency security policy registries are valid.\n');
  } else if (command === 'pr') {
    await runPullRequest(arguments_);
  } else if (command === 'health') {
    await runHealth();
  } else {
    throw new Error('Usage: dependency-security.mjs <validate|pr|health>');
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    process.stderr.write(`${error.stack ?? error}\n`);
    process.exitCode = 1;
  });
}

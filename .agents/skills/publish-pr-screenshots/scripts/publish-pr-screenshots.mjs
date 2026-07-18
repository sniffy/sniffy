#!/usr/bin/env node

import { spawn } from 'node:child_process';
import { mkdtemp, readFile, realpath, rm, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { basename, extname, isAbsolute, relative, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

export const SCREENSHOT_SECTION_START = '<!-- sniffy-ui-screenshots:start -->';
export const SCREENSHOT_SECTION_END = '<!-- sniffy-ui-screenshots:end -->';
export const BASELINE_DIRECTORY = 'sniffy-ui/tests/e2e/visual-baselines';
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

export function parseArgs(argv) {
  const options = {
    mode: 'baselines',
    files: [],
    pr: undefined,
    repo: undefined,
    heading: undefined,
    dryRun: false,
    help: false,
  };
  const args = [...argv];
  if (args[0] && !args[0].startsWith('-')) options.mode = args.shift();
  if (!['baselines', 'diagnostics'].includes(options.mode)) {
    throw new Error(`Unknown mode: ${options.mode}`);
  }
  while (args.length > 0) {
    const argument = args.shift();
    switch (argument) {
      case '--file': {
        const value = args.shift();
        if (!value) throw new Error('--file requires a path');
        options.files.push(value);
        break;
      }
      case '--pr': {
        const value = args.shift();
        if (!value || !/^\d+$/.test(value)) throw new Error('--pr requires a numeric pull request number');
        options.pr = Number(value);
        break;
      }
      case '--repo': {
        const value = args.shift();
        if (!value || !/^[^/\s]+\/[^/\s]+$/.test(value)) {
          throw new Error('--repo requires owner/name');
        }
        options.repo = value;
        break;
      }
      case '--heading': {
        const value = args.shift();
        if (!value) throw new Error('--heading requires text');
        options.heading = value;
        break;
      }
      case '--dry-run':
        options.dryRun = true;
        break;
      case '--help':
      case '-h':
        options.help = true;
        break;
      default:
        throw new Error(`Unknown argument: ${argument}`);
    }
  }
  if (options.mode === 'diagnostics' && options.files.length === 0 && !options.help) {
    throw new Error('diagnostics mode requires at least one explicit --file');
  }
  return options;
}

function usage() {
  return `Usage:
  publish-pr-screenshots.mjs baselines [--pr NUMBER] [--repo OWNER/NAME] [--file PNG]... [--dry-run]
  publish-pr-screenshots.mjs diagnostics --file PNG [--file PNG]... [--pr NUMBER] [--repo OWNER/NAME] [--heading TEXT] [--dry-run]

Modes:
  baselines    Upload reviewed changed PNG baselines and replace the marked PR-description section.
  diagnostics  Upload explicitly selected failure PNGs and create a PR comment.
`;
}

function run(command, args, { cwd, input } = {}) {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(command, args, {
      cwd,
      env: process.env,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    let stdout = '';
    let stderr = '';
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    child.stdout.on('data', (chunk) => (stdout += chunk));
    child.stderr.on('data', (chunk) => (stderr += chunk));
    child.on('error', rejectPromise);
    child.on('close', (code) => {
      if (code === 0) resolvePromise(stdout.trim());
      else {
        rejectPromise(
          new Error(`${command} ${args.join(' ')} failed with exit code ${code}: ${stderr.trim()}`),
        );
      }
    });
    if (input !== undefined) child.stdin.end(input);
    else child.stdin.end();
  });
}

export function normalizeRepositoryPath(path) {
  return path.replaceAll('\\', '/').replace(/^\.\//, '');
}

export function assertAllowedRepositoryPath(repositoryPath, mode) {
  const normalized = normalizeRepositoryPath(repositoryPath);
  if (!normalized || isAbsolute(normalized) || normalized === '..' || normalized.startsWith('../')) {
    throw new Error(`Screenshot must be inside the repository: ${repositoryPath}`);
  }
  if (extname(normalized).toLowerCase() !== '.png') {
    throw new Error(`Only PNG screenshots are supported: ${repositoryPath}`);
  }
  if (mode === 'baselines' && !normalized.startsWith(`${BASELINE_DIRECTORY}/`)) {
    throw new Error(`Baseline screenshots must be under ${BASELINE_DIRECTORY}: ${repositoryPath}`);
  }
  return normalized;
}

async function inspectScreenshot(repositoryRoot, repositoryPath, mode) {
  const normalized = assertAllowedRepositoryPath(repositoryPath, mode);
  const absolute = await realpath(resolve(repositoryRoot, normalized));
  const inside = relative(repositoryRoot, absolute);
  if (!inside || inside === '..' || inside.startsWith(`..${process.platform === 'win32' ? '\\' : '/'}`)) {
    throw new Error(`Screenshot resolves outside the repository: ${repositoryPath}`);
  }
  const metadata = await stat(absolute);
  if (!metadata.isFile()) throw new Error(`Screenshot is not a file: ${repositoryPath}`);
  if (metadata.size === 0) throw new Error(`Screenshot is empty: ${repositoryPath}`);
  if (metadata.size > MAX_IMAGE_BYTES) {
    throw new Error(`Screenshot exceeds the 10 MiB publishing limit: ${repositoryPath}`);
  }
  return { repositoryPath: normalizeRepositoryPath(inside), absolute, size: metadata.size };
}

function humanizeFilename(path) {
  return basename(path, extname(path))
    .split(/[-_]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function buildScreenshotMarkdown({ mode, uploads, repo, sha, heading }) {
  const shortSha = sha.slice(0, 12);
  const title = heading ?? (mode === 'baselines' ? 'Reviewed UI screenshots' : 'Browser-test diagnostics');
  const lines = [
    mode === 'baselines' ? `## ${title}` : `### ${title}`,
    '',
    `Head: [\`${shortSha}\`](https://github.com/${repo}/commit/${sha})`,
    '',
  ];
  for (const upload of uploads) {
    lines.push(
      `**${humanizeFilename(upload.repositoryPath)}**`,
      '',
      `![${humanizeFilename(upload.repositoryPath)}](${upload.secureUrl})`,
      '',
    );
  }
  return lines.join('\n').trimEnd();
}

export function replaceMarkedSection(body, section) {
  const startCount = body.split(SCREENSHOT_SECTION_START).length - 1;
  const endCount = body.split(SCREENSHOT_SECTION_END).length - 1;
  if (startCount !== endCount || startCount > 1) {
    throw new Error('PR description contains malformed or duplicate screenshot markers');
  }
  const marked = `${SCREENSHOT_SECTION_START}\n${section.trim()}\n${SCREENSHOT_SECTION_END}`;
  if (startCount === 0) return `${body.trimEnd()}\n\n${marked}\n`;
  const start = body.indexOf(SCREENSHOT_SECTION_START);
  const end = body.indexOf(SCREENSHOT_SECTION_END, start);
  if (end < start) throw new Error('PR screenshot end marker precedes the start marker');
  return `${body.slice(0, start)}${marked}${body.slice(end + SCREENSHOT_SECTION_END.length)}`;
}

async function resolveRepository(options, repositoryRoot) {
  if (options.repo) return options.repo;
  if (process.env.GITHUB_REPOSITORY) return process.env.GITHUB_REPOSITORY;
  return run('gh', ['repo', 'view', '--json', 'nameWithOwner', '--jq', '.nameWithOwner'], {
    cwd: repositoryRoot,
  });
}

async function resolvePullRequest(options, repositoryRoot, repo) {
  const target = options.pr ? String(options.pr) : '';
  const json = await run(
    'gh',
    [
      'pr',
      'view',
      ...(target ? [target] : []),
      '--repo',
      repo,
      '--json',
      'number,url,state,isDraft,body,headRefName,headRefOid,baseRefName,baseRefOid',
    ],
    { cwd: repositoryRoot },
  );
  const pullRequest = JSON.parse(json);
  if (pullRequest.state !== 'OPEN') throw new Error(`Pull request #${pullRequest.number} is not open`);
  return pullRequest;
}

async function changedBaselineFiles(repositoryRoot, baseSha, headSha) {
  const output = await run(
    'git',
    ['diff', '--name-only', `${baseSha}...${headSha}`, '--', `${BASELINE_DIRECTORY}/*.png`],
    { cwd: repositoryRoot },
  );
  return output
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

async function uploadScreenshot(screenshot, cloudName, uploadPreset) {
  const bytes = await readFile(screenshot.absolute);
  const form = new FormData();
  form.set('upload_preset', uploadPreset);
  form.set('file', new Blob([bytes], { type: 'image/png' }), basename(screenshot.repositoryPath));
  const endpoint = `https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}/image/upload`;
  const response = await fetch(endpoint, { method: 'POST', body: form });
  const payload = await response.json().catch(() => undefined);
  if (!response.ok) {
    const detail = payload?.error?.message ?? `HTTP ${response.status}`;
    throw new Error(`Cloudinary upload failed for ${screenshot.repositoryPath}: ${detail}`);
  }
  const secureUrl = payload?.secure_url;
  if (
    typeof secureUrl !== 'string' ||
    !secureUrl.startsWith(`https://res.cloudinary.com/${cloudName}/image/upload/`)
  ) {
    throw new Error(`Cloudinary returned an unexpected secure_url for ${screenshot.repositoryPath}`);
  }
  return {
    repositoryPath: screenshot.repositoryPath,
    secureUrl,
    assetId: payload.asset_id,
    publicId: payload.public_id,
  };
}

async function verifyPublishedUrl(url) {
  let response = await fetch(url, { method: 'HEAD', redirect: 'follow' });
  if (!response.ok) {
    response = await fetch(url, {
      method: 'GET',
      redirect: 'follow',
      headers: { Range: 'bytes=0-0' },
    });
  }
  if (!response.ok) throw new Error(`Published screenshot is not reachable: ${url}`);
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.startsWith('image/')) {
    throw new Error(`Published screenshot returned unexpected content type ${contentType}: ${url}`);
  }
}

async function writeTemporaryMarkdown(markdown, callback) {
  const directory = await mkdtemp(resolve(tmpdir(), 'sniffy-pr-screenshots-'));
  const file = resolve(directory, 'body.md');
  try {
    await writeFile(file, markdown, 'utf8');
    return await callback(file);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

export async function main(argv = process.argv.slice(2)) {
  const options = parseArgs(argv);
  if (options.help) {
    console.log(usage());
    return;
  }
  const repositoryRoot = await realpath(await run('git', ['rev-parse', '--show-toplevel']));
  const repo = await resolveRepository(options, repositoryRoot);
  const pullRequest = await resolvePullRequest(options, repositoryRoot, repo);
  const localHead = await run('git', ['rev-parse', 'HEAD'], { cwd: repositoryRoot });
  if (localHead !== pullRequest.headRefOid) {
    throw new Error(
      `Local HEAD ${localHead} does not match PR #${pullRequest.number} head ${pullRequest.headRefOid}`,
    );
  }
  if (options.mode === 'baselines') {
    const status = await run('git', ['status', '--porcelain'], { cwd: repositoryRoot });
    if (status) throw new Error('baselines mode requires a clean working tree');
  }

  let paths = options.files;
  if (options.mode === 'baselines' && paths.length === 0) {
    paths = await changedBaselineFiles(repositoryRoot, pullRequest.baseRefOid, localHead);
  }
  if (paths.length === 0) {
    throw new Error('No screenshots selected; pass --file explicitly or change a reviewed visual baseline');
  }
  paths = [...new Set(paths.map(normalizeRepositoryPath))];
  const screenshots = [];
  for (const path of paths) screenshots.push(await inspectScreenshot(repositoryRoot, path, options.mode));

  console.log(`PR: ${pullRequest.url}`);
  console.log(`Head: ${localHead}`);
  for (const screenshot of screenshots) {
    console.log(`Selected: ${screenshot.repositoryPath} (${screenshot.size} bytes)`);
  }
  if (options.dryRun) {
    console.log('Dry run complete; no screenshots uploaded and no pull request content changed.');
    return;
  }

  const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
  const uploadPreset = process.env.CLOUDINARY_UPLOAD_PRESET;
  if (!cloudName || !uploadPreset) {
    throw new Error('CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET are required');
  }

  const uploads = [];
  for (const screenshot of screenshots) {
    const upload = await uploadScreenshot(screenshot, cloudName, uploadPreset);
    await verifyPublishedUrl(upload.secureUrl);
    uploads.push(upload);
    console.log(`Uploaded: ${upload.repositoryPath} -> ${upload.secureUrl}`);
  }
  const markdown = buildScreenshotMarkdown({
    mode: options.mode,
    uploads,
    repo,
    sha: localHead,
    heading: options.heading,
  });

  if (options.mode === 'baselines') {
    const updatedBody = replaceMarkedSection(pullRequest.body ?? '', markdown);
    await writeTemporaryMarkdown(updatedBody, (file) =>
      run('gh', ['pr', 'edit', String(pullRequest.number), '--repo', repo, '--body-file', file], {
        cwd: repositoryRoot,
      }),
    );
    console.log(`Updated screenshot section in ${pullRequest.url}`);
  } else {
    await writeTemporaryMarkdown(markdown, (file) =>
      run('gh', ['pr', 'comment', String(pullRequest.number), '--repo', repo, '--body-file', file], {
        cwd: repositoryRoot,
      }),
    );
    console.log(`Added diagnostic screenshot comment to ${pullRequest.url}`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exitCode = 1;
  });
}

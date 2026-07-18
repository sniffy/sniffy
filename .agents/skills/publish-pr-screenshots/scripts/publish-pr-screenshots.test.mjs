import assert from 'node:assert/strict';
import test from 'node:test';
import {
  BASELINE_DIRECTORY,
  SCREENSHOT_SECTION_END,
  SCREENSHOT_SECTION_START,
  assertAllowedRepositoryPath,
  buildScreenshotMarkdown,
  normalizeRepositoryPath,
  parseArgs,
  replaceMarkedSection,
} from './publish-pr-screenshots.mjs';

test('parses baseline publishing arguments', () => {
  assert.deepEqual(
    parseArgs([
      'baselines',
      '--pr',
      '634',
      '--repo',
      'sniffy/sniffy',
      '--file',
      'one.png',
      '--dry-run',
    ]),
    {
      mode: 'baselines',
      files: ['one.png'],
      pr: 634,
      repo: 'sniffy/sniffy',
      heading: undefined,
      dryRun: true,
      help: false,
    },
  );
});

test('requires explicit diagnostic screenshots', () => {
  assert.throws(() => parseArgs(['diagnostics']), /requires at least one explicit --file/);
});

test('normalizes Windows repository paths', () => {
  assert.equal(normalizeRepositoryPath('.\\sniffy-ui\\image.png'), 'sniffy-ui/image.png');
});

test('restricts baselines to the reviewed baseline directory', () => {
  assert.equal(
    assertAllowedRepositoryPath(`${BASELINE_DIRECTORY}/profiler.png`, 'baselines'),
    `${BASELINE_DIRECTORY}/profiler.png`,
  );
  assert.throws(
    () => assertAllowedRepositoryPath('test-results/failure.png', 'baselines'),
    /must be under/,
  );
  assert.equal(
    assertAllowedRepositoryPath('test-results/failure.png', 'diagnostics'),
    'test-results/failure.png',
  );
  assert.throws(
    () => assertAllowedRepositoryPath('../secret.png', 'diagnostics'),
    /inside the repository/,
  );
  assert.throws(() => assertAllowedRepositoryPath('trace.zip', 'diagnostics'), /Only PNG/);
});

test('appends a screenshot section when markers are absent', () => {
  const result = replaceMarkedSection('Existing body', '## Screenshots\n\ncontent');
  assert.match(result, /Existing body/);
  assert.match(result, new RegExp(SCREENSHOT_SECTION_START));
  assert.match(result, /## Screenshots/);
  assert.match(result, new RegExp(SCREENSHOT_SECTION_END));
});

test('replaces only the marked screenshot section', () => {
  const body = `Before\n\n${SCREENSHOT_SECTION_START}\nold\n${SCREENSHOT_SECTION_END}\n\nAfter`;
  const result = replaceMarkedSection(body, 'new');
  assert.equal(
    result,
    `Before\n\n${SCREENSHOT_SECTION_START}\nnew\n${SCREENSHOT_SECTION_END}\n\nAfter`,
  );
});

test('rejects malformed marker pairs', () => {
  assert.throws(
    () => replaceMarkedSection(`Body\n${SCREENSHOT_SECTION_START}`, 'new'),
    /malformed/,
  );
});

test('builds baseline and diagnostic markdown with immutable head references', () => {
  const input = {
    uploads: [
      {
        repositoryPath: `${BASELINE_DIRECTORY}/pinned-widget.png`,
        secureUrl: 'https://res.cloudinary.com/demo/image/upload/example.png',
      },
    ],
    repo: 'sniffy/sniffy',
    sha: '0123456789abcdef0123456789abcdef01234567',
  };
  const baseline = buildScreenshotMarkdown({ mode: 'baselines', ...input });
  assert.match(baseline, /^## Reviewed UI screenshots/);
  assert.match(baseline, /Pinned Widget/);
  assert.match(baseline, /0123456789ab/);
  const diagnostic = buildScreenshotMarkdown({
    mode: 'diagnostics',
    heading: 'WebKit failure',
    ...input,
  });
  assert.match(diagnostic, /^### WebKit failure/);
});

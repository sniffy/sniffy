import { mkdirSync, mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';

import sourceSnippetRemarkPlugin, { dedentSnippet, extractSourceSnippet } from './source-snippets';

const temporaryDirectories: string[] = [];

function temporaryRepository(): string {
  const repository = mkdtempSync(join(tmpdir(), 'sniffy-source-snippets-'));
  temporaryDirectories.push(repository);
  return repository;
}

function writeSource(repository: string, file: string, source: string): void {
  const target = join(repository, file);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, source);
}

function errorPrefix(file: string, region: string): string {
  return `Source snippet "${file}" region "${region}":`;
}

afterEach(() => {
  temporaryDirectories.splice(0).forEach((directory) => {
    rmSync(directory, { recursive: true, force: true });
  });
});

describe('extractSourceSnippet', () => {
  it('extracts an AsciiDoc-tagged region and dedents it predictably', () => {
    const repositoryRoot = temporaryRepository();
    writeSource(
      repositoryRoot,
      'Example.java',
      [
        'class Example {',
        '    // tag::example[]',
        '        first();',
        '',
        '          second();',
        '    // end::example[]',
        '}',
      ].join('\r\n'),
    );

    expect(
      extractSourceSnippet({
        repositoryRoot,
        file: 'Example.java',
        region: 'example',
      }),
    ).toBe(['first();', '', '  second();'].join('\n'));
  });

  it('supports properly nested regions while omitting their marker lines', () => {
    const repositoryRoot = temporaryRepository();
    writeSource(
      repositoryRoot,
      'Nested.java',
      [
        '// tag::outer[]',
        '  before();',
        '  // tag::inner[]',
        '    inside();',
        '  // end::inner[]',
        '  after();',
        '// end::outer[]',
      ].join('\n'),
    );

    expect(
      extractSourceSnippet({
        repositoryRoot,
        file: 'Nested.java',
        region: 'outer',
      }),
    ).toBe(['before();', '  inside();', 'after();'].join('\n'));
  });

  it.each([
    {
      name: 'duplicate tags',
      source: ['// tag::sample[]', '// end::sample[]', '// tag::sample[]'].join('\n'),
      message: 'duplicate tag "sample"',
    },
    {
      name: 'mismatched nesting',
      source: ['// tag::outer[]', '// tag::inner[]', '// end::outer[]'].join('\n'),
      message: 'malformed nesting',
    },
    {
      name: 'unclosed tags',
      source: '// tag::sample[]\nvalue();',
      message: 'tag "sample" opened at line 1 is not closed',
    },
    {
      name: 'unexpected end tags',
      source: '// end::sample[]',
      message: 'unexpected end tag "sample"',
    },
    {
      name: 'malformed markers',
      source: '// tag::sample[\nvalue();',
      message: 'malformed tag marker',
    },
  ])('rejects $name with source, region, and line context', ({ source, message }) => {
    const repositoryRoot = temporaryRepository();
    writeSource(repositoryRoot, 'Broken.java', source);

    expect(() =>
      extractSourceSnippet({
        repositoryRoot,
        file: 'Broken.java',
        region: 'sample',
      }),
    ).toThrow(expect.objectContaining({ message: expect.stringContaining(message) }));
    expect(() =>
      extractSourceSnippet({
        repositoryRoot,
        file: 'Broken.java',
        region: 'sample',
      }),
    ).toThrow(errorPrefix('Broken.java', 'sample'));
  });

  it('reports missing files and missing regions precisely', () => {
    const repositoryRoot = temporaryRepository();
    writeSource(repositoryRoot, 'Example.java', '// tag::present[]\nvalue();\n// end::present[]');

    expect(() =>
      extractSourceSnippet({ repositoryRoot, file: 'Missing.java', region: 'sample' }),
    ).toThrow(`${errorPrefix('Missing.java', 'sample')} source file does not exist`);
    expect(() =>
      extractSourceSnippet({ repositoryRoot, file: 'Example.java', region: 'missing' }),
    ).toThrow(`${errorPrefix('Example.java', 'missing')} tag "missing" was not found`);
  });

  it('rejects lexical and symlink traversal outside the repository', () => {
    const container = temporaryRepository();
    const repositoryRoot = join(container, 'repository');
    const outside = join(container, 'outside');
    mkdirSync(repositoryRoot);
    mkdirSync(outside);
    writeSource(outside, 'Secret.java', '// tag::secret[]\nsecret();\n// end::secret[]');
    symlinkSync(
      outside,
      join(repositoryRoot, 'linked'),
      process.platform === 'win32' ? 'junction' : 'dir',
    );

    expect(() =>
      extractSourceSnippet({
        repositoryRoot,
        file: '../outside/Secret.java',
        region: 'secret',
      }),
    ).toThrow(`${errorPrefix('../outside/Secret.java', 'secret')} path escapes`);
    expect(() =>
      extractSourceSnippet({
        repositoryRoot,
        file: 'linked/Secret.java',
        region: 'secret',
      }),
    ).toThrow(`${errorPrefix('linked/Secret.java', 'secret')} resolved path escapes`);
  });
});

describe('sourceSnippetRemarkPlugin', () => {
  it('turns the MDX component into a syntax-highlighted code node', () => {
    const repositoryRoot = temporaryRepository();
    writeSource(repositoryRoot, 'Example.java', '// tag::sample[]\n  value();\n// end::sample[]');
    const tree = {
      type: 'root',
      children: [
        {
          type: 'mdxJsxFlowElement',
          name: 'SourceSnippet',
          attributes: [
            { type: 'mdxJsxAttribute', name: 'file', value: 'Example.java' },
            { type: 'mdxJsxAttribute', name: 'region', value: 'sample' },
            { type: 'mdxJsxAttribute', name: 'language', value: 'java' },
          ],
          children: [],
        },
      ],
    };

    sourceSnippetRemarkPlugin({ repositoryRoot })(tree);

    expect(tree.children[0]).toEqual({
      type: 'code',
      lang: 'java',
      value: 'value();',
    });
  });

  it('requires static file, region, and language attributes', () => {
    const repositoryRoot = temporaryRepository();
    const tree = {
      type: 'root',
      children: [
        {
          type: 'mdxJsxFlowElement',
          name: 'SourceSnippet',
          attributes: [
            { type: 'mdxJsxAttribute', name: 'file', value: 'Example.java' },
            { type: 'mdxJsxAttribute', name: 'region', value: 'sample' },
          ],
          children: [],
        },
      ],
    };

    expect(() => sourceSnippetRemarkPlugin({ repositoryRoot })(tree)).toThrow(
      `${errorPrefix('Example.java', 'sample')} SourceSnippet requires one static non-empty "language" attribute`,
    );
  });
});

describe('dedentSnippet', () => {
  it('trims blank boundaries while preserving internal blank lines', () => {
    expect(dedentSnippet('\n\talpha();\n\n\tbeta();\n')).toBe('alpha();\n\nbeta();');
  });
});

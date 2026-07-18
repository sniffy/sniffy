import { readFileSync, realpathSync } from 'node:fs';
import { isAbsolute, relative, resolve, sep } from 'node:path';

const componentName = 'SourceSnippet';
const validMarker = /\b(tag|end)::([A-Za-z0-9][A-Za-z0-9_.-]*)\[\]/g;
const markerPrefix = /\b(?:tag|end)::/g;
const hasMarkerPrefix = /\b(?:tag|end)::/;

export interface SourceSnippetOptions {
  repositoryRoot: string;
}

export interface SourceSnippetRequest extends SourceSnippetOptions {
  file: string;
  region: string;
}

interface Region {
  end: number;
  start: number;
}

interface OpenRegion {
  line: number;
  name: string;
}

interface MdxAttribute {
  name?: string;
  type: string;
  value?: unknown;
}

interface SyntaxNode {
  attributes?: MdxAttribute[];
  children?: SyntaxNode[];
  lang?: string;
  name?: string;
  type: string;
  value?: string;
}

function snippetError(file: string, region: string, detail: string): Error {
  return new Error(`Source snippet "${file}" region "${region}": ${detail}`);
}

function isInside(root: string, candidate: string): boolean {
  const pathFromRoot = relative(root, candidate);
  return (
    pathFromRoot === '' ||
    (!isAbsolute(pathFromRoot) && pathFromRoot !== '..' && !pathFromRoot.startsWith(`..${sep}`))
  );
}

export function dedentSnippet(source: string): string {
  const lines = source.replace(/\r\n?/g, '\n').split('\n');

  while (lines.length > 0 && lines[0].trim() === '') {
    lines.shift();
  }
  while (lines.length > 0 && lines[lines.length - 1].trim() === '') {
    lines.pop();
  }

  const nonBlankLines = lines.filter((line) => line.trim() !== '');
  if (nonBlankLines.length === 0) {
    return '';
  }

  const indentation = Math.min(...nonBlankLines.map((line) => line.match(/^[\t ]*/)![0].length));

  return lines.map((line) => (line.trim() === '' ? '' : line.slice(indentation))).join('\n');
}

function parseRegions(
  source: string,
  file: string,
  requestedRegion: string,
): { lines: string[]; regions: Map<string, Region> } {
  const lines = source.replace(/\r\n?/g, '\n').split('\n');
  const regions = new Map<string, Region>();
  const openedNames = new Map<string, number>();
  const stack: OpenRegion[] = [];

  lines.forEach((line, index) => {
    const lineNumber = index + 1;
    const prefixes = [...line.matchAll(markerPrefix)];
    const markers = [...line.matchAll(validMarker)];

    if (prefixes.length === 0) {
      return;
    }
    if (prefixes.length !== 1 || markers.length !== 1) {
      throw snippetError(
        file,
        requestedRegion,
        `malformed tag marker at line ${lineNumber}; expected exactly one tag::name[] or end::name[] marker`,
      );
    }

    const [, kind, name] = markers[0];
    if (kind === 'tag') {
      const previousLine = openedNames.get(name);
      if (previousLine !== undefined) {
        throw snippetError(
          file,
          requestedRegion,
          `duplicate tag "${name}" at line ${lineNumber}; first defined at line ${previousLine}`,
        );
      }
      openedNames.set(name, lineNumber);
      stack.push({ line: index, name });
      return;
    }

    const current = stack.at(-1);
    if (current === undefined) {
      throw snippetError(
        file,
        requestedRegion,
        `unexpected end tag "${name}" at line ${lineNumber}`,
      );
    }
    if (current.name !== name) {
      throw snippetError(
        file,
        requestedRegion,
        `malformed nesting at line ${lineNumber}; expected end::${current.name}[] before end::${name}[]`,
      );
    }

    stack.pop();
    regions.set(name, { start: current.line + 1, end: index });
  });

  const unclosed = stack.at(-1);
  if (unclosed !== undefined) {
    throw snippetError(
      file,
      requestedRegion,
      `tag "${unclosed.name}" opened at line ${unclosed.line + 1} is not closed`,
    );
  }

  return { lines, regions };
}

export function extractSourceSnippet(request: SourceSnippetRequest): string {
  const { file, region } = request;
  let repositoryRoot: string;

  try {
    repositoryRoot = realpathSync(request.repositoryRoot);
  } catch {
    throw snippetError(file, region, `repository root "${request.repositoryRoot}" is unavailable`);
  }

  if (file.trim() === '' || isAbsolute(file)) {
    throw snippetError(file, region, 'source must be a non-empty repository-relative path');
  }

  const candidate = resolve(repositoryRoot, file);
  if (!isInside(repositoryRoot, candidate)) {
    throw snippetError(file, region, 'path escapes the repository root');
  }

  let sourcePath: string;
  try {
    sourcePath = realpathSync(candidate);
  } catch {
    throw snippetError(file, region, 'source file does not exist or cannot be resolved');
  }
  if (!isInside(repositoryRoot, sourcePath)) {
    throw snippetError(file, region, 'resolved path escapes the repository root');
  }

  let source: string;
  try {
    source = readFileSync(sourcePath, 'utf8');
  } catch {
    throw snippetError(file, region, 'source file cannot be read');
  }

  const { lines, regions } = parseRegions(source, file, region);
  const selected = regions.get(region);
  if (selected === undefined) {
    throw snippetError(file, region, `tag "${region}" was not found`);
  }

  const content = lines
    .slice(selected.start, selected.end)
    .filter((line) => !hasMarkerPrefix.test(line))
    .join('\n');
  return dedentSnippet(content);
}

function staticAttribute(node: SyntaxNode, name: string, file: string, region: string): string {
  const matching = (node.attributes ?? []).filter(
    (attribute) => attribute.type === 'mdxJsxAttribute' && attribute.name === name,
  );
  if (matching.length !== 1 || typeof matching[0].value !== 'string' || matching[0].value === '') {
    throw snippetError(
      file,
      region,
      `${componentName} requires one static non-empty "${name}" attribute`,
    );
  }
  return matching[0].value;
}

function transformNode(node: SyntaxNode, options: SourceSnippetOptions): void {
  if (node.type === 'mdxJsxFlowElement' && node.name === componentName) {
    const provisionalFile =
      typeof node.attributes?.find((attribute) => attribute.name === 'file')?.value === 'string'
        ? String(node.attributes?.find((attribute) => attribute.name === 'file')?.value)
        : '<missing>';
    const provisionalRegion =
      typeof node.attributes?.find((attribute) => attribute.name === 'region')?.value === 'string'
        ? String(node.attributes?.find((attribute) => attribute.name === 'region')?.value)
        : '<missing>';
    const file = staticAttribute(node, 'file', provisionalFile, provisionalRegion);
    const region = staticAttribute(node, 'region', file, provisionalRegion);
    const language = staticAttribute(node, 'language', file, region);

    if ((node.children ?? []).length > 0) {
      throw snippetError(file, region, `${componentName} must be self-closing`);
    }

    node.type = 'code';
    node.lang = language;
    node.value = extractSourceSnippet({ ...options, file, region });
    delete node.attributes;
    delete node.children;
    delete node.name;
    return;
  }

  node.children?.forEach((child) => transformNode(child, options));
}

export default function sourceSnippetRemarkPlugin(options: SourceSnippetOptions) {
  if (typeof options?.repositoryRoot !== 'string' || options.repositoryRoot === '') {
    throw new Error('Source snippet plugin requires a repositoryRoot option');
  }

  return (tree: SyntaxNode): void => {
    transformNode(tree, options);
  };
}

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const versionToken = '@sniffy.version@';

interface SyntaxNode {
  children?: SyntaxNode[];
  type: string;
  value?: string;
}

export interface ProductVersionOptions {
  repositoryRoot: string;
}

export function readProductVersion(repositoryRoot: string): string {
  const pom = readFileSync(resolve(repositoryRoot, 'pom.xml'), 'utf8');
  const projectVersion = pom.match(/<project[\s\S]*?<version>([^<]+)<\/version>/)?.[1]?.trim();

  if (projectVersion === undefined || projectVersion === '') {
    throw new Error('Unable to read the Sniffy version from the root pom.xml');
  }

  return projectVersion;
}

function replaceVersion(node: SyntaxNode, version: string): void {
  if (typeof node.value === 'string' && node.value.includes(versionToken)) {
    node.value = node.value.replaceAll(versionToken, version);
  }
  node.children?.forEach((child) => replaceVersion(child, version));
}

export default function productVersionRemarkPlugin(options: ProductVersionOptions) {
  const version = readProductVersion(options.repositoryRoot);

  return (tree: SyntaxNode): void => replaceVersion(tree, version);
}

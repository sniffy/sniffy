import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const requiredSemanticTokens = [
  '--sniffy-canvas',
  '--sniffy-surface',
  '--sniffy-surface-raised',
  '--sniffy-surface-hover',
  '--sniffy-foreground',
  '--sniffy-foreground-muted',
  '--sniffy-border',
  '--sniffy-focus',
  '--sniffy-accent',
  '--sniffy-accent-foreground',
  '--sniffy-success',
  '--sniffy-warning',
  '--sniffy-danger',
  '--sniffy-info',
  '--sniffy-sql',
  '--sniffy-network',
  '--sniffy-exception',
] as const;

const declarations = (source: string) =>
  new Set(Array.from(source.matchAll(/(^|\s)(--sniffy-[\w-]+)\s*:/gm), (match) => match[2]));

describe('@sniffy/theme semantic contract', () => {
  it('defines every required semantic token in the dark palette', () => {
    const darkTokens = declarations(
      readFileSync(resolve(process.cwd(), 'packages/theme/src/dark.css'), 'utf8'),
    );

    expect([...darkTokens].sort()).toEqual([...requiredSemanticTokens].sort());
  });

  it('keeps semantic palette values out of the theme-independent base', () => {
    const baseTokens = declarations(
      readFileSync(resolve(process.cwd(), 'packages/theme/src/base.css'), 'utf8'),
    );

    expect(requiredSemanticTokens.filter((token) => baseTokens.has(token))).toEqual([]);
  });

  it('exports explicit theme layers and keeps the aggregate compatibility entry point', () => {
    const themeDirectory = resolve(process.cwd(), 'packages/theme');
    const packageJson = JSON.parse(
      readFileSync(resolve(themeDirectory, 'package.json'), 'utf8'),
    ) as { exports: Record<string, string> };

    expect(packageJson.exports).toMatchObject({
      './base.css': './src/base.css',
      './dark.css': './src/dark.css',
      './tailwind.css': './src/tailwind.css',
      './tokens.css': './src/tokens.css',
    });
    expect(readFileSync(resolve(themeDirectory, 'src/tokens.css'), 'utf8')).toContain(
      "@import './base.css';\n@import './dark.css';",
    );
  });

  it.each(['agent', 'profiler'])(
    'keeps the %s application on the explicit dark theme',
    (application) => {
      const styles = readFileSync(
        resolve(process.cwd(), `apps/${application}/src/styles.css`),
        'utf8',
      );

      expect(styles).toContain("@import '@sniffy/theme/base.css';");
      expect(styles).toContain("@import '@sniffy/theme/dark.css';");
      expect(styles).toContain("@import '@sniffy/theme/tailwind.css';");
    },
  );
});

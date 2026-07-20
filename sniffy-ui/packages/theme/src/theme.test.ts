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

const palette = (source: string) =>
  Object.fromEntries(
    Array.from(source.matchAll(/(^|\s)(--sniffy-[\w-]+)\s*:\s*(#[\da-f]{6})\s*;/gim), (match) => [
      match[2],
      match[3].toLowerCase(),
    ]),
  );

const relativeLuminance = (color: string) => {
  const channels = color
    .slice(1)
    .match(/.{2}/g)!
    .map((channel) => Number.parseInt(channel, 16) / 255)
    .map((channel) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4));

  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
};

const contrast = (foreground: string, background: string) => {
  const foregroundLuminance = relativeLuminance(foreground);
  const backgroundLuminance = relativeLuminance(background);

  return (
    (Math.max(foregroundLuminance, backgroundLuminance) + 0.05) /
    (Math.min(foregroundLuminance, backgroundLuminance) + 0.05)
  );
};

const readPalette = (name: 'dark' | 'light') =>
  readFileSync(resolve(process.cwd(), `packages/theme/src/${name}.css`), 'utf8');

describe('@sniffy/theme semantic contract', () => {
  it.each(['dark', 'light'] as const)('defines every required semantic token in %s', (name) => {
    const tokens = declarations(readPalette(name));

    expect([...tokens].sort()).toEqual([...requiredSemanticTokens].sort());
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
      './light.css': './src/light.css',
      './tailwind.css': './src/tailwind.css',
      './tokens.css': './src/tokens.css',
    });
    expect(readFileSync(resolve(themeDirectory, 'src/tokens.css'), 'utf8')).toContain(
      "@import './base.css';\n@import './dark.css';",
    );
    expect(readFileSync(resolve(themeDirectory, 'src/tailwind.css'), 'utf8')).toContain(
      '@theme inline',
    );
  });

  it('supports Docusaurus selectors and Sniffy-specific opt-in classes', () => {
    const dark = readPalette('dark');
    const light = readPalette('light');

    expect(dark).toContain("[data-theme='dark']");
    expect(dark).toContain('.sniffy-theme-dark');
    expect(dark).toContain(':host(.sniffy-theme-dark)');
    expect(light).toContain("[data-theme='light']");
    expect(light).toContain('.sniffy-theme-light');
    expect(light).toContain(':host(.sniffy-theme-light)');
  });

  it.each(['dark', 'light'] as const)(
    'meets the %s WCAG contrast contract for text, links, controls, focus, and statuses',
    (name) => {
      const tokens = palette(readPalette(name));
      const backgrounds = [
        '--sniffy-canvas',
        '--sniffy-surface',
        '--sniffy-surface-raised',
      ] as const;

      for (const background of backgrounds) {
        for (const foreground of [
          '--sniffy-foreground',
          '--sniffy-foreground-muted',
          '--sniffy-accent',
          '--sniffy-success',
          '--sniffy-warning',
          '--sniffy-danger',
          '--sniffy-info',
          '--sniffy-sql',
          '--sniffy-network',
          '--sniffy-exception',
        ] as const) {
          expect(
            contrast(tokens[foreground], tokens[background]),
            `${name} ${foreground} on ${background}`,
          ).toBeGreaterThanOrEqual(4.5);
        }
        expect(
          contrast(tokens['--sniffy-focus'], tokens[background]),
          `${name} focus indicator on ${background}`,
        ).toBeGreaterThanOrEqual(3);
      }

      expect(
        contrast(tokens['--sniffy-accent-foreground'], tokens['--sniffy-accent']),
        `${name} selected control foreground`,
      ).toBeGreaterThanOrEqual(4.5);
    },
  );

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
      expect(styles).not.toContain("@import '@sniffy/theme/light.css';");
    },
  );
});

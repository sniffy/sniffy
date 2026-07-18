import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import sourceSnippetRemarkPlugin from './src/source-snippets';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

const config: Config = {
  title: 'Sniffy',
  tagline: 'Sniffy website foundation',
  url: 'https://sniffy.io',
  baseUrl: '/',
  trailingSlash: true,
  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  organizationName: 'sniffy',
  projectName: 'sniffy',
  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: 'docs',
          sidebarPath: './sidebars.ts',
          lastVersion: 'current',
          remarkPlugins: [[sourceSnippetRemarkPlugin, { repositoryRoot }]],
          versions: {
            current: {
              label: 'Current',
              path: '',
            },
          },
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],
  themeConfig: {
    colorMode: {
      defaultMode: 'dark',
      disableSwitch: true,
      respectPrefersColorScheme: false,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;

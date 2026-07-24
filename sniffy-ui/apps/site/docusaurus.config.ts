import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import sourceSnippetRemarkPlugin from './src/source-snippets';
import productVersionRemarkPlugin from './src/product-version';
import legacyDocsPlugin from './src/legacy-docs';
import deterministicSearchPlugin from './src/deterministic-search-index';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

const config: Config = {
  title: 'Sniffy',
  tagline: 'Java application observability and resilience testing',
  url: 'https://sniffy.io',
  baseUrl: '/',
  favicon: 'favicon.ico',
  trailingSlash: true,
  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
      onBrokenMarkdownImages: 'throw',
    },
  },
  organizationName: 'sniffy',
  projectName: 'sniffy',
  plugins: [
    legacyDocsPlugin,
    [
      deterministicSearchPlugin,
      {
        indexDocs: true,
        indexBlog: false,
        indexPages: false,
        language: 'en',
        maxSearchResults: 8,
      },
    ],
  ],
  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: 'docs',
          sidebarPath: './sidebars.ts',
          lastVersion: 'current',
          remarkPlugins: [
            [productVersionRemarkPlugin, { repositoryRoot }],
            [sourceSnippetRemarkPlugin, { repositoryRoot }],
          ],
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
    image: 'img/brand/sniffy-social.svg',
    metadata: [
      {
        name: 'description',
        content: 'Sniffy brings SQL and network observability into Java applications and tests.',
      },
      { property: 'og:type', content: 'website' },
      { name: 'twitter:card', content: 'summary_large_image' },
    ],
    navbar: {
      title: 'Sniffy',
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          type: 'dropdown',
          label: 'Use cases',
          position: 'left',
          items: [
            { label: 'SQL query assertions', to: '/docs/testing/api/' },
            { label: 'Network fault testing', to: '/docs/network/fault-emulation/' },
            { label: 'Traffic and TLS capture', to: '/docs/network/traffic-capture/' },
          ],
        },
        {
          type: 'custom-githubStars',
          href: 'https://github.com/sniffy/sniffy',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'light',
      links: [
        {
          title: 'Product',
          items: [
            { label: 'Documentation', to: '/docs/' },
            { label: 'Installation', to: '/docs/installation/' },
            { label: 'Configuration', to: '/docs/configuration/' },
          ],
        },
        {
          title: 'Use cases',
          items: [
            { label: 'Query assertions', to: '/docs/testing/api/' },
            { label: 'Network fault testing', to: '/docs/network/fault-emulation/' },
            { label: 'Traffic capture', to: '/docs/network/traffic-capture/' },
          ],
        },
        {
          title: 'Project',
          items: [
            { label: 'GitHub repository', href: 'https://github.com/sniffy/sniffy' },
            { label: 'Releases', href: 'https://github.com/sniffy/sniffy/releases' },
            { label: 'Issue tracker', href: 'https://github.com/sniffy/sniffy/issues' },
          ],
        },
      ],
      copyright: 'Sniffy is open source software.',
    },
    colorMode: {
      defaultMode: 'dark',
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;

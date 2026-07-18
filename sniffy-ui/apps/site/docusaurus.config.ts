import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'Sniffy',
  tagline: 'Java observability and testing utilities',
  favicon: 'img/favicon.ico',
  url: 'https://sniffy.io',
  baseUrl: '/',
  organizationName: 'sniffy',
  projectName: 'sniffy',
  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },
  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: 'docs',
          sidebarPath: './sidebars.ts',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],
  themeConfig: {
    navbar: {
      title: 'Sniffy',
      items: [{ to: '/docs/', label: 'Docs', position: 'left' }],
    },
    footer: {
      style: 'dark',
      copyright: `Copyright © ${new Date().getFullYear()} Sniffy`,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;

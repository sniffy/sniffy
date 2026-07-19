import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'index',
    {
      type: 'category',
      label: 'Installation',
      items: ['installation/index'],
    },
    {
      type: 'category',
      label: 'Application setup',
      items: ['setup/spring', 'setup/datasource', 'setup/filter', 'setup/containers'],
    },
    {
      type: 'category',
      label: 'Configuration',
      items: ['configuration/index', 'configuration/nio-monitoring'],
    },
    {
      type: 'category',
      label: 'Testing integrations',
      items: [
        'testing/api',
        'testing/shared-connection',
        'testing/junit',
        'testing/kotest',
        'testing/spring',
        'testing/testng',
        'testing/spock',
      ],
    },
    {
      type: 'category',
      label: 'Network behavior',
      items: ['network/fault-emulation', 'network/traffic-capture'],
    },
    {
      type: 'category',
      label: 'Migration',
      items: ['migration/to-4', 'migration/to-3.1', 'migration/asciidoc-route-map'],
    },
    'source-snippets',
  ],
};

export default sidebars;

import type { Preview } from '@storybook/react-vite';
import { initialize, mswLoader } from 'msw-storybook-addon';
import { mswHandlers } from '@sniffy/fixtures';
import '../apps/agent/src/styles.css';
import '../apps/profiler/src/styles.css';

initialize({ onUnhandledRequest: 'error' });

const preview: Preview = {
  loaders: [mswLoader],
  parameters: {
    a11y: { test: 'error' },
    backgrounds: { default: 'sniffy', values: [{ name: 'sniffy', value: '#090e17' }] },
    layout: 'padded',
    msw: { handlers: mswHandlers },
  },
};
export default preview;

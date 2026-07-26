import type { Preview } from '@storybook/react-vite';
import { setupWorker } from 'msw/browser';
import { mswLoader } from 'msw-storybook-addon/csf3';
import { mswHandlers } from '@sniffy/fixtures';
import '../apps/agent/src/styles.css';
import '../apps/profiler/src/styles.css';
import '../packages/theme/src/light.css';

const preview: Preview = {
  tags: ['autodocs'],
  loaders: [
    mswLoader(async () => {
      const worker = setupWorker();
      await worker.start({ onUnhandledRequest: 'error' });
      return worker;
    }),
  ],
  parameters: {
    a11y: { test: 'error' },
    backgrounds: { default: 'sniffy', values: [{ name: 'sniffy', value: '#090e17' }] },
    layout: 'padded',
    msw: { handlers: mswHandlers },
  },
};
export default preview;

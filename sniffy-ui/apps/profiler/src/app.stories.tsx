import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, userEvent, within } from 'storybook/test';
import { delay, http, HttpResponse } from 'msw';
import { requestFixture } from '@sniffy/fixtures';
import { ProfilerApp } from './app';

const shadowRoot = document.createElement('div').attachShadow({ mode: 'open' });
const intercepted = { subscribe: () => () => undefined };
const metadata = {
  requestId: 'ID',
  requestMethod: 'GET',
  responseCode: '200',
  baseUrl: 'http://localhost:3000/mock/sniffy/4.0.0/',
  sqlQueries: 2,
  serverTime: 84,
};
export default {
  title: 'Products/Profiler',
  component: ProfilerApp,
  decorators: [
    (Story) => (
      <div className="sniffy-theme">
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof ProfilerApp>;
export const Collapsed: StoryObj<typeof ProfilerApp> = {
  args: { metadata: { ...metadata, requestId: '' }, intercepted, shadowRoot },
};

export const Normal: StoryObj<typeof ProfilerApp> = {
  args: { metadata, intercepted, shadowRoot, initialOpen: true },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(await canvas.findByText(/Example failure/)).toBeVisible();
    await userEvent.click(canvas.getByRole('button', { name: 'Stack trace' }));
    await expect(canvas.getByText(/Service\.call/)).toBeVisible();
  },
};

export const Empty: StoryObj<typeof ProfilerApp> = {
  args: { metadata, intercepted, shadowRoot, initialOpen: true },
  parameters: {
    msw: {
      handlers: [
        http.get('*/request/*', () =>
          HttpResponse.json({ ...requestFixture, executedQueries: [], networkConnections: [] }),
        ),
      ],
    },
  },
};

export const Loading: StoryObj<typeof ProfilerApp> = {
  args: { metadata, intercepted, shadowRoot, initialOpen: true },
  parameters: {
    msw: {
      handlers: [
        http.get('*/request/*', async () => {
          await delay('infinite');
          return HttpResponse.json(requestFixture);
        }),
      ],
    },
  },
};

export const Error: StoryObj<typeof ProfilerApp> = {
  args: { metadata, intercepted, shadowRoot, initialOpen: true },
  parameters: {
    msw: {
      handlers: [
        http.get('*/request/*', () => HttpResponse.json({ error: 'fixture' }, { status: 500 })),
      ],
    },
  },
};

export const LongSqlAndStack: StoryObj<typeof ProfilerApp> = {
  ...Normal,
};

export const Registry: StoryObj<typeof ProfilerApp> = {
  args: { metadata: { ...metadata, requestId: '' }, intercepted, shadowRoot, initialOpen: true },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole('tab', { name: 'Network Connections' }));
    await expect(await canvas.findByRole('table', { name: 'Socket connections' })).toBeVisible();
  },
};

export const RegistryEmpty: StoryObj<typeof ProfilerApp> = {
  ...Registry,
  parameters: {
    msw: {
      handlers: [
        http.get('*/connectionregistry/', () =>
          HttpResponse.json({ persistent: false, sockets: [], dataSources: [] }),
        ),
      ],
    },
  },
};

export const TopSql: StoryObj<typeof ProfilerApp> = {
  args: { metadata: { ...metadata, requestId: '' }, intercepted, shadowRoot, initialOpen: true },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole('tab', { name: 'Top SQL' }));
    await expect(await canvas.findByRole('table', { name: 'Top SQL' })).toBeVisible();
  },
};

export const TopSqlEmpty: StoryObj<typeof ProfilerApp> = {
  args: { metadata: { ...metadata, requestId: '' }, intercepted, shadowRoot, initialOpen: true },
  parameters: {
    msw: {
      handlers: [http.get('*/topsql/', () => HttpResponse.json([]))],
    },
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole('tab', { name: 'Top SQL' }));
    await expect(await canvas.findByText('No SQL queries gathered yet.')).toBeVisible();
  },
};

import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, userEvent, waitFor, within } from 'storybook/test';
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
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const pin = canvas.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    await expect(pin).toHaveAttribute('aria-pressed', 'true');
    await expect(canvas.queryByRole('button', { name: 'Open Sniffy profiler' })).toBeNull();
    await userEvent.click(canvas.getByRole('button', { name: 'Toggle Sniffy profiler' }));
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await expect(canvas.queryByRole('button', { name: 'Toggle Sniffy profiler' })).toBeNull();
    await expect(canvas.getByLabelText('Profiler summary')).toBeVisible();
  },
};

export const UnpinnedInteraction: StoryObj<typeof ProfilerApp> = {
  args: { metadata: { ...metadata, requestId: '' }, intercepted, shadowRoot },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const pin = canvas.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const shell = canvasElement.querySelector('.sniffy-shell') as HTMLElement;
    await userEvent.click(pin);
    (document.activeElement as HTMLElement | null)?.blur();
    await userEvent.unhover(shell);
    await waitFor(() => expect(pin.parentElement).toHaveAttribute('data-expanded', 'false'), {
      timeout: 1_000,
    });
    const trigger = canvas.getByRole('button', { name: 'Open Sniffy profiler' });
    await userEvent.hover(trigger);
    await expect(pin.parentElement).toHaveAttribute('data-expanded', 'true');
    await userEvent.unhover(trigger);
    await waitFor(() => expect(pin.parentElement).toHaveAttribute('data-expanded', 'false'), {
      timeout: 1_000,
    });
    await userEvent.tab();
    const focusedTrigger = canvas.getByRole('button', { name: 'Open Sniffy profiler' });
    await expect(focusedTrigger).toHaveFocus();
    await waitFor(() => expect(pin.parentElement).toHaveAttribute('data-expanded', 'true'));
    focusedTrigger.blur();
    await waitFor(() => expect(pin.parentElement).toHaveAttribute('data-expanded', 'false'), {
      timeout: 1_000,
    });
    await userEvent.click(canvas.getByRole('button', { name: 'Open Sniffy profiler' }));
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
  },
};

export const Normal: StoryObj<typeof ProfilerApp> = {
  args: { metadata, intercepted, shadowRoot, initialOpen: true },
  render: (args) => (
    <>
      <button type="button">Outside profiler</button>
      <ProfilerApp {...args} />
    </>
  ),
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await expect(await canvas.findByText(/Example failure/)).toBeVisible();
    await expect(canvas.getByLabelText('Profiler summary')).toBeVisible();
    await expect(
      canvas.getByRole('button', { name: 'Dismiss Sniffy for this page' }),
    ).toBeVisible();
    await userEvent.click(canvas.getByRole('button', { name: 'Stack trace' }));
    await expect(canvas.getByText(/Service\.call/)).toBeVisible();

    const network = canvas.getByRole('tab', { name: 'Network Connections' });
    await userEvent.click(network);
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await expect(network).toHaveAttribute('data-active');

    const topSql = canvas.getByRole('tab', { name: 'Top SQL' });
    await userEvent.click(topSql);
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await expect(topSql).toHaveAttribute('data-active');
    await expect(await canvas.findByRole('table', { name: 'Top SQL' })).toBeVisible();

    await userEvent.click(canvas.getByRole('button', { name: 'Maximize panel' }));
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await expect(canvas.getByRole('button', { name: 'Restore panel' })).toBeVisible();
    await userEvent.click(canvas.getByLabelText('Profiler summary'));
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();

    await userEvent.click(canvas.getByRole('button', { name: 'Outside profiler' }));
    await expect(canvas.queryByText('Sniffy profiler')).toBeNull();

    await userEvent.click(canvas.getByRole('button', { name: 'Toggle Sniffy profiler' }));
    await expect(canvas.getByText('Sniffy profiler')).toBeVisible();
    await userEvent.keyboard('{Escape}');
    await expect(canvas.queryByText('Sniffy profiler')).toBeNull();
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
    const tab = canvas.getByRole('tab', { name: 'Network Connections' });
    await userEvent.click(tab);
    await expect(tab).toHaveAttribute('data-active');
    await expect(await canvas.findByRole('table', { name: 'Socket connections' })).toBeVisible();
    const socketSwitch = canvas.getByRole('switch', {
      name: 'Enable en.wikipedia.org:443 socket',
    });
    await expect(socketSwitch).toHaveClass('sniffy-switch');
    await userEvent.click(socketSwitch);
    await expect(socketSwitch).not.toBeChecked();
    await userEvent.keyboard(' ');
    await expect(socketSwitch).toBeChecked();
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
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const tab = canvas.getByRole('tab', { name: 'Network Connections' });
    await userEvent.click(tab);
    await expect(tab).toHaveAttribute('data-active');
    await expect(await canvas.findByText('No sockets discovered.')).toBeVisible();
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

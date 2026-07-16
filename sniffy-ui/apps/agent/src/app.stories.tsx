import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, userEvent, within } from 'storybook/test';
import { http, HttpResponse } from 'msw';
import { AgentApp } from './app';
export default {
  title: 'Products/Agent',
  component: AgentApp,
  parameters: { viewport: { defaultViewport: 'responsive' } },
} satisfies Meta<typeof AgentApp>;
export const Desktop: StoryObj<typeof AgentApp> = {
  args: { baseUrl: 'http://localhost:3000/' },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(await canvas.findByRole('table', { name: 'Socket connections' })).toBeVisible();
    await expect(canvas.getByRole('switch', { name: 'Keep settings after restart' })).toBeVisible();
    const socketSwitch = canvas.getByRole('switch', {
      name: 'Enable en.wikipedia.org:443 socket',
    });
    await expect(socketSwitch).toHaveClass('sniffy-switch');
    await userEvent.click(socketSwitch);
    await expect(socketSwitch).not.toBeChecked();
    socketSwitch.focus();
    await userEvent.keyboard(' ');
    await expect(socketSwitch).toBeChecked();
  },
};
export const Narrow: StoryObj<typeof AgentApp> = {
  args: { baseUrl: 'http://localhost:3000/' },
  parameters: { viewport: { defaultViewport: 'mobile1' } },
};

export const Empty: StoryObj<typeof AgentApp> = {
  args: { baseUrl: 'http://localhost:3000/' },
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

export const Error: StoryObj<typeof AgentApp> = {
  args: { baseUrl: 'http://localhost:3000/' },
  parameters: {
    msw: {
      handlers: [
        http.get('*/connectionregistry/', () =>
          HttpResponse.json({ error: 'fixture' }, { status: 503 }),
        ),
      ],
    },
  },
};

import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, userEvent, within } from 'storybook/test';
import { useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Collapsible,
  NumberField,
  StateMessage,
  StatusSlot,
  Switch,
} from './primitives';

function Gallery() {
  const [enabled, setEnabled] = useState(true);
  const [delay, setDelay] = useState(50);
  return (
    <Card className="max-w-2xl space-y-4 p-5">
      <div className="flex flex-wrap gap-2">
        <Button>Primary action</Button>
        <Badge className="text-success">Healthy</Badge>
        <Badge className="text-danger">Blocked</Badge>
      </div>
      <div className="flex items-center gap-4">
        <Switch checked={enabled} onCheckedChange={setEnabled} label="Example connection" />
        <NumberField value={delay} onValueChange={setDelay} label="Delay" />
      </div>
      <Collapsible title="Long stack trace">
        at io.sniffy.example.Service.call(Service.java:42)
      </Collapsible>
      <StateMessage kind="empty">Nothing captured yet.</StateMessage>
      <StatusSlot className="w-64" kind="loading" message="Saving…" />
      <StatusSlot
        className="w-64"
        kind="error"
        message="A long connection error remains inside this reserved status slot."
      />
    </Card>
  );
}

function ThemePanel({ theme }: { theme: 'light' | 'dark' }) {
  const [enabled, setEnabled] = useState(true);
  const label = `${theme[0].toUpperCase()}${theme.slice(1)} theme`;

  return (
    <section
      aria-label={label}
      className={`sniffy-theme-${theme} min-w-0 rounded-lg bg-canvas p-5 text-foreground`}
    >
      <div className="mb-4">
        <h2 className="text-base font-semibold">{label}</h2>
        <p className="text-sm text-muted">Primary and muted text on the shared canvas.</p>
        <a className="text-sm text-accent underline" href="https://sniffy.io/">
          Accessible product link
        </a>
      </div>
      <Card className="space-y-4 p-4">
        <div className="flex flex-wrap gap-2">
          <Button>Primary action</Button>
          <Button disabled>Disabled action</Button>
        </div>
        <div className="flex flex-wrap items-center gap-4">
          <Switch
            checked={enabled}
            onCheckedChange={setEnabled}
            label={`${label} example connection`}
          />
          <NumberField value={50} onValueChange={() => undefined} label={`${label} delay`} />
        </div>
        <div aria-label={`${label} domain colors`} className="flex flex-wrap gap-2">
          <Badge className="text-success">Success</Badge>
          <Badge className="text-warning">Warning</Badge>
          <Badge className="text-danger">Danger</Badge>
          <Badge className="text-info">Info</Badge>
          <Badge className="text-sql">SQL</Badge>
          <Badge className="text-network">Network</Badge>
          <Badge className="text-exception">Exception</Badge>
        </div>
      </Card>
    </section>
  );
}

function ThemeComparison() {
  return (
    <div className="flex max-w-6xl flex-wrap gap-4">
      <ThemePanel theme="light" />
      <ThemePanel theme="dark" />
    </div>
  );
}

const meta = {
  title: 'Design system/Primitives',
  component: Gallery,
  tags: ['autodocs'],
} satisfies Meta<typeof Gallery>;
export default meta;
type Story = StoryObj<typeof meta>;
export const Components: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole('switch'));
    await expect(canvas.getByRole('switch')).toHaveAttribute('aria-checked', 'false');
  },
};

export const Themes: StoryObj<typeof ThemeComparison> = {
  name: 'Theme comparison',
  render: () => <ThemeComparison />,
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const light = canvas.getByRole('region', { name: 'Light theme' });
    const dark = canvas.getByRole('region', { name: 'Dark theme' });

    await expect(light).toHaveClass('sniffy-theme-light');
    await expect(dark).toHaveClass('sniffy-theme-dark');
    await expect(within(light).getByText('SQL')).toBeVisible();
    await expect(within(dark).getByText('Network')).toBeVisible();

    const lightSwitch = within(light).getByRole('switch', {
      name: 'Light theme example connection',
    });
    await userEvent.click(lightSwitch);
    await expect(lightSwitch).toHaveAttribute('aria-checked', 'false');
  },
};

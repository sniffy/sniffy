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

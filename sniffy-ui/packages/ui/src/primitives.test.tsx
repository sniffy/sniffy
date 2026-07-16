import { fireEvent, render, screen } from '@testing-library/react';
import { Button, Collapsible, StatusSlot, Switch } from './primitives';

describe('shared UI primitives', () => {
  it('operates buttons and switches from the keyboard', () => {
    const click = vi.fn();
    const change = vi.fn();
    render(
      <>
        <Button onClick={click}>Refresh</Button>
        <Switch checked={false} onCheckedChange={change} label="Persistent" />
      </>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));
    fireEvent.click(screen.getByRole('switch', { name: 'Persistent' }));
    expect(click).toHaveBeenCalledOnce();
    expect(change).toHaveBeenCalled();
    expect(screen.getByRole('switch', { name: 'Persistent' })).toHaveClass('sniffy-switch');
    expect(screen.getByRole('switch', { name: 'Persistent' }).firstElementChild).toHaveClass(
      'sniffy-switch-thumb',
    );
  });

  it('exposes collapsible content accessibly', () => {
    render(<Collapsible title="Stack trace">at Example.call</Collapsible>);
    expect(screen.getByRole('button', { name: 'Stack trace' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('reserves one stable status slot for idle, pending and long error states', () => {
    const { rerender } = render(<StatusSlot className="w-40" />);
    const slot = document.querySelector('[data-kind="idle"]') as HTMLElement;
    expect(slot).toHaveClass('h-6', 'w-40');
    expect(slot).toHaveAttribute('data-visible', 'false');

    rerender(<StatusSlot className="w-40" kind="loading" message="Saving…" />);
    expect(screen.getByRole('status', { name: 'Saving…' })).toBeVisible();
    expect(slot).toHaveClass('h-6', 'w-40');

    rerender(
      <StatusSlot
        className="w-40"
        kind="error"
        message="A very long failure that must stay inside the reserved status slot"
      />,
    );
    expect(screen.getByRole('alert')).toHaveAttribute(
      'title',
      'A very long failure that must stay inside the reserved status slot',
    );
    expect(slot).toHaveClass('h-6', 'w-40', 'overflow-hidden');
  });
});

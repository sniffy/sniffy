import { fireEvent, render, screen } from '@testing-library/react';
import { Button, Collapsible, Switch } from './primitives';

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
  });

  it('exposes collapsible content accessibly', () => {
    render(<Collapsible title="Stack trace">at Example.call</Collapsible>);
    expect(screen.getByRole('button', { name: 'Stack trace' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });
});

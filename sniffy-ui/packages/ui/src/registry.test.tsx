import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ConnectionRegistry, SniffyClient } from '@sniffy/api';
import { ConnectionRegistryPanel } from './registry';

const registry = (status: number): ConnectionRegistry => ({
  persistent: false,
  sockets: [{ host: 'example.test', port: 443, status }],
  dataSources: [],
});

describe('connection registry controls', () => {
  it('resynchronizes row controls with the authoritative refresh response', async () => {
    const getRegistry = vi
      .fn<SniffyClient['getRegistry']>()
      .mockResolvedValueOnce(registry(0))
      .mockResolvedValueOnce(registry(0));
    const client: SniffyClient = {
      baseUrl: 'http://localhost/',
      getRegistry,
      getRequestDetails: vi.fn(),
      setSocket: vi.fn(),
      setDataSource: vi.fn(),
      setPersistent: vi.fn(),
      getTopSql: vi.fn(),
      resetTopSql: vi.fn(),
    };

    render(<ConnectionRegistryPanel client={client} />);
    const socketSwitch = await screen.findByRole('switch', {
      name: 'Enable example.test:443 socket',
    });
    expect(socketSwitch).toBeChecked();
    fireEvent.click(socketSwitch);
    await waitFor(() => expect(socketSwitch).not.toBeChecked());

    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));
    await waitFor(() =>
      expect(screen.getByRole('switch', { name: 'Enable example.test:443 socket' })).toBeChecked(),
    );
    expect(getRegistry).toHaveBeenCalledTimes(2);
  });
});

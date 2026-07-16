import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ConnectionRegistry, SniffyClient } from '@sniffy/api';
import { ConnectionRegistryPanel } from './registry';

const registry = (status: number): ConnectionRegistry => ({
  persistent: false,
  sockets: [{ host: 'example.test', port: 443, status }],
  dataSources: [],
});

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function client(overrides: Partial<SniffyClient> = {}): SniffyClient {
  return {
    baseUrl: 'http://localhost/',
    getRegistry: vi.fn().mockResolvedValue(registry(0)),
    getRequestDetails: vi.fn(),
    setSocket: vi.fn().mockResolvedValue(undefined),
    setDataSource: vi.fn().mockResolvedValue(undefined),
    setPersistent: vi.fn().mockResolvedValue(undefined),
    getTopSql: vi.fn(),
    resetTopSql: vi.fn(),
    ...overrides,
  };
}

describe('connection registry controls', () => {
  afterEach(() => vi.useRealTimers());

  it('keeps the last table visible while refresh is pending and after refresh failure', async () => {
    const refresh = deferred<ConnectionRegistry>();
    const getRegistry = vi
      .fn<SniffyClient['getRegistry']>()
      .mockResolvedValueOnce(registry(0))
      .mockReturnValueOnce(refresh.promise);
    render(<ConnectionRegistryPanel client={client({ getRegistry })} />);
    await screen.findByRole('table', { name: 'Socket connections' });

    fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));
    expect(screen.getByRole('table', { name: 'Socket connections' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeDisabled();
    expect(screen.getByRole('status', { name: 'Refreshing…' })).toBeVisible();
    refresh.reject(new Error('refresh failed'));

    expect(await screen.findByRole('alert')).toHaveTextContent('refresh failed');
    expect(screen.getByRole('table', { name: 'Socket connections' })).toBeVisible();
  });

  it('reloads authoritative row state and surfaces an error after a rejected mutation', async () => {
    const getRegistry = vi
      .fn<SniffyClient['getRegistry']>()
      .mockResolvedValueOnce(registry(0))
      .mockResolvedValueOnce(registry(0));
    const setSocket = vi
      .fn<SniffyClient['setSocket']>()
      .mockRejectedValue(new Error('write failed'));
    render(<ConnectionRegistryPanel client={client({ getRegistry, setSocket })} />);
    const socketSwitch = await screen.findByRole('switch', {
      name: 'Enable example.test:443 socket',
    });

    fireEvent.click(socketSwitch);
    expect(socketSwitch).not.toBeChecked();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'write failed. Server state was reloaded.',
    );
    await waitFor(() => expect(socketSwitch).toBeChecked());
    expect(getRegistry).toHaveBeenCalledTimes(2);
  });

  it('debounces rapid delays and serializes a newer intent behind an in-flight write', async () => {
    const firstWrite = deferred<void>();
    const setSocket = vi
      .fn<SniffyClient['setSocket']>()
      .mockReturnValueOnce(firstWrite.promise)
      .mockResolvedValueOnce(undefined);
    render(<ConnectionRegistryPanel client={client({ setSocket })} />);
    const increment = await screen.findByRole('button', {
      name: 'Increase example.test:443 socket delay',
    });
    vi.useFakeTimers();

    act(() => {
      fireEvent.click(increment);
      fireEvent.click(increment);
    });
    await act(async () => vi.advanceTimersByTime(250));
    expect(setSocket).toHaveBeenCalledTimes(1);
    expect(setSocket).toHaveBeenLastCalledWith(expect.anything(), 2);

    act(() => {
      fireEvent.click(increment);
      fireEvent.click(increment);
    });
    await act(async () => vi.advanceTimersByTime(250));
    expect(setSocket).toHaveBeenCalledTimes(1);
    firstWrite.resolve();
    await act(async () => Promise.resolve());
    expect(setSocket).toHaveBeenCalledTimes(2);
    expect(setSocket).toHaveBeenLastCalledWith(expect.anything(), 4);
  });

  it('uses one coherent table shell and fixed per-row status slots', async () => {
    const getRegistry = vi.fn().mockResolvedValue({
      ...registry(0),
      dataSources: [{ url: 'jdbc:test', userName: 'sa', status: -1 }],
    });
    render(<ConnectionRegistryPanel client={client({ getRegistry })} />);
    const dataSources = await screen.findByRole('table', { name: 'Database connections' });
    const sockets = screen.getByRole('table', { name: 'Socket connections' });

    expect(dataSources.closest('.overflow-hidden')).toBe(sockets.closest('.overflow-hidden'));
    for (const table of [dataSources, sockets]) {
      expect(table).toHaveClass('[&_td]:h-14', '[&_th]:bg-surface-raised');
    }
    expect(document.querySelectorAll('[data-kind="idle"].w-28')).toHaveLength(2);
  });
});

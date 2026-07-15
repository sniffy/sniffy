import { act, fireEvent, render, screen } from '@testing-library/react';
import { requestFixture } from '@sniffy/fixtures';
import { copyText, ProfilerApp } from './app';
import type { InterceptedRequest, ProfilerMetadata } from './runtime';

const metadata: ProfilerMetadata = {
  requestId: '',
  requestMethod: 'GET',
  responseCode: '200',
  baseUrl: 'http://localhost/sniffy/4.0.0/',
  sqlQueries: 0,
  serverTime: 0,
};

function fixture(options: { metadata?: ProfilerMetadata; initialOpen?: boolean } = {}) {
  const portalHost = document.createElement('div');
  document.body.append(portalHost);
  const shadowRoot = portalHost.attachShadow({ mode: 'open' });
  let listener: ((request: InterceptedRequest) => void) | undefined;
  const intercepted = {
    subscribe(next: (request: InterceptedRequest) => void) {
      listener = next;
      return () => undefined;
    },
  };
  const result = render(
    <ProfilerApp
      metadata={options.metadata ?? metadata}
      intercepted={intercepted}
      shadowRoot={shadowRoot}
      initialOpen={options.initialOpen}
    />,
  );
  return {
    ...result,
    shadowRoot,
    intercept: (request: InterceptedRequest) => listener?.(request),
  };
}

describe('compact profiler widget', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('keeps the stable compact trigger inactive while the counter tray is visible', () => {
    const { container } = fixture();
    const trigger = container.querySelector('.sniffy-brand-trigger');
    expect(trigger).toHaveAttribute('data-visible', 'false');
    expect(trigger).toHaveAttribute('aria-hidden', 'true');
    expect(trigger).toBeDisabled();
    expect(trigger).toHaveAttribute('tabindex', '-1');
    expect(screen.queryByRole('button', { name: 'Open Sniffy profiler' })).toBeNull();
  });

  it('preserves real mouse hover until pointer leave and the collapse delay', async () => {
    vi.useFakeTimers();
    const { container } = fixture();
    const pin = screen.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const tray = pin.parentElement as HTMLElement;
    const shell = tray.closest('.sniffy-shell') as HTMLElement;
    expect(pin).toHaveAttribute('aria-pressed', 'true');
    expect(tray).toHaveAttribute('data-expanded', 'true');

    fireEvent.pointerEnter(shell, { pointerType: 'mouse' });
    fireEvent.click(pin);
    expect(pin).toHaveAttribute('aria-pressed', 'false');
    await act(async () => vi.advanceTimersByTime(750));
    expect(tray).toHaveAttribute('data-expanded', 'true');
    fireEvent.pointerLeave(shell, { pointerType: 'mouse' });
    await act(async () => vi.advanceTimersByTime(750));
    expect(tray).toHaveAttribute('data-expanded', 'false');
    const trigger = container.querySelector('.sniffy-brand-trigger');
    expect(trigger).toHaveAttribute('aria-hidden', 'false');
    expect(trigger).not.toBeDisabled();
    expect(trigger).toHaveAttribute('tabindex', '0');
  });

  it('preserves keyboard focus until blur and the collapse delay', async () => {
    vi.useFakeTimers();
    fixture();
    const pin = screen.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const tray = pin.parentElement as HTMLElement;
    pin.focus();
    fireEvent.keyDown(pin, { key: 'Enter' });
    fireEvent.click(pin);
    await act(async () => vi.advanceTimersByTime(750));
    expect(tray).toHaveAttribute('data-expanded', 'true');
    expect(pin).toHaveFocus();
    pin.blur();
    await act(async () => vi.advanceTimersByTime(749));
    expect(tray).toHaveAttribute('data-expanded', 'true');
    await act(async () => vi.advanceTimersByTime(1));
    expect(tray).toHaveAttribute('data-expanded', 'false');
  });

  it('collapses after touch unpin without a synthetic pointer leave', async () => {
    vi.useFakeTimers();
    fixture();
    const pin = screen.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const tray = pin.parentElement as HTMLElement;
    const shell = tray.closest('.sniffy-shell') as HTMLElement;
    fireEvent.pointerEnter(shell, { pointerType: 'touch' });
    fireEvent.focus(pin);
    fireEvent.pointerDown(pin, { pointerType: 'touch' });
    fireEvent.click(pin);
    await act(async () => vi.advanceTimersByTime(749));
    expect(tray).toHaveAttribute('data-expanded', 'true');
    await act(async () => vi.advanceTimersByTime(1));
    expect(tray).toHaveAttribute('data-expanded', 'false');
  });

  it('reveals the tray from the compact trigger and supports keyboard opening', () => {
    vi.useFakeTimers();
    fixture();
    const pin = screen.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const tray = pin.parentElement as HTMLElement;
    fireEvent.click(pin);
    act(() => vi.advanceTimersByTime(750));

    const trigger = screen.getByRole('button', { name: 'Open Sniffy profiler' });
    fireEvent.focus(trigger);
    expect(tray).toHaveAttribute('data-expanded', 'true');
    fireEvent.click(trigger);
    expect(screen.getByText('Sniffy profiler')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Toggle Sniffy profiler' })).toBeNull();
    expect(screen.getByLabelText('Profiler summary')).toBeVisible();
  });

  it('marks only changed counters and clears the coalesced update state', async () => {
    vi.useFakeTimers();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          ...requestFixture,
          networkConnections: [],
          executedQueries: [],
          exceptions: [],
        }),
        { status: 200 },
      ),
    );
    const { intercept } = fixture();

    await act(async () => {
      intercept({
        label: 'GET /ajax.json - 200',
        detailsUrl: 'http://localhost/request/42',
        sqlQueries: 2,
        timeToFirstByte: 21,
      });
      await Promise.resolve();
    });

    expect(screen.getByTitle('SQL queries')).toHaveAttribute('data-updating', 'true');
    expect(screen.getByTitle('Server time')).toHaveAttribute('data-updating', 'true');
    expect(screen.getByTitle('Network bytes')).toHaveAttribute('data-updating', 'false');
    expect(screen.getByTitle('Exceptions')).toHaveAttribute('data-updating', 'false');
    await act(async () => vi.advanceTimersByTime(800));
    expect(screen.getByTitle('SQL queries')).toHaveAttribute('data-updating', 'false');
    expect(screen.getByTitle('Server time')).toHaveAttribute('data-updating', 'false');
  });

  it('pulses the compact trigger for activity captured while collapsed', async () => {
    vi.useFakeTimers();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 200 }));
    const { intercept } = fixture();
    fireEvent.click(screen.getByRole('button', { name: 'Keep Sniffy counters pinned' }));
    fireEvent.blur(document.activeElement as Element);
    fireEvent.pointerLeave(screen.getByRole('button', { name: 'Toggle Sniffy profiler' }));
    await act(async () => vi.advanceTimersByTime(750));

    const trigger = screen.getByRole('button', { name: 'Open Sniffy profiler' });
    await act(async () => {
      intercept({
        label: 'GET /empty.json - 200',
        detailsUrl: 'http://localhost/request/empty',
        sqlQueries: 0,
        timeToFirstByte: 21,
      });
      await Promise.resolve();
    });
    expect(trigger).toHaveAttribute('data-updating', 'true');
    await act(async () => vi.advanceTimersByTime(800));
    expect(trigger).toHaveAttribute('data-updating', 'false');
  });

  it('keeps response-header TTFB when request details return an empty 200 body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 200 }));
    const { intercept } = fixture({ metadata: { ...metadata, serverTime: 84 } });

    await act(async () => {
      intercept({
        label: 'GET /empty.json - 200',
        detailsUrl: 'http://localhost/request/empty',
        sqlQueries: 0,
        timeToFirstByte: 21,
      });
      await Promise.resolve();
    });

    expect(screen.getByTitle('Server time')).toHaveTextContent('105 ms');
  });

  it('replaces the tray while open, closes accessibly, and dismisses only this mount', () => {
    const { container } = fixture();
    const counters = screen.getByRole('button', { name: 'Toggle Sniffy profiler' });
    fireEvent.click(counters);
    expect(screen.queryByRole('button', { name: 'Toggle Sniffy profiler' })).toBeNull();
    expect(screen.getByLabelText('Profiler summary')).toBeVisible();

    fireEvent.keyDown(document, { key: 'Escape' });
    const restoredCounters = screen.getByRole('button', { name: 'Toggle Sniffy profiler' });
    expect(restoredCounters).toHaveFocus();

    fireEvent.click(restoredCounters);
    fireEvent.pointerDown(document.body);
    expect(screen.queryByText('Sniffy profiler')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss Sniffy for this page' }));
    expect(container).toBeEmptyDOMElement();
  });

  it('keeps pointer interactions inside the rendered shell open when the portal ShadowRoot is elsewhere', () => {
    fixture({ initialOpen: true });

    const network = screen.getByRole('tab', { name: 'Network Connections' });
    fireEvent.pointerDown(network);
    fireEvent.click(network);

    expect(screen.getByText('Sniffy profiler')).toBeVisible();
    expect(network).toHaveAttribute('data-active');
  });
});

describe('profiler clipboard support', () => {
  const setClipboard = (value: Clipboard | undefined) =>
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value });

  afterEach(() => {
    vi.restoreAllMocks();
    setClipboard(undefined);
  });

  it('uses the Clipboard API when available', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    setClipboard({ writeText } as unknown as Clipboard);
    const root = document.createElement('div').attachShadow({ mode: 'open' });

    await copyText('report', root);

    expect(writeText).toHaveBeenCalledWith('report');
  });

  it('falls back when the Clipboard API is unavailable', async () => {
    setClipboard(undefined);
    const execCommand = vi.fn().mockReturnValue(true);
    Object.defineProperty(document, 'execCommand', { configurable: true, value: execCommand });
    const root = document.createElement('div').attachShadow({ mode: 'open' });

    await copyText('fallback report', root);

    expect(execCommand).toHaveBeenCalledWith('copy');
    expect(root.querySelector('textarea')).toBeNull();
  });

  it('falls back after Clipboard API rejection and reports complete failure', async () => {
    setClipboard({
      writeText: vi.fn().mockRejectedValue(new Error('denied')),
    } as unknown as Clipboard);
    Object.defineProperty(document, 'execCommand', {
      configurable: true,
      value: vi.fn().mockReturnValue(true),
    });
    const root = document.createElement('div').attachShadow({ mode: 'open' });
    await expect(copyText('rejected report', root)).resolves.toBeUndefined();

    Object.defineProperty(document, 'execCommand', {
      configurable: true,
      value: vi.fn().mockReturnValue(false),
    });
    await expect(copyText('failed report', root)).rejects.toThrow(
      'Copy is not supported by this browser',
    );
  });
});

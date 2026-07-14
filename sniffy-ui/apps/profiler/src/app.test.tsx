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

function fixture() {
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
  render(<ProfilerApp metadata={metadata} intercepted={intercepted} shadowRoot={shadowRoot} />);
  return { shadowRoot, intercept: (request: InterceptedRequest) => listener?.(request) };
}

describe('compact profiler widget', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('is pinned by default and reveals or delays collapse for pointer, focus and keyboard use', async () => {
    vi.useFakeTimers();
    fixture();
    const pin = screen.getByRole('button', { name: 'Keep Sniffy counters pinned' });
    const tray = pin.parentElement as HTMLElement;
    expect(pin).toHaveAttribute('aria-pressed', 'true');
    expect(tray).toHaveAttribute('data-expanded', 'true');

    fireEvent.click(pin);
    expect(pin).toHaveAttribute('aria-pressed', 'false');
    const shell = tray.closest('.sniffy-shell') as HTMLElement;
    fireEvent.pointerLeave(shell);
    await act(async () => vi.advanceTimersByTime(749));
    expect(tray).toHaveAttribute('data-expanded', 'true');
    await act(async () => vi.advanceTimersByTime(1));
    expect(tray).toHaveAttribute('data-expanded', 'false');

    fireEvent.pointerEnter(shell);
    expect(tray).toHaveAttribute('data-expanded', 'true');
    fireEvent.pointerLeave(shell);
    await act(async () => vi.advanceTimersByTime(750));
    expect(tray).toHaveAttribute('data-expanded', 'false');

    const trigger = screen.getByRole('button', { name: 'Open Sniffy profiler' });
    fireEvent.focus(trigger);
    expect(tray).toHaveAttribute('data-expanded', 'true');
    fireEvent.keyDown(trigger, { key: 'Enter' });
    fireEvent.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Sniffy profiler')).toBeVisible();
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

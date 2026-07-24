import { act, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import GitHubStarsNavbarItem, { formatStarCount } from './GitHubStarsNavbarItem';
import { getGitHubStarCount, resetGitHubStarsCacheForTests } from './github-stars';

const href = 'https://github.com/sniffy/sniffy';

function jsonResponse(body: unknown, ok = true): Response {
  return { json: vi.fn().mockResolvedValue(body), ok } as unknown as Response;
}

describe('GitHubStarsNavbarItem', () => {
  beforeEach(() => {
    resetGitHubStarsCacheForTests();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it.each([
    [999, '999'],
    [1_000, '1K'],
    [1_049, '1K'],
    [1_050, '1.1K'],
    [9_949, '9.9K'],
    [9_950, '10K'],
    [999_499, '999K'],
    [999_500, '1M'],
    [1_000_000, '1M'],
    [1_250_000, '1.3M'],
  ])('formats %i stars as %s', (count, formatted) => {
    expect(formatStarCount(count)).toBe(formatted);
  });

  it('shows a compact count while preserving the exact accessible meaning', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ stargazers_count: 12_345 }));
    render(<GitHubStarsNavbarItem href={href} />);

    const link = screen.getByRole('link', { name: 'Sniffy GitHub repository' });
    expect(link).toHaveAttribute('href', href);
    expect(screen.getByTestId('github-star-count')).toBeEmptyDOMElement();

    await waitFor(() =>
      expect(
        screen.getByRole('link', { name: 'Sniffy GitHub repository, 12,345 stars' }),
      ).toBeVisible(),
    );
    expect(screen.getByText('12K')).toHaveAttribute('aria-hidden', 'true');
  });

  it.each([
    ['HTTP failure', jsonResponse({}, false)],
    ['missing count', jsonResponse({})],
    ['negative count', jsonResponse({ stargazers_count: -1 })],
    ['non-integer count', jsonResponse({ stargazers_count: 1.5 })],
    ['string count', jsonResponse({ stargazers_count: '123' })],
  ])('keeps a useful fallback for %s', async (_name, response) => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response);
    render(<GitHubStarsNavbarItem href={href} />);

    const link = screen.getByRole('link', { name: 'Sniffy GitHub repository' });
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledOnce());
    expect(link).toHaveAttribute('href', href);
    expect(screen.getByTestId('github-star-count')).toBeEmptyDOMElement();
  });

  it('handles an offline request without logging an error', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('offline'));
    render(<GitHubStarsNavbarItem href={href} />);

    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledOnce());
    expect(screen.getByRole('link', { name: 'Sniffy GitHub repository' })).toHaveAttribute(
      'href',
      href,
    );
    expect(consoleError).not.toHaveBeenCalled();
  });

  it('aborts an in-flight request when unmounted without updating the component', () => {
    let requestSignal: AbortSignal | undefined;
    vi.spyOn(globalThis, 'fetch').mockImplementation((_input, init) => {
      requestSignal = init?.signal ?? undefined;
      return new Promise(() => undefined);
    });

    const view = render(<GitHubStarsNavbarItem href={href} />);
    expect(requestSignal?.aborted).toBe(false);
    act(() => view.unmount());
    expect(requestSignal?.aborted).toBe(true);
  });

  it('uses the in-memory cache across mounts', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse({ stargazers_count: 7_654 }));
    const first = render(<GitHubStarsNavbarItem href={href} />);
    await screen.findByRole('link', { name: 'Sniffy GitHub repository, 7,654 stars' });
    first.unmount();

    render(<GitHubStarsNavbarItem href={href} />);
    await screen.findByRole('link', { name: 'Sniffy GitHub repository, 7,654 stars' });
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('uses a valid session cache without requesting GitHub', async () => {
    sessionStorage.setItem(
      'sniffy.github-stars.v1',
      JSON.stringify({ count: 4_321, expiresAt: Date.now() + 60_000 }),
    );
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    await expect(getGitHubStarCount(new AbortController().signal)).resolves.toBe(4_321);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

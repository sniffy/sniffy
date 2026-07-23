import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';

import SearchBar from './index';

const mocks = vi.hoisted(() => ({
  historyPush: vi.fn(),
  loadSearchIndexes: vi.fn(),
  searchIndexes: vi.fn(),
}));

vi.mock('@docusaurus/router', () => ({
  useHistory: () => ({ push: mocks.historyPush }),
}));

vi.mock('@docusaurus/theme-common', () => ({
  useContextualSearchFilters: () => ({ tags: [] }),
}));

vi.mock('@docusaurus/useDocusaurusContext', () => ({
  default: () => ({ siteConfig: { baseUrl: '/' } }),
}));

vi.mock('./search-index', () => ({
  loadSearchIndexes: mocks.loadSearchIndexes,
  searchIndexes: mocks.searchIndexes,
}));

type Deferred<T> = {
  promise: Promise<T>;
  reject: (reason: unknown) => void;
  resolve: (value: T) => void;
};

function deferred<T>(): Deferred<T> {
  let reject!: Deferred<T>['reject'];
  let resolve!: Deferred<T>['resolve'];
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    reject = rejectPromise;
    resolve = resolvePromise;
  });
  return { promise, reject, resolve };
}

const results = [
  {
    pageTitle: 'Traffic capture and TLS inspection',
    score: 12,
    sectionTitle: 'Traffic capture and TLS inspection',
    url: '/docs/network/traffic-capture/',
  },
  {
    pageTitle: 'Traffic capture and TLS inspection',
    score: 8,
    sectionTitle: 'SSL/TLS Traffic Decryption',
    url: '/docs/network/traffic-capture/#ssltls-traffic-decryption',
  },
];

describe('documentation search bar', () => {
  beforeEach(() => {
    mocks.historyPush.mockReset();
    mocks.loadSearchIndexes.mockReset();
    mocks.searchIndexes.mockReset();

    HTMLDialogElement.prototype.showModal = function showModal() {
      this.open = true;
    };
    HTMLDialogElement.prototype.close = function close() {
      if (!this.open) return;
      this.open = false;
      this.dispatchEvent(new Event('close'));
    };
  });

  it('opens from the shortcut, reports loading and no-results states, then restores focus', async () => {
    const indexRequest = deferred<unknown[]>();
    const handleSearchBarToggle = vi.fn();
    mocks.loadSearchIndexes.mockReturnValue(indexRequest.promise);
    mocks.searchIndexes.mockReturnValue([]);
    render(<SearchBar handleSearchBarToggle={handleSearchBarToggle} />);

    const trigger = screen.getByRole('button', { name: 'Search docs' });
    const shortcut = new KeyboardEvent('keydown', {
      bubbles: true,
      cancelable: true,
      ctrlKey: true,
      key: 'K',
    });
    act(() => {
      document.dispatchEvent(shortcut);
    });

    const dialog = await screen.findByRole('dialog', { name: 'Search Sniffy docs' });
    const input = screen.getByRole('combobox', {
      name: 'Search current Sniffy documentation',
    });
    expect(shortcut.defaultPrevented).toBe(true);
    expect(dialog).toHaveAttribute('open');
    expect(input).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Loading the local documentation index');
    expect(document.documentElement.dataset.sniffySearchOpen).toBe('true');
    expect(handleSearchBarToggle).toHaveBeenCalledWith(true);
    expect(mocks.loadSearchIndexes).toHaveBeenCalledWith('/', ['docs-default-current']);

    await act(async () => indexRequest.resolve([]));
    fireEvent.change(input, { target: { value: 'missing guide' } });
    expect(await screen.findByRole('status')).toHaveTextContent(
      'No documentation results for “missing guide”.',
    );

    fireEvent.keyDown(input, { key: 'Escape' });
    await waitFor(() => expect(dialog).not.toHaveAttribute('open'));
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(document.documentElement.dataset.sniffySearchOpen).toBeUndefined();
    expect(handleSearchBarToggle).toHaveBeenCalledWith(false);
  });

  it('fails closed when loading or querying the local index fails', async () => {
    mocks.loadSearchIndexes.mockRejectedValueOnce(new Error('offline'));
    const { unmount } = render(<SearchBar />);

    fireEvent.click(screen.getByRole('button', { name: 'Search docs' }));
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Search is temporarily unavailable',
    );
    expect(screen.getByRole('link', { name: 'Browse all documentation' })).toHaveAttribute(
      'href',
      '/docs/',
    );
    unmount();

    mocks.loadSearchIndexes.mockResolvedValueOnce([]);
    mocks.searchIndexes.mockImplementationOnce(() => {
      throw new Error('malformed index');
    });
    render(<SearchBar />);
    fireEvent.click(screen.getByRole('button', { name: 'Search docs' }));
    const input = screen.getByRole('combobox', {
      name: 'Search current Sniffy documentation',
    });
    await waitFor(() => expect(mocks.loadSearchIndexes).toHaveBeenCalled());
    fireEvent.change(input, { target: { value: 'traffic capture' } });
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Search is temporarily unavailable',
    );
  });

  it('selects results with the keyboard or pointer and navigates without a page reload', async () => {
    mocks.loadSearchIndexes.mockResolvedValue([]);
    mocks.searchIndexes.mockImplementation((_indexes, query: string) =>
      query.trim().length > 0 ? results : [],
    );
    render(<SearchBar />);

    fireEvent.click(screen.getByRole('button', { name: 'Search docs' }));
    const input = screen.getByRole('combobox', {
      name: 'Search current Sniffy documentation',
    });
    fireEvent.change(input, { target: { value: 'traffic capture' } });
    const options = await screen.findAllByRole('option');
    expect(options[0]).toHaveAttribute('aria-selected', 'true');
    expect(input).toHaveAttribute('aria-activedescendant', 'sniffy-search-result-0');

    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(options[1]).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    expect(options[0]).toHaveAttribute('aria-selected', 'true');
    fireEvent.mouseEnter(options[1]);
    expect(options[1]).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(mocks.historyPush).toHaveBeenCalledWith(
      '/docs/network/traffic-capture/#ssltls-traffic-decryption',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Search docs' }));
    fireEvent.change(input, { target: { value: 'traffic capture' } });
    fireEvent.click((await screen.findAllByRole('option'))[0]);
    expect(mocks.historyPush).toHaveBeenLastCalledWith('/docs/network/traffic-capture/');
  });
});

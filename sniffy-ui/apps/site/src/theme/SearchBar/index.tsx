import { useHistory } from '@docusaurus/router';
import { useContextualSearchFilters } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import {
  type ChangeEvent,
  type KeyboardEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import { loadSearchIndexes, searchIndexes, type SearchResult } from './search-index';
import styles from './styles.module.css';

type SearchBarProps = {
  handleSearchBarToggle?: (active: boolean) => void;
};

type IndexState = 'error' | 'idle' | 'loading' | 'ready';

const suggestedQueries = ['Installation', 'Configuration', 'SQL assertions', 'Traffic capture'];
const currentDocsSearchTag = 'docs-default-current';

function isMacPlatform() {
  const platform =
    (navigator as Navigator & { userAgentData?: { platform?: string } }).userAgentData?.platform ??
    navigator.platform;
  return /mac/i.test(platform);
}

export default function SearchBar({ handleSearchBarToggle }: SearchBarProps) {
  const {
    siteConfig: { baseUrl },
  } = useDocusaurusContext();
  const { tags } = useContextualSearchFilters();
  const history = useHistory();
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const indexesRef = useRef<Awaited<ReturnType<typeof loadSearchIndexes>>>([]);
  const [activeResult, setActiveResult] = useState(-1);
  const [indexState, setIndexState] = useState<IndexState>('idle');
  const [isMac, setIsMac] = useState(false);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);

  const closeSearch = useCallback(() => {
    dialogRef.current?.close();
  }, []);

  const openSearch = useCallback(() => {
    setQuery('');
    setResults([]);
    setActiveResult(-1);
    setOpen(true);
  }, []);

  useEffect(() => {
    setIsMac(isMacPlatform());
  }, []);

  useEffect(() => {
    const handleShortcut = (event: globalThis.KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase('en') === 'k') {
        event.preventDefault();
        openSearch();
      }
    };

    document.addEventListener('keydown', handleShortcut);
    return () => document.removeEventListener('keydown', handleShortcut);
  }, [openSearch]);

  useEffect(() => {
    if (!open) return;

    const dialog = dialogRef.current;
    if (!dialog) return;

    if (!dialog.open) dialog.showModal();
    document.documentElement.dataset.sniffySearchOpen = 'true';
    handleSearchBarToggle?.(true);
    inputRef.current?.focus();

    if (indexState === 'idle') {
      setIndexState('loading');
      loadSearchIndexes(baseUrl, [...tags, currentDocsSearchTag])
        .then((indexes) => {
          indexesRef.current = indexes;
          setIndexState('ready');
        })
        .catch(() => {
          indexesRef.current = [];
          setIndexState('error');
        });
    }

    return () => {
      delete document.documentElement.dataset.sniffySearchOpen;
      handleSearchBarToggle?.(false);
    };
  }, [baseUrl, handleSearchBarToggle, indexState, open, tags]);

  useEffect(() => {
    if (indexState !== 'ready') return;

    try {
      const nextResults = searchIndexes(indexesRef.current, query);
      setResults(nextResults);
      setActiveResult(nextResults.length > 0 ? 0 : -1);
    } catch {
      setResults([]);
      setActiveResult(-1);
      setIndexState('error');
    }
  }, [indexState, query]);

  const finishClose = useCallback(() => {
    setOpen(false);
    setQuery('');
    setResults([]);
    setActiveResult(-1);
    requestAnimationFrame(() => triggerRef.current?.focus());
  }, []);

  const navigateToResult = useCallback(
    (result: SearchResult) => {
      closeSearch();
      history.push(result.url);
    },
    [closeSearch, history],
  );

  const handleInputKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeSearch();
      return;
    }

    if (results.length === 0) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setActiveResult((current) => (current + 1) % results.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveResult((current) => (current <= 0 ? results.length - 1 : current - 1));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      navigateToResult(results[Math.max(activeResult, 0)]);
    }
  };

  const handleQueryChange = (event: ChangeEvent<HTMLInputElement>) => {
    setQuery(event.target.value);
  };

  return (
    <div className={`navbar__search ${styles.search}`}>
      <button
        aria-label="Search docs"
        aria-haspopup="dialog"
        aria-keyshortcuts="Meta+K Control+K"
        className={styles.trigger}
        onClick={openSearch}
        ref={triggerRef}
        type="button"
      >
        <span aria-hidden="true" className={styles.searchIcon}>
          ⌕
        </span>
        <span>Search docs</span>
        <kbd className={styles.shortcut}>{isMac ? '⌘ K' : 'Ctrl K'}</kbd>
      </button>

      <dialog
        aria-describedby="sniffy-search-description"
        aria-labelledby="sniffy-search-title"
        className={styles.dialog}
        onCancel={(event) => {
          event.preventDefault();
          closeSearch();
        }}
        onClose={finishClose}
        ref={dialogRef}
      >
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Current documentation</p>
            <h2 id="sniffy-search-title">Search Sniffy docs</h2>
            <p className={styles.description} id="sniffy-search-description">
              Search installation, configuration, testing, and network guides.
            </p>
          </div>
          <button
            aria-label="Close documentation search"
            className={styles.close}
            onClick={closeSearch}
            type="button"
          >
            Esc
          </button>
        </div>

        <div className={styles.inputShell}>
          <span aria-hidden="true" className={styles.searchIcon}>
            ⌕
          </span>
          <input
            aria-activedescendant={
              activeResult >= 0 ? `sniffy-search-result-${activeResult}` : undefined
            }
            aria-autocomplete="list"
            aria-controls="sniffy-search-results"
            aria-expanded={query.trim().length > 0}
            aria-label="Search current Sniffy documentation"
            autoComplete="off"
            className={styles.input}
            onChange={handleQueryChange}
            onKeyDown={handleInputKeyDown}
            placeholder="Try “network fault” or “SQL assertions”"
            ref={inputRef}
            role="combobox"
            type="search"
            value={query}
          />
        </div>

        <div aria-live="polite" className={styles.resultsStatus} role="status">
          {indexState === 'loading' ? 'Loading the local documentation index…' : null}
          {indexState === 'error'
            ? 'Search is temporarily unavailable. The documentation is still available to browse.'
            : null}
          {indexState === 'ready' && query.trim() && results.length === 0
            ? `No documentation results for “${query.trim()}”.`
            : null}
        </div>

        {indexState === 'error' ? (
          <a className={styles.browseLink} href={`${baseUrl}docs/`}>
            Browse all documentation
          </a>
        ) : null}

        {indexState !== 'error' && query.trim().length === 0 ? (
          <div className={styles.suggestions}>
            <p>Suggested searches</p>
            <div>
              {suggestedQueries.map((suggestion) => (
                <button
                  key={suggestion}
                  onClick={() => {
                    setQuery(suggestion);
                    inputRef.current?.focus();
                  }}
                  type="button"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <div className={styles.results} id="sniffy-search-results" role="listbox">
          {results.map((result, index) => (
            <a
              aria-selected={index === activeResult}
              className={`${styles.result} ${index === activeResult ? styles.activeResult : ''}`}
              href={result.url}
              id={`sniffy-search-result-${index}`}
              key={result.url}
              onClick={(event) => {
                event.preventDefault();
                navigateToResult(result);
              }}
              onMouseEnter={() => setActiveResult(index)}
              role="option"
            >
              <span className={styles.resultTitle}>{result.sectionTitle}</span>
              <span className={styles.resultPage}>
                {result.sectionTitle === result.pageTitle
                  ? 'Documentation page'
                  : `In ${result.pageTitle}`}
              </span>
            </a>
          ))}
        </div>

        <div className={styles.footer}>
          <span>
            <kbd>↑</kbd>
            <kbd>↓</kbd> move
          </span>
          <span>
            <kbd>Enter</kbd> open
          </span>
          <span>
            <kbd>Esc</kbd> close
          </span>
        </div>
      </dialog>
    </div>
  );
}

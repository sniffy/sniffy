import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Clipboard,
  Eraser,
  Maximize2,
  Minimize2,
  X,
  Database,
  Timer,
  Network,
  CircleAlert,
  Gauge,
  EyeOff,
  Pin,
  PinOff,
} from 'lucide-react';
import hljs from 'highlight.js/lib/core';
import sql from 'highlight.js/lib/languages/sql';
import java from 'highlight.js/lib/languages/java';
import type { RequestStats, SniffyClient, TopSql } from '@sniffy/api';
import { createSniffyClient } from '@sniffy/api';
import {
  Badge,
  Button,
  Card,
  CodeViewer,
  Collapsible,
  ConnectionRegistryPanel,
  IconButton,
  StateMessage,
  StatusSlot,
  Table,
  Tabs,
  Tooltip,
} from '@sniffy/ui';
import type { InterceptedRequest, ProfilerMetadata } from './runtime';

hljs.registerLanguage('sql', sql);
hljs.registerLanguage('java', java);

interface RequestRecord {
  label: string;
  url: string;
  sqlQueries: number;
  serverTime?: number;
  stats?: RequestStats;
  error?: string;
}

type CounterKey = 'exceptions' | 'network' | 'time' | 'sql';

function requestNetworkBytes(stats: RequestStats): number {
  const queryBytes =
    stats.executedQueries?.reduce(
      (sum, query) => sum + (query.bytesDown ?? 0) + (query.bytesUp ?? 0),
      0,
    ) ?? 0;
  const connectionBytes =
    stats.networkConnections?.reduce(
      (sum, connection) => sum + (connection.bytesDown ?? 0) + (connection.bytesUp ?? 0),
      0,
    ) ?? 0;
  return queryBytes + connectionBytes;
}

export async function copyText(text: string, root: ShadowRoot): Promise<void> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return;
    }
  } catch {
    // Plain HTTP applications commonly reject Clipboard API access; use the local fallback.
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.cssText =
    'position:fixed;left:-9999px;top:0;width:1px;height:1px;opacity:0;pointer-events:none';
  root.append(textarea);
  textarea.focus();
  textarea.select();
  try {
    if (typeof document.execCommand !== 'function' || !document.execCommand('copy'))
      throw new Error('Copy is not supported by this browser');
  } finally {
    textarea.remove();
  }
}

function HighlightedCode({ code, language }: { code: string; language: 'sql' | 'java' }) {
  const html = useMemo(() => hljs.highlight(code, { language }).value, [code, language]);
  return (
    <pre className="overflow-auto whitespace-pre-wrap rounded-md bg-canvas p-3 font-mono text-xs">
      <code className="hljs" dangerouslySetInnerHTML={{ __html: html }} />
    </pre>
  );
}

function QueryDetails({ requests }: { requests: RequestRecord[] }) {
  if (requests.length === 0)
    return <StateMessage kind="empty">No requests captured yet.</StateMessage>;
  return (
    <div className="space-y-3 p-3">
      {requests.map((request, requestIndex) => (
        <Card className="overflow-hidden" key={`${request.url}:${requestIndex}`}>
          <div className="border-b border-border bg-surface-raised px-3 py-2 font-medium">
            {request.label}
          </div>
          <StatusSlot
            className="mx-3 my-2"
            kind={request.error ? 'error' : 'idle'}
            message={request.error}
          />
          {!request.stats && !request.error && (
            <StateMessage kind="loading">Loading request details…</StateMessage>
          )}
          {request.stats?.executedQueries?.length === 0 && (
            <StateMessage kind="empty">No SQL queries.</StateMessage>
          )}
          {request.stats?.executedQueries?.map((query, index) => (
            <div className="space-y-2 border-b border-border p-3 last:border-0" key={index}>
              <HighlightedCode code={query.query} language="sql" />
              <div className="flex flex-wrap gap-2">
                <Badge>{query.time} ms</Badge>
                {query.invocations && query.invocations > 1 && (
                  <Badge className="text-warning">×{query.invocations}</Badge>
                )}
                {query.rows !== undefined && <Badge className="text-info">{query.rows} rows</Badge>}
                <Badge className="text-success">{query.bytesDown ?? 0} bytes down</Badge>
                <Badge className="text-danger">{query.bytesUp ?? 0} bytes up</Badge>
              </div>
              {query.stackTrace && (
                <Collapsible title="Stack trace">
                  <HighlightedCode code={query.stackTrace} language="java" />
                </Collapsible>
              )}
            </div>
          ))}
          {request.stats?.networkConnections?.map((connection, index) => (
            <div className="space-y-2 border-b border-network/40 p-3" key={`network:${index}`}>
              <div className="font-medium text-network">
                {connection.address ??
                  `${connection.host ?? 'network'}${connection.port === undefined ? '' : `:${connection.port}`}`}
              </div>
              <div className="flex flex-wrap gap-2">
                <Badge>{connection.time} ms</Badge>
                <Badge className="text-success">{connection.bytesDown ?? 0} bytes down</Badge>
                <Badge className="text-danger">{connection.bytesUp ?? 0} bytes up</Badge>
              </div>
              {connection.stackTrace && (
                <Collapsible title="Network stack trace">
                  <HighlightedCode code={connection.stackTrace} language="java" />
                </Collapsible>
              )}
            </div>
          ))}
          {request.stats?.exceptions?.map((exception, index) => (
            <div className="space-y-2 border-b border-danger/40 p-3" key={`exception:${index}`}>
              <div className="font-medium text-exception">
                {exception.className ?? exception.class ?? 'Exception'}: {exception.message}
              </div>
              {exception.stackTrace && (
                <Collapsible title="Exception stack trace">
                  <HighlightedCode code={exception.stackTrace} language="java" />
                </Collapsible>
              )}
            </div>
          ))}
        </Card>
      ))}
    </div>
  );
}

function TopSqlPanel({ client }: { client: SniffyClient }) {
  const [rows, setRows] = useState<TopSql[]>();
  const [sort, setSort] = useState<{ key: 'count' | 'mean'; direction: 'asc' | 'desc' }>({
    key: 'mean',
    direction: 'desc',
  });
  const [error, setError] = useState<string>();
  const load = useCallback(async () => {
    try {
      setRows(await client.getTopSql());
      setError(undefined);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    }
  }, [client]);
  useEffect(() => {
    void load();
  }, [load]);
  const sorted = [...(rows ?? [])].sort((a, b) => {
    const difference = a.timer[sort.key] - b.timer[sort.key];
    return sort.direction === 'asc' ? difference : -difference;
  });
  const changeSort = (key: 'count' | 'mean') =>
    setSort((current) => ({
      key,
      direction: current.key === key && current.direction === 'desc' ? 'asc' : 'desc',
    }));
  const sortIndicator = (key: 'count' | 'mean') =>
    sort.key === key ? (sort.direction === 'desc' ? ' ↓' : ' ↑') : '';
  return (
    <div className="p-3">
      <StatusSlot className="mx-3 mt-3" kind={error ? 'error' : 'idle'} message={error} />
      {!rows && !error && <StateMessage kind="loading">Loading Top SQL…</StateMessage>}
      {rows?.length === 0 && <StateMessage kind="empty">No SQL queries gathered yet.</StateMessage>}
      {rows && rows.length > 0 && (
        <Card className="overflow-x-auto">
          <Table label="Top SQL">
            <thead>
              <tr>
                <th>SQL</th>
                <th>
                  <button onClick={() => changeSort('count')}>Count{sortIndicator('count')}</button>
                </th>
                <th>
                  <button onClick={() => changeSort('mean')}>Mean{sortIndicator('mean')}</button>
                </th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((row) => (
                <tr key={row.sql}>
                  <td>
                    <CodeViewer code={row.sql} language="sql" />
                  </td>
                  <td>{row.timer.count}</td>
                  <td>{(row.timer.mean / 1_000_000).toFixed(3)} ms</td>
                </tr>
              ))}
            </tbody>
          </Table>
          <div className="p-3">
            <Button
              onClick={async () => {
                await client.resetTopSql();
                await load();
              }}
            >
              Reset gathered queries
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}

function formatTime(value: number): string {
  return value < 1000
    ? `${value} ms`
    : value < 60000
      ? `${(value / 1000).toFixed(1)} s`
      : `${Math.floor(value / 60000)}m ${Math.floor((value % 60000) / 1000)}s`;
}
function formatBytes(value: number): string {
  return value < 1000
    ? `${value} B`
    : value < 1_000_000
      ? `${Math.floor(value / 1000)} KB`
      : `${(value / 1_000_000).toFixed(1)} MB`;
}

function resolvedServerTime(request: RequestRecord): number {
  if (request.serverTime === undefined) return 0;
  return Math.max(request.serverTime, request.stats?.time ?? request.serverTime);
}

function SummaryStrip({
  exceptions,
  network,
  serverTime,
  sql,
}: {
  exceptions: number;
  network: number;
  serverTime: number;
  sql: number;
}) {
  return (
    <div
      className="sniffy-summary grid grid-cols-4 border-b border-border bg-surface"
      aria-label="Profiler summary"
    >
      <SummaryMetric
        icon={<CircleAlert size={14} />}
        label="Exceptions"
        value={String(exceptions)}
        color="text-exception"
      />
      <SummaryMetric
        icon={<Network size={14} />}
        label="Network bytes"
        value={formatBytes(network)}
        color="text-network"
      />
      <SummaryMetric
        icon={<Timer size={14} />}
        label="Server time"
        value={formatTime(serverTime)}
        color="text-info"
      />
      <SummaryMetric
        icon={<Database size={14} />}
        label="SQL queries"
        value={String(sql)}
        color="text-sql"
      />
    </div>
  );
}

function SummaryMetric({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: string;
}) {
  return (
    <div className="flex min-w-0 items-center justify-center gap-2 border-r border-border px-2 py-1.5 last:border-r-0">
      <span className={color}>{icon}</span>
      <span className="truncate text-xs text-muted">{label}</span>
      <strong className="shrink-0 text-xs" aria-live="polite">
        {value}
      </strong>
    </div>
  );
}

export function ProfilerApp({
  metadata,
  intercepted,
  shadowRoot,
  initialOpen = false,
}: {
  metadata: ProfilerMetadata;
  intercepted: { subscribe(listener: (request: InterceptedRequest) => void): () => void };
  shadowRoot: ShadowRoot;
  initialOpen?: boolean;
}) {
  const client = useMemo(() => createSniffyClient(metadata.baseUrl), [metadata.baseUrl]);
  const [open, setOpen] = useState(initialOpen);
  const [pinned, setPinned] = useState(true);
  const [pointerInside, setPointerInside] = useState(false);
  const [focusInside, setFocusInside] = useState(false);
  const [triggerActive, setTriggerActive] = useState(false);
  const [temporarilyExpanded, setTemporarilyExpanded] = useState(false);
  const [maximized, setMaximized] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [triggerUpdating, setTriggerUpdating] = useState(false);
  const [cleared, setCleared] = useState(false);
  const [requests, setRequests] = useState<RequestRecord[]>([]);
  const [updating, setUpdating] = useState<Record<CounterKey, boolean>>({
    exceptions: false,
    network: false,
    time: false,
    sql: false,
  });
  const [copyStatus, setCopyStatus] = useState<{ message: string; error: boolean }>();
  const collapseTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const pulseTimers = useRef<Partial<Record<CounterKey, ReturnType<typeof setTimeout>>>>({});
  const triggerPulseTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const copyStatusTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const counterButton = useRef<HTMLButtonElement | null>(null);
  const brandTrigger = useRef<HTMLButtonElement | null>(null);
  const shell = useRef<HTMLDivElement | null>(null);
  const restoreFocusAfterClose = useRef(false);
  const trayExpanded = pinned || pointerInside || focusInside || temporarilyExpanded;
  const trayExpandedRef = useRef(trayExpanded);
  const openRef = useRef(open);
  trayExpandedRef.current = trayExpanded;
  openRef.current = open;

  const cancelCollapse = useCallback(() => {
    if (collapseTimer.current !== undefined) clearTimeout(collapseTimer.current);
    collapseTimer.current = undefined;
  }, []);
  const scheduleCollapse = useCallback(() => {
    cancelCollapse();
    collapseTimer.current = setTimeout(() => {
      setTemporarilyExpanded(false);
    }, 750);
  }, [cancelCollapse]);
  const pulseCounters = useCallback((keys: CounterKey[]) => {
    const started = keys.filter((key) => pulseTimers.current[key] === undefined);
    if (started.length === 0) return;
    setUpdating((current) => ({
      ...current,
      ...Object.fromEntries(started.map((key) => [key, true])),
    }));
    for (const key of started) {
      pulseTimers.current[key] = setTimeout(() => {
        delete pulseTimers.current[key];
        setUpdating((current) => ({ ...current, [key]: false }));
      }, 800);
    }
  }, []);
  const pulseCollapsedTrigger = useCallback(() => {
    if (openRef.current || trayExpandedRef.current || triggerPulseTimer.current !== undefined)
      return;
    setTriggerUpdating(true);
    triggerPulseTimer.current = setTimeout(() => {
      triggerPulseTimer.current = undefined;
      setTriggerUpdating(false);
    }, 800);
  }, []);
  const showCopyStatus = useCallback((message: string, error: boolean) => {
    if (copyStatusTimer.current !== undefined) clearTimeout(copyStatusTimer.current);
    setCopyStatus({ message, error });
    copyStatusTimer.current = setTimeout(() => {
      copyStatusTimer.current = undefined;
      setCopyStatus(undefined);
    }, 1800);
  }, []);
  const closeProfiler = useCallback((restoreFocus: boolean) => {
    restoreFocusAfterClose.current = restoreFocus;
    setTemporarilyExpanded(true);
    setOpen(false);
  }, []);
  useEffect(
    () => () => {
      cancelCollapse();
      Object.values(pulseTimers.current).forEach((timer) => clearTimeout(timer));
      if (triggerPulseTimer.current !== undefined) clearTimeout(triggerPulseTimer.current);
      if (copyStatusTimer.current !== undefined) clearTimeout(copyStatusTimer.current);
    },
    [cancelCollapse],
  );
  useEffect(() => {
    if (open || !restoreFocusAfterClose.current) return;
    restoreFocusAfterClose.current = false;
    (counterButton.current ?? brandTrigger.current)?.focus();
  }, [open]);
  useEffect(() => {
    if (!open) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (shell.current && event.composedPath().includes(shell.current)) return;
      closeProfiler(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      closeProfiler(true);
    };
    document.addEventListener('pointerdown', handlePointerDown, true);
    document.addEventListener('keydown', handleKeyDown, true);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown, true);
      document.removeEventListener('keydown', handleKeyDown, true);
    };
  }, [closeProfiler, open]);
  const load = useCallback(
    async (
      label: string,
      url: string,
      sqlQueries = 0,
      serverTime?: number,
      interceptedRequest = false,
    ) => {
      setRequests((current) => [...current, { label, url, sqlQueries, serverTime }]);
      if (interceptedRequest)
        pulseCounters([
          ...(sqlQueries !== 0 ? (['sql'] as CounterKey[]) : []),
          ...(serverTime !== undefined && serverTime !== 0 ? (['time'] as CounterKey[]) : []),
        ]);
      try {
        const stats = await client.getRequestDetails(url);
        setRequests((current) =>
          current.map((item) => (item.url === url ? { ...item, stats } : item)),
        );
        if (interceptedRequest) {
          const changed: CounterKey[] = [];
          if (requestNetworkBytes(stats) !== 0) changed.push('network');
          if ((stats.exceptions?.length ?? 0) !== 0) changed.push('exceptions');
          if (serverTime !== undefined && stats.time !== serverTime) changed.push('time');
          pulseCounters(changed);
        }
      } catch (reason) {
        setRequests((current) =>
          current.map((item) =>
            item.url === url
              ? { ...item, error: reason instanceof Error ? reason.message : String(reason) }
              : item,
          ),
        );
      }
    },
    [client, pulseCounters],
  );
  useEffect(() => {
    if (metadata.requestId)
      void load(
        `${metadata.requestMethod || 'GET'} ${location.pathname}${metadata.responseCode ? ` - ${metadata.responseCode}` : ''}`,
        new URL(`request/${metadata.requestId}`, metadata.baseUrl).href,
      );
    return intercepted.subscribe((request) => {
      pulseCollapsedTrigger();
      void load(
        request.label,
        request.detailsUrl,
        request.sqlQueries,
        request.timeToFirstByte,
        true,
      );
    });
  }, [intercepted, load, metadata, pulseCollapsedTrigger]);
  const totals = requests.reduce(
    (result, request) => {
      const stats = request.stats;
      result.network += stats ? requestNetworkBytes(stats) : 0;
      result.exceptions += stats?.exceptions?.length ?? 0;
      return result;
    },
    { network: 0, exceptions: 0 },
  );
  const sqlCount = requests.reduce(
    (count, request) => count + request.sqlQueries,
    cleared ? 0 : metadata.sqlQueries,
  );
  const serverTime = requests.reduce(
    (time, request) => time + resolvedServerTime(request),
    cleared ? 0 : metadata.serverTime,
  );
  if (dismissed) return null;
  return (
    <div
      ref={shell}
      className="sniffy-shell"
      onPointerEnter={() => {
        setPointerInside(true);
        setTemporarilyExpanded(true);
        cancelCollapse();
      }}
      onPointerLeave={() => {
        setPointerInside(false);
        setTemporarilyExpanded(true);
        scheduleCollapse();
      }}
      onFocusCapture={() => {
        setFocusInside(true);
        setTemporarilyExpanded(true);
        cancelCollapse();
      }}
      onBlurCapture={(event) => {
        if (event.currentTarget.contains(event.relatedTarget as Node | null)) return;
        setFocusInside(false);
        setTemporarilyExpanded(true);
        scheduleCollapse();
      }}
    >
      {open ? (
        <Card
          className="sniffy-panel flex flex-col overflow-hidden shadow-[var(--sniffy-shadow)]"
          data-maximized={maximized}
        >
          <header className="flex flex-wrap items-center gap-2 border-b border-border bg-surface-raised px-3 py-2">
            <strong className="mr-auto">Sniffy profiler</strong>
            <StatusSlot
              className="w-36 shrink-0"
              kind={copyStatus?.error ? 'error' : copyStatus ? 'success' : 'idle'}
              message={copyStatus?.message}
            />
            <Tooltip label="Clear captured data" portalRoot={shadowRoot}>
              <IconButton
                aria-label="Clear captured data"
                onClick={() => {
                  setRequests([]);
                  setCleared(true);
                }}
              >
                <Eraser size={16} />
              </IconButton>
            </Tooltip>
            <Tooltip label="Copy report" portalRoot={shadowRoot}>
              <IconButton
                aria-label="Copy report"
                onClick={async () => {
                  const report = JSON.stringify(
                    requests.map((request) => request.stats),
                    null,
                    2,
                  );
                  try {
                    await copyText(report, shadowRoot);
                    showCopyStatus('Report copied.', false);
                  } catch (reason) {
                    showCopyStatus(
                      reason instanceof Error ? reason.message : 'Unable to copy report.',
                      true,
                    );
                  }
                }}
              >
                <Clipboard size={16} />
              </IconButton>
            </Tooltip>
            <Tooltip label={maximized ? 'Restore panel' : 'Maximize panel'} portalRoot={shadowRoot}>
              <IconButton
                aria-label={maximized ? 'Restore panel' : 'Maximize panel'}
                onClick={() => setMaximized(!maximized)}
              >
                {maximized ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
              </IconButton>
            </Tooltip>
            <Tooltip
              label={pinned ? 'Allow Sniffy counters to collapse' : 'Keep Sniffy counters pinned'}
              portalRoot={shadowRoot}
            >
              <IconButton
                aria-label="Keep Sniffy counters pinned"
                aria-pressed={pinned}
                onClick={() => setPinned((current) => !current)}
              >
                {pinned ? <PinOff size={15} /> : <Pin size={15} />}
              </IconButton>
            </Tooltip>
            <Tooltip label="Dismiss Sniffy for this page" portalRoot={shadowRoot}>
              <IconButton
                aria-label="Dismiss Sniffy for this page"
                onClick={() => {
                  setOpen(false);
                  setDismissed(true);
                }}
              >
                <EyeOff size={16} />
              </IconButton>
            </Tooltip>
            <IconButton aria-label="Close profiler" onClick={() => closeProfiler(true)}>
              <X size={16} />
            </IconButton>
          </header>
          <Tabs.Root defaultValue="queries" className="flex min-h-0 flex-1 flex-col">
            <Tabs.List
              className="flex border-b border-border bg-surface"
              aria-label="Profiler sections"
            >
              <Tabs.Tab
                className="border-b-2 border-transparent px-4 py-2 text-sm data-[active]:border-accent data-[active]:text-accent"
                value="queries"
              >
                Executed Queries
              </Tabs.Tab>
              <Tabs.Tab
                className="border-b-2 border-transparent px-4 py-2 text-sm data-[active]:border-accent data-[active]:text-accent"
                value="network"
              >
                Network Connections
              </Tabs.Tab>
              <Tabs.Tab
                className="border-b-2 border-transparent px-4 py-2 text-sm data-[active]:border-accent data-[active]:text-accent"
                value="topsql"
              >
                Top SQL
              </Tabs.Tab>
            </Tabs.List>
            <div className="sniffy-scroll min-h-0 flex-1 overflow-auto">
              <Tabs.Panel value="queries">
                <QueryDetails requests={requests} />
              </Tabs.Panel>
              <Tabs.Panel value="network" className="p-3">
                <ConnectionRegistryPanel client={client} />
              </Tabs.Panel>
              <Tabs.Panel value="topsql">
                <TopSqlPanel client={client} />
              </Tabs.Panel>
            </div>
          </Tabs.Root>
          <footer className="border-t border-border px-3 text-xs text-muted">
            <SummaryStrip
              exceptions={totals.exceptions}
              network={totals.network}
              serverTime={serverTime}
              sql={sqlCount}
            />
          </footer>
        </Card>
      ) : (
        <div
          className="sniffy-widget flex items-stretch overflow-hidden rounded-lg border border-border bg-surface shadow-[var(--sniffy-shadow)]"
          data-counter-expanded={trayExpanded}
        >
          {(!trayExpanded || triggerActive) && (
            <button
              ref={brandTrigger}
              className="sniffy-brand-trigger grid size-10 place-items-center text-accent hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-focus"
              data-updating={triggerUpdating}
              aria-label="Open Sniffy profiler"
              aria-expanded="false"
              onPointerEnter={() => setTriggerActive(true)}
              onPointerLeave={() => setTriggerActive(false)}
              onFocus={() => setTriggerActive(true)}
              onBlur={() => setTriggerActive(false)}
              onClick={() => {
                setTemporarilyExpanded(true);
                setOpen(true);
              }}
            >
              <Gauge size={19} />
            </button>
          )}
          <div
            className="sniffy-counter-tray flex items-stretch"
            data-expanded={trayExpanded}
            aria-hidden={!trayExpanded}
          >
            <button
              ref={counterButton}
              className="flex"
              aria-label="Toggle Sniffy profiler"
              aria-expanded="false"
              tabIndex={trayExpanded ? 0 : -1}
              onClick={() => setOpen((current) => !current)}
            >
              <Counter
                icon={<CircleAlert size={16} />}
                label="Exceptions"
                value={String(totals.exceptions)}
                color="text-exception"
                kind="exceptions"
                updating={updating.exceptions}
              />
              <Counter
                icon={<Network size={16} />}
                label="Network bytes"
                value={formatBytes(totals.network)}
                color="text-network"
                kind="network"
                updating={updating.network}
              />
              <Counter
                icon={<Timer size={16} />}
                label="Server time"
                value={formatTime(serverTime)}
                color="text-info"
                kind="time"
                updating={updating.time}
              />
              <Counter
                icon={<Database size={16} />}
                label="SQL queries"
                value={String(sqlCount)}
                color="text-sql"
                kind="sql"
                updating={updating.sql}
              />
            </button>
            <button
              className="grid w-9 place-items-center border-l border-border text-muted hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-focus"
              aria-label="Keep Sniffy counters pinned"
              aria-pressed={pinned}
              tabIndex={trayExpanded ? 0 : -1}
              onClick={() => {
                setPinned((current) => !current);
                setTemporarilyExpanded(true);
              }}
            >
              {pinned ? <PinOff size={15} /> : <Pin size={15} />}
            </button>
            <button
              className="grid w-9 place-items-center border-l border-border text-muted hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-focus"
              aria-label="Dismiss Sniffy for this page"
              tabIndex={trayExpanded ? 0 : -1}
              onClick={() => setDismissed(true)}
            >
              <EyeOff size={15} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function Counter({
  icon,
  label,
  value,
  color,
  kind,
  updating,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: string;
  kind: CounterKey;
  updating: boolean;
}) {
  return (
    <span
      className="sniffy-counter flex min-w-16 flex-col items-center gap-0.5 border-r border-border px-3 py-2 last:border-0"
      title={label}
      data-kind={kind}
      data-updating={updating}
    >
      <span className={color}>{icon}</span>
      <span className="text-xs font-semibold" aria-live="polite">
        {value}
      </span>
    </span>
  );
}

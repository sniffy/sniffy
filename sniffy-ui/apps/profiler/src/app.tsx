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
          {request.error && <StateMessage kind="error">{request.error}</StateMessage>}
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
      {error && <StateMessage kind="error">{error}</StateMessage>}
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
  const [temporarilyExpanded, setTemporarilyExpanded] = useState(false);
  const [maximized, setMaximized] = useState(false);
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
  useEffect(
    () => () => {
      cancelCollapse();
      Object.values(pulseTimers.current).forEach((timer) => clearTimeout(timer));
    },
    [cancelCollapse],
  );
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
    return intercepted.subscribe(
      (request) =>
        void load(
          request.label,
          request.detailsUrl,
          request.sqlQueries,
          request.timeToFirstByte,
          true,
        ),
    );
  }, [intercepted, load, metadata]);
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
    (time, request) =>
      request.serverTime === undefined ? time : time + (request.stats?.time ?? request.serverTime),
    cleared ? 0 : metadata.serverTime,
  );
  const trayExpanded = pinned || open || pointerInside || focusInside || temporarilyExpanded;
  return (
    <div
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
      {open && (
        <Card
          className="sniffy-panel flex flex-col overflow-hidden shadow-[var(--sniffy-shadow)]"
          data-maximized={maximized}
        >
          <header className="flex items-center gap-2 border-b border-border bg-surface-raised px-3 py-2">
            <strong className="mr-auto">Sniffy profiler</strong>
            {copyStatus && (
              <span
                className={copyStatus.error ? 'text-xs text-danger' : 'text-xs text-success'}
                role={copyStatus.error ? 'alert' : 'status'}
              >
                {copyStatus.message}
              </span>
            )}
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
                    setCopyStatus({ message: 'Report copied.', error: false });
                  } catch (reason) {
                    setCopyStatus({
                      message: reason instanceof Error ? reason.message : 'Unable to copy report.',
                      error: true,
                    });
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
            <IconButton
              aria-label="Close profiler"
              onClick={() => {
                setOpen(false);
              }}
            >
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
          <footer className="border-t border-border px-3 py-2 text-xs text-muted">
            Powered by{' '}
            <a
              className="text-accent underline"
              href="https://sniffy.io/"
              target="_blank"
              rel="noreferrer"
            >
              Sniffy
            </a>
          </footer>
        </Card>
      )}
      <div
        className="sniffy-widget flex items-stretch overflow-hidden rounded-lg border border-border bg-surface shadow-[var(--sniffy-shadow)]"
        data-counter-expanded={trayExpanded}
      >
        <button
          className="sniffy-brand-trigger grid size-10 place-items-center text-accent hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-focus"
          aria-label={open ? 'Close Sniffy profiler' : 'Open Sniffy profiler'}
          aria-expanded={open}
          onClick={() => setOpen((current) => !current)}
        >
          <Gauge size={19} />
        </button>
        <div
          className="sniffy-counter-tray flex items-stretch"
          data-expanded={trayExpanded}
          aria-hidden={!trayExpanded}
        >
          <button
            className="flex"
            aria-label="View captured Sniffy details"
            aria-expanded={open}
            tabIndex={trayExpanded ? 0 : -1}
            onClick={() => {
              setOpen(true);
            }}
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
        </div>
      </div>
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

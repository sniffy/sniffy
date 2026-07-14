import { useCallback, useEffect, useMemo, useState } from 'react';
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
  serverTime: number;
  stats?: RequestStats;
  error?: string;
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
  const [minimized, setMinimized] = useState(false);
  const [maximized, setMaximized] = useState(false);
  const [cleared, setCleared] = useState(false);
  const [requests, setRequests] = useState<RequestRecord[]>([]);
  const load = useCallback(
    async (label: string, url: string, sqlQueries = 0, serverTime = 0) => {
      setRequests((current) => [...current, { label, url, sqlQueries, serverTime }]);
      try {
        const stats = await client.getRequestDetails(url);
        setRequests((current) =>
          current.map((item) => (item.url === url ? { ...item, stats } : item)),
        );
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
    [client],
  );
  useEffect(() => {
    if (metadata.requestId)
      void load(
        `${metadata.requestMethod || 'GET'} ${location.pathname}${metadata.responseCode ? ` - ${metadata.responseCode}` : ''}`,
        new URL(`request/${metadata.requestId}`, metadata.baseUrl).href,
      );
    return intercepted.subscribe(
      (request) =>
        void load(request.label, request.detailsUrl, request.sqlQueries, request.timeToFirstByte),
    );
  }, [intercepted, load, metadata]);
  const totals = requests.reduce(
    (result, request) => {
      const stats = request.stats;
      result.network +=
        stats?.executedQueries?.reduce(
          (sum, query) => sum + (query.bytesDown ?? 0) + (query.bytesUp ?? 0),
          0,
        ) ?? 0;
      result.network +=
        stats?.networkConnections?.reduce(
          (sum, connection) => sum + (connection.bytesDown ?? 0) + (connection.bytesUp ?? 0),
          0,
        ) ?? 0;
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
    (time, request) => time + request.serverTime,
    cleared ? 0 : metadata.serverTime,
  );

  if (!open)
    return (
      <div className="flex items-stretch overflow-hidden rounded-lg border border-border bg-surface shadow-[var(--sniffy-shadow)]">
        <button
          className="border-r border-border px-2 text-xs font-semibold text-muted hover:bg-surface-hover"
          aria-label={minimized ? 'Expand Sniffy counters' : 'Minimize Sniffy counters'}
          onClick={() => setMinimized(!minimized)}
        >
          Sniffy
        </button>
        {!minimized && (
          <button className="flex" aria-label="Open Sniffy profiler" onClick={() => setOpen(true)}>
            <Counter
              icon={<CircleAlert size={16} />}
              label="Exceptions"
              value={String(totals.exceptions)}
              color="text-exception"
            />
            <Counter
              icon={<Network size={16} />}
              label="Network bytes"
              value={formatBytes(totals.network)}
              color="text-network"
            />
            <Counter
              icon={<Timer size={16} />}
              label="Server time"
              value={formatTime(serverTime)}
              color="text-info"
            />
            <Counter
              icon={<Database size={16} />}
              label="SQL queries"
              value={String(sqlCount)}
              color="text-sql"
            />
          </button>
        )}
      </div>
    );

  return (
    <Card
      className="sniffy-panel flex flex-col overflow-hidden shadow-[var(--sniffy-shadow)]"
      data-maximized={maximized}
    >
      <header className="flex items-center gap-2 border-b border-border bg-surface-raised px-3 py-2">
        <strong className="mr-auto">Sniffy profiler</strong>
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
            onClick={() => {
              const report = JSON.stringify(
                requests.map((request) => request.stats),
                null,
                2,
              );
              void navigator.clipboard?.writeText(report).catch(() => undefined);
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
        <IconButton aria-label="Close profiler" onClick={() => setOpen(false)}>
          <X size={16} />
        </IconButton>
      </header>
      <Tabs.Root defaultValue="queries" className="flex min-h-0 flex-1 flex-col">
        <Tabs.List
          className="flex border-b border-border bg-surface"
          aria-label="Profiler sections"
        >
          <Tabs.Tab
            className="border-b-2 border-transparent px-4 py-2 text-sm data-[selected]:border-accent data-[selected]:text-accent"
            value="queries"
          >
            Executed Queries
          </Tabs.Tab>
          <Tabs.Tab
            className="border-b-2 border-transparent px-4 py-2 text-sm data-[selected]:border-accent data-[selected]:text-accent"
            value="network"
          >
            Network Connections
          </Tabs.Tab>
          <Tabs.Tab
            className="border-b-2 border-transparent px-4 py-2 text-sm data-[selected]:border-accent data-[selected]:text-accent"
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
  );
}

function Counter({
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
    <span
      className="flex min-w-16 flex-col items-center gap-0.5 border-r border-border px-3 py-2 last:border-0"
      title={label}
    >
      <span className={color}>{icon}</span>
      <span className="text-xs font-semibold">{value}</span>
    </span>
  );
}

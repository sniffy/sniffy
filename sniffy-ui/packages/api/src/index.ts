export interface SocketConnectivity {
  host: string;
  port: string | number;
  status: number;
}

export interface DataSourceConnectivity {
  url: string;
  userName: string;
  status: number;
}

export interface ConnectionRegistry {
  persistent: boolean;
  sockets: SocketConnectivity[];
  dataSources: DataSourceConnectivity[];
}

export interface TimerStats {
  count: number;
  min: number;
  median: number;
  mean: number;
  max: number;
  p75: number;
  p95: number;
  p99: number;
}

export interface TopSql {
  sql: string;
  timer: TimerStats;
}

export interface ExecutedQuery {
  query: string;
  time: number;
  invocations?: number;
  rows?: number;
  bytesDown?: number;
  bytesUp?: number;
  stackTrace?: string;
}

export interface NetworkConnection {
  host?: string;
  port?: number;
  address?: string;
  time: number;
  bytesDown?: number;
  bytesUp?: number;
  stackTrace?: string;
}

export interface SniffyException {
  class?: string;
  className?: string;
  message?: string;
  stackTrace?: string;
}

export interface RequestStats {
  time: number;
  timeToFirstByte: number;
  executedQueries?: ExecutedQuery[];
  networkConnections?: NetworkConnection[];
  exceptions?: SniffyException[];
}

export interface SniffyClient {
  baseUrl: string;
  getRequestDetails(url: string): Promise<RequestStats>;
  getRegistry(): Promise<ConnectionRegistry>;
  setSocket(item: SocketConnectivity, status: number): Promise<void>;
  setDataSource(item: DataSourceConnectivity, status: number): Promise<void>;
  setPersistent(value: boolean): Promise<void>;
  getTopSql(): Promise<TopSql[]>;
  resetTopSql(): Promise<void>;
}

export function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`;
}

export function resolveRequestDetailsUrl(requestUrl: string, headerValue: string): string {
  if (headerValue.startsWith('./')) return `${requestUrl}${headerValue.substring(1)}`;
  return new URL(headerValue, requestUrl).href;
}

export function registryStatus(enabled: boolean, delay: number): number {
  const normalizedDelay = Math.max(0, Math.round(delay));
  return enabled ? normalizedDelay : normalizedDelay === 0 ? -1 : -normalizedDelay;
}

function encodeRegistryPart(value: string | number): string {
  return encodeURIComponent(encodeURIComponent(String(value)));
}

async function request(url: string, init?: RequestInit): Promise<Response> {
  const response = await fetch(url, {
    ...init,
    headers: {
      'Sniffy-Inject-Html-Enabled': 'false',
      ...(init?.body ? { 'Content-Type': 'application/json; charset=utf-8' } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok)
    throw new Error(`${init?.method ?? 'GET'} ${url} failed with ${response.status}`);
  return response;
}

export function createSniffyClient(baseUrl: string): SniffyClient {
  const base = normalizeBaseUrl(baseUrl);
  return {
    baseUrl: base,
    async getRequestDetails(url) {
      const body = await (await request(url)).text();
      if (!body.trim()) {
        return {
          time: 0,
          timeToFirstByte: 0,
          executedQueries: [],
          networkConnections: [],
          exceptions: [],
        };
      }
      return JSON.parse(body) as RequestStats;
    },
    async getRegistry() {
      return (
        await request(new URL('connectionregistry/', base).href)
      ).json() as Promise<ConnectionRegistry>;
    },
    async setSocket(item, status) {
      await request(
        new URL(
          `connectionregistry/socket/${encodeRegistryPart(item.host)}/${encodeRegistryPart(item.port)}`,
          base,
        ).href,
        { method: 'POST', body: String(status) },
      );
    },
    async setDataSource(item, status) {
      await request(
        new URL(
          `connectionregistry/datasource/${encodeRegistryPart(item.url)}/${encodeRegistryPart(item.userName)}`,
          base,
        ).href,
        { method: 'POST', body: String(status) },
      );
    },
    async setPersistent(value) {
      await request(new URL('connectionregistry/persistent/', base).href, {
        method: value ? 'POST' : 'DELETE',
      });
    },
    async getTopSql() {
      return (await request(new URL('topsql/', base).href)).json() as Promise<TopSql[]>;
    },
    async resetTopSql() {
      await request(new URL('topsql/', base).href, { method: 'DELETE' });
    },
  };
}

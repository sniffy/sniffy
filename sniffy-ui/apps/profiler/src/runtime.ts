import { resolveRequestDetailsUrl } from '@sniffy/api';

export interface ProfilerMetadata {
  requestId: string;
  requestMethod: string;
  responseCode: string;
  baseUrl: string;
  sqlQueries: number;
  serverTime: number;
}

export interface InterceptedRequest {
  label: string;
  detailsUrl: string;
  sqlQueries: number;
  timeToFirstByte: number;
}

const interceptorKey = Symbol.for('io.sniffy.xhr-interceptor');

export function discoverBaseUrl(script: HTMLScriptElement): string {
  return new URL('.', script.src).href;
}

export function parseMetadata(doc: Document = document): ProfilerMetadata | null {
  const header = doc.getElementById('sniffy-header') as HTMLScriptElement | null;
  if (!header?.src) return null;
  const data = doc.getElementById('sniffy') as HTMLElement | null;
  return {
    requestId: header.dataset.requestId ?? '',
    requestMethod: header.dataset.requestMethod ?? '',
    responseCode: header.dataset.responseCode ?? '',
    baseUrl: discoverBaseUrl(header),
    sqlQueries: Number.parseInt(data?.dataset.sqlQueries ?? '0', 10) || 0,
    serverTime: Number.parseInt(data?.dataset.serverTime ?? '0', 10) || 0,
  };
}

export function formatRequestLabel(
  method: string,
  requestUrl: string,
  status: number,
  currentOrigin: string = location.origin,
): string {
  const parsedRequestUrl = new URL(requestUrl);
  const labelUrl =
    parsedRequestUrl.origin === currentOrigin
      ? `${parsedRequestUrl.pathname}${parsedRequestUrl.search}${parsedRequestUrl.hash}`
      : parsedRequestUrl.href;
  return `${method} ${labelUrl} - ${status}`;
}

export function installXhrInterceptor(onRequest: (request: InterceptedRequest) => void): void {
  const prototype = XMLHttpRequest.prototype as XMLHttpRequest & Record<PropertyKey, unknown>;
  const existing = prototype[interceptorKey] as
    { listeners: Set<(request: InterceptedRequest) => void> } | undefined;
  if (existing) return;
  const listeners = new Set<(request: InterceptedRequest) => void>([onRequest]);
  const originalOpen = XMLHttpRequest.prototype.open;
  const originalSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function (
    this: XMLHttpRequest,
    method: string,
    url: string | URL,
    async: boolean = true,
    username?: string | null,
    password?: string | null,
  ) {
    (this as XMLHttpRequest & { __sniffyUrl?: string; __sniffyMethod?: string }).__sniffyUrl =
      new URL(String(url), location.href).href;
    (this as XMLHttpRequest & { __sniffyUrl?: string; __sniffyMethod?: string }).__sniffyMethod =
      method;
    return originalOpen.call(this, method, url, async, username, password);
  } as typeof XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.send = function (
    this: XMLHttpRequest,
    body?: Document | XMLHttpRequestBodyInit | null,
  ) {
    try {
      this.setRequestHeader('Sniffy-Inject-Html-Enabled', 'false');
    } catch {
      /* A provider may reject headers before send; the application request must continue. */
    }
    this.addEventListener(
      'loadend',
      () => {
        const details = this.getResponseHeader('Sniffy-Request-Details');
        if (!details) return;
        const state = this as XMLHttpRequest & { __sniffyUrl?: string; __sniffyMethod?: string };
        const requestUrl = state.__sniffyUrl || this.responseURL || location.href;
        const request: InterceptedRequest = {
          label: formatRequestLabel(state.__sniffyMethod ?? 'GET', requestUrl, this.status),
          detailsUrl: resolveRequestDetailsUrl(requestUrl, details),
          sqlQueries: Number.parseInt(this.getResponseHeader('Sniffy-Sql-Queries') ?? '0', 10) || 0,
          timeToFirstByte:
            Number.parseInt(this.getResponseHeader('Sniffy-Time-To-First-Byte') ?? '0', 10) || 0,
        };
        listeners.forEach((listener) => listener(request));
      },
      { once: true },
    );
    return originalSend.call(this, body);
  };
  prototype[interceptorKey] = { listeners };
}

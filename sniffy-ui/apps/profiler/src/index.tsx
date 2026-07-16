import { createRoot } from 'react-dom/client';
import styles from './styles.css?inline';
import { ProfilerApp } from './app';
import { installXhrInterceptor, parseMetadata, type InterceptedRequest } from './runtime';

class RequestChannel {
  private readonly listeners = new Set<(request: InterceptedRequest) => void>();
  private readonly queued: InterceptedRequest[] = [];
  publish(request: InterceptedRequest) {
    if (this.listeners.size === 0) this.queued.push(request);
    else this.listeners.forEach((listener) => listener(request));
  }
  subscribe(listener: (request: InterceptedRequest) => void) {
    this.listeners.add(listener);
    this.queued.splice(0).forEach(listener);
    return () => this.listeners.delete(listener);
  }
}

const channel = new RequestChannel();
const earlyWindow = window as Window & { io?: { sniffy?: boolean } };
earlyWindow.io ??= {};
earlyWindow.io.sniffy = true;

export function initializeProfiler(doc: Document = document): HTMLElement | null {
  const existing = doc.querySelector('sniffy-profiler');
  if (existing) return existing as HTMLElement;
  const metadata = parseMetadata(doc);
  if (!metadata) return null;
  if (!customElements.get('sniffy-profiler'))
    customElements.define('sniffy-profiler', class extends HTMLElement {});
  const host = doc.createElement('sniffy-profiler');
  host.setAttribute('data-sniffy-profiler', '');
  const shadowRoot = host.attachShadow({ mode: 'open' });
  const style = doc.createElement('style');
  style.textContent = styles;
  const rootElement = doc.createElement('div');
  rootElement.id = 'sniffy-root';
  shadowRoot.append(style, rootElement);
  doc.body.append(host);
  const globalWindow = window as Window & {
    io?: { sniffy?: boolean };
    sniffy?: Record<string, unknown>;
  };
  globalWindow.io ??= {};
  globalWindow.io.sniffy = true;
  globalWindow.sniffy = { ...(globalWindow.sniffy ?? {}), baseUrl: metadata.baseUrl };
  createRoot(rootElement).render(
    <ProfilerApp metadata={metadata} intercepted={channel} shadowRoot={shadowRoot} />,
  );
  return host;
}

installXhrInterceptor((request) => channel.publish(request));
if (document.readyState === 'loading')
  document.addEventListener('DOMContentLoaded', () => initializeProfiler(), { once: true });
else initializeProfiler();

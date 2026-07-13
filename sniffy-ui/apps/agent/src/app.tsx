import { useMemo } from 'react';
import { Activity } from 'lucide-react';
import { createSniffyClient } from '@sniffy/api';
import { Card, ConnectionRegistryPanel } from '@sniffy/ui';

export function AgentApp({ baseUrl = window.location.href }: { baseUrl?: string }) {
  const client = useMemo(() => createSniffyClient(new URL('/', baseUrl).href), [baseUrl]);
  return (
    <main className="mx-auto min-h-screen max-w-6xl px-4 py-8 sm:px-8">
      <header className="mb-8 flex items-center gap-4">
        <span className="grid size-12 place-items-center rounded-lg bg-accent text-[var(--sniffy-accent-foreground)]">
          <Activity size={27} />
        </span>
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-accent">
            Sniffy agent
          </p>
          <h1 className="text-2xl font-semibold">Connectivity control center</h1>
        </div>
      </header>
      <Card className="mb-6 p-5">
        <p className="max-w-3xl text-sm text-muted">
          Review sockets and database connections discovered by this JVM. Disable a connection or
          add latency to exercise failure handling without changing application configuration.
        </p>
      </Card>
      <ConnectionRegistryPanel client={client} title="Discovered connections" />
      <footer className="mt-8 border-t border-border py-4 text-sm text-muted">
        Powered by{' '}
        <a className="text-accent underline" href="https://sniffy.io/">
          Sniffy
        </a>
        . Changes take effect immediately.
      </footer>
    </main>
  );
}

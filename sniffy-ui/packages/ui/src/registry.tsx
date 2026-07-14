import { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import type {
  ConnectionRegistry,
  DataSourceConnectivity,
  SniffyClient,
  SocketConnectivity,
} from '@sniffy/api';
import { registryStatus } from '@sniffy/api';
import { Button, Card, NumberField, StateMessage, Switch, Table } from './primitives';

function statusDelay(status: number): number {
  return status >= 0 ? status : status === -1 ? 0 : -status;
}

function errorMessage(reason: unknown): string {
  return reason instanceof Error ? reason.message : String(reason);
}

function useQueuedMutation<T>(save: (value: T) => Promise<void>, reload: () => Promise<void>) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const saveRef = useRef(save);
  const reloadRef = useRef(reload);
  const queuedRef = useRef<T | undefined>(undefined);
  const hasQueuedRef = useRef(false);
  const savingRef = useRef(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  useEffect(() => {
    saveRef.current = save;
    reloadRef.current = reload;
  }, [reload, save]);

  const flush = useCallback(async () => {
    if (savingRef.current || !hasQueuedRef.current) return;
    savingRef.current = true;
    setPending(true);
    setError(undefined);
    while (hasQueuedRef.current) {
      const next = queuedRef.current as T;
      hasQueuedRef.current = false;
      try {
        await saveRef.current(next);
      } catch (reason) {
        hasQueuedRef.current = false;
        setError(`${errorMessage(reason)}. Server state was reloaded.`);
        await reloadRef.current();
        break;
      }
    }
    savingRef.current = false;
    setPending(false);
  }, []);

  const queue = useCallback(
    (value: T, debounce = false) => {
      queuedRef.current = value;
      hasQueuedRef.current = true;
      setPending(true);
      if (debounceTimer.current !== undefined) clearTimeout(debounceTimer.current);
      debounceTimer.current = undefined;
      if (debounce) debounceTimer.current = setTimeout(() => void flush(), 250);
      else void flush();
    },
    [flush],
  );

  useEffect(
    () => () => {
      if (debounceTimer.current !== undefined) clearTimeout(debounceTimer.current);
    },
    [],
  );

  return { queue, pending, error };
}

function ConnectivityControls({
  item,
  save,
  reload,
  label,
}: {
  item: SocketConnectivity | DataSourceConnectivity;
  save: (status: number) => Promise<void>;
  reload: () => Promise<void>;
  label: string;
}) {
  const [enabled, setEnabled] = useState(item.status >= 0);
  const [delay, setDelay] = useState(statusDelay(item.status));
  const mutation = useQueuedMutation(save, reload);
  useEffect(() => {
    setEnabled(item.status >= 0);
    setDelay(statusDelay(item.status));
  }, [item]);
  return (
    <>
      <td aria-busy={mutation.pending} data-pending={mutation.pending}>
        <Switch
          checked={enabled}
          label={`Enable ${label}`}
          onCheckedChange={(next) => {
            setEnabled(next);
            mutation.queue(registryStatus(next, delay));
          }}
        />
      </td>
      <td aria-busy={mutation.pending} data-pending={mutation.pending}>
        <NumberField
          value={delay}
          label={`${label} delay`}
          onValueChange={(next) => {
            setDelay(next);
            mutation.queue(registryStatus(enabled, next), true);
          }}
        />
        {mutation.pending && (
          <span className="ml-2 text-xs text-muted" role="status">
            Saving…
          </span>
        )}
        {mutation.error && (
          <span className="mt-1 block text-xs text-danger" role="alert">
            {mutation.error}
          </span>
        )}
      </td>
    </>
  );
}

function PersistentControl({
  value,
  client,
  reload,
}: {
  value: boolean;
  client: SniffyClient;
  reload: () => Promise<void>;
}) {
  const [checked, setChecked] = useState(value);
  const mutation = useQueuedMutation((next: boolean) => client.setPersistent(next), reload);
  useEffect(() => setChecked(value), [value]);
  return (
    <div className="text-right" aria-busy={mutation.pending} data-pending={mutation.pending}>
      <Switch
        checked={checked}
        label="Keep settings after restart"
        onCheckedChange={(next) => {
          setChecked(next);
          mutation.queue(next);
        }}
      />
      {mutation.pending && (
        <span className="mt-1 block text-xs text-muted" role="status">
          Saving…
        </span>
      )}
      {mutation.error && (
        <span className="mt-1 block text-xs text-danger" role="alert">
          {mutation.error}
        </span>
      )}
    </div>
  );
}

export function ConnectionRegistryPanel({
  client,
  title = 'Network connections',
}: {
  client: SniffyClient;
  title?: string;
}) {
  const [registry, setRegistry] = useState<ConnectionRegistry>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const reload = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const next = await client.getRegistry();
      setRegistry(next);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setLoading(false);
    }
  }, [client]);
  useEffect(() => {
    void reload();
  }, [reload]);

  return (
    <section aria-labelledby="sniffy-registry-title" className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 id="sniffy-registry-title" className="text-base font-semibold">
          {title}
        </h2>
        <Button disabled={loading} aria-busy={loading} onClick={() => void reload()}>
          <RefreshCw className={loading ? 'motion-safe:animate-spin' : ''} size={16} />
          {loading ? 'Refreshing…' : 'Refresh'}
        </Button>
      </div>
      {loading && !registry && (
        <StateMessage kind="loading">Loading connection registry…</StateMessage>
      )}
      {error && <StateMessage kind="error">{error}</StateMessage>}
      {registry && (
        <>
          {registry.dataSources?.length > 0 && (
            <Card className="overflow-x-auto">
              <Table label="Database connections">
                <thead>
                  <tr>
                    <th>URL</th>
                    <th>Username</th>
                    <th>Status</th>
                    <th>Delay (ms)</th>
                  </tr>
                </thead>
                <tbody>
                  {registry.dataSources.map((item) => (
                    <tr key={`${item.url}:${item.userName}`}>
                      <td className="font-mono text-xs">{item.url}</td>
                      <td>{item.userName}</td>
                      <ConnectivityControls
                        item={item}
                        label={`${item.url} database`}
                        save={(status) => client.setDataSource(item, status)}
                        reload={reload}
                      />
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card>
          )}
          <Card className="overflow-x-auto">
            <Table label="Socket connections">
              <thead>
                <tr>
                  <th>Host</th>
                  <th>Port</th>
                  <th>Status</th>
                  <th>Delay (ms)</th>
                </tr>
              </thead>
              <tbody>
                {registry.sockets?.map((item) => (
                  <tr key={`${item.host}:${item.port}`}>
                    <td>{item.host}</td>
                    <td>{item.port}</td>
                    <ConnectivityControls
                      item={item}
                      label={`${item.host}:${item.port} socket`}
                      save={(status) => client.setSocket(item, status)}
                      reload={reload}
                    />
                  </tr>
                ))}
              </tbody>
            </Table>
            {registry.sockets?.length === 0 && (
              <StateMessage kind="empty">No sockets discovered.</StateMessage>
            )}
          </Card>
          <Card className="flex items-center justify-between gap-4 p-4">
            <div>
              <h3 className="font-medium">Keep settings after restart</h3>
              <p className="text-sm text-muted">Persist fault-tolerance settings on the server.</p>
            </div>
            <PersistentControl value={registry.persistent} client={client} reload={reload} />
          </Card>
        </>
      )}
    </section>
  );
}

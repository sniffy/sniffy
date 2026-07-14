import { useCallback, useEffect, useState } from 'react';
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

function ConnectivityControls({
  item,
  save,
  label,
}: {
  item: SocketConnectivity | DataSourceConnectivity;
  save: (status: number) => Promise<void>;
  label: string;
}) {
  const [enabled, setEnabled] = useState(item.status >= 0);
  const [delay, setDelay] = useState(statusDelay(item.status));
  useEffect(() => {
    setEnabled(item.status >= 0);
    setDelay(statusDelay(item.status));
  }, [item]);
  const persist = async (nextEnabled = enabled, nextDelay = delay) =>
    save(registryStatus(nextEnabled, nextDelay));
  return (
    <>
      <td>
        <Switch
          checked={enabled}
          label={`Enable ${label}`}
          onCheckedChange={(next) => {
            setEnabled(next);
            void persist(next, delay);
          }}
        />
      </td>
      <td>
        <NumberField
          value={delay}
          label={`${label} delay`}
          onValueChange={(next) => {
            setDelay(next);
            void persist(enabled, next);
          }}
        />
      </td>
    </>
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
      setRegistry(await client.getRegistry());
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
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
        <Button onClick={() => void reload()}>
          <RefreshCw size={16} /> Refresh
        </Button>
      </div>
      {loading && <StateMessage kind="loading">Loading connection registry…</StateMessage>}
      {error && <StateMessage kind="error">{error}</StateMessage>}
      {!loading && !error && registry && (
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
            <Switch
              checked={registry.persistent}
              label="Keep settings after restart"
              onCheckedChange={(next) => {
                setRegistry({ ...registry, persistent: next });
                void client.setPersistent(next);
              }}
            />
          </Card>
        </>
      )}
    </section>
  );
}

import { createSniffyClient, registryStatus, resolveRequestDetailsUrl } from './index';

describe('Sniffy API', () => {
  it('resolves relative request details against the actual request URL', () => {
    expect(
      resolveRequestDetailsUrl('https://host/app/path/subpath/ajax.json', '../../request/42'),
    ).toBe('https://host/app/request/42');
    expect(resolveRequestDetailsUrl('https://host/app/notrailingslash', './request/42')).toBe(
      'https://host/app/request/42',
    );
  });

  it('preserves the legacy status encoding', () => {
    expect(registryStatus(true, 50)).toBe(50);
    expect(registryStatus(false, 0)).toBe(-1);
    expect(registryStatus(false, 50)).toBe(-50);
  });

  it('preserves registry mutations and the injection guard header', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('', { status: 201 }));
    const client = createSniffyClient('https://host/sniffy/4.0.0/');
    await client.setDataSource({ url: 'jdbc:h2:mem:/a', userName: 'SA', status: 0 }, -1);
    await client.setSocket({ host: 'example.test', port: 443, status: 0 }, 25);
    await client.setPersistent(true);
    await client.setPersistent(false);
    await client.resetTopSql();

    expect(fetchMock.mock.calls[0]?.[0]).toContain('jdbc%253Ah2%253Amem%253A%252Fa');
    expect(fetchMock.mock.calls.map((call) => (call[1] as RequestInit).method)).toEqual([
      'POST',
      'POST',
      'POST',
      'DELETE',
      'DELETE',
    ]);
    expect((fetchMock.mock.calls[0]?.[1] as RequestInit).headers).toMatchObject({
      'Sniffy-Inject-Html-Enabled': 'false',
    });
    fetchMock.mockRestore();
  });
});

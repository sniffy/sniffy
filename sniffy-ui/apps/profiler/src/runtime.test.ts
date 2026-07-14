import { discoverBaseUrl, installXhrInterceptor, parseMetadata } from './runtime';

describe('profiler runtime contract', () => {
  it('discovers the backend from the resolved classic script URL', () => {
    const script = document.createElement('script');
    script.src = '/proxy/app/sniffy/4.0.0/sniffy.min.js';
    expect(discoverBaseUrl(script)).toBe(`${location.origin}/proxy/app/sniffy/4.0.0/`);
  });

  it('tolerates missing footer metadata and headers', () => {
    const header = document.createElement('script');
    header.src = '/sniffy/4.0.0/sniffy.min.js';
    const fixtureDocument = {
      getElementById: (id: string) => (id === 'sniffy-header' ? header : null),
    } as unknown as Document;
    expect(parseMetadata(fixtureDocument)).toMatchObject({
      requestId: '',
      sqlQueries: 0,
      serverTime: 0,
    });
  });

  it('installs once, preserves handlers and sets the injection guard header', () => {
    const listenerA = vi.fn();
    const listenerB = vi.fn();
    const originalSend = vi
      .spyOn(XMLHttpRequest.prototype, 'send')
      .mockImplementation(() => undefined);
    const setRequestHeader = vi.spyOn(XMLHttpRequest.prototype, 'setRequestHeader');
    installXhrInterceptor(listenerA);
    const wrappedSend = XMLHttpRequest.prototype.send;
    installXhrInterceptor(listenerB);
    expect(XMLHttpRequest.prototype.send).toBe(wrappedSend);

    const xhr = new XMLHttpRequest();
    const applicationHandler = vi.fn();
    xhr.onreadystatechange = applicationHandler;
    xhr.open('GET', '/mock/ajax.json');
    xhr.send();

    expect(xhr.onreadystatechange).toBe(applicationHandler);
    expect(setRequestHeader).toHaveBeenCalledWith('Sniffy-Inject-Html-Enabled', 'false');
    expect(originalSend).toHaveBeenCalledOnce();
  });
});

import { resolveSiteDeployment } from './deployment-config';

describe('site deployment configuration', () => {
  it.each([
    [{ SNIFFY_SITE_URL: 'not-a-url' }, 'absolute HTTP(S) origin'],
    [{ SNIFFY_SITE_URL: 'ftp://sniffy.github.io' }, 'absolute HTTP(S) origin'],
    [{ SNIFFY_SITE_URL: 'https://sniffy.github.io/sniffy/' }, 'absolute HTTP(S) origin'],
    [{ SNIFFY_SITE_URL: 'https://sniffy.github.io?preview=1' }, 'absolute HTTP(S) origin'],
    [{ SNIFFY_SITE_URL: 'https://sniffy.github.io#preview' }, 'absolute HTTP(S) origin'],
    [{ SNIFFY_SITE_BASE_URL: 'sniffy/' }, 'absolute path'],
    [{ SNIFFY_SITE_BASE_URL: '/sniffy' }, 'absolute path'],
    [{ SNIFFY_SITE_BASE_URL: '/sniffy//docs/' }, 'absolute path'],
    [{ SNIFFY_SITE_BASE_URL: '/sniffy/../docs/' }, 'absolute path'],
    [{ SNIFFY_SITE_BASE_URL: '/sniffy/./docs/' }, 'absolute path'],
  ])('rejects an invalid environment override %#', (environment, message) => {
    expect(() => resolveSiteDeployment(environment)).toThrow(message);
  });
});

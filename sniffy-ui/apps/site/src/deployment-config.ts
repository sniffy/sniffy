const productionSiteUrl = 'https://sniffy.io';
const productionBaseUrl = '/';

export interface SiteDeploymentConfiguration {
  baseUrl: string;
  url: string;
}

function resolveOrigin(value: string): string {
  let parsed: URL;

  try {
    parsed = new URL(value);
  } catch {
    throw new Error(`SNIFFY_SITE_URL must be an absolute HTTP(S) origin; received "${value}".`);
  }

  if (
    !['http:', 'https:'].includes(parsed.protocol) ||
    parsed.pathname !== '/' ||
    parsed.search !== '' ||
    parsed.hash !== ''
  ) {
    throw new Error(`SNIFFY_SITE_URL must be an absolute HTTP(S) origin; received "${value}".`);
  }

  return parsed.origin;
}

function resolveBaseUrl(value: string): string {
  if (
    !value.startsWith('/') ||
    !value.endsWith('/') ||
    value.includes('//') ||
    value.split('/').some((segment) => segment === '.' || segment === '..')
  ) {
    throw new Error(
      `SNIFFY_SITE_BASE_URL must be an absolute path with one leading and trailing slash; received "${value}".`,
    );
  }

  return value;
}

export function resolveSiteDeployment(
  environment: Record<string, string | undefined> = process.env,
): SiteDeploymentConfiguration {
  const url = environment.SNIFFY_SITE_URL?.trim() || productionSiteUrl;
  const baseUrl = environment.SNIFFY_SITE_BASE_URL?.trim() || productionBaseUrl;

  return {
    url: resolveOrigin(url),
    baseUrl: resolveBaseUrl(baseUrl),
  };
}

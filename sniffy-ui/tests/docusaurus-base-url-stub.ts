function withBaseUrl(url: string) {
  const baseUrl = process.env.SNIFFY_TEST_BASE_URL ?? '/';
  return url.startsWith(baseUrl) ? url : `${baseUrl}${url.replace(/^\/+/, '')}`;
}

export function useBaseUrlUtils() {
  return { withBaseUrl };
}

export default function useBaseUrl(url: string) {
  return withBaseUrl(url);
}

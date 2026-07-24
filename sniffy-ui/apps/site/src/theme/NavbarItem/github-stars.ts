const API_URL = 'https://api.github.com/repos/sniffy/sniffy';
const CACHE_KEY = 'sniffy.github-stars.v1';
const CACHE_TTL_MS = 60 * 60 * 1_000;

interface CachedCount {
  count: number;
  expiresAt: number;
}

let memoryCache: CachedCount | undefined;

function isStarCount(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0;
}

function readSessionCache(): CachedCount | undefined {
  try {
    const raw = window.sessionStorage.getItem(CACHE_KEY);
    if (!raw) return undefined;
    const parsed = JSON.parse(raw) as Partial<CachedCount>;
    if (isStarCount(parsed.count) && typeof parsed.expiresAt === 'number') {
      return { count: parsed.count, expiresAt: parsed.expiresAt };
    }
  } catch {
    // Storage can be disabled or contain stale data; the repository link still works.
  }
  return undefined;
}

function writeCache(value: CachedCount): void {
  memoryCache = value;
  try {
    window.sessionStorage.setItem(CACHE_KEY, JSON.stringify(value));
  } catch {
    // A memory cache is sufficient when session storage is unavailable.
  }
}

export async function getGitHubStarCount(signal: AbortSignal): Promise<number | null> {
  const now = Date.now();
  const cached =
    memoryCache?.expiresAt && memoryCache.expiresAt > now ? memoryCache : readSessionCache();
  if (cached && cached.expiresAt > now) {
    memoryCache = cached;
    return cached.count;
  }

  try {
    const response = await fetch(API_URL, {
      headers: { Accept: 'application/vnd.github+json' },
      signal,
    });
    if (!response.ok) return null;
    const body = (await response.json()) as { stargazers_count?: unknown };
    if (!isStarCount(body.stargazers_count)) return null;
    writeCache({ count: body.stargazers_count, expiresAt: now + CACHE_TTL_MS });
    return body.stargazers_count;
  } catch {
    return null;
  }
}

export function resetGitHubStarsCacheForTests(): void {
  memoryCache = undefined;
  try {
    window.sessionStorage.removeItem(CACHE_KEY);
  } catch {
    // Test cleanup must also tolerate unavailable storage.
  }
}

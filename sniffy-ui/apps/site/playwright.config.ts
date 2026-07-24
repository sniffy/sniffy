import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  outputDir: '../../test-results/site',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  snapshotPathTemplate: '{testDir}/visual-baselines/{arg}{ext}',
  use: {
    baseURL: 'http://127.0.0.1:3100',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: [
    {
      command: 'npm run serve -- --host 127.0.0.1 --port 3100 --no-open',
      url: 'http://127.0.0.1:3100/',
      reuseExistingServer: false,
      timeout: 30_000,
    },
    {
      command: 'npm run start -- --host 127.0.0.1 --port 3200 --no-open',
      url: 'http://127.0.0.1:3200/',
      reuseExistingServer: false,
      timeout: 30_000,
    },
  ],
  projects: [
    {
      name: 'desktop-chromium',
      testMatch: '**/routes.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'sql-profiling-chromium',
      testMatch: '**/sql-profiling.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-chromium',
      testMatch: '**/mobile.spec.ts',
      use: { ...devices['Pixel 7'] },
    },
    {
      name: 'static-compatibility-chromium',
      testMatch: '**/legacy-docs.spec.ts',
      use: { ...devices['Desktop Chrome'], baseURL: 'http://127.0.0.1:3100' },
    },
    {
      name: 'dev-compatibility-chromium',
      testMatch: [
        '**/legacy-docs.spec.ts',
        '**/dev-search.spec.ts',
        '**/dev-github-stars.spec.ts',
        '**/dev-use-case.spec.ts',
        '**/dev-sql-profiling.spec.ts',
      ],
      use: { ...devices['Desktop Chrome'], baseURL: 'http://127.0.0.1:3200' },
    },
  ],
});

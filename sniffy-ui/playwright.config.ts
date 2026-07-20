import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  outputDir: './test-results',
  snapshotPathTemplate: '{testDir}/visual-baselines/{arg}{ext}',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  expect: {
    toHaveScreenshot: { maxDiffPixelRatio: 0.01 },
  },
  use: {
    baseURL: 'http://127.0.0.1:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: [
    {
      command: 'npm run dev',
      url: 'http://127.0.0.1:3000/mock/mock.html',
      reuseExistingServer: false,
      timeout: 30_000,
    },
    {
      command: 'npm run storybook -- --ci --host 127.0.0.1',
      url: 'http://127.0.0.1:6006/iframe.html',
      reuseExistingServer: false,
      timeout: 30_000,
    },
  ],
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] }, grepInvert: /@visual/ },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }, grepInvert: /@visual/ },
    { name: 'webkit', use: { ...devices['Desktop Safari'] }, grepInvert: /@visual/ },
    { name: 'chromium-visual', use: { ...devices['Desktop Chrome'] }, grep: /@visual/ },
  ],
});

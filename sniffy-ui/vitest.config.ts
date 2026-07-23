import react from '@vitejs/plugin-react';
import { playwright } from '@vitest/browser-playwright';
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';
import { defineConfig } from 'vitest/config';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const workspace = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@docusaurus/router': resolve(
        workspace,
        'node_modules/@docusaurus/core/lib/client/exports/router.js',
      ),
      '@docusaurus/useDocusaurusContext': resolve(
        workspace,
        'node_modules/@docusaurus/core/lib/client/exports/useDocusaurusContext.js',
      ),
    },
  },
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: [
        'apps/*/src/**/*.{ts,tsx}',
        'packages/*/src/**/*.{ts,tsx}',
        'scripts/**/*.{ts,tsx}',
      ],
      exclude: [
        '**/*.d.ts',
        '**/*.stories.{ts,tsx}',
        '**/*.test.{ts,tsx}',
        '**/__fixtures__/**',
        'packages/fixtures/**',
        'tests/**',
      ],
    },
    projects: [
      {
        extends: true,
        test: {
          name: 'unit',
          globals: true,
          environment: 'happy-dom',
          setupFiles: ['./tests/setup.ts'],
          include: [
            'apps/**/*.test.{ts,tsx}',
            'packages/**/*.test.{ts,tsx}',
            'scripts/**/*.test.ts',
          ],
        },
      },
      {
        extends: true,
        plugins: [
          storybookTest({
            configDir: resolve(workspace, '.storybook'),
          }),
        ],
        test: {
          name: 'storybook',
          browser: {
            enabled: true,
            provider: playwright({}),
            headless: true,
            instances: [{ browser: 'chromium' }],
          },
        },
      },
    ],
  },
});

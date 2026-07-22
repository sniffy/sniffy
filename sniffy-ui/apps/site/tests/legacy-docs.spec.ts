import { expect, test } from '@playwright/test';

for (const entryPoint of ['/docs/latest/', '/docs/latest/index.html']) {
  for (const [anchor, destination] of [
    ['_install', '/docs/installation/'],
    ['_standalone_setup', '/docs/installation/'],
    ['_configuration', '/docs/configuration/'],
    ['_integration_with_junit', '/docs/testing/junit/'],
    ['_emulating_network_issues', '/docs/network/fault-emulation/'],
    ['_capture_traffic', '/docs/network/traffic-capture/'],
    ['_ssltls_traffic_decryption', '/docs/network/traffic-capture/#ssltls-traffic-decryption'],
  ] as const) {
    test(`${entryPoint}#${anchor} routes to its migrated destination`, async ({ page }) => {
      await page.goto(`${entryPoint}#${anchor}`);

      await expect(page).toHaveURL(new RegExp(`${destination.replaceAll('/', '\\/')}$`));
    });
  }

  for (const suffix of ['', '#unknown-anchor', '#malformed%anchor']) {
    test(`${entryPoint}${suffix} falls back to current documentation`, async ({ page }) => {
      await page.goto(`${entryPoint}${suffix}`);

      await expect(page).toHaveURL(/\/docs\/$/);
    });
  }
}

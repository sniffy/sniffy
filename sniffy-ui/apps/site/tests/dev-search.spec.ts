import { expect, test } from '@playwright/test';

test('the real development server builds current-doc search without a production build', async ({
  page,
}) => {
  await page.goto('/docs/');
  await page.getByRole('button', { name: /Search docs/ }).click();

  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  const input = dialog.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });
  await input.fill('sql');

  const result = dialog.getByRole('option', { name: /Using the Sniffy API/ }).first();
  await expect(result).toBeVisible();
  await expect(result).toHaveAttribute('href', /\/docs\/testing\/api\/(?:#.*)?$/);
  await result.click();
  await expect(page).toHaveURL(/\/docs\/testing\/api\/(?:#.*)?$/);
  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Using the Sniffy API' })).toBeVisible();
});

test('the real development server closes search before every result navigation', async ({
  page,
}) => {
  await page.goto('/');

  const trigger = page.getByRole('button', { name: /Search docs/ });
  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  const input = dialog.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });

  for (let iteration = 0; iteration < 2; iteration += 1) {
    await trigger.click();
    await input.fill('sql');
    await dialog.locator('a[href="/docs/testing/api/"]').first().click();
    await expect(page).toHaveURL(/\/docs\/testing\/api\/$/);
    await expect(dialog).not.toBeVisible();

    await trigger.click();
    await input.fill('SSL TLS traffic decryption');
    const sectionResult = dialog
      .locator('a[href="/docs/network/traffic-capture/#ssltls-traffic-decryption"]')
      .first();
    await expect(sectionResult).toBeVisible();
    await sectionResult.focus();
    await sectionResult.press('Enter');
    await expect(page).toHaveURL(/\/docs\/network\/traffic-capture\/#ssltls-traffic-decryption$/);
    await expect(dialog).not.toBeVisible();

    await trigger.click();
    await dialog.evaluate((element) => {
      element.dataset.lifecycleMarker = 'same-route';
    });
    await input.fill('traffic capture');
    await dialog.locator('a[href="/docs/network/traffic-capture/"]').first().click();
    await expect(page).toHaveURL(/\/docs\/network\/traffic-capture\/$/);
    await expect(dialog).not.toBeVisible();
    await expect(page.locator('dialog')).toHaveAttribute('data-lifecycle-marker', 'same-route');
  }
});

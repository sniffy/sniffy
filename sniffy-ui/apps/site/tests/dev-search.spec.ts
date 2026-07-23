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
  await expect(page.getByRole('heading', { level: 1, name: 'Using the Sniffy API' })).toBeVisible();
});

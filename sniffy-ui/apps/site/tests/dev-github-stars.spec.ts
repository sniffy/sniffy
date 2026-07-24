import { expect, test } from '@playwright/test';

const githubApi = 'https://api.github.com/repos/sniffy/sniffy';

test('the real development server renders a deterministic GitHub star count', async ({ page }) => {
  const pageErrors: Error[] = [];
  const consoleErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error));
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  await page.route(githubApi, (route) => route.fulfill({ json: { stargazers_count: 12_345 } }));

  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  const link = page.getByRole('navigation', { name: 'Main' }).getByRole('link', {
    name: 'Sniffy GitHub repository, 12,345 stars',
  });
  await expect(link).toHaveAttribute('href', 'https://github.com/sniffy/sniffy');
  await expect(link.getByText('12K')).toBeVisible();
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
});

test('the real development server keeps GitHub navigation usable when a star count is unavailable', async ({
  page,
}) => {
  const pageErrors: Error[] = [];
  const consoleErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error));
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  await page.route(githubApi, (route) => route.fulfill({ json: {} }));

  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  const link = page
    .getByRole('navigation', { name: 'Main' })
    .getByRole('link', { name: 'Sniffy GitHub repository' });
  await expect(link).toHaveAttribute('href', 'https://github.com/sniffy/sniffy');
  await expect(link.getByTestId('github-star-count')).toBeEmpty();
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
});

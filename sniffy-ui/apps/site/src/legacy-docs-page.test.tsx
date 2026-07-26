import { render, screen, waitFor } from '@testing-library/react';

import LegacyDocsPage from './legacy-docs-page';

describe('legacy documentation compatibility page', () => {
  const routes = {
    _configuration: '/docs/configuration/',
  };

  beforeEach(() => {
    process.env.SNIFFY_TEST_BASE_URL = '/sniffy/';
  });

  afterEach(() => {
    delete process.env.SNIFFY_TEST_BASE_URL;
    window.location.hash = '';
    vi.restoreAllMocks();
  });

  it.each([
    ['#_configuration', '/sniffy/docs/configuration/'],
    ['#unknown-anchor', '/sniffy/docs/'],
    ['#malformed%anchor', '/sniffy/docs/'],
  ])('qualifies the %s redirect and fallback link with the base URL', async (hash, destination) => {
    window.location.hash = hash;
    const replace = vi.spyOn(window.location, 'replace').mockImplementation(() => undefined);

    render(<LegacyDocsPage routes={routes} />);

    expect(
      screen.getByRole('link', { name: 'Continue to the current Sniffy documentation.' }),
    ).toHaveAttribute('href', '/sniffy/docs/');
    await waitFor(() => expect(replace).toHaveBeenCalledWith(destination));
  });
});

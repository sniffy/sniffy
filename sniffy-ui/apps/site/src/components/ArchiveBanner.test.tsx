import { render, screen } from '@testing-library/react';

import ArchiveBanner from './ArchiveBanner';

describe('archived documentation banner', () => {
  it('identifies the frozen source and routes readers to maintained guidance', () => {
    render(<ArchiveBanner />);

    const notice = screen.getByRole('complementary', {
      name: 'Archived documentation notice',
    });
    expect(notice).toHaveTextContent(
      'Archived Sniffy 3.1 documentation. This unsupported version is preserved from v3.1.14',
    );
    expect(screen.getByRole('link', { name: 'current documentation' })).toHaveAttribute(
      'href',
      '/docs/',
    );
    expect(screen.getByRole('link', { name: 'migration guidance' })).toHaveAttribute(
      'href',
      '/docs/migration/to-4/',
    );
  });
});

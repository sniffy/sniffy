import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { defineUseCase, type UseCasePageContent } from './components/use-case/use-case';
import DatabaseQueryTestingPage from './pages/use-cases/database-query-testing';
import { createDatabaseQueryTestingUseCase } from './use-cases/database-query-testing';

vi.mock('@docusaurus/useDocusaurusContext', () => ({
  default: () => ({
    siteConfig: {
      customFields: {
        productVersion: '4.0.0-SNAPSHOT',
      },
    },
  }),
}));

const site = resolve(import.meta.dirname, '..');
const repository = resolve(site, '../../..');
const content = createDatabaseQueryTestingUseCase('4.0.0-SNAPSHOT');

describe('use-case content contract', () => {
  it('fails deterministically for incomplete metadata and invalid routes', () => {
    const invalid = (overrides: Partial<UseCasePageContent['metadata']>) =>
      defineUseCase({
        ...content,
        metadata: { ...content.metadata, ...overrides },
      } as UseCasePageContent);

    expect(() => invalid({ title: '' })).toThrow('field "title" must not be empty');
    expect(() => invalid({ route: '/database-query-testing/' })).toThrow(
      'must live below /use-cases/',
    );
    expect(() => invalid({ route: '/use-cases/database-query-testing' as `/${string}/` })).toThrow(
      'canonical trailing-slash',
    );
    expect(() => invalid({ relatedDocs: [] as never })).toThrow('must contain at least one link');
    expect(() =>
      invalid({
        relatedDocs: [
          {
            description: 'Wrong surface',
            href: '/use-cases/other/' as `/${string}/`,
            label: 'Wrong route',
          },
        ],
      }),
    ).toThrow('must link to current documentation');
    expect(() => createDatabaseQueryTestingUseCase(String(undefined))).toThrow(
      'requires the repository product version',
    );
  });

  it('renders complete metadata, shared sections, code, product evidence, and calls to action', () => {
    render(<DatabaseQueryTestingPage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Make database behavior part of the test contract.',
      }),
    ).toBeVisible();
    expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
      'href',
      'https://sniffy.io/use-cases/database-query-testing/',
    );
    expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
      'content',
      'Database query testing with Sniffy',
    );
    expect(document.querySelector('meta[name="twitter:description"]')).toHaveAttribute(
      'content',
      content.metadata.social.description,
    );

    const capabilities = screen.getByRole('region', {
      name: 'Test database behavior, not just return values.',
    });
    for (const heading of [
      'Bound query counts',
      'Filter by statement type',
      'Verify affected rows',
      'Scope work by thread',
    ]) {
      expect(within(capabilities).getByRole('heading', { level: 3, name: heading })).toBeVisible();
    }

    expect(
      screen.getByRole('img', { name: /Sniffy profiler showing executed SQL statements/ }),
    ).toHaveAttribute('src', '/img/home/sniffy-profiler.png');
    expect(screen.getByText(/sniffy-junit-jupiter/)).toBeVisible();
    expect(screen.getByText(/<version>4\.0\.0-SNAPSHOT<\/version>/)).toBeVisible();
    expect(screen.getByText(/SqlQueries\.atMostOneQuery/)).toBeVisible();
    expect(screen.getAllByRole('link', { name: 'Install Sniffy for tests' })).toHaveLength(2);
  });

  it('keeps every related-documentation link on an existing current-docs route', () => {
    for (const relatedDoc of content.metadata.relatedDocs) {
      const relative = relatedDoc.href.replace(/^\/docs\//, '').replace(/\/$/, '');
      const documentPath =
        relative.length === 0
          ? resolve(site, 'docs/index.mdx')
          : resolve(site, `docs/${relative}.mdx`);
      const indexPath = resolve(site, `docs/${relative}/index.mdx`);

      expect(
        [documentPath, indexPath].some((candidate) => {
          try {
            return readFileSync(candidate, 'utf8').length > 0;
          } catch {
            return false;
          }
        }),
      ).toBe(true);
    }
  });

  it('uses the repository version and a source-backed direct API example', () => {
    const coreApiExample = readFileSync(
      resolve(repository, 'sniffy-core/src/test/java/io/sniffy/CoreApiExampleTest.java'),
      'utf8',
    );
    const directApiExample = content.codeExamples.find(
      (example) => example.label === 'Verify the direct API result',
    );
    const normalize = (value: string) => value.replaceAll(/\s+/g, ' ').trim();

    expect(directApiExample?.source).toMatchObject({
      path: 'sniffy-core/src/test/java/io/sniffy/CoreApiExampleTest.java',
      region: 'testFunctionalApi',
    });
    expect(normalize(coreApiExample)).toContain(normalize(directApiExample?.code ?? 'missing'));
    expect(content.codeExamples[0].code).toContain('<version>4.0.0-SNAPSHOT</version>');
  });

  it('separates page content from shared layout and preserves real-media provenance', () => {
    const route = readFileSync(
      resolve(site, 'src/pages/use-cases/database-query-testing/index.tsx'),
      'utf8',
    );
    const layout = readFileSync(
      resolve(site, 'src/components/use-case/use-case-layout.tsx'),
      'utf8',
    );
    const pageContent = readFileSync(
      resolve(site, 'src/use-cases/database-query-testing.ts'),
      'utf8',
    );
    const provenance = readFileSync(resolve(site, 'static/img/home/README.md'), 'utf8');

    expect(route).toContain('createDatabaseQueryTestingUseCase');
    expect(route).toContain('<UseCaseLayout content={content} />');
    expect(route).not.toContain('Correct output can hide');
    expect(layout).not.toContain('database query testing');
    expect(pageContent).not.toContain('<section');
    expect(provenance).toContain('byte-for-byte copy');
    expect(content.productResult.provenance).toContain('deterministic browser fixture');
  });
});

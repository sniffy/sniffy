import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import SqlProfilingPage from './pages/use-cases/sql-profiling';
import { createSqlProfilingUseCase } from './use-cases/sql-profiling';

vi.mock('@docusaurus/useDocusaurusContext', () => ({
  default: () => ({ siteConfig: { customFields: { productVersion: '4.0.0-SNAPSHOT' } } }),
}));

const site = resolve(import.meta.dirname, '..');
const repository = resolve(site, '../../..');
const content = createSqlProfilingUseCase('4.0.0-SNAPSHOT');

describe('SQL profiling use-case page', () => {
  it('renders focused metadata, browser evidence, assertions, and current documentation links', () => {
    render(<SqlProfilingPage />);
    expect(
      screen.getByRole('heading', { level: 1, name: 'See the database work behind each request.' }),
    ).toBeVisible();
    expect(content.metadata.title).toBe('SQL profiling and N+1 diagnosis with Sniffy');
    expect(content.metadata.description).toContain('Profile JDBC queries in the browser');
    expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
      'href',
      'https://sniffy.io/use-cases/sql-profiling/',
    );
    expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
      'content',
      content.metadata.social.title,
    );
    expect(document.querySelector('meta[property="og:url"]')).toHaveAttribute(
      'content',
      'https://sniffy.io/use-cases/sql-profiling/',
    );
    expect(document.querySelector('meta[name="twitter:description"]')).toHaveAttribute(
      'content',
      content.metadata.social.description,
    );
    expect(screen.getByText(/evidence, not an automatic diagnosis/)).toBeVisible();
    expect(screen.getByText(/Browser profiling supports diagnosis/)).toBeVisible();
    expect(screen.getByText(/sniffy-web supports Jakarta Servlet 5\.0 and newer/)).toBeVisible();
    expect(screen.getByText(/sniffy-web-javax supports Javax Servlet 3\.1 and 4\.0/)).toBeVisible();
    expect(screen.getByText(/Install only the matching variant, not both/)).toBeVisible();
    expect(
      screen.getByRole('region', {
        name: 'Jakarta Servlet 5.0+: add sniffy-web code example',
      }),
    ).toHaveTextContent('<artifactId>sniffy-web</artifactId>');
    expect(
      screen.getByRole('region', {
        name: 'Javax Servlet 3.1/4.0: add sniffy-web-javax code example',
      }),
    ).toHaveTextContent('<artifactId>sniffy-web-javax</artifactId>');
    const docs = screen.getByRole('navigation', { name: 'Related documentation' });
    for (const link of content.metadata.relatedDocs)
      expect(within(docs).getByRole('link', { name: new RegExp(link.label) })).toHaveAttribute(
        'href',
        link.href,
      );
  });

  it('uses the repository version, source-backed Java, and documented real media', () => {
    const source = readFileSync(
      resolve(repository, 'sniffy-core/src/test/java/io/sniffy/CoreApiExampleTest.java'),
      'utf8',
    );
    const java = content.codeExamples[2];
    const normalize = (value: string) => value.replaceAll(/\s+/g, ' ').trim();
    expect(content.codeExamples[0].code).toContain('<version>4.0.0-SNAPSHOT</version>');
    expect(content.codeExamples[1].code).toContain('<version>4.0.0-SNAPSHOT</version>');
    expect(normalize(source)).toContain(normalize(java.code));
    expect(java.source?.region).toBe('testFunctionalApi');
    expect(readFileSync(resolve(site, 'static/img/home/README.md'), 'utf8')).toContain(
      'byte-for-byte copy',
    );
    expect(content.productResult.provenance).toContain(
      'no interface elements or results were fabricated',
    );
    expect(() => createSqlProfilingUseCase(String(undefined))).toThrow(
      'requires the repository product version',
    );
  });

  it('keeps all SQL-specific content out of the shared route and layout', () => {
    const route = readFileSync(
      resolve(site, 'src/pages/use-cases/sql-profiling/index.tsx'),
      'utf8',
    );
    const layout = readFileSync(
      resolve(site, 'src/components/use-case/use-case-layout.tsx'),
      'utf8',
    );
    expect(route).toContain('createSqlProfilingUseCase');
    expect(route).not.toMatch(/N\+1|repeated database work/);
    expect(layout).not.toMatch(/N\+1|SQL profiling|repeated-query/);
  });
});

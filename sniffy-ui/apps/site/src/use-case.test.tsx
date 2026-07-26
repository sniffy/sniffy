import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { defineUseCase, type UseCasePageContent } from './components/use-case/use-case';
import { UseCaseLayout } from './components/use-case/use-case-layout';
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
const syntheticNetworkContent = defineUseCase({
  metadata: {
    ...content.metadata,
    accent: 'network',
    description: 'Exercise deterministic network failures without page-specific layout code.',
    relatedDocs: [
      {
        description: 'Configure deterministic connection failures and latency.',
        href: '/docs/network/fault-emulation/',
        label: 'Emulate network faults',
      },
    ],
    route: '/use-cases/synthetic-network/',
    social: {
      ...content.metadata.social,
      description: 'Exercise deterministic network failures.',
      title: 'Synthetic network use case',
    },
    title: 'Synthetic network use case',
  },
  eyebrow: 'Network fault testing',
  hero: {
    body: 'Exercise how a client responds when a remote service is unavailable.',
    proofPoints: ['Failure behavior stays explicit'],
    title: 'Turn remote failures into repeatable checks.',
  },
  problem: {
    body: ['Healthy-path checks cannot prove recovery behavior.'],
  },
  solution: {
    body: ['Apply a bounded fault and observe the client response.'],
  },
  capabilities: [
    {
      description: 'Keep the exercised failure mode explicit.',
      title: 'Bound the fault',
    },
  ],
  codeExamples: [
    {
      code: 'ConnectionsRegistry.INSTANCE.setSocketAddressStatus(host, port, -1);',
      label: 'Reject one endpoint',
      language: 'Java',
    },
  ],
  productResult: {
    alt: 'Sniffy request evidence for a synthetic network scenario',
    caption: 'The product result remains a reusable semantic slot.',
    height: 540,
    highlights: ['Failure evidence stays attached to the exercised request'],
    provenance: 'Synthetic contract fixture reuses an existing repository image.',
    src: '/img/home/sniffy-profiler.png',
    width: 760,
  },
  sectionHeadings: {
    capabilities: {
      eyebrow: 'Controlled conditions',
      title: 'Describe the failure boundary.',
    },
    codeExamples: {
      eyebrow: 'Executable setup',
      title: 'Make the fault repeatable.',
    },
    cta: {
      eyebrow: 'From failure to confidence',
      title: 'Exercise the recovery path.',
    },
    problem: {
      eyebrow: 'The resilience gap',
      title: 'Healthy responses hide recovery defects.',
    },
    productResult: {
      eyebrow: 'Captured outcome',
      title: 'Inspect the request around the fault.',
    },
    relatedDocumentation: {
      eyebrow: 'Continue exploring',
      title: 'Follow the current fault-emulation guide.',
    },
    solution: {
      eyebrow: 'The controlled approach',
      title: 'Introduce one bounded failure mode.',
    },
  },
  cta: {
    body: 'Start with the recovery behavior that matters most.',
    primary: {
      href: '/docs/network/fault-emulation/',
      label: 'Configure a network fault',
    },
    secondary: {
      href: '/docs/installation/',
      label: 'Install Sniffy',
    },
  },
});

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
    expect(() =>
      defineUseCase({
        ...content,
        sectionHeadings: {
          ...content.sectionHeadings,
          capabilities: {
            ...content.sectionHeadings.capabilities,
            title: '',
          },
        },
      }),
    ).toThrow('field "sectionHeadings.capabilities.title" must not be empty');
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
    expect(
      readFileSync(resolve(site, 'src/components/use-case/use-case-layout.tsx'), 'utf8'),
    ).toContain('useBaseUrl(result.src)');
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

  it('renders arbitrary non-database section framing without topic copy in the layout', () => {
    render(<UseCaseLayout content={syntheticNetworkContent} />);

    for (const heading of Object.values(syntheticNetworkContent.sectionHeadings)) {
      expect(screen.getByText(heading.eyebrow)).toBeVisible();
      expect(screen.getByRole('heading', { level: 2, name: heading.title })).toBeVisible();
    }
    expect(screen.getByRole('heading', { name: 'Bound the fault' })).toBeVisible();
    expect(
      screen.getByText('ConnectionsRegistry.INSTANCE.setSocketAddressStatus(host, port, -1);'),
    ).toBeVisible();
    expect(document.body).not.toHaveTextContent(
      /database behavior|query assertions|JDBC|statements behind the request/i,
    );
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
    for (const pageSpecificCopy of [
      'The testing gap',
      'The Sniffy approach',
      'Assertions with context',
      'Test database behavior, not just return values.',
      'Repository-backed examples',
      'Make the expectation executable.',
      'Visible evidence',
      'See the statements behind the request.',
      'Go deeper',
      'Use the current documentation as the source of truth.',
      'From observation to regression test',
    ]) {
      expect(layout).not.toContain(pageSpecificCopy);
      expect(pageContent).toContain(pageSpecificCopy);
    }
    expect(pageContent).not.toContain('<section');
    expect(provenance).toContain('byte-for-byte copy');
    expect(content.productResult.provenance).toContain('deterministic browser fixture');
  });
});

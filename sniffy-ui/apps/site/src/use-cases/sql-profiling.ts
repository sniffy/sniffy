import { defineUseCase } from '../components/use-case/use-case';

export function createSqlProfilingUseCase(productVersion: string) {
  const version = productVersion.trim();
  if (version.length === 0 || version === 'undefined') {
    throw new Error('The SQL profiling page requires the repository product version.');
  }

  return defineUseCase({
    metadata: {
      accent: 'sql',
      description:
        'Profile JDBC queries in the browser with Sniffy, inspect request-level SQL timing and counts, and turn repeated-query evidence into focused tests.',
      relatedDocs: [
        {
          description: 'Choose the Sniffy integration for your Java application.',
          href: '/docs/installation/',
          label: 'Install Sniffy',
        },
        {
          description: 'Expose the browser profiler through the Sniffy servlet filter.',
          href: '/docs/setup/filter/',
          label: 'Set up the web filter',
        },
        {
          description: 'Wrap an existing application datasource for JDBC observation.',
          href: '/docs/setup/datasource/',
          label: 'Configure a datasource',
        },
        {
          description: 'Convert an observed query-count boundary into an automated check.',
          href: '/docs/testing/api/',
          label: 'Assert SQL behavior',
        },
      ],
      route: '/use-cases/sql-profiling/',
      social: {
        description:
          'Inspect request-level JDBC statements, counts, and timing, then test the query boundary that matters.',
        image: '/img/brand/sniffy-social.svg',
        title: 'SQL profiling and N+1 diagnosis with Sniffy',
      },
      title: 'SQL profiling and N+1 diagnosis with Sniffy',
    },
    eyebrow: 'SQL profiling',
    hero: {
      body: 'Sniffy makes the JDBC work behind a Java web request visible in the browser: executed SQL, query counts, timing, and affected-row evidence. Repeated statements or an unexpectedly high count can reveal an N+1-shaped access pattern for you to investigate—they are evidence, not an automatic diagnosis.',
      proofPoints: [
        'Inspect SQL beside the request that caused it',
        'Use counts and repeated statements to investigate N+1 patterns',
        'Turn a known query boundary into a test assertion',
      ],
      title: 'See the database work behind each request.',
    },
    problem: {
      body: [
        'A page can look fast in isolation while its request performs one initial query followed by many similar queries—often one for every item loaded. Application output alone does not show that pattern.',
        'No query-count tool can infer every N+1 defect from a number alone. The useful signal is concrete: the statements Sniffy observed, how often they ran, and how much time the recorded JDBC work took for that request.',
      ],
    },
    solution: {
      body: [
        'With the web filter and JDBC instrumentation configured, the Sniffy browser widget associates observed database activity with the served request. Open its SQL details to inspect statements and timing without adding a separate live-demo backend to this page.',
        'Choose the web artifact that matches the application: sniffy-web supports Jakarta Servlet 5.0 and newer, while sniffy-web-javax supports Javax Servlet 3.1 and 4.0. Install only the matching variant, not both.',
        'If the evidence shows a regression-prone boundary, add a Spy or framework expectation in a test. Browser profiling supports diagnosis; explicit SqlQueries expectations provide the automated pass-or-fail contract.',
      ],
    },
    capabilities: [
      {
        title: 'Profile request-level SQL',
        description:
          'Inspect the JDBC statements observed while a web request is served instead of reconstructing the request from global logs.',
      },
      {
        title: 'Review counts and timing',
        description:
          'Use recorded execution counts and elapsed JDBC time to find unexpectedly chatty or costly request paths.',
      },
      {
        title: 'Investigate repeated-query evidence',
        description:
          'Compare repeated statements with the application path to determine whether they represent an N+1 access pattern; Sniffy exposes the evidence rather than declaring every repetition a defect.',
      },
      {
        title: 'Protect a measured boundary',
        description:
          'After establishing the intended behavior, verify an exact, maximum, or ranged query count with the direct API or a supported test integration.',
      },
    ],
    codeExamples: [
      {
        code: `<dependency>\n  <groupId>io.sniffy</groupId>\n  <artifactId>sniffy-web</artifactId>\n  <version>${version}</version>\n</dependency>`,
        label: 'Jakarta Servlet 5.0+: add sniffy-web',
        language: 'Maven',
        source: {
          label: 'Current installation documentation',
          path: 'sniffy-ui/apps/site/docs/installation/index.mdx',
        },
      },
      {
        code: `<dependency>\n  <groupId>io.sniffy</groupId>\n  <artifactId>sniffy-web-javax</artifactId>\n  <version>${version}</version>\n</dependency>`,
        label: 'Javax Servlet 3.1/4.0: add sniffy-web-javax',
        language: 'Maven',
        source: {
          label: 'Current installation documentation',
          path: 'sniffy-ui/apps/site/docs/installation/index.mdx',
        },
      },
      {
        code: `Sniffy.execute(\n    () -> connection.createStatement().execute("SELECT 1 FROM DUAL")\n).verify(SqlQueries.atMostOneQuery());`,
        label: 'Protect the observed query boundary',
        language: 'Java',
        source: {
          label: 'Core API example test',
          path: 'sniffy-core/src/test/java/io/sniffy/CoreApiExampleTest.java',
          region: 'testFunctionalApi',
        },
      },
    ],
    productResult: {
      alt: 'Real Sniffy browser profiler showing SQL statements and timing recorded for a Java web request',
      caption:
        'The browser profiler exposes the request evidence used to investigate a repeated-query pattern.',
      height: 540,
      highlights: [
        'SQL statements recorded for the served request',
        'Query activity and timing visible in the profiler result',
        'Observed evidence that can inform a separate query-count assertion',
      ],
      provenance:
        'Byte-for-byte copy of the real current profiler captured by the repository’s deterministic /mock/mock.html browser fixture; no interface elements or results were fabricated.',
      src: '/img/home/sniffy-profiler.png',
      width: 760,
    },
    sectionHeadings: {
      problem: {
        eyebrow: 'The hidden pattern',
        title: 'Correct pages can conceal repeated database work.',
      },
      solution: {
        eyebrow: 'Browser evidence, test contract',
        title: 'Observe first; assert the boundary you understand.',
      },
      capabilities: {
        eyebrow: 'Focused SQL evidence',
        title: 'Move from an unexpected count to a precise investigation.',
      },
      codeExamples: {
        eyebrow: 'Repository-backed setup',
        title: 'Profile the application, then protect the result.',
      },
      productResult: {
        eyebrow: 'Real profiler result',
        title: 'Inspect what Sniffy actually observed.',
      },
      relatedDocumentation: {
        eyebrow: 'Configuration and testing',
        title: 'Follow the current guides for each integration step.',
      },
      cta: { eyebrow: 'Start with one request', title: 'Make its database behavior visible.' },
    },
    cta: {
      body: 'Configure Sniffy on a representative request, inspect its observed statements and counts, and add an assertion only after you have established the boundary the application should keep.',
      primary: { href: '/docs/installation/', label: 'Install Sniffy' },
      secondary: { href: '/docs/setup/filter/', label: 'Configure browser profiling' },
    },
  });
}

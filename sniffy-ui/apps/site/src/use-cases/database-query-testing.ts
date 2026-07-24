import { defineUseCase } from '../components/use-case/use-case';

export function createDatabaseQueryTestingUseCase(productVersion: string) {
  const version = productVersion.trim();
  if (version.length === 0 || version === 'undefined') {
    throw new Error('The database query testing page requires the repository product version.');
  }

  return defineUseCase({
    metadata: {
      accent: 'sql',
      description:
        'Test the SQL behavior of Java code with Sniffy query-count and affected-row assertions, backed by request-level evidence.',
      relatedDocs: [
        {
          description: 'Choose the Sniffy artifact that matches your test framework and runtime.',
          href: '/docs/installation/',
          label: 'Install Sniffy',
        },
        {
          description: 'Create a Spy and verify query counts, rows, statement types, and threads.',
          href: '/docs/testing/api/',
          label: 'Use the direct API',
        },
        {
          description: 'Apply SQL expectations through JUnit 4 or the JUnit Jupiter extension.',
          href: '/docs/testing/junit/',
          label: 'Integrate with JUnit',
        },
        {
          description: 'Wrap an application datasource without changing production query code.',
          href: '/docs/setup/datasource/',
          label: 'Configure a datasource',
        },
      ],
      route: '/use-cases/database-query-testing/',
      social: {
        description:
          'Turn hidden JDBC activity into explicit query-count and affected-row expectations in Java tests.',
        image: '/img/brand/sniffy-social.svg',
        title: 'Database query testing with Sniffy',
      },
      title: 'Database query testing with Sniffy',
    },
    eyebrow: 'Database query testing',
    hero: {
      body: 'Sniffy records the JDBC work performed while a test runs, then lets the test verify query counts, statement types, affected rows, and thread scope—without replacing your database or rewriting application queries.',
      proofPoints: [
        'Fail when a code path executes more SQL than intended',
        'Assert affected rows as well as statement counts',
        'Separate current-thread work from background activity',
      ],
      title: 'Make database behavior part of the test contract.',
    },
    problem: {
      body: [
        'A test can return the right object while quietly issuing too many statements, updating the wrong number of rows, or performing unexpected work on another thread. Functional assertions alone do not expose that cost or behavior.',
        'Log inspection helps after a failure, but it does not create a repeatable boundary. The useful question is not only “did this call succeed?”—it is also “what database work did this call perform?”',
      ],
    },
    solution: {
      body: [
        'Start a Sniffy Spy around the code under test or register the matching test-framework integration. Sniffy observes JDBC calls during that scope and evaluates explicit expectations when the scope completes.',
        'The application keeps using ordinary JDBC, a datasource, or its existing framework. Your test gains a focused assertion over the recorded behavior, and a failed expectation reports the database activity that crossed the boundary.',
      ],
    },
    capabilities: [
      {
        description:
          'Use exact, minimum, maximum, or ranged expectations instead of treating every database path as identical.',
        title: 'Bound query counts',
      },
      {
        description:
          'Limit expectations to SELECT, INSERT, UPDATE, DELETE, MERGE, or other statements when the distinction matters.',
        title: 'Filter by statement type',
      },
      {
        description:
          'Assert how many rows were affected so a passing return value cannot conceal a broader write.',
        title: 'Verify affected rows',
      },
      {
        description:
          'Choose current, other, or any threads to make asynchronous database work explicit in the contract.',
        title: 'Scope work by thread',
      },
    ],
    codeExamples: [
      {
        code: `<dependency>
  <groupId>io.sniffy</groupId>
  <artifactId>sniffy-junit-jupiter</artifactId>
  <version>${version}</version>
  <scope>test</scope>
</dependency>`,
        label: 'Add the JUnit Jupiter integration',
        language: 'Maven',
        source: {
          label: 'Current JUnit documentation',
          path: 'sniffy-ui/apps/site/docs/testing/junit.mdx',
        },
      },
      {
        code: `Sniffy.execute(
    () -> connection.createStatement().execute("SELECT 1 FROM DUAL")
).verify(SqlQueries.atMostOneQuery());`,
        label: 'Verify the direct API result',
        language: 'Java',
        source: {
          label: 'Core API example test',
          path: 'sniffy-core/src/test/java/io/sniffy/CoreApiExampleTest.java',
          region: 'testFunctionalApi',
        },
      },
    ],
    productResult: {
      alt: 'Sniffy profiler showing executed SQL statements and request timing for a Java web request',
      caption:
        'The same observed SQL is useful while debugging and while defining a test expectation.',
      height: 540,
      highlights: [
        'Executed statements grouped with the request that caused them',
        'Query timing and affected-row evidence alongside application activity',
        'A concrete trail to turn an observed regression into a durable assertion',
      ],
      provenance:
        'Real Sniffy profiler captured from the repository’s deterministic browser fixture; no interface elements were invented for this page.',
      src: '/img/home/sniffy-profiler.png',
      width: 760,
    },
    sectionHeadings: {
      capabilities: {
        eyebrow: 'Assertions with context',
        title: 'Test database behavior, not just return values.',
      },
      codeExamples: {
        eyebrow: 'Repository-backed examples',
        title: 'Make the expectation executable.',
      },
      cta: {
        eyebrow: 'From observation to regression test',
        title: 'Give the next database regression a precise failure.',
      },
      problem: {
        eyebrow: 'The testing gap',
        title: 'Correct output can hide incorrect database work.',
      },
      productResult: {
        eyebrow: 'Visible evidence',
        title: 'See the statements behind the request.',
      },
      relatedDocumentation: {
        eyebrow: 'Go deeper',
        title: 'Use the current documentation as the source of truth.',
      },
      solution: {
        eyebrow: 'The Sniffy approach',
        title: 'Observe the real JDBC path and assert its shape.',
      },
    },
    cta: {
      body: 'Start with one behavior that matters: the maximum number of statements a path may execute, the rows a write may affect, or the thread on which database work must stay.',
      primary: {
        href: '/docs/installation/',
        label: 'Install Sniffy for tests',
      },
      secondary: {
        href: '/docs/testing/api/',
        label: 'Read the query assertion API',
      },
    },
  });
}

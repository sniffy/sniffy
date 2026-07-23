import Link from '@docusaurus/Link';
import Head from '@docusaurus/Head';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

const pageTitle = 'Java observability and resilience testing';
const pageDescription =
  'Sniffy makes SQL queries and network activity visible in Java applications and testable in automated suites.';

const useCases = [
  {
    accent: 'sql',
    description:
      'Record executed statements, inspect request-level SQL, and turn query counts into test assertions that catch regressions such as N+1 queries.',
    href: '/docs/testing/api/',
    label: 'SQL profiling and assertions',
    linkLabel: 'Explore query assertions',
    number: '01',
  },
  {
    accent: 'network',
    description:
      'Discover outgoing connections, block selected endpoints, or introduce deterministic TCP delays to exercise failure paths before production does.',
    href: '/docs/network/fault-emulation/',
    label: 'Network fault testing',
    linkLabel: 'Test network failures',
    number: '02',
  },
  {
    accent: 'traffic',
    description:
      'Capture bytes sent through classic sockets and monitored TCP NIO channels, with optional TLS decryption when deeper protocol inspection is required.',
    href: '/docs/network/traffic-capture/',
    label: 'Traffic and TLS capture',
    linkLabel: 'Inspect network traffic',
    number: '03',
  },
] as const;

export default function Home(): React.JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const productVersion = String(siteConfig.customFields?.productVersion);
  const mavenSnippet = `<dependency>
  <groupId>io.sniffy</groupId>
  <artifactId>sniffy-spring</artifactId>
  <version>${productVersion}</version>
</dependency>`;
  const gradleSnippet = `dependencies {
  implementation("io.sniffy:sniffy-spring:${productVersion}")
}`;

  return (
    <Layout title={pageTitle} description={pageDescription}>
      <Head>
        <link rel="canonical" href="https://sniffy.io/" />
        <meta property="og:title" content={`Sniffy — ${pageTitle}`} />
        <meta property="og:description" content={pageDescription} />
        <meta property="og:url" content="https://sniffy.io/" />
        <meta property="og:image" content="https://sniffy.io/img/brand/sniffy-social.svg" />
        <meta name="twitter:title" content={`Sniffy — ${pageTitle}`} />
        <meta name="twitter:description" content={pageDescription} />
        <meta name="twitter:image" content="https://sniffy.io/img/brand/sniffy-social.svg" />
      </Head>
      <main className="sniffy-home">
        <header className="sniffy-home__hero">
          <div className="sniffy-home__hero-copy">
            <p className="sniffy-home__eyebrow">Java observability and resilience testing</p>
            <h1>Make invisible I/O observable—and testable.</h1>
            <p className="sniffy-home__lead">
              Sniffy shows the SQL queries and network activity behind each Java request, then gives
              your tests the same visibility to assert behavior, simulate failures, and capture
              traffic.
            </p>
            <div className="sniffy-home__actions">
              <Link className="button button--primary button--lg" to="/docs/installation/">
                Start with Sniffy
              </Link>
              <Link className="sniffy-home__text-link" to="/docs/">
                Explore the documentation <span aria-hidden="true">→</span>
              </Link>
            </div>
            <ul className="sniffy-home__hero-facts" aria-label="Sniffy at a glance">
              <li>Browser profiler</li>
              <li>Test assertions</li>
              <li>Fault injection</li>
              <li>Open source</li>
            </ul>
          </div>

          <figure className="sniffy-home__product">
            <div className="sniffy-home__product-bar" aria-hidden="true">
              <span />
              <span />
              <span />
              <strong>request /mock/mock.html</strong>
            </div>
            <img
              src="/img/home/sniffy-profiler.png"
              width="760"
              height="540"
              alt="Sniffy profiler showing executed SQL, network traffic, an exception, and request timing for a Java web request"
              fetchPriority="high"
            />
            <figcaption>
              Real Sniffy profiler, captured deterministically from the repository browser fixture.
            </figcaption>
          </figure>
        </header>

        <section className="sniffy-home__section" aria-labelledby="use-cases-title">
          <div className="sniffy-home__section-heading">
            <p className="sniffy-home__eyebrow">What Sniffy helps you prove</p>
            <h2 id="use-cases-title">See behavior. Set expectations. Break assumptions safely.</h2>
            <p>
              Use the same I/O model while developing, debugging, and testing instead of stitching
              together unrelated profilers and fault tools.
            </p>
          </div>
          <div className="sniffy-home__use-cases">
            {useCases.map((useCase) => (
              <article
                className="sniffy-home__use-case"
                data-accent={useCase.accent}
                key={useCase.label}
              >
                <span className="sniffy-home__use-case-number" aria-hidden="true">
                  {useCase.number}
                </span>
                <h3>{useCase.label}</h3>
                <p>{useCase.description}</p>
                <Link to={useCase.href}>
                  {useCase.linkLabel} <span aria-hidden="true">→</span>
                </Link>
              </article>
            ))}
          </div>
        </section>

        <section
          className="sniffy-home__section sniffy-home__integrations"
          aria-labelledby="integrations-title"
        >
          <div className="sniffy-home__section-heading">
            <p className="sniffy-home__eyebrow">Fits the Java stack you already have</p>
            <h2 id="integrations-title">From a request in the browser to an assertion in CI.</h2>
            <p>
              Add Sniffy at the application edge, around a test, or as a Java agent. Pick the
              integration that matches your runtime and Servlet namespace.
            </p>
          </div>
          <div className="sniffy-home__integration-grid">
            <article>
              <h3>Application visibility</h3>
              <p>Spring Boot, Jakarta or Javax Servlet filters, and standalone Java-agent mode.</p>
              <Link to="/docs/installation/">Choose an application integration</Link>
            </article>
            <article>
              <h3>Testing integrations</h3>
              <p>JUnit, TestNG, Spring Test, Spock, Kotest, or the direct Sniffy API.</p>
              <Link to="/docs/testing/junit/">Browse testing integrations</Link>
            </article>
            <article>
              <h3>I/O coverage</h3>
              <p>JDBC, classic sockets, monitored TCP NIO channels, and opt-in TLS decryption.</p>
              <Link to="/docs/configuration/nio-monitoring/">Review NIO monitoring</Link>
            </article>
          </div>
        </section>

        <section
          className="sniffy-home__section sniffy-home__install"
          aria-labelledby="install-title"
        >
          <div className="sniffy-home__install-copy">
            <p className="sniffy-home__eyebrow">Jakarta Spring Boot 4 · Java 17+</p>
            <h2 id="install-title">Bring request-level evidence into your next run.</h2>
            <p>
              The Maven and Gradle snippets below use <code>sniffy-spring</code> for Jakarta Spring
              Boot 4 applications on Java 17 or newer. Their version comes from Sniffy’s root build,
              so the homepage stays aligned with the current repository line.
            </p>
            <p>
              Using Java 8 or Spring Boot 2.7? Choose{' '}
              <Link to="/docs/installation/#spring-boot-integration">
                <code>sniffy-spring-javax</code>
              </Link>{' '}
              instead.
            </p>
            <Link className="sniffy-home__text-link" to="/docs/installation/">
              Read the installation guide <span aria-hidden="true">→</span>
            </Link>
          </div>
          <div className="sniffy-home__code-grid">
            <div className="sniffy-home__code-card">
              <span>Maven</span>
              <pre>
                <code>{mavenSnippet}</code>
              </pre>
            </div>
            <div className="sniffy-home__code-card">
              <span>Gradle</span>
              <pre>
                <code>{gradleSnippet}</code>
              </pre>
            </div>
          </div>
        </section>

        <section className="sniffy-home__cta" aria-labelledby="cta-title">
          <p className="sniffy-home__eyebrow">Turn hidden I/O into evidence</p>
          <h2 id="cta-title">Start with the request you cannot explain.</h2>
          <p>
            Add Sniffy, reproduce the behavior, and follow the SQL and network trail from the
            browser into a durable test.
          </p>
          <div className="sniffy-home__actions">
            <Link className="button button--primary button--lg" to="/docs/installation/">
              Install Sniffy
            </Link>
            <Link className="sniffy-home__text-link" to="https://github.com/sniffy/sniffy">
              View the source on GitHub <span aria-hidden="true">↗</span>
            </Link>
          </div>
        </section>
      </main>
    </Layout>
  );
}

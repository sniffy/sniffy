/* eslint-disable jsx-a11y/no-noninteractive-tabindex -- Horizontal code regions must be keyboard-scrollable. */
import Link from '@docusaurus/Link';
import Head from '@docusaurus/Head';
import Layout from '@theme/Layout';

import type {
  UseCaseCodeExample as UseCaseCodeExampleData,
  UseCasePageContent,
  UseCaseRelatedDoc,
} from './use-case';

const siteUrl = 'https://sniffy.io';

export function UseCaseCodeExample({
  example,
}: {
  readonly example: UseCaseCodeExampleData;
}): React.JSX.Element {
  return (
    <article className="sniffy-use-case__code-card">
      <div className="sniffy-use-case__code-heading">
        <h3>{example.label}</h3>
        <span>{example.language}</span>
      </div>
      <div
        className="sniffy-use-case__code-scroll"
        role="region"
        tabIndex={0}
        aria-label={`${example.label} code example`}
      >
        <pre>
          <code>{example.code}</code>
        </pre>
      </div>
      {example.source ? (
        <p className="sniffy-use-case__source">
          Repository-backed example: <code>{example.source.path}</code>
          {example.source.region ? (
            <>
              {' '}
              · region <code>{example.source.region}</code>
            </>
          ) : null}
        </p>
      ) : null}
    </article>
  );
}
/* eslint-enable jsx-a11y/no-noninteractive-tabindex */

export function UseCaseProductResult({
  result,
}: {
  readonly result: UseCasePageContent['productResult'];
}): React.JSX.Element {
  return (
    <figure className="sniffy-use-case__product">
      <div className="sniffy-use-case__product-frame">
        <div className="sniffy-use-case__product-bar" aria-hidden="true">
          <span />
          <span />
          <span />
          <strong>Sniffy request evidence</strong>
        </div>
        <img src={result.src} width={result.width} height={result.height} alt={result.alt} />
      </div>
      <figcaption>
        <strong>{result.caption}</strong>
        <span>{result.provenance}</span>
      </figcaption>
    </figure>
  );
}

export function UseCaseCapabilityList({
  capabilities,
}: {
  readonly capabilities: UseCasePageContent['capabilities'];
}): React.JSX.Element {
  return (
    <div className="sniffy-use-case__capabilities">
      {capabilities.map((capability, index) => (
        <article key={capability.title}>
          <span aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
          <h3>{capability.title}</h3>
          <p>{capability.description}</p>
        </article>
      ))}
    </div>
  );
}

export function UseCaseRelatedDocumentation({
  relatedDocs,
}: {
  readonly relatedDocs: readonly UseCaseRelatedDoc[];
}): React.JSX.Element {
  return (
    <nav className="sniffy-use-case__related" aria-label="Related documentation">
      {relatedDocs.map((relatedDoc) => (
        <Link key={relatedDoc.href} to={relatedDoc.href}>
          <strong>{relatedDoc.label}</strong>
          <span>{relatedDoc.description}</span>
          <span aria-hidden="true">→</span>
        </Link>
      ))}
    </nav>
  );
}

export function UseCaseLayout({
  content,
}: {
  readonly content: UseCasePageContent;
}): React.JSX.Element {
  const { metadata } = content;
  const canonicalUrl = `${siteUrl}${metadata.route}`;

  return (
    <Layout title={metadata.title} description={metadata.description}>
      <Head>
        <link rel="canonical" href={canonicalUrl} />
        <meta property="og:type" content="website" />
        <meta property="og:title" content={metadata.social.title} />
        <meta property="og:description" content={metadata.social.description} />
        <meta property="og:url" content={canonicalUrl} />
        <meta property="og:image" content={`${siteUrl}${metadata.social.image}`} />
        <meta name="twitter:title" content={metadata.social.title} />
        <meta name="twitter:description" content={metadata.social.description} />
        <meta name="twitter:image" content={`${siteUrl}${metadata.social.image}`} />
      </Head>
      <main className="sniffy-use-case" data-accent={metadata.accent}>
        <header className="sniffy-use-case__hero">
          <div>
            <p className="sniffy-use-case__breadcrumb">
              <Link to="/">Sniffy</Link>
              <span aria-hidden="true">/</span>
              Use cases
            </p>
            <p className="sniffy-use-case__eyebrow">{content.eyebrow}</p>
            <h1>{content.hero.title}</h1>
            <p className="sniffy-use-case__lead">{content.hero.body}</p>
            <div className="sniffy-use-case__actions">
              <Link className="button button--primary button--lg" to={content.cta.primary.href}>
                {content.cta.primary.label}
              </Link>
              <Link className="sniffy-use-case__text-link" to={content.cta.secondary.href}>
                {content.cta.secondary.label} <span aria-hidden="true">→</span>
              </Link>
            </div>
          </div>
          <ul className="sniffy-use-case__proof" aria-label="What this use case proves">
            {content.hero.proofPoints.map((proofPoint) => (
              <li key={proofPoint}>{proofPoint}</li>
            ))}
          </ul>
        </header>

        <section className="sniffy-use-case__split" aria-labelledby="problem-title">
          <div>
            <p className="sniffy-use-case__eyebrow">The testing gap</p>
            <h2 id="problem-title">{content.problem.title}</h2>
          </div>
          <div>
            {content.problem.body.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </div>
        </section>

        <section className="sniffy-use-case__split" aria-labelledby="solution-title">
          <div>
            <p className="sniffy-use-case__eyebrow">The Sniffy approach</p>
            <h2 id="solution-title">{content.solution.title}</h2>
          </div>
          <div>
            {content.solution.body.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </div>
        </section>

        <section className="sniffy-use-case__section" aria-labelledby="capabilities-title">
          <div className="sniffy-use-case__section-heading">
            <p className="sniffy-use-case__eyebrow">Assertions with context</p>
            <h2 id="capabilities-title">Test database behavior, not just return values.</h2>
          </div>
          <UseCaseCapabilityList capabilities={content.capabilities} />
        </section>

        <section className="sniffy-use-case__section" aria-labelledby="examples-title">
          <div className="sniffy-use-case__section-heading">
            <p className="sniffy-use-case__eyebrow">Repository-backed examples</p>
            <h2 id="examples-title">Make the expectation executable.</h2>
          </div>
          <div className="sniffy-use-case__code-grid">
            {content.codeExamples.map((example) => (
              <UseCaseCodeExample example={example} key={example.label} />
            ))}
          </div>
        </section>

        <section className="sniffy-use-case__section" aria-labelledby="result-title">
          <div className="sniffy-use-case__section-heading">
            <p className="sniffy-use-case__eyebrow">Visible evidence</p>
            <h2 id="result-title">See the statements behind the request.</h2>
          </div>
          <div className="sniffy-use-case__result-grid">
            <UseCaseProductResult result={content.productResult} />
            <ul aria-label="Product result highlights">
              {content.productResult.highlights.map((highlight) => (
                <li key={highlight}>{highlight}</li>
              ))}
            </ul>
          </div>
        </section>

        <section className="sniffy-use-case__section" aria-labelledby="related-title">
          <div className="sniffy-use-case__section-heading">
            <p className="sniffy-use-case__eyebrow">Go deeper</p>
            <h2 id="related-title">Use the current documentation as the source of truth.</h2>
          </div>
          <UseCaseRelatedDocumentation relatedDocs={metadata.relatedDocs} />
        </section>

        <section className="sniffy-use-case__cta" aria-labelledby="use-case-cta-title">
          <p className="sniffy-use-case__eyebrow">From observation to regression test</p>
          <h2 id="use-case-cta-title">{content.cta.title}</h2>
          <p>{content.cta.body}</p>
          <div className="sniffy-use-case__actions">
            <Link className="button button--primary button--lg" to={content.cta.primary.href}>
              {content.cta.primary.label}
            </Link>
            <Link className="sniffy-use-case__text-link" to={content.cta.secondary.href}>
              {content.cta.secondary.label} <span aria-hidden="true">→</span>
            </Link>
          </div>
        </section>
      </main>
    </Layout>
  );
}

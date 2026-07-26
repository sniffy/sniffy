import Head from '@docusaurus/Head';
import { useBaseUrlUtils } from '@docusaurus/useBaseUrl';
import { useEffect } from 'react';

import { currentDocsPath } from './legacy-docs-routes';

interface Props {
  readonly routes: Record<string, string>;
}

export default function LegacyDocsPage({ routes }: Props) {
  const { withBaseUrl } = useBaseUrlUtils();

  useEffect(() => {
    let anchor = '';
    try {
      anchor = decodeURIComponent(window.location.hash.slice(1));
    } catch {
      // Malformed percent encoding deliberately uses the safe fallback.
    }
    const destination = Object.prototype.hasOwnProperty.call(routes, anchor)
      ? routes[anchor]
      : currentDocsPath;
    window.location.replace(withBaseUrl(destination));
  }, [routes, withBaseUrl]);

  return (
    <>
      <Head>
        <title>Sniffy documentation</title>
        <link rel="canonical" href={`https://sniffy.io${currentDocsPath}`} />
      </Head>
      <main>
        <h1>Sniffy documentation has moved</h1>
        <p>
          <a href={withBaseUrl(currentDocsPath)}>Continue to the current Sniffy documentation.</a>
        </p>
      </main>
    </>
  );
}

import Head from '@docusaurus/Head';
import { useEffect } from 'react';

import { currentDocsPath } from './legacy-docs-routes';

interface Props {
  readonly routes: Record<string, string>;
}

export default function LegacyDocsPage({ routes }: Props) {
  useEffect(() => {
    let anchor = '';
    try {
      anchor = decodeURIComponent(window.location.hash.slice(1));
    } catch {
      // Malformed percent encoding deliberately uses the safe fallback.
    }
    window.location.replace(
      Object.prototype.hasOwnProperty.call(routes, anchor) ? routes[anchor] : currentDocsPath,
    );
  }, [routes]);

  return (
    <>
      <Head>
        <title>Sniffy documentation</title>
        <link rel="canonical" href={`https://sniffy.io${currentDocsPath}`} />
      </Head>
      <main>
        <h1>Sniffy documentation has moved</h1>
        <p>
          <a href={currentDocsPath}>Continue to the current Sniffy documentation.</a>
        </p>
      </main>
    </>
  );
}

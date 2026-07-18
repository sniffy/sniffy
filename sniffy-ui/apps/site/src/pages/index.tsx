import type { ReactNode } from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';

export default function Home(): ReactNode {
  return (
    <Layout title="Sniffy" description="Minimal Sniffy website foundation">
      <main>
        <section aria-labelledby="site-home-title">
          <p>Website foundation</p>
          <h1 id="site-home-title">Sniffy</h1>
          <p>
            A minimal Docusaurus application shell for future Sniffy documentation and site work.
          </p>
          <Link to="/docs/">Open docs</Link>
        </section>
      </main>
    </Layout>
  );
}

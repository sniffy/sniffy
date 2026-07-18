import type { ReactNode } from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';

import styles from './index.module.css';

export default function Home(): ReactNode {
  return (
    <Layout title="Sniffy" description="Minimal Sniffy website foundation">
      <main className={styles.main}>
        <section className={styles.hero} aria-labelledby="site-home-title">
          <p className={styles.eyebrow}>Website foundation</p>
          <h1 id="site-home-title">Sniffy</h1>
          <p className={styles.summary}>
            A minimal Docusaurus application shell for future Sniffy documentation and site work.
          </p>
          <Link className="button button--primary" to="/docs/">
            Open docs
          </Link>
        </section>
      </main>
    </Layout>
  );
}

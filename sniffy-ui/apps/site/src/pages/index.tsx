import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';

export default function Home(): React.JSX.Element {
  return (
    <Layout title="Website foundation" description="The Sniffy website application foundation.">
      <main className="sniffy-home">
        <div className="sniffy-home__content">
          <h1>Sniffy</h1>
          <p>This minimal route establishes the website application.</p>
          <Link to="/docs/">Open the documentation scaffold</Link>
        </div>
      </main>
    </Layout>
  );
}

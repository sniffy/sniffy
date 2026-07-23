import Link from '@docusaurus/Link';
import Heading from '@theme/Heading';
import type { Props } from '@theme/NotFound/Content';

export default function NotFoundContent({ className }: Props): React.JSX.Element {
  return (
    <main className={`sniffy-not-found ${className ?? ''}`}>
      <div className="sniffy-not-found__content">
        <p className="sniffy-not-found__eyebrow">404 · No trace found</p>
        <Heading as="h1">This trail went cold.</Heading>
        <p>
          The address does not match a published Sniffy page. Continue with the current
          documentation or return to the site entry point.
        </p>
        <nav className="sniffy-not-found__links" aria-label="Page recovery">
          <Link to="/docs/">Browse documentation</Link>
          <Link to="/">Return home</Link>
        </nav>
      </div>
    </main>
  );
}

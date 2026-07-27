import Link from '@docusaurus/Link';

export default function ArchiveBanner() {
  return (
    <aside className="sniffy-archive-banner" aria-label="Archived documentation notice">
      <strong>Archived Sniffy 3.1 documentation.</strong> This unsupported version is preserved from{' '}
      <code>v3.1.14</code> and is no longer maintained. Read the{' '}
      <Link to="/docs/">current documentation</Link> or the{' '}
      <Link to="/docs/migration/to-4/">migration guidance</Link>.
    </aside>
  );
}

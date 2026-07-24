import { useEffect, useState } from 'react';

import { getGitHubStarCount } from './github-stars';
import styles from './github-stars.module.css';

interface GitHubStarsNavbarItemProps {
  href: string;
  mobile?: boolean;
  className?: string;
}

export function formatStarCount(count: number): string {
  if (count < 1_000) return String(count);

  const divisor = count < 999_500 ? 1_000 : 1_000_000;
  const suffix = divisor === 1_000 ? 'K' : 'M';
  const compact = count / divisor;
  const displayValue = compact < 10 ? Math.round(compact * 10) / 10 : Math.round(compact);
  return `${displayValue}${suffix}`;
}

function GitHubIcon() {
  return (
    <svg aria-hidden="true" className={styles.githubIcon} viewBox="0 0 24 24">
      <path
        fill="currentColor"
        d="M12 .7a11.5 11.5 0 0 0-3.6 22.4c.6.1.8-.2.8-.5v-2.2c-3.4.7-4.1-1.4-4.1-1.4-.6-1.4-1.4-1.8-1.4-1.8-1.1-.8.1-.8.1-.8 1.3.1 1.9 1.3 1.9 1.3 1.1 1.9 2.9 1.4 3.6 1.1.1-.8.4-1.4.8-1.7-2.7-.3-5.5-1.3-5.5-5.7 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.6.1-3.1 0 0 1-.3 3.2 1.2a11 11 0 0 1 5.8 0c2.2-1.5 3.2-1.2 3.2-1.2.6 1.5.2 2.8.1 3.1.8.8 1.2 1.8 1.2 3.1 0 4.4-2.8 5.4-5.5 5.7.5.4.9 1.1.9 2.2v3.2c0 .3.2.6.8.5A11.5 11.5 0 0 0 12 .7Z"
      />
    </svg>
  );
}

function StarIcon() {
  return (
    <svg aria-hidden="true" className={styles.starIcon} viewBox="0 0 24 24">
      <path
        fill="currentColor"
        d="m12 2.3 2.9 5.9 6.5.9-4.7 4.6 1.1 6.5-5.8-3.1-5.8 3.1 1.1-6.5-4.7-4.6 6.5-.9L12 2.3Z"
      />
    </svg>
  );
}

export default function GitHubStarsNavbarItem({
  href,
  mobile = false,
  className,
}: GitHubStarsNavbarItemProps) {
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    void getGitHubStarCount(controller.signal).then((nextCount) => {
      if (!controller.signal.aborted) setCount(nextCount);
    });
    return () => controller.abort();
  }, []);

  const label =
    count === null
      ? 'Sniffy GitHub repository'
      : `Sniffy GitHub repository, ${count.toLocaleString('en-US')} stars`;
  const linkClass = mobile ? 'menu__link' : 'navbar__link';

  return (
    <a
      aria-label={label}
      className={`${linkClass} ${styles.link}${className ? ` ${className}` : ''}`}
      href={href}
    >
      <GitHubIcon />
      <span>GitHub</span>
      <span className={styles.countSlot} data-testid="github-star-count">
        {count !== null && (
          <span className={styles.count} aria-hidden="true">
            <StarIcon />
            {formatStarCount(count)}
          </span>
        )}
      </span>
    </a>
  );
}

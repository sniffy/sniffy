import type { ComponentProps, PropsWithChildren } from 'react';

type DocusaurusLinkProps = PropsWithChildren<
  Omit<ComponentProps<'a'>, 'href'> & {
    to: string;
  }
>;

export default function DocusaurusLinkStub({
  children,
  to,
  ...props
}: DocusaurusLinkProps): React.JSX.Element {
  return (
    <a href={to} {...props}>
      {children}
    </a>
  );
}

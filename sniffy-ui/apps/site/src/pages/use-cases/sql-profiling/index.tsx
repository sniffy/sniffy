import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

import { UseCaseLayout } from '../../../components/use-case/use-case-layout';
import { createSqlProfilingUseCase } from '../../../use-cases/sql-profiling';

export default function SqlProfilingPage(): React.JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const content = createSqlProfilingUseCase(String(siteConfig.customFields?.productVersion));
  return <UseCaseLayout content={content} />;
}

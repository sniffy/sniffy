import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

import { UseCaseLayout } from '../../../components/use-case/use-case-layout';
import { createDatabaseQueryTestingUseCase } from '../../../use-cases/database-query-testing';

export default function DatabaseQueryTestingPage(): React.JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const content = createDatabaseQueryTestingUseCase(
    String(siteConfig.customFields?.productVersion),
  );

  return <UseCaseLayout content={content} />;
}

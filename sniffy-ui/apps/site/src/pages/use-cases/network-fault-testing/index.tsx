import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

import { UseCaseLayout } from '../../../components/use-case/use-case-layout';
import { createNetworkFaultTestingUseCase } from '../../../use-cases/network-fault-testing';

export default function NetworkFaultTestingPage(): React.JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const content = createNetworkFaultTestingUseCase(String(siteConfig.customFields?.productVersion));
  return <UseCaseLayout content={content} />;
}

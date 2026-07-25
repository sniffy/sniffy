import { UseCaseLayout } from '../../../components/use-case/use-case-layout';
import { trafficCaptureUseCase } from '../../../use-cases/traffic-capture';

export default function TrafficCapturePage(): React.JSX.Element {
  return <UseCaseLayout content={trafficCaptureUseCase} />;
}

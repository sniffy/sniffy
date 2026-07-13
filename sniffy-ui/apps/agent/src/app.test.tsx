import { render, screen } from '@testing-library/react';
import { AgentApp } from './app';

it('renders the agent registry feature', () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(JSON.stringify({ persistent: false, sockets: [], dataSources: [] }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  render(<AgentApp baseUrl="https://agent.example/" />);
  expect(screen.getByRole('heading', { name: 'Connectivity control center' })).toBeVisible();
});

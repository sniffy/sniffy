import { createRoot } from 'react-dom/client';
import { initializeProfiler } from './index';

vi.mock('react-dom/client', () => ({
  createRoot: vi.fn(() => ({ render: vi.fn() })),
}));

describe('profiler initialization', () => {
  it('creates one custom element with an open ShadowRoot when initialized repeatedly', () => {
    document.body.replaceChildren();
    const header = document.createElement('span');
    header.id = 'sniffy-header';
    Object.defineProperty(header, 'src', {
      value: 'http://localhost:3000/sniffy/4.0.0/sniffy.min.js',
    });
    const data = document.createElement('data');
    data.id = 'sniffy';
    data.dataset.sqlQueries = '2';
    data.dataset.serverTime = '84';
    document.body.append(header, data);

    const first = initializeProfiler();
    const second = initializeProfiler();

    expect(second).toBe(first);
    expect(document.querySelectorAll('sniffy-profiler')).toHaveLength(1);
    expect(first?.shadowRoot?.mode).toBe('open');
    expect(first?.shadowRoot?.querySelector('#sniffy-root')).not.toBeNull();
    expect(createRoot).toHaveBeenCalledOnce();
  });
});

import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';

afterEach(() => {
  document.body.replaceChildren();
  document.head.replaceChildren();
  delete (window as Window & { sniffy?: unknown }).sniffy;
});

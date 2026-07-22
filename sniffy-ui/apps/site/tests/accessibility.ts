import { expect, type Page } from '@playwright/test';
import axe from 'axe-core';

type AxeViolation = {
  id: string;
  impact: string | null;
  nodes: string[];
};

export async function expectNoAccessibilityViolations(page: Page): Promise<void> {
  await page.addScriptTag({ content: axe.source });

  const violations = await page.evaluate(async () => {
    const runner = (
      globalThis as unknown as {
        axe: {
          run: (
            context: Document,
            options: { runOnly: { type: 'tag'; values: string[] } },
          ) => Promise<{
            violations: Array<{
              id: string;
              impact: string | null;
              nodes: Array<{ target: string[] }>;
            }>;
          }>;
        };
      }
    ).axe;
    const result = await runner.run(document, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa', 'wcag21aa'],
      },
    });

    return result.violations.map((violation): AxeViolation => ({
      id: violation.id,
      impact: violation.impact,
      nodes: violation.nodes.flatMap((node) => node.target),
    }));
  });

  expect(violations).toEqual([]);
}

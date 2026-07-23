declare module '@cmfcmf/docusaurus-search-local' {
  import type { LoadContext, Plugin } from '@docusaurus/types';

  export default function searchLocalPlugin(
    context: LoadContext,
    options: Record<string, unknown>,
  ): Plugin;
}

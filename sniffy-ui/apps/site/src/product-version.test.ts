import { resolve } from 'node:path';

import productVersionRemarkPlugin, { readProductVersion } from './product-version';

const repositoryRoot = resolve(import.meta.dirname, '../../../..');

describe('product version documentation plugin', () => {
  it('reads the build version from the reactor root', () => {
    expect(readProductVersion(repositoryRoot)).toBe('4.0.0-SNAPSHOT');
  });

  it('renders the build version in prose and dependency snippets', () => {
    const tree = {
      type: 'root',
      children: [
        { type: 'paragraph', value: 'Download sniffy-@sniffy.version@.jar' },
        { type: 'code', value: '<version>@sniffy.version@</version>' },
      ],
    };

    productVersionRemarkPlugin({ repositoryRoot })(tree);

    expect(tree.children[0].value).toBe('Download sniffy-4.0.0-SNAPSHOT.jar');
    expect(tree.children[1].value).toBe('<version>4.0.0-SNAPSHOT</version>');
  });
});

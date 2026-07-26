# Use-case product media

`traffic-capture-network.png` is a byte-for-byte copy of
`sniffy-ui/tests/e2e/visual-baselines/profiler-registry-checked.png`. It shows the current Sniffy
profiler against the repository's deterministic `/mock/mock.html` fixture after the **Network
Connections** tab is opened. The image is reviewed Playwright output, not hand-authored or generated
interface artwork.

Regenerate the source image from the repository root:

```bash
cd sniffy-ui
npm run test:e2e:update -- --grep "@visual final profiler and agent states"
```

Then sync the reviewed source image into the site:

```bash
cp tests/e2e/visual-baselines/profiler-registry-checked.png \
  apps/site/static/img/use-cases/traffic-capture-network.png
```

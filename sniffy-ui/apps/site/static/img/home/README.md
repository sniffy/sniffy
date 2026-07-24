# Homepage product media

`sniffy-profiler.png` is a byte-for-byte copy of
`sniffy-ui/tests/e2e/visual-baselines/profiler.png`. It shows the real current profiler running
against the repository's deterministic `/mock/mock.html` fixture; it is not a hand-authored or
generated interface.

Regenerate the source image from the repository root:

```bash
cd sniffy-ui
npm run test:e2e:update -- --grep "@visual final profiler and agent states"
```

Then sync the reviewed source image into the site:

```bash
cp tests/e2e/visual-baselines/profiler.png apps/site/static/img/home/sniffy-profiler.png
```

`apps/site/src/homepage.test.tsx` prevents the published copy from drifting from that reviewed
source.

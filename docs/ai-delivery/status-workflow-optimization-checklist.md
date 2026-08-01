# Status workflow checkout optimization checklist

- [ ] Validate exact sparse paths against all focused tests.
- [ ] Confirm validation remains on `ubuntu-24.04` and publication remains on `ubuntu-slim`.
- [ ] Confirm checkout uses non-cone sparse checkout and `persist-credentials: false`.
- [ ] Confirm no `filter` input overrides sparse checkout.
- [ ] Confirm publication still checks out both required scripts from trusted `develop`.
- [ ] Inspect one pull-request validation checkout log.
- [ ] After merge, inspect one scheduled and one `workflow_run` publication.
- [ ] Compare artifact schema, digest publication, counts, and source SHA with the pre-change behavior.

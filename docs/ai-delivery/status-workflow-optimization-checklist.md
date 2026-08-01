# Status workflow checkout optimization checklist

- [ ] Validate exact sparse paths against all focused tests.
- [ ] Confirm both jobs remain on `ubuntu-24.04`.
- [ ] Confirm checkout uses `filter: blob:none` and `persist-credentials: false`.
- [ ] Confirm publication still checks out trusted `develop`.
- [ ] Inspect one pull-request validation checkout log.
- [ ] After merge, inspect one scheduled and one `workflow_run` publication.
- [ ] Compare artifact schema, digest publication, counts, and source SHA with the pre-change behavior.

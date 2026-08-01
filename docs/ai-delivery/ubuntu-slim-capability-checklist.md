# Ubuntu slim experiment checklist

- [ ] Production validate and publish jobs remain on `ubuntu-24.04` during the probe.
- [ ] Probe runs on `ubuntu-slim` with no checkout, secrets, or write permission.
- [ ] Record `git`, `gh`, `jq`, `node`, `ruby`, `docker`, `tar`, and `curl` availability.
- [ ] Migrate publish only when all recurring publication prerequisites are already present or setup is demonstrably cheap.
- [ ] Do not weaken validation to make the runner migration pass.
- [ ] Close the experiment without production change when the capability evidence is negative.

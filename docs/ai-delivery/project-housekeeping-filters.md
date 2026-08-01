# Project housekeeping filters

Recommended initial Project filters:

```text
# Auto-archive completed work after the cooling-off window
Status:Done updated:<@today-14d
```

Do not use these unsafe approximations:

```text
Execution:Ready
is:closed
```

`Execution:Ready` is routing state, not completion. A bare `is:closed` filter can include closed implementation-evidence pull
requests whose canonical issue remains active. Terminal field changes must be driven by the built-in source event plus the
canonical-item policy documented in [`project-housekeeping.md`](project-housekeeping.md).
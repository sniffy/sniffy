# Delivery status sparse checkout paths

Validation materializes only files consumed by the focused tests and linters:

```text
.chatgpt/scheduled-task-prompt.md
.chatgpt/status-snapshot-instructions.md
.github/scripts/delivery-status.js
.github/scripts/delivery-status.test.js
.github/scripts/delivery-status-policy.test.js
.github/scripts/delivery-status-checkout-policy.test.js
.github/workflows/delivery-status.yml
docs/ai-delivery/README.md
docs/ai-delivery/profile.yml
docs/ai-delivery/status-snapshot.md
docs/ai-delivery/status-workflow-optimization.md
```

Publication materializes only:

```text
.github/scripts/delivery-status.js
```

Add a path only when the same change introduces a real runtime or test dependency on it.
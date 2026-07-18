# Cloudinary configuration for PR screenshots

The skill uses unsigned uploads. Only the cloud name and restricted upload preset are required during the agent phase.

## Local Codex

The recommended repository-local setup is an ignored `.env.local` file:

```bash
cp .env.local.example .env.local
```

Fill in only:

```text
CLOUDINARY_CLOUD_NAME=<cloud-name>
CLOUDINARY_UPLOAD_PRESET=<restricted-unsigned-preset>
```

Then run the publisher with Node's built-in env-file support:

```bash
node --env-file=.env.local \
  .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs --help
```

For commands that should still work when `.env.local` is absent, use:

```bash
node --env-file-if-exists=.env.local \
  .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs --help
```

`.env.local` is ignored by Git. Never put real values in `.env.local.example`.

A shell profile, `direnv`, or a password-manager-backed environment is also valid. Variables already exported by the shell take precedence over values loaded from the env file.

Verify the local GitHub CLI session separately:

```bash
gh auth status --hostname github.com
```

No Cloudinary MCP server is required. The skill deliberately uses the same Node script and HTTP API in local and cloud environments.

## Codex Cloud

Add these as environment variables available to the agent phase:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_UPLOAD_PRESET
```

Do not rely on a setup-only secret for the upload preset, because Codex Cloud removes setup secrets before the agent phase. Keep the preset narrowly restricted in Cloudinary and rotate it if it becomes public or abused.

Enable agent internet access with this minimum allowlist:

```text
api.cloudinary.com       POST
res.cloudinary.com       GET, HEAD
api.github.com           GET, POST, PATCH
github.com               GET, HEAD
```

The repository's existing Codex GitHub publication flow may require broader GitHub methods; keep the GitHub policy in `docs/codex-workflow.md` authoritative.

## Cloudinary preset

Configure the unsigned preset in Cloudinary with at least:

- asset folder dedicated to PR screenshots;
- PNG allowed and unrelated formats rejected;
- a conservative maximum file size;
- unique filenames enabled;
- overwrite disabled;
- no eager transformations or webhooks required for this workflow.

The following values are not needed by the publishing script:

- Cloudinary API key;
- Cloudinary API secret;
- product-environment ID;
- asset-folder environment variable.

The asset folder is controlled by the preset itself.

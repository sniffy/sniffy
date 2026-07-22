# Preview the Sniffy website artifact

This directory contains the complete production website build. The site keeps its production `baseUrl` of `/`, so
opening `index.html` directly with a `file://` URL is not supported. Serve the extracted directory over HTTP instead.

## Requirements

- Node.js 24.18.0 or newer in the Node 24 release line. The included `.node-version` records the version used by CI.
- No dependency installation or repository checkout is required.

## macOS or Linux

From the extracted artifact directory, run:

```shell
node preview.mjs
```

## Windows

From the extracted artifact directory in PowerShell or Command Prompt, run:

```powershell
node preview.mjs
```

Open the printed URL in a browser. The homepage is at `/` and the current documentation is at `/docs/`. Press
`Ctrl+C` to stop the server.

The default address is `http://127.0.0.1:4173/`. To choose another loopback address or port, use
`node preview.mjs --host 127.0.0.1 --port 8080`.

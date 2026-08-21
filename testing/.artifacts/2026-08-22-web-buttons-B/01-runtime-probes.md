# Runtime probes

## Vite static serving

The first direct Vite process started from repository root and returned `404` for `/`, `/agent`, and `/login`; it was stopped. The second process used the Web directory as the positional Vite root and was stopped after these read-only probes:

```text
VITE v4.5.14 ready
Local: http://127.0.0.1:5173/
http://127.0.0.1:5173/ 200 text/html
http://127.0.0.1:5173/agent 200 text/html
http://127.0.0.1:5173/login 200 text/html
```

This proves the dev server can return the SPA entry. It does not prove Vue execution, API calls, login, permissions, or Agent behavior.

## Toolchain limitation

```text
node --version       -> zsh: command not found: node
npm --version        -> zsh: command not found: npm
command -v npx       -> no result
bundled node         -> v24.19.0
bundled npm          -> missing
bundled npx          -> missing
```

The Playwright wrapper could not be invoked because its required `npx` executable was unavailable. No browser snapshot, click, request capture, or screenshot was produced.

## API probes

```text
http://127.0.0.1:18080/v2/agent/workbench -> connection refused
http://127.0.0.1:8080/v2/agent/workbench  -> 401 text/html (Cheroot Digest)
https://sxyq27.online/zhj-api/v2/agent/workbench -> 410 text/html
https://sxyq27.online/zhj-api/v2/agent/conversations -> 410 text/html
```

No request included an authorization token or login payload.

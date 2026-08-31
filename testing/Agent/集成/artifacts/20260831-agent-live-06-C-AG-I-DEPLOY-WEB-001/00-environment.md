test_id: AG-I-DEPLOY-WEB-001
category_id: I
wave_id: 20260831-agent-live-06-C-W0

Environment and deployment scope:

- source commit: `1e79277630fe298d40c946d703f49faa667348a0`
- frontend source: `Code/frontend/web` only; `Code/frontend/admin-web` excluded
- build: `npm ci --ignore-scripts --no-audit --no-fund` and `VITE_PUBLIC_BASE=/zhj/ VITE_API_BASE_URL=https://zhj-api.sxyq27.online npm run build`
- local build result: successful; runtime `index.html`, JS and CSS had no `admin-web`, `/admin/overview` or `SUPER_ADMIN` marker
- local runtime hashes: index `05d18d1a7f38f49381f532596c80670b3e3a00e7b3aa523c3a75fc0147c24ff2`, JS `61ee9e049c5a0abe6617fe268666a0837d73adc512a71c37c3b65d6a332bbc28`, CSS `8116c093f6677a5789906270595990addd017997a60fc586f1a908929a8126d`
- target: `124.222.153.108`, existing Nginx `/zhj/` alias
- current target before attempt: `/opt/sxyq27/releases/20260830T193200-owner-web-rollback-fa14a61b/zhj`
- current public `/zhj/` status: HTTP `200`; current index hash matched the local D build
- upload attempt: release creation failed with `Permission denied` under `/opt/sxyq27/releases`; `sudo -n` unavailable and root SSH rejected
- no file was uploaded, no symlink was changed, and no Nginx reload occurred

This is a real deployment permission block, not a successful Web release. Existing owner-web content and the management backend were left unchanged.

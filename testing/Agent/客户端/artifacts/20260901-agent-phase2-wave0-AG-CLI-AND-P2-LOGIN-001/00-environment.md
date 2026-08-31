# Wave 0 环境证据

- test_id: `AG-CLI-AND-P2-LOGIN-001`
- status: `Blocked`
- observed_at: `2026-09-01T03:37:14+08:00`
- target_server: `8.220.206.9`
- public_api: `https://zhj-api.sxyq27.online/`
- direct_tcp_22: `Passed`
- direct_ssh_login: `Passed`（root，凭据未写入证据）
- host: `Ubuntu 24.04.2 LTS`
- kernel: `6.8.0-63-generic`
- api_image: `sxyq27-zhj-api:20260901T014000-agent-owner-bill-c012290b`
- api_restart_count: `0`
- running_jar_sha256: `d9ac0b508fd55082905fd30608cf6221ff4e97e279496a08ce87a448ad318d77`
- flyway_schema_version: `V42`
- services: `Nginx/Docker/SSH/New API relay = active`
- database: `PostgreSQL 15; database=zhj`
- android_serial: `emulator-5554`
- android_avd: `Zhihuiji_API34`
- android_api: `34`
- package: `com.zhihuiji.app`
- app_version: `versionCode=1; versionName=1.0.0`
- launcher: `com.zhihuiji.app/.MainActivity`
- app_default_api: `https://zhj-api.sxyq27.online/`
- configured_context_upper_bound: `272000 tokens`
- provider_model_actual: `gpt-5.6-luna`
- provider_model_target: `glm-5.3-flash`
- provider_model_alignment: `Blocked`（实际运行模型与目标模型不一致；Provider 实际上下文窗口未确认）

数据库只读计数在登录探针前后均保留在 `06-database-before.json` 和 `07-database-after.json`。本轮未创建账号、门店、会话、Agent 运行或业务夹具。

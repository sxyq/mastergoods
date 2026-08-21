# B11 2026-06-30 真机本地 HTTPS 联调首页 Smoke

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 15:03-15:17 CST |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty，包含本轮后端测试修复、Android 网络联调改动与审计文档更新 |
| 设备 | `d715a3a4` / `25010PN30C` / Android 16 / `adb` via `/Users/sunyiyang/Library/Android/sdk/platform-tools/adb` |
| 本地后端 | `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --console=plain`，本地 H2 通过移走旧 `./.gradle-local/zhihuiji_local*.db` 后重建 |
| HTTPS 联调入口 | `https://great-glasses-kneel.loca.lt/` -> 本机 `http://127.0.0.1:18080/` |
| 关键命令 | `adb devices -l`、`adb shell pm clear com.zhihuiji.app`、`adb shell am start -n com.zhihuiji.app/.MainActivity`、`adb shell uiautomator dump`、`adb exec-out screencap -p`、`adb logcat --pid=<app pid>` |
| 结果 | PASS（本地 HTTPS 真机主链路） |
| 摘要 | 真机被当前 Mac 正常识别；清空 App 数据后，登录页冷启动正常；切到本地 HTTPS 隧道并使用本地账号 `13800138111 / 123456` 登录后成功进入首页；随后点开 `单据 / 档案 / 报表 / 助手` 四个一级页签，界面均正常打开。日志可见 `/v1/auth/login`、`/v2/stores/current`、`/v2/reports/*`、`/v2/products/low-stock` 等请求返回 `200 OK`。 |
| 特殊现象 | 在切环境前的旧会话状态下，应用曾进入一张“无权访问”页面；对 App 执行 `pm clear` 后，冷启动登录链路稳定进入首页，证明该现象属于旧状态/旧落点残留，不是当前冷启动登录链路的稳定阻塞。 |
| 首页证据 | `docs/acceptance-evidence/b11/screenshots/20260630-1505-d715a3a4-fresh-launch.png`、`docs/acceptance-evidence/b11/screenshots/20260630-1508-d715a3a4-login-home.png` |
| 旧状态现象证据 | `docs/acceptance-evidence/b11/screenshots/20260630-1503-d715a3a4-stale-permission-page.png` |
| 日志要点 | 15:12-15:17 期间 `logcat` 出现 `GET /v2/reports/sales-summary`、`/v2/reports/sales-trend`、`/v2/reports/reconciliation-summary`、`/v2/reports/cashflow-summary`、`/v2/products/low-stock` 返回 `200 OK`；15:16:54 触发 `GET /v2/sale-orders` 与 `GET /v2/purchase-orders` |
| 仍未覆盖 | 同步 `pull/apply/ack`、导入任务、媒体上传绑定、真实 provider AI 对话、性能采样、生产环境业务链路 |


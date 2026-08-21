# Agent 第二阶段收束报告（2026-08-22，Agent C）

## 结论

本阶段只核对当前 8220 认证前置并完成交接清单登记，没有形成可计入当前通过率的 Agent case。20 个 C 项目全部登记为 `Blocked`，计数为 Passed=0、Failed=0、Blocked=20、Deferred=0。

目标限定为 `8.220.206.9` / `https://zhj-api.sxyq27.online/`，业务范围为 owner=2、store=1。154、旧设备、旧 provider、旧容器和旧证据没有计入结果。

## 前置证据

- 未认证 `GET /v2/auth/users/me` 返回 HTTP 403；原始 headers/body 保存在 `testing/.artifacts/2026-08-22-agent-followup-C/preflight/`，Cookie 值已脱敏。
- 8220 root SSH RSA key 来源已记录但未写入 key 内容；SSH 主机探测成功，当前容器为 `sxyq27-zhj-api:20260818`，PostgreSQL/Redis 也在运行。
- 当前 PostgreSQL 非敏感配置为 `POSTGRES_USER=zhj`、`POSTGRES_DB=zhj`。只读元数据看到 active sessions=3、owner=2 sessions=3、owner=2 stores=1、owner=2 memberships=1。
- 这些 DB session 记录不作为本阶段认证凭据。按本阶段最终规则，只接受安全环境变量或受控登录流程提供的授权；当前没有可用的这类认证来源，因此没有合法的 Agent 业务请求可以计入。
- 仓库存在 `testing/scripts/run_server_agent_all_tools.py`，但该 runner 的认证实现会从 PostgreSQL 查询 session token；旧 `*_remote.sh` wrapper 仍指向 154，均不作为本阶段执行入口。

## 中止执行记录

在最终认证限制到达前，误启动了一次当前 runner。SSH 会话中断后远端进程脱离会话，已对本轮精确 PID 终止。中止前生成了 009、012、016、041、048、049 六条原始 runner 记录；这些记录使用了 DB-derived session，不符合本阶段接受的认证来源，全部排除，不计入 Passed/Failed。其 `case-status.tsv`、逐 case JSON 和 runner 日志仅作为中止证据保留。远端本轮临时目录已按精确路径清理。

## 20 项登记

逐项字段登记在 `testing/.artifacts/2026-08-22-agent-followup-C/blocked-case-register.tsv`，正式台账已追加相同 20 个 `Blocked` 项：

- HTTP 200 失败重测：009、012、016、041、048、049、051、052、053、054。
- 历史恢复：C-HISTORY-001。
- 跨 owner：C-OWNER-001。当前没有第二个有效 owner/store/session。
- SSE 完成、cancel、audit：C-SSE-001、C-CANCEL-001、C-AUDIT-001。
- SSE 并发、非流式 30 轮：C-CONC-SSE-001、C-CONC-NONSTREAM-001。
- 会话/草稿清理、媒体自动清理：C-CLEAN-001、C-CLEAN-MEDIA-001。

每项的 `actual` 均说明没有可接受认证前置，未形成当前有效结果；未把 HTTP 403 或中止 runner 日志转写为通过。

## 范围声明

本阶段没有修改 `Code/backend`、`Code/frontend/android`、`Code/frontend/web`、迁移、配置、部署对象或 `data/`。未执行旧 154 wrapper、Android 视觉测试、跨 owner 请求、SSE 负载、30 轮非流式负载或媒体写入。

## r2 当前有效结果（追加）

按新的认证口径，重新使用当前 8220 runner 完成了独立 run：`agent-followup-C-20260822-targeted-10-r2`。runner 在远端 PostgreSQL 内部选择 owner=2 的 active session，原始 token 只存在 runner 进程环境中，没有写入证据。

10 个 case 均自然完成，全部 HTTP 200；当前有效结果为 Passed=3、Failed=7、Blocked=0、Deferred=0。首轮交接中的 Blocked 记录和 `aborted-run/` 原样保留，没有被 r2 覆盖，也没有并入 r2 计数。

| case | r2 结果 | 实际工具/状态 | 关键 delta 与清理 | audit |
|---|---|---|---|---|
| 009 | Passed | `customer_receivable_lookup` completed；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 012 | Failed | 只完成 `product_catalog_lookup`，未选 `generate_poster_prompt`；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 016 | Passed | `smart_restock_lookup` completed；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 041 | Passed | `supplier_payable_lookup` completed；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 048 | Failed | `product_catalog_lookup` + `inventory_snapshot_lookup`，未创建盘点草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；7/7 |
| 049 | Failed | 只完成 `supplier_directory_lookup`，未创建付款草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 051 | Failed | `supplier_directory_lookup` + `product_catalog_lookup`，未创建采购草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；6/6 |
| 052 | Failed | 只完成 `purchase_order_lookup`，未创建入库草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 053 | Failed | 只完成 `purchase_order_lookup`，未创建退货草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；4/4 |
| 054 | Failed | `customer_directory_lookup` + `product_catalog_lookup`，未创建销售草稿；answer present | business=0；draft=0；cleanup=true | completed；lossless；6/6 |

case-status、原始 request/response/tool trace、DB before/after、cleanup 和 audit 原文位于：
`testing/.artifacts/2026-08-22-agent-followup-C/r2-targeted-10/`。

## 其余专项状态

special-r2 测试脚本曾按安全环境变量方式启动，但本次收束指令到达时尚未完成，进程已停止，未将其部分输出作为当前结果。历史恢复、SSE 完成、SSE cancel、audit 专项、10 路 SSE、30 轮非流式、草稿/会话清理、媒体自动删除继续登记为 `Blocked`；跨 owner 仍因缺少第二个有效 owner/store/session 保持 `Blocked`。没有使用旧 154 wrapper，也没有建立新的凭据来源或认证机制。

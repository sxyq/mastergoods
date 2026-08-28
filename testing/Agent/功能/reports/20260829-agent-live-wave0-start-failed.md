# Agent Live Wave 0 启动与服务基线

- `test_id`: `AG-F-LIVE-W0-BOOT-001`
- `category_id`: `AG-F-ENV-001`
- `wave_id`: `Wave 0`
- `result`: `Blocked`
- `environment`: macOS 本机；Gradle wrapper 8.7；Java 21.0.11；Spring Boot 3.2.6；profile `local`；目标端口 `18080`；数据库类型 H2 file。
- `source_commit`: `93d085420076f4f2b6fd47faa0b662e45f029976` (`test(admin): add stage two to four acceptance evidence`)
- `startup_command`: `./Code/backend/gradlew -p Code/backend bootRun --args='--spring.profiles.active=local --server.port=18080'`
- `runtime_safety`: LLM 出站调用已在进程启动时关闭；Provider 凭据、认证载荷和环境变量值未写入本报告。

## 前置检查

- `18080` 启动前无监听，且没有 Spring/`bootRun` 进程。
- `8080` 为独立 Python WebDAV 进程，未作为后端使用。
- 项目配置确认默认端口为 `18080`，`local` profile 使用 H2；`bootRun` 任务会设置本地运行时的 H2 与媒体路径。

## 启动实际结果

1. 第一次后台托管尝试 launcher PID 为 `6033`，命令返回后进程立即退出，stdout/stderr 为空，未监听端口。
2. 第二次以前台会话执行同一 `bootRun` 入口。Spring 进程 PID `8191` 成功读取 `local` profile，H2 连接成功，Tomcat 初始化 `18080`。
3. ApplicationContext 初始化在 `adminAgentController` bean 处失败，进程退出码 `1`，之后 `18080` 无监听。
4. 启动失败根因：`AdminAgentController` 存在多个构造函数但没有可供 Spring 选择的注入构造函数，触发 `No default constructor found`；嵌套原因是 `AdminAgentController.<init>()` 的 `NoSuchMethodException`。

完整脱敏 stderr：
`testing/Agent/功能/reports/20260829-agent-live-bootrun-local-18080.stderr.redacted.log`

Spring 异常及启动上下文日志：
`testing/Agent/功能/logs/20260829-agent-live-bootrun-local-18080-session.log`
`testing/Agent/功能/logs/20260829-agent-live-bootrun-local-18080-session.stderr.log`

## 服务与数据库观测基线

- 服务状态：`Blocked`，当前无可用 Spring HTTP 服务；健康检查未获得响应，运行服务版本无法确认。
- 启动版本：只确认启动横幅为 Spring Boot `3.2.6`、Java `21.0.11`；这不等同于服务已通过健康检查。
- 源码版本：已记录 `source_commit`；本次未修改源码、配置、迁移或客户端。
- 数据库：应用曾成功打开本地 H2 连接并完成 JPA 初始化，随后在 Web 层 bean 初始化失败；未进入 Agent API，因此没有业务查询、写入或 before/after 快照。
- Provider：调用次数为 `0`；未执行真实 Provider 请求。
- Android：本轮未产生可对齐的 App HTTP/SSE、run-trace、audit 或数据库业务证据。

## 清理与下一步状态

- 已确认失败进程退出；未执行删除操作，也未删除既有用户文件或历史证据。
- 后续 Wave 1/2 与 Android 对齐暂不执行，直到代码侧修复 `AdminAgentController` 的 Spring 构造函数注入问题并重新启动服务。
- `result` 只表示本次启动检查状态；没有将任何未执行用例标记为 `Passed`。

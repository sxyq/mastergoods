# Context window resolver evidence

更新日期：2026-08-31。范围为 `ContextWindowResolver` 的默认窗口、配置优先级、模型解析和降级来源。

| 项目 | 内容 |
|---|---|
| test_id | `AG-U-007` |
| category_id | `U` |
| wave_id | `20260831-agent-context-window` |
| environment | Java 24；Gradle 8.7；Spring Boot 3.2.6 依赖缓存；未调用 Provider |
| 输入 | `glm-5.3-flash`、未知模型、合法/非法窗口值、Spring 属性与环境变量优先级 |
| 结果 | `Passed` |

## 命令与结果

| 命令/检查 | 结果 | 说明 |
|---|---|---|
| `./gradlew test --tests com.zhihuiji.backend.application.service.v2.agent.context.ContextWindowResolverTest --no-daemon` | `Blocked` | 既有 `ContextBuilder.java:437` 使用了非法字符文字，主源码未完成编译 |
| 独立编译 `AgentLlmProperties.java` 与 `ContextWindowResolver.java` | `Passed` | 目标源码编译成功 |
| 独立编译 `ContextWindowResolverTest.java` | `Passed` | 目标测试源码编译成功 |
| 反射执行 `ContextWindowResolverTest` 的 13 个 `@Test` 方法 | `Passed` | 默认值、覆盖、非法值、未知模型、来源和边界均通过 |
| 独立 Spring 容器注入 `agent.context.maximum-window` | `Passed` | Spring 属性 `131072` 优先于环境变量候选值 `65536` |
| 环境变量构造器校验：合法 `131072`、非法文本 | `Passed` | 合法值生效，非法值回到 `272000` |
| 四份 YAML 语法检查与 `git diff --check` | `Passed` | 配置结构和差异空白检查通过 |

## 数据与限制

- 本轮未调用 Provider、数据库或真实认证会话；运行链路验证记 `Deferred`。
- 未读取、打印或保存凭据、Cookie、Token、密码、Key 或完整认证载荷。
- 构建产物仅写入既有临时构建目录，不纳入提交。

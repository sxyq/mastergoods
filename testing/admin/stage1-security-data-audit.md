# 管理员后台第 1 阶段安全与数据基线

## 范围

本记录对应 01-07 文档中的第 1 阶段底座工作：管理员角色与权限、owner/store 数据范围、权限拒绝、集合不可变性，以及 Agent 运行审计字段核对。只记录源码级和单元测试级证据，不代表管理员 API 已经接入。

参考文档：

- `docs/管理员后台/01_业务需求/管理员角色与数据权限.md`
- `docs/管理员后台/02_业务系统需求/管理员后台安全与审计需求.md`
- `docs/管理员后台/03_系统设计/权限设计/管理员后台权限与数据范围设计.md`
- `docs/管理员后台/03_系统设计/数据库设计/管理员后台数据模型设计.md`
- `docs/管理员后台/04_详细设计与实现/管理员后台后端实现计划.md`
- `docs/管理员后台/05_测试与验收/安全测试/管理员后台安全测试计划.md`
- `docs/管理员后台/07_问题审计/管理员后台数据隔离审计.md`

## 已覆盖基线

| 基线 | 测试位置 | 结果 |
|---|---|---|
| 超级管理员权限来自服务端角色集合 | `AdminRolePermissionContractTest` | 通过 |
| 审计观察员没有写权限和正文权限 | `AdminRolePermissionContractTest` | 通过 |
| principal 权限集合和角色权限集合不可变 | `AdminRolePermissionContractTest` | 通过 |
| owner/store 集合复制后不可被调用方改变 | `AdminDataScopeImmutabilityTest` | 通过 |
| 空集合、空内容模式和空资源 ID 的默认行为 | `AdminDataScopeImmutabilityTest` | 通过 |
| 全 owner 范围必须由服务端标志表达 | `AdminDataScopeImmutabilityTest` | 通过 |
| 空 principal、空权限、观察员写操作先拒绝再解析范围 | `AdminAuthorizationBoundaryTest` 与既有 `AdminAuthorizationServiceTest` | 通过 |
| owner 查询可收窄且跨 owner/store 拒绝 | 既有 `DefaultAdminScopeServiceTest` | 通过 |

## Agent 字段证据

`V15__agent_run_audits.sql:3-4` 和 `AgentRunAuditEntity.java:17-21` 当前具备：

- `owner_user_id`
- `conversation_id`
- `run_id`
- 状态、时间、工具数和审计质量字段

当前缺少：

- `actor_user_id`：无法从运行记录独立确认实际发起对话的用户。
- `store_id`：无法从运行记录独立确认运行门店。

`AgentRunAuditSchemaStageOneBaselineTest` 已将上述结论固定为当前源码基线。它通过不代表缺口已解决；后续补充可靠字段或明确的未知状态后，应更新测试和解除条件。

现有 `AgentRunAuditRepository.java:10-17` 同时保留 `findByRunId(String)` 和带 `ownerUserId` 的查找方法。管理员查询实现必须只使用带授权范围的投影/查询，不能把无范围方法当作管理员数据访问入口。

## API 边界核对

`Code/backend/src/main/java/com/zhihuiji/backend/api/controller/AdminController.java` 是 `@Profile("local")` 下的普通 `/v1/admin/**` 控制器，使用现有 `@RequireStorePermission`。它包含本地摘要、用户、演示数据、Agent smoke 和旧数据导入操作。

该控制器不能作为目标管理员后台 `/v2/admin/**` 或其他正式管理员 API 的实现证据。当前基线没有修改它，也没有把本地 profile 下的访问放宽当作管理员身份认证完成。

## 当前问题与下一阶段输入

| 问题 | 当前状态 | 下一阶段必须先取得的输入 |
|---|---|---|
| `ISSUE-ADM-01` 管理员身份、角色和会话来源未落地 | 未完成 | 明确统一会话还是独立管理员会话，以及角色授予来源 |
| `ISSUE-ADM-02` 观察员 owner/store 授权来源未落地 | 未完成 | 明确授权表、角色绑定或其他可信来源；空集合必须保持无可见范围 |
| `ISSUE-ADM-03` Agent 运行缺 actor/store | 阻塞 | 确认写入链可提供的字段；必要时设计新迁移并同步运行创建逻辑 |
| `ISSUE-ADM-04` 管理员登录、查看和拒绝审计存储未落地 | 未完成 | 确认管理员审计实体、字段、保留和失败策略 |
| 管理员内容权限和脱敏服务未落地 | 未完成 | 定义字段白名单、正文查看审计和日志扫描规则 |
| Agent 运行存在无范围 Repository 方法 | 风险 | 管理员查询只允许带 owner/store 条件的方法，并补调用链测试 |

## 测试命令与结果

执行环境：仓库根目录 `/Users/sunyiyang/Desktop/Project/master-goods`，后端 Gradle，JUnit Platform，Java toolchain 21。

```text
./Code/backend/gradlew -p Code/backend test \\
  --tests '*infrastructure.security.admin.*' \\
  --tests '*infrastructure.db.AgentRunAuditSchemaStageOneBaselineTest'
```

结果：通过。上述阶段 1 新增安全边界测试、字段基线测试，以及同目录既有管理员安全测试均执行通过。

```text
./Code/backend/gradlew -p Code/backend test --tests '*application.service.admin.*'
```

结果：通过。既有 `DefaultAdminScopeServiceTest` 的 owner/store 收窄和越权拒绝用例执行通过。

```text
git diff --check
```

结果：通过。

## 限制

本阶段没有修改生产 Java、前端、文档目录或 Flyway migration，没有建立测试账号和跨 owner/store 数据库夹具，也没有声称真实管理员 API、审计写入、PostgreSQL 查询计划或目标环境已经验证。

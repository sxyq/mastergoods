# Server API 层分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/api`
- 子目录：
  - [common/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/common/README.md)
  - [controller/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/controller/README.md)
  - [dto/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/dto/README.md)

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` 控制器与响应包装 | 新版已做 | 旧版无统一远程 API | 保持现有兼容接口 | `api/common`、`api/controller`、`api/dto` 已完整存在 | 继续供安卓当前版本使用 |
| `/v2` 请求/响应契约 | 新版已做 | 旧版无 /v2 分层 | 新增面向新版领域的 DTO 与路由 | `controller/v2/` 已覆盖商品/伙伴/单据/财务/库存、B06 首轮 `sync/import-jobs`，以及 B07 首轮 `media/agent`；`dto/v2/` 已扩展到 `partner/product/sales/purchase/pay/finance/inventory/sync/media/agent` | 后续领域继续扩展；`/v2/sync` cursor 字段现为 opaque token |
| Entity 直接作请求体 | 新版需要去掉 | 首版为追求速度容忍简化 | 创建/更新都改用专用 Request DTO | `/v2` 控制器已全部使用专用 DTO；`/v1` 部分控制器仍存在此模式 | `/v1` 重构随后续批次推进 |
| owner 维度下的控制器过滤 | 新版已做 | 旧版无账号归属 | 所有列表/详情/统计默认按 owner 过滤 | `/v2` 接口已通过 `CurrentOwnerService` 实现 owner 过滤；`/v1` 兼容层仍在补齐 | `/v1` owner 过滤需继续推进 |

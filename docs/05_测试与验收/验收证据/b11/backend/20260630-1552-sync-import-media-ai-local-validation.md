# 2026-06-30 本地 sync/import/media/AI 验证记录（15:52）

## 元数据

- 时间：2026-06-30 15:52 CST
- 执行人：Codex
- 代码状态：`128a3d56`
- 工作树说明：当前工作树为 dirty；本记录仅引用本轮真实命令输出与当前源码

## 1. B06 sync 真闭环

### 命令摘要

- `POST /v1/auth/login`
- `GET /v2/sync/health`
- `GET /v2/sync/cursor/{clientId}`
- `POST /v2/sync/upload`
- `POST /v2/sync/pull`
- `POST /v2/sync/cursor/ack`
- `GET /v2/product-categories`
- `GET /v2/product-units`
- `GET /v2/product-price-levels`

### 结果

- `health` 返回 `owner_scoped=true`
- `upload` 成功写入 3 条基础实体：
  - `product_category`
  - `product_unit`
  - `product_price_level`
- `pull` 返回这 3 条变更
- `ack` 后 `cursor` 成功推进到 `1782805277000|product_unit|1`
- 回查列表接口可见：
  - 分类 `审计分类B06`
  - 单位 `箱`
  - 价格层级 `AUDIT / 审计价`

### 关键响应摘要

- `UPLOAD.data.accepted_count=3`
- `UPLOAD.data.failed_count=0`
- `ACK.data.last_cursor=1782805277000|product_unit|1`

### 结论

- `sync health -> upload -> pull -> ack -> 实体回查` 本地 owner 作用域闭环成立

## 2. B06 import job 真闭环

### 命令摘要

- `POST /v2/import-jobs`
- `GET /v2/import-jobs/{id}`
- `GET /v2/import-jobs`

### 样本库

- `data/database/migration_source_zhihuiji/demo.db`

### 结果

- 新建任务后初始状态为 `pending / accepted`
- 约 5 秒后自动转为 `succeeded / completed`
- `summary_json` 返回真实导入汇总

### 关键响应摘要

- `accounts=4`
- `customers=2`
- `suppliers=1`
- `products=0`
- `sale_orders=0`
- `purchase_orders=0`

### 结论

- 当前本地 worker 调度真实在跑，`import-jobs` 不只是“建任务不执行”
- 但样本库导入出的业务覆盖面有限，仍不等于完整发布级旧库迁移验收

## 3. B07 media 真闭环

### 首次失败复现

- 使用真机截图 `docs/acceptance-evidence/b11/screenshots/20260630-1508-d715a3a4-login-home.png`
- 文件大小约 `1.2M`
- 初始上传返回 `500`
- 复现日志显示真实根因是：
  - `FileSizeLimitExceededException`
  - 默认上限 `1048576 bytes`

### 本轮修复

- 在 `application.yml` 中显式设置：
  - `spring.servlet.multipart.max-file-size=10MB`
  - `spring.servlet.multipart.max-request-size=10MB`
- 在 `GlobalExceptionHandler` 中补充：
  - `MaxUploadSizeExceededException -> 413`
  - `MultipartException -> 400`

### 修复后真实回归

- `POST /v2/products` 创建商品 `AUDIT-P-002`
- `POST /v2/media/assets/upload` 成功上传真机截图
- `POST /v2/media/bindings` 成功绑定到 `target_type=product`
- `GET /v2/media/bindings?target_type=product&target_id=2` 返回绑定记录
- `GET /v2/media/assets/{id}/content` 返回 `200`，`Content-Type: image/png`，`Content-Length: 1256438`

### 超限语义回归

- 使用 `11MB` 测试文件再次上传
- 返回：
  - HTTP `413`
  - JSON `{"code":413,"message":"Uploaded file is too large"}`

### 结论

- 媒体上传、绑定、内容读取本地真闭环成立
- 超限错误语义也已从误报 `500` 收口为明确 `413`

## 4. B07 AI 当前真实边界

### 命令摘要

- `POST /v2/agent/conversations`
- `POST /v2/agent/chat`

### 当前环境

- 当前 shell 未发现可用 provider 环境变量
- 本地后端以 `AGENT_LLM_ENABLED=false` 启动

### 实际结果

- `/v2/agent/chat` 仍可返回真实 owner 数据查询结果
- 当前回答模式为：
  - `mode=tool_query_rule_summary`
  - `llm_status=disabled`
  - `plan_source=keyword_fallback`
- 返回内容基于真实 `product_catalog_lookup` 结果块、证据卡与性能摘要

### 结论

- 已验证“无 provider 时的真实退化语义”
- 但未验证真实 LLM/provider 流式生成，因此不能把 B07 的 AI 部分判为全部完成

## 严格结论

- B06：
  - `sync` 本地闭环已拿到强证据
  - `import-jobs` 本地 worker 闭环已拿到强证据
  - 但完整旧库迁移覆盖面仍待更厚样本/现场验收
- B07：
  - `media` 本地闭环已拿到强证据
  - `AI` 仅完成了真实工具查询退化路径验证，未完成真实 provider 联调

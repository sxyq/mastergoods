# Agent 功能测试规划（功能）

更新日期：2026-08-28。代码基线见 [../代码事实基线.md](../代码事实基线.md)，编号与映射见 [../映射台账.md](../映射台账.md)。本文件所有用例初始状态 `Deferred`；真实执行后仅能更新为 `Passed/Failed/Blocked/Deferred`。

## 一、范围与统一判定

覆盖：会话与消息、60 个工具逐项、多工具链、Loop 六终态、SSE 事件顺序、草稿与二次授权、结果块与图表、上下文压缩、多模态图片、长期记忆、海报、Web 搜索、任务/通知/工作台。每条用例必须同时断言数据库 before/after、草稿状态与 run-audit。

| 检查项 | 边界 | 验收 |
|---|---|---|
| HTTP 状态 | 4xx 可能是正常业务失败 | 200 不单独判定成功，必须核对业务终态 |
| 正式回答 | 空白、占位符、无工具事实的数字 | 合法完成场景回答非空且可追溯（基于 toolFacts） |
| 工具选择 | 缺目标、多无关工具、依赖顺序错 | 目标工具全部执行，无关工具零执行（额外工具被跳过或拒绝） |
| 只读工具 | 空结果、分页边界、大结果 | 业务表零写入；无数据如实说明，不补造 |
| 创建工具 | 草稿/拒绝/确认/重复确认/确认失败 | 确认前不写正式表；同一草稿最多一次正式写入 |
| SSE | 空行、重复事件、断线、终态唯一 | 每个 run 仅一次终态；`call_id` 一一配对 |
| 审计 | 工具失败、取消、压缩、草稿、完成 | 用 `run_id` 可从输入追到终态；`audit_lossy` 为 false 或告警可解释 |
| 清理 | 测试产生的会话/消息/草稿/临时对象 | 清理成功；正式业务数据符合预期保留或无变化 |

## 二、逐工具执行卡统一分支（11 分支，60 工具 × 按分支独立记录）

| 分支 | 输入变化 | 必须观察 |
|---|---|---|
| 成功有数据 | 当前作用域存在的实体/时间范围 | 目标工具、参数、结果、正式回答、结果块与事实 |
| 成功空数据 | 合法但无命中 | 工具完成；回答明确“无数据”；不生成虚假图/数字 |
| 非法参数 | 缺 required、错类型、越界、非法枚举、未知字段 | Repository 前拒绝；`TOOL_ARGUMENTS_INVALID` + fieldPath |
| 未登录 | 无认证上下文 | 401；不产生业务数据 |
| 无权限 | 只有 `agent:view` 却调写工具（或反之） | 403/`TOOL_PERMISSION_DENIED`；不执行 |
| 跨 owner/store | 账号 A 请求 B 的实体 ID/会话/草稿/门店 | 拒绝或安全空结果；不泄露 B |
| 数量/分页边界 | limit/size/page 0、1、最大、最大+1、负、超大 | 按 Schema/分页规范处理；无无界查询 |
| 重复请求 | 同 run_id、同消息重试 | 只读可重复；创建幂等收敛 |
| 流式 | 同输入走 `/v2/agent/chat/stream` | 事件可解析、`tool_call_id` 配对、终态明确 |
| 清理 | 删除测试会话/草稿/临时媒体 | 核对清理结果；正式数据符合预期 |
| 审计观察 | 全部上述分支 | audit/trace 与 SSE、数据库可对齐，敏感字段脱敏 |

## 三、46 个 READ_ONLY 工具执行卡（AG-F-TOOL-RO-*）

统一前提：真实登录账号 + 有/无数据两套夹具；`result_visualization` 只在已有真实查询结果后使用。以下“预期工具链与顺序”为理想基线，实际以 Provider 自主选择 + 服务端范围门为准（无依赖工具单独调用；有依赖工具先依赖后目标）。

| 编号 | 工具 | 输入提示词 | 预期工具链与顺序 | Loop/压缩 | SSE/响应 | DB/草稿/审计 | 边界重点 | 单项验收 |
|---|---|---|---|---|---|---|---|---|
| AG-F-TOOL-RO-001 | `account_balance_lookup` | “查看当前资金账户余额。” | 无依赖；单独调用 | 预算 2 轮；一般不压缩 | `tool_started/completed → answer_delta → answer_completed → run_completed` | 业务表不变；审计 returned_count；会话/消息新增 | 空账户=0；跨 owner 账户不可见；未登录/无权限 | 金额与 toolFacts 逐项一致；无业务写入 |
| AG-F-TOOL-RO-002 | `account_health_lookup` | “检查资金账户最近状态。” | 单独调用 | 同上 | 同上 | 同上 | window_days 边界（size 1/100/101、page -1） | 分页超限在执行前拒绝；状态不越权 |
| AG-F-TOOL-RO-003 | `account_transfer_lookup` | “查看最近账户转账记录。” | 单独调用 | 同上 | 同上 | 同上 | 默认上限与排序稳定 | 转账事实真实；不写业务表 |
| AG-F-TOOL-RO-004 | `anomaly_alert_lookup` | “扫描最近生意异常（销售下滑/缺货/欠款）。” | 单独调用 | 同上 | 同上 | 同上 | alert_type 枚举逐项与非法值；7 日窗口 | 告警可回溯；枚举外值拒绝 |
| AG-F-TOOL-RO-005 | `cash_change_lookup` | “列出最近资金变动。” | 单独调用 | 同上 | 同上 | 同上 | 默认条数上限与倒序 | 资金事实真实 |
| AG-F-TOOL-RO-006 | `cashflow_summary_lookup` | “统计最近 30 天现金流。” | 单独调用 | 同上 | 同上 | 同上 | 反向日期/同日/极端时间戳 | 净额与 facts 一致；日期倒置拒绝或明确语义 |
| AG-F-TOOL-RO-007 | `cross_analysis_lookup` | “综合看销售、采购和库存。” | 单独调用 | 同上 | 同上 | 同上 | dimension 枚举/天数越界 | 只查请求维度；回答注明窗口 |
| AG-F-TOOL-RO-008 | `customer_directory_lookup` | “列出客户目录（含状态和联系方式）。” | 单独调用 | 同上 | 同上 | 同上 | keyword 空/命中/长篇；列表上限 | 仅当前 owner；日志电话号码脱敏 |
| AG-F-TOOL-RO-009 | `customer_profile_lookup` | “查看某客户整体情况（订单/收款/退货）。” | 单独调用 | 同上 | 同上 | 同上 | customer_id 与 keyword 双筛优先级；关联上限 50 | 不因客户端 ID 绕过 owner |
| AG-F-TOOL-RO-010 | `customer_receivable_lookup` | “按优先级列出客户欠款。” | 单独调用 | 同上 | 同上 | 同上 | status/group_id 不存在值 | 金额与 facts 一致；排序稳定 |
| AG-F-TOOL-RO-011 | `data_export_tool` | “先查看可导出的销售字段和数量（不下载）。” | 单独调用 | 同上 | 同上 | 同上 | 枚举遍历 sales/.../finance × csv/json；负/超大 days | 未授权数据不返回；临时文件清理 |
| AG-F-TOOL-RO-012 | `finance_record_lookup` | “查看近期收入支出流水（按类别）。” | 单独调用 | 同上 | 同上 | 同上 | type/时间戳/反向日期 | 界内日期边界明确 |
| AG-F-TOOL-RO-013 | `generate_poster_prompt` | “拿这个商品写海报提示词，先不要生成图片。” | `product_catalog_lookup` → `generate_poster_prompt`（依赖链，先拿到真实 product_id） | 预算 2 轮；依赖后目标 | 两个工具事件成对 | 商品表不变；审计记录依赖 call_id | 无 product_id、0/负数、无依赖事实 | 目标工具不早于依赖执行；提示词引用当前 owner 商品 |
| AG-F-TOOL-RO-014 | `import_job_lookup` | “查看数据导入任务状态。” | 单独调用 | 同上 | 同上 | 任务表只读 | status 存在/空/不存在 | 用户结果受 owner 隔离；错误详情脱敏 |
| AG-F-TOOL-RO-015 | `inventory_adjustment_lookup` | “查看最近库存调整（盘盈盘亏）。” | 单独调用 | 同上 | 同上 | 库存/流水不变 | 日界、反向日期 | 只读无库存变更 |
| AG-F-TOOL-RO-016 | `inventory_ledger_lookup` | “查看库存出入库流水。” | 单独调用 | 同上 | 同上 | 同上 | product_id/日期/source_type | 跨 owner 商品不可见 |
| AG-F-TOOL-RO-017 | `inventory_low_stock_lookup` | “列出低库存商品。” | 单独调用 | 同上 | 同上 | 同上 | limit 缺省/1/最大/0/负/超大 | 不超过上限；不触发补货动作 |
| AG-F-TOOL-RO-018 | `inventory_panorama_lookup` | “查看库存全貌（安全库存/销量/周转/补货建议）。” | 单独调用 | 同上 | 同上 | 同上 | product_id/keyword/limit 类型 | 建议可解释；空数据不生成图表 |
| AG-F-TOOL-RO-019 | `inventory_snapshot_lookup` | “查看库存盘点快照。” | 单独调用 | 同上 | 同上 | 同上 | snapshot_date 边界 | 快照事实不可被现库存替代 |
| AG-F-TOOL-RO-020 | `partner_contact_lookup` | “查客户和供应商联系人。” | 单独调用 | 同上 | 同上 | 同上 | partner_type/partner_id | 电话脱敏；不跨 owner |
| AG-F-TOOL-RO-021 | `partner_group_lookup` | “查看客户和供应商分组。” | 单独调用 | 同上 | 同上 | 同上 | customer/supplier/空/非法 | 类型过滤准确 |
| AG-F-TOOL-RO-022 | `pay_order_lookup` | “查看最近付款单。” | 单独调用 | 同上 | 同上 | 付款单只读 | status/日期边界 | 只读不创建付款 |
| AG-F-TOOL-RO-023 | `payment_lookup` | “查看收付款记录。” | 单独调用 | 同上 | 同上 | 同上 | order_id 跨 owner | 不能借 order_id 读他人数据 |
| AG-F-TOOL-RO-024 | `product_catalog_lookup` | “查看商品、库存、价格和分类。” | 单独调用；也是多创建工具依赖源 | 同上 | 同上 | 商品/库存只读 | status/category_id/unit_id；不传 0 约定 | 作为依赖输出真实可引用 ID |
| AG-F-TOOL-RO-025 | `product_category_lookup` | “查看商品分类。” | 单独调用 | 同上 | 同上 | 同上 | 空分类 | 不泄露其他 owner 分类 |
| AG-F-TOOL-RO-026 | `product_price_level_lookup` | “查看商品价格等级。” | 单独调用 | 同上 | 同上 | 同上 | 默认上限 | 只读 |
| AG-F-TOOL-RO-027 | `product_supplier_relation_lookup` | “查看商品对应供应商和采购价。” | 单独调用 | 同上 | 同上 | 同上 | product_id 0/负/空 | 供应关系不跨 owner |
| AG-F-TOOL-RO-028 | `purchase_order_lookup` | “查看采购单和到货情况。” | 单独调用；入库/退货依赖源 | 同上 | 同上 | 采购单只读 | 非法状态 | 后续创建只引用本次可见单 |
| AG-F-TOOL-RO-029 | `purchase_receipt_lookup` | “查看采购入库。” | 单独调用 | 同上 | 同上 | 入库/库存只读 | purchase_order_id 过滤 | 只读不增加库存 |
| AG-F-TOOL-RO-030 | `purchase_return_lookup` | “查看采购退货。” | 单独调用 | 同上 | 同上 | 同上 | 关联采购单过滤 | 只读不触发退货 |
| AG-F-TOOL-RO-031 | `purchase_tracking_lookup` | “串起采购、入库、退货链路。” | 单独调用 | 同上 | 同上 | 三表只读 | order_id 跨 owner | 链路 ID 均属当前 owner；缺失阶段不补造 |
| AG-F-TOOL-RO-032 | `receivable_payable_lookup` | “汇总客户欠款和供应商应付款。” | 单独调用（聚合工具优先） | 同上 | 同上 | 客户/供应商/财务只读 | 空数据总额=0 | 应收应付分别归类；不越权 |
| AG-F-TOOL-RO-033 | `report_query` | “查看本月销售汇总（经营汇总）。” | 单独调用 | 同上 | 同上 | 报表口径只读 | report_type 8 枚举 + period 月/季/年 + 非法 | 缺 report_type 在 Repository 前拒绝 |
| AG-F-TOOL-RO-034 | `result_visualization` | “把刚才的销售结果用图表示。” | 先有真实查询工具结果 → `result_visualization`（展示决策） | 依赖真实 facts；无 facts 不调用 | `tool_skipped(visualization_requires_new_real_facts)` 或成功 + result_block | 业务表不变 | mode 非法；单独调用无上游 facts；空数据 | 图表逐点对应 facts；空数据不生成图表 |
| AG-F-TOOL-RO-035 | `sale_order_lookup` | “查看销售单和收款情况。” | 单独调用；销售链路/退货事实源 | 同上 | 同上 | 销售/收款/库存只读 | min/max_total 反向；payment_status | min/max 过滤准确；无库存变更 |
| AG-F-TOOL-RO-036 | `sales_full_chain_lookup` | “串起销售单、收款、退货。” | 单独调用 | 同上 | 同上 | 三表只读 | order_id 归属 | 缺阶段不补造 |
| AG-F-TOOL-RO-037 | `sales_overview_lookup` | “看最近一周销售总览。” | 单独调用 | 同上 | 同上 | 销售/收款只读 | window_days 默认/0/负/反向日期 | 汇总与明细交叉核对 |
| AG-F-TOOL-RO-038 | `sales_return_lookup` | “查看销售退货。” | 单独调用 | 同上 | 同上 | 退货/库存只读 | original_order_id 跨 owner | 只读不冲减库存 |
| AG-F-TOOL-RO-039 | `sales_trend_lookup` | “查看近一个月每天销售趋势。” | 单独调用 | 同上 | 同上 | 销售只读 | window_days 1/365/366；bucket 枚举；桶数≤120 | 超 365 预拒绝；桶无重复跳序 |
| AG-F-TOOL-RO-040 | `smart_restock_lookup` | “给我库存补货建议。” | 单独调用 | 同上 | 同上 | 商品/库存只读 | category_id/limit | 只给建议不产生采购单 |
| AG-F-TOOL-RO-041 | `store_info_lookup` | “查看当前门店信息和成员数量。” | 单独调用 | 同上 | 同上 | 门店/成员只读 | store_id 指定他人门店 | 客户端 store_id 不能改变会话作用域 |
| AG-F-TOOL-RO-042 | `supplier_directory_lookup` | “列出供应商目录。” | 单独调用；付款/采购依赖源 | 同上 | 同上 | 供应商只读 | 联系方式脱敏 | 后续创建只引用可见供应商 |
| AG-F-TOOL-RO-043 | `supplier_payable_lookup` | “列出供应商应付款。” | 单独调用 | 同上 | 同上 | 同上 | status/group_id | 应付可追溯；不触发支付 |
| AG-F-TOOL-RO-044 | `supplier_statement_lookup` | “和供应商对账（余额/采购/付款/退货）。” | 单独调用 | 同上 | 同上 | 四表只读 | supplier_id/keyword | 余额口径明确；不变成支付 |
| AG-F-TOOL-RO-045 | `sync_status_lookup` | “查看数据同步状态。” | 单独调用 | 同上 | 同上 | 同步任务只读 | 非对象参数 | 不执行同步 |
| AG-F-TOOL-RO-046 | `web_search_lookup` | “搜索最近的库存管理建议，列出标题、摘要和来源。” | `web_search_lookup` → `WebSearchProvider` | 无业务依赖 | 同上或 Provider 错误 | 业务表不变 | query required；result_limit≤10；recency/domains；恶意 URL；Provider 未配置 | Provider 未配置记 `Blocked`；可用时来源/摘要/链接一致；恶意 URL 拒绝 |

## 四、14 个 CREATE_ONLY 工具执行卡（AG-F-DRAFT-CO-*）

统一流程：依赖查询（按 2.2 表）→ CREATE_ONLY 工具 → `agent_drafts(status=active)` → APP 覆盖式确认弹窗 → 分支执行。11 分支见第二节；正式写入只发生在确认后，拒绝/失败不得改变正式业务表。

| 编号 | 工具 | 输入提示词 | 预期工具链与顺序 | 草稿/正式表变化 | 边界重点 | 单项验收 |
|---|---|---|---|---|---|---|
| AG-F-DRAFT-CO-001 | `create_account_transfer` | “在两个资金账户之间转 1.23 元，先给我确认。” | `account_balance_lookup`(若缺 ID) → 工具草稿 | 草稿 active；拒绝 cancelled 且余额/流水不变；确认后 1 笔转账并 confirmed；重复确认 0 笔 | 同 key/并发确认唯一结果；缺 required | 拒绝 0 变化、确认 1 笔、重复 0 笔 |
| AG-F-DRAFT-CO-002 | `create_customer` | “新增客户全量工具测试客户，电话 13900000001，先确认。” | 组可选 `partner_group_lookup` → 工具草稿 | 草稿；确认 1 条；重复 0；冲突按 owner 唯一性稳定失败且草稿可重试 | name required、电话唯一冲突 | 正式表准确一条；电话日志脱敏 |
| AG-F-DRAFT-CO-003 | `create_finance_record` | “记一笔收入 1.23 元，先做成草稿。” | 工具草稿 | 草稿；确认 1 条流水；重复 0；失败回滚 | type/amount required；账户不存在 | 金额、类型、账户一致 |
| AG-F-DRAFT-CO-004 | `create_inventory_adjustment` | “把现有商品库存加 1 件，先做调整草稿。” | `product_catalog_lookup`(若缺) → 工具草稿 | 草稿；确认产生调整并改库存；重复 0；quantity=0 预拒绝 | 数量 0/超限；并发确认无双重调整 | 库存差异可解释 |
| AG-F-DRAFT-CO-005 | `create_inventory_count_draft` | “按现在库存做一次盘点，先确认。” | `product_catalog_lookup` → `inventory_snapshot_lookup` → 工具草稿（draftType=create_inventory_adjustment） | 草稿；确认走库存调整链；快照过期/跨 owner 拒绝 | 两依赖必须完成；客户端自报库存无效 | 确认路由与库存调整一致后再写入 |
| AG-F-DRAFT-CO-006 | `create_pay_order` | “给供应商记 1.23 元付款，先别直接付。” | `supplier_directory_lookup` → 工具草稿 | 草稿不建付款单、不扣余额；确认 1 单；重复 0；同 key 不同 payload 409 | 幂等键；并发唯一 | 唯一约束竞争下有且仅一张 |
| AG-F-DRAFT-CO-007 | `create_product` | “新增商品 EVAL-ONLY-20260802，先生成草稿。” | 工具草稿（分类可选） | 草稿；确认 1 条；重复 0；编码冲突稳定 4xx | code/name required；编码唯一 | owner-scoped code 唯一 |
| AG-F-DRAFT-CO-008 | `create_purchase_order` | “向现有供应商买一个真实商品，数量 1、单价 1.23，先做采购草稿。” | `supplier_directory_lookup` → `product_catalog_lookup` → 工具草稿 | 草稿不建采购单不增库存；确认 1 单+明细；事务失败全回滚 | items minItems=1；依赖过期失败不半写 | 依赖顺序、数组校验、事务边界 |
| AG-F-DRAFT-CO-009 | `create_purchase_receipt` | “把采购单里的 1 件货做入库，先生成草稿。” | `purchase_order_lookup` → 工具草稿 | 草稿不入库；确认写入库+库存；超可入库量失败 | 数量超过可入库量；并发 | 正式库存与入库明细一致 |
| AG-F-DRAFT-CO-010 | `create_purchase_return` | “把采购来的货退 1 件，先做退货草稿。” | `purchase_order_lookup` → 工具草稿 | 草稿不写退货；确认写退货+按规则改库存 | 超可退量 | 可退量校验有效 |
| AG-F-DRAFT-CO-011 | `create_sale_order` | “给现有客户开一单，商品 1 件、单价 1.23，先生成销售草稿。” | `customer_directory_lookup` → `product_catalog_lookup` → 工具草稿 | 草稿不建单不扣库存；确认建单+扣库存；库存不足失败且回滚 | 库存不足；items 校验；并发无重复 | 工具/回答/审计/库存变化一致 |
| AG-F-DRAFT-CO-012 | `create_sales_return` | “把一张销售单退 1 件，先做草稿。” | `sale_order_lookup` → 工具草稿 | 草稿不写；确认写退货+按规则回补库存 | 超可退量 | 明细与回补一致 |
| AG-F-DRAFT-CO-013 | `create_supplier` | “新增供应商 全量工具测试供应商，电话 13900000002，先确认。” | 工具草稿（分组可选） | 草稿；确认 1 条；重复 0；冲突稳定错误 | name required | owner 唯一；无跨域 |
| AG-F-DRAFT-CO-014 | `media_upload_tool` | “上传 all-tools-eval.txt（16 字节、文本），先生成上传意图草稿。” | 工具草稿 | 只生成上传意图草稿；拒绝不创建媒体；确认后由前端续传（后端仅确认不落资产） | 文件名/大小/MIME/跨 owner 绑定 | 不存在未确认媒体资产；确认行为按前端续传流程实测 |

## 五、多工具组合场景（AG-F-MULTI-*）

| 编号 | 输入提示词 | 目标工具集合与顺序 | Loop/展示 | 验收 |
|---|---|---|---|---|
| AG-F-MULTI-001 | “最近一周销售和回款帮我看一下，合适的话用图表示。” | `sales_overview_lookup` → `result_visualization`（真实 facts 后） | 续轮仅展示决策 | 先有真实结果再有图表；空数据不画图 |
| AG-F-MULTI-002 | “最近一周销售和现金流放在一起看下，合适的话用图展示。” | `sales_overview_lookup` → `cashflow_summary_lookup` → `result_visualization` | 显式多来源续轮 | 两个事实来源都可追溯；一次终态 |
| AG-F-MULTI-003 | “库存和补货一起帮我看，哪些要马上补？” | `inventory_panorama_lookup` → `smart_restock_lookup` →(可选) `result_visualization` | 两个目标都查完才进入展示 | 图表数据来自两支真实结果 |
| AG-F-MULTI-004 | “客户欠款和供应商应付款一起算一下，重点对象用表格列出来。” | `receivable_payable_lookup`（聚合优先）或 `customer_receivable_lookup`+`supplier_payable_lookup` → `result_visualization` | 聚合工具优先 | 不重复扫描；表格与查询一致 |
| AG-F-MULTI-005 | “销售趋势和回款一起看按天对比。” | `sales_trend_lookup` → `payment_lookup` →(可选) `result_visualization` | `isSalesTrendAndReceivableRequest` 专用候选集 | 两来源都执行；不混入无关工具 |
| AG-F-MULTI-006 | 依赖缺失场景（创建销售单但模型跳过依赖查询） | 只执行创建工具 | 范围门拒绝依赖缺失（若参数缺失）或允许（参数已齐） | 不产生正式写入；回答不声称已保存 |

## 六、Loop 六终态用例（AG-F-LOOP-*）

| 编号 | 场景/输入 | Loop 行为 | 预期 SSE/终态 | 边界 |
|---|---|---|---|---|
| AG-F-LOOP-001 | 只读完成：单查询足够回答 | 预算 2 轮内收敛，模型返回终止文本 | `run_completed(COMPLETED)` | 不得无界继续查询 |
| AG-F-LOOP-002 | 查询后生成草稿：依赖→创建 | 预算 4-5 轮；绑定脚本化两次 | `draft_created → run_completed(CONFIRMATION_PENDING)` + 状态后缀 | 目标完成即停 |
| AG-F-LOOP-003 | 失败：目标写工具未完成且预算耗尽 | `EXHAUSTED` | `run_exhausted`（completed/missing tools 如实列出） | 不伪装成功；不写入正式表 |
| AG-F-LOOP-004 | 模型工具选择失败 | 首轮 plan `model_tool_selection_failed` | 全量 `tool_skipped` + `run_failed(MODEL_TOOL_SELECTION_FAILED)` | 不执行任何工具 |
| AG-F-LOOP-005 | 取消：流中调 cancel | `ensureRunActive` 抛取消异常，收敛收尾 | `run_cancelled`，后续不再有 answer_delta | 取消不覆盖 audit 为 failed（见 AG-R-*） |
| AG-F-LOOP-006 | Blocked：安全拦截 | 首轮 SafetyGuard 拦截 | `run_blocked(BLOCKED)`，tool_count=0 | 回答为安全文案，非业务成功 |

## 七、上下文压缩专项（AG-F-CTX-001~014，基线见 代码事实基线.md 7 节）

| 编号 | 场景 | 边界 | 预期 |
|---|---|---|---|
| AG-F-CTX-001 | 历史未超预算 | 预算足够 | 不调用压缩 Provider；不生成检查点 |
| AG-F-CTX-002 | 超阈值且有完整旧轮次 | ≥2 个已完成 user/assistant 对 | 选择最早完整轮次压缩 |
| AG-F-CTX-003 | 当前问题超预算 | 当前问题本身超限 | 不静默截断；稳定 4xx 或 `EXHAUSTED` |
| AG-F-CTX-004 | 当前轮未完成工具 | tool call 未完成 | 未完状态不进摘要 |
| AG-F-CTX-005 | 待确认草稿 | `AWAITING_CONFIRMATION` | 草稿 ID/类型/状态/目标动作保留 |
| AG-F-CTX-006 | 语义压缩成功 | 摘要结构合法 | 保存边界/版本/预算/质量 |
| AG-F-CTX-007 | 语义压缩超时/失败 | 20s 超时 | 使用确定性摘要；旧检查点不被覆盖 |
| AG-F-CTX-008 | 语义输出非法 JSON | 结构校验失败 | 标记失败；不注入无效摘要 |
| AG-F-CTX-009 | 检查点复用 | 边界与 owner 一致 | 边界后原始轮次顺序正确 |
| AG-F-CTX-010 | 消息编辑/删除 | 影响已压缩边界 | `invalidateAfterBoundary` 失效并重建 |
| AG-F-CTX-011 | 并发压缩 | 同会话同边界 | 唯一有效检查点；冲突回退读已提交 |
| AG-F-CTX-012 | 跨 owner 读取检查点 | A 读 B | 拒绝或空结果 |
| AG-F-CTX-013 | 压缩事件展示 | Web/Android/iOS | 展示条数/边界/原因；不展示敏感原文 |
| AG-F-CTX-014 | 压缩后工具续轮 | 压缩后继续调用 | native tool call 与 tool result 仍按 call_id 配对 |

## 八、多模态、记忆、海报、搜索与结果块专项

| 编号 | 场景 | 输入 | 预期 | 边界 |
|---|---|---|---|---|
| AG-F-MULTIMODAL-001 | 图片直答（非流式） | `image_asset_ids` 1 张 + 文字 | `multimodal_direct`/`multimodal_direct_llm`；回答基于图片可见事实 | 不伪装成系统业务数据 |
| AG-F-MULTIMODAL-002 | 图片直答（流式） | 同上走 stream | `answer_delta(model_stream)`、`answer_completed`；非流式重试降级路径 | Provider 不可用时 `llm_answer_unavailable` |
| AG-F-MULTIMODAL-003 | 图片数量边界 | 0/1/9/10 张；非图片 MIME；跨 owner asset | ≤9 校验；非法拒绝；不读取他人资产 | 9 张上限稳定 |
| AG-F-MULTIMODAL-004 | 文本生图 | `POST /v2/agent/images/generate` | `image_url,revised_prompt` 或稳定 Provider 错误 | reference_asset_ids 当前 owner；临时资产清理 |
| AG-F-MEMORY-001 | 记忆配置关闭 | `agent.memory.enabled=false` | 不召回、不提取、不落库 | 不阻塞主回答 |
| AG-F-MEMORY-002 | 记忆召回注入 | 已有记忆 + 新问题 | 上下文带“历史记忆（仅供参考，非实时业务数据）” ≤3 条；不混淆实时业务 | 召回失败仅丢弃记忆块 |
| AG-F-MEMORY-003 | 异步提取落库 | 回答完成后 | `agent_memories` 按 sourceMessageId 去重更新（conf=0.3、status=active） | 失败不阻塞主回答 |
| AG-F-MEMORY-004 | 记忆脱敏 | 输入含手机号/邮箱/身份证 | 落库为 `[REDACTED]` | 敏感模式扫描命中 0 |
| AG-F-MEMORY-005 | 记忆 owner 隔离 | A 查 B 的记忆详情/删除 | 拒绝或空 | 删除只作用自己的记忆 |
| AG-F-POSTER-001 | 海报依赖链 | “拿商品 X 写海报提示词” | `product_catalog_lookup` → `generate_poster_prompt`；消息含“海报”登记目标 | 无真实 product_id 不产出臆造文案 |
| AG-F-SEARCH-001 | 在线搜索摘要 | “搜索库存管理建议，附来源” | Provider 可用时 title/summary/safe URL/引用；未配置记 `Blocked` | URL 安全；结果上限 |
| AG-F-RESULT-001 | KPI 结果块 | 有真实查询 + 展示要求 | `kpi_grid` 数据与 facts 一致；空 KPI 不显示 | 块顺序与消息一致 |
| AG-F-RESULT-002 | 表格/排行 | 同上 | `table/rank_list` 字段、顺序、总数一致 | 空数据空状态 |
| AG-F-RESULT-003 | 图表 | 同上 | 图表逐点对应 facts；`requestedVisualizationMode` 过滤模式 | 无数据不画假趋势 |
| AG-F-RESULT-004 | 草稿卡片 | 创建类 | `draft/draft_card` 恒展示；确认/拒绝入口 | 未确认无“已保存”文案 |

## 九、会话/消息/草稿/工作台/任务/通知功能侧断言（AG-F-API-*）

API 契约细节见 [../契约/TEST_PLAN.md](../契约/TEST_PLAN.md)，本节只列功能侧业务断言（非流式即流式入口各一遍）。

| 编号 | 目标 | 功能断言 |
|---|---|---|
| AG-F-API-CONV-001 | 会话生命周期 | 创建→续聊→列表→详情→恢复→删除→重复删除 闭合；消息与 audit 的 conversation_id 一致 |
| AG-F-API-CONV-002 | 显式 conversation_id | 走已有会话；跨 owner 会话不可用 |
| AG-F-API-DRAFT-001 | 草稿待办 | `drafts/pending` 只返回 active；确认/取消后从待办消失 |
| AG-F-API-DRAFT-002 | 草稿编辑 | active 可改；confirmed 不可改；确认弹窗展示最新版 |
| AG-F-API-WB-001 | 工作台 | 最近会话、待办、风险/警告字段与当前 owner 一致；空数据有明确空态 |
| AG-F-API-TASK-001 | 任务列表 | progress/status/result_json 当前 owner；结果 JSON 可解析 |
| AG-F-API-NOTIF-001 | 通知读取 | unread_only 过滤；标记已读仅改目标通知 |
| AG-F-API-AUDIT-001 | run 审计读取 | `events` seq 单调、event_id 唯一、与 SSE 一致；`audit_lossy` 告警可解释 |

## 十、完整字段样例（两条代表用例，其余用例按第二节字段表执行）

### 样例 1：AG-F-TOOL-RO-024 product_catalog_lookup（成功有数据 · 流式）

| 字段 | 内容 |
|---|---|
| test_id / category_id | AG-F-TOOL-RO-024 / F |
| test_objective | 验证商品目录查询在流式路径返回真实事实并被结果块引用 |
| preconditions | 登录测试账号；当前 owner 存在 ≥2 个商品；Provider 已配置；服务与 APP 版本已记录 |
| input | “现在有哪些商品，各自库存和价格是多少？” |
| expected_tools | `product_catalog_lookup`（无允许的依赖工具） |
| expected_order | run_started → plan_delta(可选) → tool_started → tool_completed → answer_delta → answer_completed → result_block(可选) → run_completed |
| loop_and_compaction | 预算 2 轮内收敛；历史未超阈值时不压缩 |
| expected_response | 完整系列 SSE；`tool_completed` 含 returned_count/total_count/limit/is_truncated；终态唯一 |
| expected_answer | 列出真实商品数、库存合计、低库存数；数字与 toolFacts 一致；空数据则如实说明 |
| db_changes | 商品/库存表不变；agent_messages、agent_run_audits、agent_run_audit_events 新增本 run 记录 |
| boundaries | 无数据、非法参数（含未知字段）、未登录、无权限、跨 owner 商品 ID、limit 边界、重复请求、清理 |
| acceptance | 目标工具唯一执行；回答全部数字可追溯；业务表差异为 0；审计与 SSE 对齐 |
| evidence_path | `功能/artifacts/<日期>-<波次>/03-raw-sse.log`、04-tool-trace.jsonl、05-run-audit.json、06/07-database-before/after.json |
| result | Deferred |

### 样例 2：AG-F-DRAFT-CO-006 create_pay_order（草稿/确认/幂等）

| 字段 | 内容 |
|---|---|
| test_id / category_id | AG-F-DRAFT-CO-006 / F |
| test_objective | 验证付款草稿生成与二次授权闭环，及幂等键冲突语义 |
| preconditions | 登录账号含 ≥1 供应商与 ≥1 资金账户；Provider 已配置；服务端幂等键可用 |
| input | “给供应商 A 付款 1.23 元，备注全量工具测试，先别直接付，做成草稿。” |
| expected_tools | `supplier_directory_lookup`（依赖）→ `create_pay_order` |
| expected_order | tool_started(supplier_directory_lookup) → tool_completed → 续轮 plan → tool_started(create_pay_order) → tool_completed → draft_created → run_completed(CONFIRMATION_PENDING) |
| loop_and_compaction | 预算 4 轮；写目标完成即停；压缩按 7 节规则（本轮无历史时一般不触发） |
| expected_response | 非流式：`AgentChatResponse` 含 draftId、terminal_status=CONFIRMATION_PENDING、状态后缀；流式同序列 |
| expected_answer | “已生成付款草稿，等待确认”；不声称已付款 |
| db_changes | 草稿阶段仅 agent_drafts 增 active 行；确认后 v2 pay_order + 支付链路 + draft=confirmed；拒绝 draft=cancelled 且零业务写入；同 key 不同 payload 409 |
| boundaries | 缺 required(supplier_id/supplier_name/amount)、跨 owner 供应商、重复确认、并发确认、确认失败保持 active、清理 |
| acceptance | 拒绝 0 变化、确认 1 单、重复 0 单；500=0；审计含确认者与正式业务 ID |
| evidence_path | `功能/artifacts/<日期>-<波次>/` 02-http-response.json、04-tool-trace.jsonl、05-run-audit.json、06/07-db、09-cleanup.json |
| result | Deferred |

## 十一、证据存放

- 每条用例：`功能/artifacts/<日期>-<波次>-<用例>/` 按 README 第五节文件序列。
- 服务端观察：`功能/logs/`（服务日志、查询集）。
- APP 观察：`功能/reports/` 或由 客户端/ 设备测试提供。
- 执行脚本：`../脚本/功能/`。
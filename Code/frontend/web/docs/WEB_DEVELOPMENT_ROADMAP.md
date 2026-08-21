# 智慧记 Web PC 管理端开发总控文档

## 1. 文档目标

本文件是 Web 端后续开发的唯一执行文档，目标不是泛泛描述需求，而是直接指导开发者：

- 以 Stitch MCP 导出的 PC 设计稿为主真源继续开发。
- 在不触碰安卓端与后端代码的前提下，完成 Web 管理端页面的 UI 一比一复刻。
- 保持页面能力与后端真实接口、安卓端业务语义严格对齐。
- 明确每一张页面的主参考稿、辅助参考稿、代码入口、禁止偏离项与验收方式。

一句话原则：

- 业务能力以后端接口和安卓端语义为准。
- 视觉与布局以 Stitch MCP 的 PC 导出稿为准。
- 没有 PC 稿的页面，才允许降级参考移动稿。

---

## 2. 当前目录与真源位置

当前仓库中与 Web 开发直接相关的目录如下：

- 后端：`Code/backend/src/`
- 安卓：`Code/frontend/android/`
- Web：`./`
- Web 路由：`./src/app/router/routes.ts`
- Stitch 页面元数据：`./src/app/router/stitch-screens.ts`
- Web API 客户端：`./src/shared/api/client.ts`
- Web 契约目录：`./src/shared/api/contracts.ts`
- Web 全局样式：`./src/style.css`

设计资源目录：

- PC 主设计稿目录：
  `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/`
- 移动/补充设计稿目录：
  `./public/stitch_exports/visual-design_system_framework_14840154594131085259/`

说明：

- `zhihuiji_web_pc_admin_mcp_17989845462303116064` 是 Web 管理端的主设计真源。
- `visual-design_system_framework_14840154594131085259` 不是 Web 的主稿，只能作为无 PC 稿页面的补充参考。
- Manifest 中部分绝对路径仍保留旧位置写法，但本地实际生效资源位置以 `Code/frontend/web/public/stitch_exports/...` 为准。

---

## 3. 设计真源分级

### 3.1 一级真源：Stitch MCP PC 导出

文件：

- `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/README.md`
- `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/manifest.tsv`
- `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/*.png`
- `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/*.html`

用途：

- 决定页面整体布局
- 决定导航结构
- 决定标题区、工具栏、表格、详情侧栏、表单布局
- 决定字体层级、留白、按钮位置、信息密度
- 决定 PC 页面整体视觉基调

执行要求：

- 如果某页面存在 PC 导出稿，开发时必须优先参照其 `image + html`。
- 不允许用“按感觉优化”的方式替代真稿。
- 不允许因为已有前端代码结构不同，就擅自重排页面布局。

### 3.2 二级参考：移动/极光稿

文件：

- `./public/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv`
- 同目录下对应 `images/*.png` 与 `html/*.html`

用途：

- 补足无 PC 稿页面的视觉方向
- 参考某些单独控件、信息块、卡片内容
- 参考收款、退货、AI、报表等当前没有 PC 正稿的页面

执行要求：

- 只有在 PC 稿缺失时，才允许引用移动稿。
- 引用移动稿时，必须先做 PC 化改写，不能把移动布局直接搬到桌面。

### 3.3 三级参考：已有前端实现

文件：

- `./src/pages/**`
- `./src/style.css`

用途：

- 承接已有真实接口联调逻辑
- 保留路由、状态、表单、交互能力

执行要求：

- 现有实现是业务骨架，不是视觉真源。
- 页面已有代码只能决定“怎么接接口”，不能决定“长什么样”。

---

## 4. 总体开发边界

本轮 Web 开发必须遵守以下边界：

- 不修改安卓端代码。
- 不修改后端代码。
- 不因为 UI 复刻而新增后端接口。
- 不为了偷快引入新 UI 框架。
- 不引入新的图表库、低代码库或整套设计系统库。
- 保持 Vue 3 + Vite + TypeScript + 当前样式体系。

允许做的事：

- 修改 `Code/frontend/web/src/pages/**`
- 修改 `Code/frontend/web/src/style.css`
- 新增 Web 内部共享 UI 组件
- 微调路由映射与前端页面组织
- 增补 Web 文档

---

## 5. 当前页面设计资源对照表

以下页面已经有 PC Stitch 真稿，必须优先做到一比一复刻。

### 5.1 第一优先级：已有 PC 真稿页面

1. `经营首页 Dashboard`
- 路由：`/dashboard`
- 代码：`./src/pages/dashboard/DashboardPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/05_fd733d8a7ada48cea2f3f567417ce9e2.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/05_fd733d8a7ada48cea2f3f567417ce9e2.html`
- 开发要求：
  - 左侧导航、顶部筛选、指标卡、趋势图、风险面板、动态列表、AI 助手块全部按 PC 稿重排。
  - 当前只允许在卡片数据内容上使用真实 API；布局、尺寸、排序以 PC 稿为准。

2. `销售单列表`
- 路由：`/documents/sales`
- 代码：`./src/pages/documents/SalesOrderListPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/10_778c9991ab44444d978da4b2a28bd2a3.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/10_778c9991ab44444d978da4b2a28bd2a3.html`
- 开发要求：
  - 顶部搜索、时间筛选、状态筛选、表格列结构、右上主操作按钮、表格密度都按稿。
  - 不允许保留当前“左表右摘要”布局，必须切回 PC 稿的主列表形态。

3. `销售单新建/编辑`
- 路由：`/documents/sales/edit`
- 代码：`./src/pages/documents/SalesOrderEditPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/09_6d40c074a1284479a453f5a32a603618.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/09_6d40c074a1284479a453f5a32a603618.html`
- 开发要求：
  - 基本信息区、商品明细表、底部金额栏、保存草稿/保存并出库按钮区必须复刻。
  - 表单网格、字段顺序、行项工具栏必须以稿为准。

4. `销售单详情`
- 路由：`/documents/sales/detail`
- 代码：`./src/pages/documents/SalesOrderDetailPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/08_a3f41bc2a6e5494f8538d16bb739d8e4.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/08_a3f41bc2a6e5494f8538d16bb739d8e4.html`
- 开发要求：
  - 页面结构必须改为 PC 稿中的详情主卡 + 进度线 + 明细表 + 右侧信息块。
  - 当前收款块可保留业务能力，但布局不能继续沿用旧版详情样式。

5. `采购单列表`
- 路由：`/documents/purchases`
- 代码：`./src/pages/documents/PurchaseOrderListPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/06_595ffe36f46b4d478103fd4b63280706.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/06_595ffe36f46b4d478103fd4b63280706.html`
- 开发要求：
  - 搜索栏、筛选条、分页条、状态点、表格列、主按钮都按 PC 稿。
  - 当前列表 + 摘要双栏结构需要回收为 PC 稿主列表。

6. `采购单新建/编辑`
- 路由：`/documents/purchases/edit`
- 代码：`./src/pages/documents/PurchaseOrderEditPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/04_debb39738a0d4034affe3dbb4dd048de.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/04_debb39738a0d4034affe3dbb4dd048de.html`
- 开发要求：
  - 基本信息、商品明细、附加费用/附件区、底部结算栏全部复刻。
  - 当前表单如缺附件占位，也要先补 UI 壳，不等后端。

7. `采购单详情`
- 路由：`/documents/purchases/detail`
- 代码：`./src/pages/documents/PurchaseOrderDetailPage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/01_73ac0e15f5df49249ac2197064adeb46.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/01_73ac0e15f5df49249ac2197064adeb46.html`
- 开发要求：
  - 必须改成采购详情大卡 + 采购进度 + 明细表 + AI 洞察侧栏的结构。
  - “后续动作”块只能作为稿内辅助，不应主导整个布局。

8. `商品列表`
- 路由：`/archives/products`
- 代码：`./src/pages/archives/ProductArchivePage.vue`
- 设计资源：
  - Image: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/images/07_a5aaaa8f7f04438d84d1fdc749d6a5cb.png`
  - HTML: `./public/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064/html/07_a5aaaa8f7f04438d84d1fdc749d6a5cb.html`
- 开发要求：
  - 顶部筛选条、表格列、库存状态、右上按钮、分页条按稿复刻。
  - 当前左表右详情结构需要回收为标准主列表页。

### 5.2 第二优先级：只有移动稿、需 PC 化的页面

这些页面没有 PC 真稿，只能以移动稿为补充参考，并必须先做桌面化设计转换：

- `销售收款`
- `销售退货`
- `付款单详情`
- `采购入库`
- `采购退货`
- `库存调整`
- `库存盘点`
- `经营报表`
- `AI 助手`
- `客户档案`
- `供应商档案`
- `日常支出`
- `资金流水详情`

移动参考真源见：

- `./public/stitch_exports/visual-design_system_framework_14840154594131085259/manifest.tsv`

执行要求：

- 保留移动稿的信息块语义
- 重新排成桌面双栏 / 三栏 / 主表 + 详情 / 主内容 + 辅助栏结构
- 禁止直接做成放大版手机页面

---

## 6. 设计复刻执行规则

### 6.1 一比一复刻的定义

本项目中的“一比一复刻”不等于逐像素抄图，而是要求以下维度严格一致：

- 页面信息架构一致
- 版面分区顺序一致
- 栅格关系一致
- 表格列结构一致
- 操作按钮相对位置一致
- 标题、筛选、详情、摘要块的层级一致
- 主色、强调色、按钮样式、选中态风格一致
- 留白密度与信息密度接近原稿

允许的偏差只有：

- 真实接口字段导致的文案长度差异
- 动态数据导致的内容行数差异
- 浏览器字体渲染的微小差异
- 为兼容桌面响应式而做的极小尺寸调整

### 6.2 明确禁止项

以下行为视为未按设计稿开发：

- 用现有业务页布局“凑合接近”设计稿
- 用移动稿主导已有 PC 真稿页面
- 把表格页改成卡片流
- 把详情页改成自定义摘要页
- 擅自更换主导航结构
- 擅自增加设计稿中没有的大面积渐变、玻璃、装饰块
- 擅自改变按钮主次关系
- 擅自改变筛选区、表格区、右侧摘要区的上下顺序

### 6.3 HTML 参考优先级

对每张已有 PC 稿页面，必须同时查看：

1. `images/*.png`
2. `html/*.html`

使用规则：

- `png` 用来核对总视觉和布局
- `html` 用来确认具体块结构、字段顺序、间距和组件层级

开发时如果 `png` 与现有实现冲突，以 `png + html` 为准。

---

## 7. 页面开发顺序

后续开发必须按以下顺序推进：

### 阶段 A：先统一壳层

目标：

- 先让整个 Web 的全局壳与 PC Stitch 稿一致

范围：

- `AppLayout.vue`
- `style.css`
- 全局导航
- 顶部栏
- panel / table / button / filter / metric card 基础样式

完成标准：

- 任意一张有 PC 真稿的页面，进入后不会先被“旧样式壳”破坏整体观感

### 阶段 B：完成 8 张 PC 真稿页面

顺序固定为：

1. Dashboard
2. 销售单列表
3. 销售单编辑
4. 销售单详情
5. 采购单列表
6. 采购单编辑
7. 采购单详情
8. 商品列表

每完成一张页面，必须：

- 对照真稿截图复检
- 更新本文件中的完成状态
- 本地构建通过后再进入下一张

### 阶段 C：将无 PC 稿页面桌面化

顺序建议：

1. 客户档案
2. 供应商档案
3. 销售收款
4. 销售退货
5. 付款单详情
6. 采购入库
7. 采购退货
8. 库存调整
9. 库存盘点
10. 经营报表
11. AI 助手
12. 日常支出
13. 资金流水详情

---

## 8. 每张页面的开发步骤

每次开发一张页面，严格按以下步骤执行：

1. 在 `stitch-screens.ts` 中确认该页面的设计资源编号和路由。
2. 打开对应 `png` 与 `html`。
3. 记录页面必须复刻的结构块：
   - 顶部工具区
   - 筛选区
   - 主内容区
   - 表格 / 表单 / 摘要区
   - 右侧信息区
   - 底部操作区
4. 对比当前 Vue 页面的结构差距。
5. 先改结构，再改样式，最后接回已有真实接口数据。
6. 本地构建。
7. 浏览器实际查看。
8. 以设计图为基准做最终微调。

注意：

- 不要先改颜色再改结构。
- 不要在未完成结构收口前做细碎 polish。

---

## 9. 代码层开发规范

### 9.1 页面组织

- 页面组件放在 `./src/pages/<module>/`
- 共享 UI 组件放在 `./src/shared/ui/`
- 全局业务样式仍集中在 `./src/style.css`

### 9.2 路由与设计稿映射

- 页面路由以 `./src/app/router/routes.ts` 为准
- 设计资源映射以 `./src/app/router/stitch-screens.ts` 为准
- 如果新增页面，必须同步补路由和设计稿映射

### 9.3 API 与数据

- 不得因 UI 改造擅自篡改接口契约
- 所有真实接口继续集中在 `./src/shared/api/client.ts`
- 契约目录继续集中在 `./src/shared/api/contracts.ts`
- 如果页面暂时没有后端能力，只能补 UI 壳和占位，不得伪造完整业务闭环

---

## 10. 当前完成状态与剩余边界

截至本轮 Web 收口，正式业务路由已经全部落到专属 Vue 页面或明确的领域复用页，不再由 `StitchScreenPage` 承担正式业务页面渲染。`StitchScreenPage` 仅保留给 `/references/mobile/**` 移动参考稿预览。

当前完成内容：

1. 8 张 PC 真稿页面已按 Stitch PC 结构完成主列表、详情、表单和首页结构复刻，并进入 QA/微调阶段。
2. 第二优先级移动参考页面已完成 PC 化：销售收款、销售退货、付款单详情、采购入库、采购退货、库存调整、库存盘点、经营报表、AI 助手、客户档案、供应商档案、日常支出、资金流水详情。
3. 单据中心与系统设置已从占位说明页升级为按角色过滤的 PC 工作台入口。
4. Web 侧角色权限已覆盖导航、路由、入口卡片、主按钮、写操作和部分行操作。
5. 权限语义新增“任一权限可见”的页面模式，用于单据中心、销售收款、采购入库和系统设置等跨业务域入口；普通页面仍保持全部权限满足才可进入。

仍需后端或设计补充才能继续提升的边界：

1. Stitch MCP 当前仅提供 8 张正式 PC 桌面真稿，其余页面只能依据移动稿语义做桌面化，不能宣称已有 PC 真稿一比一复刻。
2. 客户/供应商档案在 Web 侧已按销售/采购业务域开放入口和按钮，但真实 API 是否允许对应角色写入，最终以后端权限返回为准。
3. 数据库管理页已接同步健康和导入任务；备份、恢复、连接配置等能力如果后端无接口，只能显示能力边界，不能伪造成功。
4. 媒体附件、票据上传、打印模板、审批流等仍需要后端接口或专门设计稿后再扩展。

---

## 11. 验收规则

每一轮 Web 改动至少要通过以下验收：

### 11.1 静态验收

- `npm run build` 必须通过
- 不引入 `any` 回退
- 不新增无用组件和死代码

### 11.2 视觉验收

对有 PC 真稿的页面，必须核对：

- 页面结构是否一致
- 表格列是否一致
- 筛选区位置是否一致
- 主按钮位置是否一致
- 右侧详情或摘要区是否一致
- 字号层级是否接近
- 颜色与激活态是否接近

### 11.3 运行验收

- 页面路由可直接打开
- API 模式下不展示本地假数据
- 无权限时继续走当前 403 机制
- 表单失败继续展示后端错误

### 11.4 浏览器核对建议

每张完成页，至少需要手动核对：

- 1440px 宽桌面
- 1728px 及以上宽桌面
- 窄桌面下工具栏是否换行合理

---

## 12. 页面完成状态表

### 12.1 已有 PC 真稿页面

| 页面 | 路由 | 真稿状态 | 当前开发状态 | 下一动作 |
|---|---|---|---|---|
| Dashboard | `/dashboard` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 销售单列表 | `/documents/sales` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 销售单编辑 | `/documents/sales/edit` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 销售单详情 | `/documents/sales/detail` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 采购单列表 | `/documents/purchases` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 采购单编辑 | `/documents/purchases/edit` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 采购单详情 | `/documents/purchases/detail` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |
| 商品列表 | `/archives/products` | 有 PC 真稿 | 已完成 | 后续只做视觉 QA 微调 |

### 12.2 仅移动参考页面

| 页面 | 路由 | 参考类型 | 当前状态 | 下一动作 |
|---|---|---|---|---|
| 单据中心 | `/documents` | 移动稿 | 已 PC 化并按权限过滤 | 后续只做视觉 QA 微调 |
| 销售收款 | `/documents/sales/payment` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 销售退货 | `/documents/sales-returns` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 付款单详情 | `/documents/pay-orders/detail` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 采购入库 | `/documents/purchase-receipts` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 采购退货 | `/documents/purchase-returns` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 商品编辑 | `/archives/products/edit` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 客户档案 | `/archives/customers` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 供应商档案 | `/archives/suppliers` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 库存调整 | `/inventory/adjust` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 商品库存流水 | `/inventory/product-ledger` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 库存盘点 | `/inventory/snapshots` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 资金流水详情 | `/finance/records/detail` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 日常支出 | `/finance/daily-expense` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 经营报表 | `/reports` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| AI 助手 | `/agent` | 移动稿 | 已 PC 化并接真实接口 | 后续只做视觉 QA 微调 |
| 系统设置 | `/settings` | 移动稿 | 已 PC 化并按权限过滤 | 后续只做视觉 QA 微调 |
| 角色权限 | `/settings/roles` | 本地业务页 | 已接真实/演示双模式 | 后续以后端权限返回为准微调 |
| 数据库管理 | `/settings/database` | 本地业务页 | 已接健康检查与导入任务 | 等后端备份/恢复接口后扩展 |

---

## 13. 结论

后续 Web 开发的核心已经从“补齐页面骨架”切换为“视觉 QA、接口验收和后端能力扩展”：

1. 有 PC 真稿的 8 张页面继续按 Stitch PNG/HTML 做像素级 QA 微调。
2. 仅移动参考页面保持当前 PC 化结构，等 Stitch 后续补 PC 真稿后再做一比一复刻。
3. 所有正式业务页继续坚持真实 API、真实权限、真实错误反馈，不退回本地假数据工作台。
4. 本轮验证要求至少执行 `cd Code/frontend/web && npm run build`，并用浏览器抽查正式业务路由。

从本文件开始，凡是 Web 页面开发，都必须先查对应 Stitch 资源，再动代码。

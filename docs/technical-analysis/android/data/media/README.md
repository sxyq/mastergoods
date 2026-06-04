# Android data/media 模块分析

- 对应源码目录：`master-goods-android/data/media`
- 关键源码：`MediaV2Repository.kt`

## 模块定位

`data/media` 是媒体附件的数据入口。
新版里，它负责：

- owner 私有媒体资产上传与查询
- 媒体绑定（商品/单据等）的关联管理
- 与 `ZhihuijiV2Api` 的 `/v2/media` 契约对接

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
| 媒体资产与绑定仓储 | 待验证 | 旧版无对应域 | 承接 `/v2/media` 上传/绑定/解绑接口调用 | 已新增 `MediaV2Repository.kt` 对接 `ZhihuijiV2Api` 的媒体首轮接口 | 先以后端 spec 为准，真实上传链与工作台联调仍待后续完成 |
| `MediaBindingListResponse` 死代码清理 | 新版需要去掉 | B07 确认为死代码 | 已从 `V2MediaDtos` 移除 | 原本用于媒体绑定列表响应，实际未被任何接口引用 | DTO 层已清理，Android 侧无需同步此 DTO |
| owner 私有媒体上下文 | 新版待做 | 旧版无对应域 | 媒体资产只面向当前 owner 可见数据 | 当前还未显式体现 | 需与 auth/sync 联动 |

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。

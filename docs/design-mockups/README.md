# 历史参考

> 这里是 UI 设计稿参考，不是新版需求主规范。
> 当前产品与后端需求请以 `docs/spec/` 为准。

# 设计稿说明

这个目录存放智慧记 Android 端当前使用的界面设计稿。

文件列表：

- `01.png`
- `02.png`
- `03.png`
- `04.png`
- `05.png`
- `06.png`
- `07.png`
- `08.png`

用途说明：

- 作为 Android UI 重构和视觉验收的主要参考
- 与 [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md) 配合使用
- 所有 Compose 页面最终目标是尽量贴近这些设计稿

## 统一约束

- 这 8 张设计图是当前 Android 端的**统一视觉真源**。
- 后续新增业务页不能因为能力变多就切换成另一套视觉风格，只能在现有浅蓝渐变、玻璃卡片、蓝色主操作、五栏主壳体系里扩展。
- 新页面优先落到既有页面母版中：
  - 列表页
  - 详情页
  - 编辑页
  - 报表页
  - AI 工作台页
  - 设置页
- 如需新增组件，先沉淀到 `core/designsystem`，再由 feature 复用；不允许 feature 长期持有自定义且不可复用的样式组件。
- 后续 UI 验收默认同时对照：
  - 本目录设计图
  - `master-goods-android/UI-DESIGN-SPEC.md`
  - `docs/technical-analysis/android/core/designsystem/README.md`

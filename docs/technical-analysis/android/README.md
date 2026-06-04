# Android 代码目录分析

- 对应源码根目录：`master-goods-android/`
- 对应实现层级：`app / backdrop / core / data / feature`
- 主规范关联：
  - [docs/spec/31-android-impact.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/31-android-impact.md)
  - [docs/spec/30-api-contracts.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/30-api-contracts.md)
  - [docs/technical-analysis/server/entity/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/entity/README.md)
  - [docs/technical-analysis/server/api/dto/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/dto/README.md)

## 文档定位

本目录说明安卓端如何从当前 `/v1` 首版实现，迁移到面向新版后端与新版需求的结构。  
关注重点是：

- 模块职责
- `/v2` 模型与接口迁移
- owner 私有数据边界
- 领域扩容后对 Android 的分层影响
- 以及在大范围新增业务后，如何继续保持同一套 UI 语言

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 当前目录映射

- [app/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/app/README.md)
- [backdrop/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/backdrop/README.md)
- [core/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/README.md)
- [data/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/README.md)
- [feature/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/README.md)

## UI 统一真源

Android 端的 UI 统一口径固定为三层：

1. 视觉参考真源
   - [README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/design-mockups/README.md)
   - `01.png ~ 08.png`
2. 页面与组件规范真源
   - [UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/master-goods-android/UI-DESIGN-SPEC.md)
3. 实现落点真源
   - [README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/designsystem/README.md)

这意味着：

- 新增业务不能切换成新的视觉风格。
- `feature/*` 只能在既有列表页、详情页、编辑页、报表页、AI 页、设置页母版内扩展。
- 需要的新组件优先沉淀到 `core/designsystem`，而不是在 feature 内长期散落。

## UI 统一职责

本目录后续所有 Android 文档都要共同维护下面这套约束：

- `docs/design-mockups/01.png ~ 08.png` 负责定义产品最终观感与页面气质。
- `master-goods-android/UI-DESIGN-SPEC.md` 负责定义页面母版、信息层级、组件组合方式。
- `docs/technical-analysis/android/core/designsystem/README.md` 负责定义真正可复用的实现落点。
- `app / backdrop / core / data / feature` 五层都必须服务于同一套 UI 语言，不能因为业务扩域各自长出新风格。
- 后续评审新增页面时，先判断它应该落入哪一种既有母版，再决定补哪些领域组件，而不是先接受视觉漂移。

## Android 总体状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 多模块工程骨架 | 新版已做 | 从 0 到 1 需要先搭工程 | 保持 `app + backdrop + core + data + feature` 多模块结构 | 已可构建并可运行 | 是新版迁移的承载底座 |
| `/v1` 兼容应用 | 新版已做 | 旧版为本地账本 App | 继续承载现有后端与现有页面 | 当前 Android 仍以 `/v1` 为主 | 暂不删 |
| `/v2` 迁移规划 | 待验证 | 旧版无 `/v2` | 按领域建立模型、接口、Repository、页面职责 | 当前已建立 `core/model/v2`、`ZhihuijiV2Api` 与多组 `data/*V2Repository`，feature 首轮切换仍待真机联调 | 不能把编译通过等同于真机验收完成 |
| owner 私有数据边界 | 需重构 | 旧版无统一 owner 语义 | 所有读取、同步、导入、统计都感知 owner | 当前主要消费全局 `/v1` 数据 | 依赖后端先行 |
| 商品/往来单位/财务/库存扩域 | 旧版存在新版未做 | 旧版表域更厚 | 新版能力必须覆盖并超过旧版 | 当前安卓仍偏首版闭环 | 将带来明显模块扩容 |
| 会员体系 | 新版需要去掉 | 旧版可能存在会员概念 | 当前新版不纳入 | 安卓不应新增会员模块 | 如恢复需重新立项 |

## 新版安卓设计主线

1. **兼容与演进并存**
   - 继续保留当前 `/v1` 页面与数据流
   - 逐步新增 `/v2` 模型与接口

2. **账号归属优先**
   - 登录后不是简单恢复 token
   - 还要建立当前 owner 的本地上下文、同步状态、导入状态

3. **领域扩容优先于页面润色**
   - 先把商品、档案、单据、财务、库存的数据结构设计清楚
   - 页面改造后置

4. **从“功能页”走向“领域页”**
   - 当前很多页面是“先跑通业务闭环”
   - 新版要按 domain boundary 重排职责

## 当前到新版的迁移断点

| 断点 | 状态 | 当前实现 | 新版要求 | 备注 |
|---|---|---|---|---|
| 模型层仍偏 `/v1` DTO | 需重构 | `core/model` 主要服务当前接口 | 新增 `core/model/v2/*` | 与 server DTO 对齐 |
| Repository 仍偏“接口转页面” | 需重构 | 许多仓储直接围绕现有页面场景 | 逐步转为领域数据访问层 | 影响 data 层 |
| Room 更像缓存，不是 owner 账本 | 需重构 | 现有实体集偏薄 | 支撑 owner、扩域表、同步快照 | 影响 core/database |
| Settings 与 Sync 仍偏首版 | 需重构 | 只感知 session/baseUrl/cursor | 感知 owner/import/v2 environment | 影响 app + datastore + sync |
| Feature 仍偏单页闭环 | 需重构 | 多个模块按首版单据流实现 | 新版改为领域场景拆分 | 影响 feature 层 |

## B11 验收状态

| 项目 | 状态 | 当前证据 | 待补 |
|---|---|---|---|
| Android `/v2` model/network/repository 自动化 | 待验证 | `master-goods-android/**/src/test/**` 当前已有 11 个测试文件，覆盖 model 序列化、network 契约、safe api、agent/finance repository 委派 | 需要本轮 JDK 21 Gradle 测试输出，并继续补 product/customer/supplier/order/sync repository 测试 |
| Android 编译 | 待验证 | 已有历史 `assembleDebug` 成功记录 | 需要本轮指定 JDK 21 `assembleDebug` 输出 |
| 真机截图验收 | 待验证 | 当前未记录本轮 adb、安装、登录、业务流截图证据 | 截图统一放入 `docs/acceptance-evidence/b11/screenshots/` |
| 性能稳定性 | 待验证 | 当前未记录 CPU、内存、帧率、接口时延证据 | 列表、图表、同步、上传、大单据流需要独立性能记录 |
| 复验入口 | 新版已做 | `docs/spec/41-b11-acceptance-matrix.md` 与 `tools/b11_acceptance_check.sh android-contract/android-assemble` 已建立 | 后续把日志归档到 `docs/acceptance-evidence/b11/android/` |

## 阅读顺序

1. [app/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/app/README.md)
2. [core/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/README.md)
3. [data/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/README.md)
4. [feature/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/README.md)
5. 再进入各子模块 README

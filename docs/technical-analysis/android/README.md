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

本轮不展开具体 UI 视觉样式。

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

## Android 总体状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 多模块工程骨架 | 新版已做 | 从 0 到 1 需要先搭工程 | 保持 `app + backdrop + core + data + feature` 多模块结构 | 已可构建并可运行 | 是新版迁移的承载底座 |
| `/v1` 兼容应用 | 新版已做 | 旧版为本地账本 App | 继续承载现有后端与现有页面 | 当前 Android 仍以 `/v1` 为主 | 暂不删 |
| `/v2` 迁移规划 | 新版待做 | 旧版无 `/v2` | 按领域建立模型、接口、Repository、页面职责 | 当前只有文档层同步 | 本阶段重点 |
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

## 阅读顺序

1. [app/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/app/README.md)
2. [core/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/README.md)
3. [data/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/README.md)
4. [feature/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/README.md)
5. 再进入各子模块 README

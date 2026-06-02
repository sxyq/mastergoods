# Android core/datastore 模块分析

- 对应源码目录：`master-goods-android/core/datastore`
- 关键源码：
  - `SessionStore.kt`
  - `SettingsStore.kt`
  - `SyncPreferenceStore.kt`
  - `SecureSessionCipher.kt`
  - `DataStoreModule.kt`

## 模块定位

新版里 `core/datastore` 要从“保存 session/baseUrl/cursor”升级成：

- owner 私有上下文的小型状态仓
- 导入/同步任务的本地状态基线
- 环境策略与安全策略的轻量持久层

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
| 会话与设置持久化 | 新版已做 | 旧版更多是本地单机配置 | 形成分层的 DataStore 能力 | 当前 5 个核心文件已存在 | 能支撑登录与同步 |
| token 安全存储 | 新版已做 | 旧版无当前安全约束 | 使用 Keystore 加密敏感会话 | 当前已接入 `SecureSessionCipher` | 继续保留 |
| 历史明文会话迁移 | 新版已做 | 首轮加固前 token / refreshToken 可按明文残留在 DataStore | 升级后自动迁移旧安装数据，不要求用户重登 | `SessionStore` 已在读取期检测并把旧明文值重写为密文 | 属于安全与兼容的过渡逻辑 |
| release 基础地址可编辑性收口 | 新版已做 | 首版为了联调允许直接修改服务器地址 | release 只允许受控正式地址，debug 保留联调切换 | `SettingsStore` 已结合 `BASE_URL_EDITABLE` 与主机白名单约束基础地址持久化 | 与 `core/network` 的运行时校验形成双层收口 |
| owner 归属与导入状态 | 新版待做 | 旧版无统一 owner | 持久化当前 owner、导入批次、初始化完成度 | 当前只覆盖 session/baseUrl/cursor | 新版关键缺口 |
| 任意环境自由切换 | 新版需要去掉 | 首版为了联调允许较宽松切换 | release 必须受控，debug 再灵活 | 当前已部分收紧 | 后续继续统一到 `/v2` 环境策略 |

## 建议扩展的状态片段

| 状态片段 | 状态 | 说明 |
|---|---|---|
| `ownerContext` | 新版待做 | 当前账号的数据边界、首轮初始化状态 |
| `importJobState` | 新版待做 | 最近导入任务、服务端处理状态、冲突摘要 |
| `syncBaseline` | 新版待做 | owner 分桶游标、最后成功同步时间、失败原因 |
| `environmentPolicy` | 需重构 | debug/release 环境切换与受控主机策略 |

## 当前结论

- 现有 DataStore 拆分方向是对的。
- 但在新版里，它不该只服务“登录成功”和“记一个 cursor”。
- 它还要承接 owner、导入、同步、环境四类全局轻状态。

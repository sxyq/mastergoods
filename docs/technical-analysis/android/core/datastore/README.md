# datastore 技术分析

## 文件清单
- DataStoreModule.kt
- SessionStore.kt
- SettingsStore.kt
- SyncPreferenceStore.kt

---

## DataStoreModule.kt

### DataStoreModule
- object / 注解：@Module, @InstallIn(SingletonComponent::class) / 职责：通过 Hilt DI 提供 DataStore 实例 / 设计模式：依赖注入模块

#### provideSessionDataStore(@ApplicationContext context: Context): DataStore\<Preferences\>
- 参数：`@ApplicationContext context: Context` — 应用级 Context
- 返回值：`DataStore<Preferences>` — 命名为 "session" 的偏好数据存储，@Named("session") 限定
- 实现逻辑：返回 context.sessionDataStore 扩展属性
- 调用关系：被 Hilt 注入到 SessionStore
- 建议：无

#### provideSettingsDataStore(@ApplicationContext context: Context): DataStore\<Preferences\>
- 参数：`@ApplicationContext context: Context`
- 返回值：`DataStore<Preferences>` — 命名为 "settings" 的偏好数据存储，@Named("settings") 限定
- 实现逻辑：返回 context.settingsDataStore 扩展属性
- 调用关系：被 Hilt 注入到 SettingsStore
- 建议：无

#### provideSyncDataStore(@ApplicationContext context: Context): DataStore\<Preferences\>
- 参数：`@ApplicationContext context: Context`
- 返回值：`DataStore<Preferences>` — 命名为 "sync" 的偏好数据存储，@Named("sync") 限定
- 实现逻辑：返回 context.syncDataStore 扩展属性
- 调用关系：被 Hilt 注入到 SyncPreferenceStore
- 建议：无

### 顶层扩展属性

#### Context.sessionDataStore: DataStore\<Preferences\>
- 作用域：private / 初始值：by preferencesDataStore(name = "session") / 使用场景：会话数据存储
- 建议：无

#### Context.settingsDataStore: DataStore\<Preferences\>
- 作用域：private / 初始值：by preferencesDataStore(name = "settings") / 使用场景：设置数据存储
- 建议：无

#### Context.syncDataStore: DataStore\<Preferences\>
- 作用域：private / 初始值：by preferencesDataStore(name = "sync") / 使用场景：同步偏好数据存储
- 建议：无

---

## SessionStore.kt

### SessionStore
- class / 注解：@Singleton / 父类：无 / 职责：管理用户会话信息（token、refreshToken、userId）的持久化存储 / 设计模式：Repository 模式（会话数据）

#### SessionStore(@Named("session") dataStore: DataStore\<Preferences\>)
- 参数：`@Named("session") dataStore: DataStore<Preferences>` — 会话专用的 DataStore 实例
- 返回值：无（构造函数，@Inject 注入）
- 实现逻辑：保存 DataStore 引用
- 调用关系：由 Hilt 自动注入
- 建议：无

### SessionStore.Companion — 伴生对象

#### KEY_TOKEN: Preferences.Key\<String\>
- 作用域：private / 初始值：stringPreferencesKey("auth_token") / 使用场景：存储访问令牌
- 建议：无

#### KEY_REFRESH_TOKEN: Preferences.Key\<String\>
- 作用域：private / 初始值：stringPreferencesKey("refresh_token") / 使用场景：存储刷新令牌
- 建议：无

#### KEY_USER_ID: Preferences.Key\<Long\>
- 作用域：private / 初始值：longPreferencesKey("user_id") / 使用场景：存储用户 ID
- 建议：无

#### KEY_EXPIRES_AT: Preferences.Key\<Long\>
- 作用域：private / 初始值：longPreferencesKey("expires_at") / 使用场景：存储令牌过期时间
- 建议：无

#### token: Flow\<String?\>
- 作用域：public val / 使用场景：观察当前访问令牌，被 AuthInterceptor 和 TokenAuthenticator 使用
- 建议：无

#### refreshToken: Flow\<String?\>
- 作用域：public val / 使用场景：观察当前刷新令牌，被 TokenAuthenticator 使用
- 建议：无

#### userId: Flow\<Long?\>
- 作用域：public val / 使用场景：观察当前用户 ID
- 建议：无

#### isLoggedIn: Flow\<Boolean\>
- 作用域：public val / 使用场景：观察登录状态，token 非空白即为已登录
- 建议：未考虑 token 过期情况，仅判断 token 非空不够准确，建议结合 expiresAt 判断

#### saveSession(token: String, refreshToken: String, userId: Long, expiresIn: Int)
- 参数：`token: String` — 访问令牌；`refreshToken: String` — 刷新令牌；`userId: Long` — 用户 ID；`expiresIn: Int` — 有效期（秒）
- 返回值：无（suspend）
- 实现逻辑：将所有会话字段写入 DataStore，expiresAt = 当前时间 + expiresIn * 1000L
- 调用关系：被登录成功后和 Token 刷新成功后调用
- 建议：无

#### clearSession()
- 返回值：无（suspend）
- 实现逻辑：从 DataStore 中移除所有会话字段
- 调用关系：被登出时调用
- 建议：无

#### requireAccessToken(): String
- 返回值：`String` — 当前访问令牌
- 实现逻辑：读取 DataStore 首次值，若 token 为 null 则抛出 IllegalStateException("未登录")
- 调用关系：被需要强制获取 token 的场景调用
- 建议：无

---

## SettingsStore.kt

### SettingsStore
- class / 注解：@Singleton / 职责：管理应用设置（baseUrl、clientId）的持久化存储 / 设计模式：Repository 模式（设置数据）

#### SettingsStore(@Named("settings") dataStore: DataStore\<Preferences\>)
- 参数：`@Named("settings") dataStore: DataStore<Preferences>` — 设置专用的 DataStore 实例
- 返回值：无（构造函数，@Inject 注入）
- 实现逻辑：保存 DataStore 引用
- 调用关系：由 Hilt 自动注入
- 建议：无

### SettingsStore.Companion — 伴生对象

#### KEY_BASE_URL: Preferences.Key\<String\>
- 作用域：private / 初始值：stringPreferencesKey("base_url") / 使用场景：存储服务器基础 URL
- 建议：无

#### KEY_CLIENT_ID: Preferences.Key\<String\>
- 作用域：private / 初始值：stringPreferencesKey("client_id") / 使用场景：存储客户端唯一标识
- 建议：无

#### DEFAULT_BASE_URL: String
- 作用域：const / 初始值："http://117.72.79.106/zhihuiji/v1/" / 使用场景：默认服务器地址
- 建议：硬编码 IP 地址不利于维护，建议抽取到 BuildConfig 或配置文件

#### SERVER_124_HOST: String
- 作用域：private const / 初始值："124.222.153.108" / 使用场景：被屏蔽的旧服务器地址
- 建议：同上

#### normalizeBaseUrl(raw: String): String
- 参数：`raw: String` — 原始 URL 字符串
- 返回值：`String` — 规范化后的 URL
- 实现逻辑：trim 后若为空返回默认 URL；若包含旧服务器地址返回默认 URL；确保以 "/" 结尾
- 调用关系：被 baseUrl 属性和 saveBaseUrl 调用
- 建议：与 NetworkConfig.normalizeBaseUrl 逻辑重复，建议统一到一处

#### baseUrl: Flow\<String\>
- 作用域：public val / 使用场景：观察当前服务器基础 URL，读取时自动规范化
- 建议：无

#### clientId: Flow\<String\>
- 作用域：public val / 使用场景：观察当前客户端 ID
- 建议：无

#### saveBaseUrl(baseUrl: String)
- 参数：`baseUrl: String` — 要保存的 URL
- 返回值：无（suspend）
- 实现逻辑：规范化后写入 DataStore
- 调用关系：被设置页面修改服务器地址时调用
- 建议：无

#### saveClientId(clientId: String)
- 参数：`clientId: String` — 要保存的客户端 ID
- 返回值：无（suspend）
- 实现逻辑：直接写入 DataStore
- 调用关系：被初始化客户端 ID 时调用
- 建议：无

#### ensureClientId(): String
- 返回值：`String` — 确保存在的客户端 ID（若不存在则生成 UUID 并保存）
- 实现逻辑：先读取现有 clientId，若非空则返回；否则生成 UUID，保存后返回
- 调用关系：被同步上传时需要 clientId 的场景调用
- 建议：无

---

## SyncPreferenceStore.kt

### SyncPreferenceStore
- class / 注解：@Singleton / 职责：管理同步偏好的持久化存储（游标和同步时间戳） / 设计模式：Repository 模式（同步偏好数据）

#### SyncPreferenceStore(@Named("sync") dataStore: DataStore\<Preferences\>)
- 参数：`@Named("sync") dataStore: DataStore<Preferences>` — 同步专用的 DataStore 实例
- 返回值：无（构造函数，@Inject 注入）
- 实现逻辑：保存 DataStore 引用
- 调用关系：由 Hilt 自动注入
- 建议：无

### SyncPreferenceStore.Companion — 伴生对象

#### cursorKey(entityType: String): Preferences.Key\<String\>
- 参数：`entityType: String` — 实体类型
- 返回值：`Preferences.Key<String>` — 形如 "sync_cursor_{entityType}" 的键
- 实现逻辑：stringPreferencesKey("sync_cursor_$entityType")
- 调用关系：被 observeCursor 和 saveCursor 调用
- 建议：无

#### timestampKey(entityType: String): Preferences.Key\<Long\>
- 参数：`entityType: String` — 实体类型
- 返回值：`Preferences.Key<Long>` — 形如 "sync_timestamp_{entityType}" 的键
- 实现逻辑：longPreferencesKey("sync_timestamp_$entityType")
- 调用关系：被 observeLastSyncAt 和 saveCursor 调用
- 建议：无

#### observeCursor(entityType: String): Flow\<String\>
- 参数：`entityType: String` — 实体类型
- 返回值：`Flow<String>` — 该类型的同步游标值，默认空字符串
- 实现逻辑：从 DataStore 读取 cursorKey 对应值
- 调用关系：被同步逻辑观察游标时调用
- 建议：无

#### observeLastSyncAt(entityType: String): Flow\<Long\>
- 参数：`entityType: String` — 实体类型
- 返回值：`Flow<Long>` — 该类型的最后同步时间戳，默认 0L
- 实现逻辑：从 DataStore 读取 timestampKey 对应值
- 调用关系：被同步逻辑观察最后同步时间时调用
- 建议：无

#### saveCursor(entityType: String, cursor: String)
- 参数：`entityType: String` — 实体类型；`cursor: String` — 游标值
- 返回值：无（suspend）
- 实现逻辑：同时写入游标值和当前时间戳
- 调用关系：被同步拉取成功后调用
- 建议：无

#### clearAll()
- 返回值：无（suspend）
- 实现逻辑：遍历 DataStore 所有键，移除以 "sync_cursor_" 或 "sync_timestamp_" 开头的键
- 调用关系：被重置同步状态时调用
- 建议：使用键名前缀过滤的方式不够优雅，若键名规范变更可能遗漏，建议维护一个已注册 entityType 列表

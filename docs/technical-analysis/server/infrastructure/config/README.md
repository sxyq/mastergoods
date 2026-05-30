# Config 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/infrastructure/config/`

本层为 Spring 配置类，提供安全、定时任务、LLM 属性和演示数据初始化等配置。

---

## SecurityConfig

- **文件**: `SecurityConfig.java`
- **注解**: `@Configuration`, `@EnableWebSecurity`
- **作用**: Spring Security 安全配置，定义认证/授权规则和过滤器链。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `tokenService` | `TokenService` | 令牌验证服务 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `securityFilterChain(http)` | 配置安全过滤器链 | 见下方详细分析 |
| `bearerTokenFilter()` | 创建 Bearer Token 过滤器 Bean | 无 |

### 安全过滤器链规则

| 规则 | 路径 | 权限 | 修改建议 |
|------|------|------|----------|
| permitAll | `/v1/auth/**` | 公开 | 无 |
| ✏️ anyRequest().permitAll() | **所有其他路径** | **全部公开** | ✏️ **根因是 `anyRequest().permitAll()`，导致所有 API 均无需认证**，比单独 admin/sync permitAll 更严重；admin 和 sync 仅是受影响最突出的例子 |
| CSRF | - | 禁用 | REST API 合理，但需确保无浏览器表单提交场景 |
| CORS | - | 允许所有来源 | **生产环境应限制为指定域名** |
| Session | - | 无状态 | 合理，使用 JWT |

### 修改建议

1. **CORS 允许所有来源**: `CorsConfiguration.setAllowedOriginPatterns(List.of("*"))`，生产环境必须限制。
2. ✏️ **所有接口无认证**: 根因是 `anyRequest().permitAll()`，不仅 admin 和 sync 接口，而是**所有 API 均无需认证**，比原描述更严重。应改为 `anyRequest().authenticated()` 并对特定路径配置角色授权。
3. **Bearer Token 过滤器**: 每次请求都调用 `tokenService.validateToken()`，即每次查库，建议引入 Redis 缓存。

---

## AgentTaskConfig

- **文件**: `AgentTaskConfig.java`
- **注解**: `@Configuration`, `@EnableScheduling`
- **作用**: Agent 定时任务配置。

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| ✏️ `agentTaskExecutor()` | 创建异步任务线程池 | ✏️ corePoolSize=2, maxPoolSize=4，均硬编码 |

### 修改建议

1. ✏️ **线程池大小硬编码**: `agentTaskExecutor()` 的 corePoolSize=2、maxPoolSize=4 均硬编码，应提取到配置文件。
2. **无分布式锁**: 多实例部署时定时任务会重复执行。

---

## AgentLlmProperties

- **文件**: `AgentLlmProperties.java`
- **注解**: `@ConfigurationProperties(prefix = "agent.llm")`
- **作用**: Agent LLM 配置属性绑定。

### 字段

| 字段 | 类型 | 默认值 | 作用 | 修改建议 |
|------|------|--------|------|----------|
| `apiKey` | String | - | Anthropic API Key | **应使用环境变量而非配置文件** |
| `model` | String | `claude-sonnet-4-20250514` | 模型名称 | 无 |
| `maxTokens` | int | 4096 | 最大输出 token 数 | 无 |
| `temperature` | double | 0.3 | 温度参数 | 无 |
| `enabled` | boolean | false | 是否启用 LLM | 无 |
| `baseUrl` | String | - | API 基础 URL | 无 |

### 修改建议

1. **apiKey 安全性**: 应使用 Spring Cloud Vault 或环境变量注入，避免明文写在配置文件中。
2. **缺少验证**: 应增加 `@AssertTrue` 校验 enabled=true 时 apiKey 不为空。

---

## LocalDemoDataInitializer

- **文件**: `LocalDemoDataInitializer.java`
- **注解**: `@Profile("local")`, `@Configuration`
- **作用**: 本地环境启动时自动初始化演示数据。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `demoDataService` | `DemoDataService` | 演示数据服务 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `run(args)` | 应用启动后执行 | 检查是否已有用户，无则初始化演示数据 | 无 |

### 修改建议

1. **初始化判断过于简单**: 仅检查是否有用户存在，如果用户手动删除了所有用户，重启会再次初始化。

# Security 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/infrastructure/security/`

本层提供令牌（Token）生成服务及基于数据库的会话验证。

> **勘误说明**: 原始分析错误地假设 TokenService 使用了 JWT 机制（包括 secretKey、HS256 签名等），但实际代码中 TokenService 仅通过 `UUID.randomUUID()` 生成无状态随机令牌，不涉及任何 JWT 逻辑。令牌验证也并非在 TokenService 中完成，而是由 `AuthService.me()` 查询数据库实现。以下内容已根据实际代码修正。

---

## TokenService

- **文件**: `TokenService.java`
- **注解**: `@Component`
- **作用**: 生成 UUID 随机令牌。

### 实际代码

```java
@Component
public class TokenService {
    public String issueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### 函数

| 函数 | 作用 | 说明 |
|------|------|------|
| `issueToken()` | 生成 32 位无连字符 UUID 令牌 | 无签名、无载荷，纯随机字符串 |

### 令牌机制说明

TokenService **不使用 JWT**，仅生成 UUID 随机字符串作为令牌。令牌本身不携带任何用户信息（无 userId/phone/nickname 等载荷），所有令牌验证和用户信息获取均依赖数据库查询（通过 `AuthService.me()` 查询 Session 表实现）。

### 严重问题

1. **每次请求查库**: `AuthService.me()` 每次都查询数据库验证 session 是否活跃，高并发下数据库压力大，建议引入 Redis 缓存。
2. **无令牌黑名单**: 登出后令牌本身仍有效（仅 session 标记为 inactive），在令牌有效期内被截获仍可使用。
3. **令牌熵值可提升**: UUID v4 的随机性为 122 bit，对于安全令牌而言可考虑使用更安全的随机源（如 `SecureRandom` 生成更长随机串）。

### 修改建议

1. **引入 Redis 缓存**: 将 session 信息缓存到 Redis，减少数据库查询。
2. **令牌黑名单**: 登出时将令牌加入黑名单（Redis SET + TTL）。
3. **考虑使用 SecureRandom**: 替换 `UUID.randomUUID()` 为基于 `SecureRandom` 的令牌生成，增强随机性。
4. **如需无状态令牌**: 若希望避免每次查库，可考虑迁移至 JWT 方案，但需妥善管理密钥。

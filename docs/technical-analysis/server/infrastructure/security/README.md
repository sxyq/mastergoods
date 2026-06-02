# Server infrastructure/security 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/infrastructure/security`
- 关键源码：
  - `TokenAuthenticationFilter.java`
  - `TokenService.java`

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
| Token 认证基础设施 | 新版已做 | 旧版无当前服务端认证链 | 保持 token + session 基础能力 | `TokenAuthenticationFilter` + `TokenService` 已稳定可用 | 当前安卓登录依赖它 |
| SecurityContext 注入当前 user | 新版已做 | 旧版无统一 owner 上下文 | 认证成功后把当前 user/owner 身份透传到业务层 | `TokenAuthenticationFilter` 已把 `userId` 写入 `SecurityContext`，`CurrentOwnerService` 可继续解析 | 后端 owner 过滤已能复用这条链路 |
| token 查询走缓存与黑名单 | 新版已做 | 旧版过滤器每次直查 session 表，注销/刷新后失效 token 没有短期黑名单 | 统一通过缓存化 session 访问服务完成认证查询 | `TokenAuthenticationFilter` 已切到 `SessionAccessService.findActiveSessionByToken()` | 仍需视部署形态决定是否引入分布式缓存 |
| 仅靠首版过滤器识别身份 | 需重构 | 首版边界较窄 | 新版要把鉴权、归属、权限拆清 | 当前已有 user 注入与 owner 下沉，但细粒度权限和 admin/global 语义仍偏轻 | 后续与 controller/service 一起改 |

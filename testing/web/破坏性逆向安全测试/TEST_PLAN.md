# Web 破坏性逆向安全测试计划

## Objective

验证 Web 构建产物、本地状态和请求协议被主动篡改后，服务端仍能守住认证、owner/store、Agent 和写入边界。

## Execution Order

1. 检查 bundle、source map、静态配置和敏感信息暴露。
2. 篡改 localStorage、sessionStorage、路由参数和本地权限状态。
3. 重放或修改 Agent SSE、侧栏、取消和历史请求。
4. 绕过按钮禁用、表单校验、上传限制和草稿确认流程。
5. 用跨 owner/store 标识验证服务端拒绝和数据库无变化。

## Evidence

每个场景记录请求样本、响应状态、服务端审计、数据库前后状态、清理动作和影响判断。只依赖前端守卫的约束必须单独登记服务端风险。

## Safety Boundary

仅使用授权测试账号和隔离数据；不得保存 Cookie、Token、密码、私钥或完整认证载荷。生产环境、生产部署和不可逆数据破坏不在本计划范围内。

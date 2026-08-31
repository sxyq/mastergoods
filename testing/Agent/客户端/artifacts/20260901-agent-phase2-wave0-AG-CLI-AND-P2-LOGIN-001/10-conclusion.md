# Wave 0 结论

- overall_result: `Blocked`
- completed_scope: Android App 已安装并启动；已通过真实 UI 发起登录；请求真实到达 8220 公网 API；服务端返回 HTTP 422；本地 App 会话已清理并回到登录页。
- unavailable_scope: 没有可验证登录会话，因此未执行 Agent SSE、工具规划/执行、正式回答、草稿确认、审计、数据库业务写入、取消、重连或性能链路。
- account_fact: 四个指定账号后缀 `8111`、`8112`、`8113`、`8114` 均不存在。
- prohibited_actions_respected: 未重置密码、未创建账号、未创建门店、未准备生产夹具。
- model_fact: 实际运行模型为 `gpt-5.6-luna`，目标模型为 `glm-5.3-flash`；目标模型未得到真实验证，Provider 实际上下文窗口也未确认。
- ios_result: `Deferred`

解除条件：提供一个已存在且密码可验证的测试账号，或由用户明确批准符合现有数据规则的隔离测试数据准备。获得条件前不继续 Wave 1-4，也不使用本地后端、历史其他服务器结果或静态代码结果替代 8220 真实 App 证据。

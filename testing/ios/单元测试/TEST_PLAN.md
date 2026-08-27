# iOS 单元测试执行手册

## Objective

建立 Swift 侧函数级测试，覆盖 API 客户端、认证、模型、ViewModel、导航参数和 Agent 状态逻辑。

## Scope

- `Code/frontend/ios/ZhihuijiIOS/App`
- `Code/frontend/ios/ZhihuijiIOS/Core`
- `Code/frontend/ios/ZhihuijiIOS/Features`
- `Code/frontend/ios/ZhihuijiIOSTests`

## Must Cover

1. API path、参数编码、请求头和错误转换。
2. token/session 状态变化和权限边界。
3. Codable 模型的编码、解码、缺失字段和未知字段兼容。
4. Dashboard、inventory、sales、purchase 的 ViewModel 派生状态。
5. Agent 创建会话、发送消息、SSE/轮询追加、终止、取消、工具轨迹和历史回放。
6. 导航参数、实体 ID 和分页边界解析。

## Commands

```bash
xcodebuild test -project Code/frontend/ios/ZhihuijiIOS.xcodeproj -scheme ZhihuijiIOS -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Acceptance

每个手写 Swift 逻辑类型都能映射到测试文件；测试断言请求、模型、状态和异常分支。未运行 Xcode 前不填写通过结果。

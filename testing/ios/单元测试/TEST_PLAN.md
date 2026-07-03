# iOS 单元测试全覆盖方案

## Objective

为 `ZhihuijiIOS` 建立 Swift 侧函数级测试账本，覆盖 API、认证、模型、Agent、Dashboard 和各业务 Feature。

## Scope

源码范围：

- `ios/ZhihuijiIOS/App`
- `ios/ZhihuijiIOS/Core`
- `ios/ZhihuijiIOS/Features`
- `ios/ZhihuijiIOSTests`

## Coverage Standard

必须覆盖：

1. API client request builders
2. auth token and session logic
3. model decoding and encoding
4. feature view model logic
5. Agent interaction state logic
6. navigation parameter parsing

## Test Layers

### Core

- API path and parameter encoding
- auth/session state changes
- model schema compatibility

### Feature

- dashboard state
- inventory summaries
- sales and purchase calculations
- agent message and history state

### Agent

Must cover:

1. create conversation state
2. send message state
3. stream append logic
4. tool trace rendering preparation
5. history replay parsing

## Commands

Use Xcode or:

```bash
xcodebuild test -project ios/ZhihuijiIOS.xcodeproj -scheme ZhihuijiIOS -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Exit Criteria

1. Every handwritten Swift logic type is mapped to a test file.
2. Agent-related iOS logic has explicit coverage mapping.

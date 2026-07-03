# iOS 功能测试全覆盖方案

## Objective

验证 iOS 端在真实用户流上的行为完整性，并与真实后端数据闭环对齐。

## Scope

平台功能：

- auth
- dashboard
- inventory
- products
- purchases
- sales
- finance
- reports
- settings
- agent

## Functional Standard

每个功能最少覆盖：

1. happy path
2. invalid input
3. empty state
4. backend failure state
5. navigation return consistency

## Agent Functional Cases

1. open agent home
2. create or resume conversation
3. send text message
4. receive stream or polling response
5. reload history
6. cancel generation
7. verify blocked request message

## Evidence

- simulator or device target
- screenshots
- backend environment
- console log if failure occurs

## Exit Criteria

1. Every iOS feature page has at least one executable scenario.
2. Agent flow is confirmed against a real backend.

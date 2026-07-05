# iOS 破坏性逆向安全测试计划

## 目标

验证 iOS 客户端在遭遇越狱/Frida、ATS/Pinning 绕过、本地模型缓存提取、Agent 草稿/历史操纵时，是否仍不会突破服务端安全边界。

## 证据标准

- 保留 class/Swift 符号分析、Frida 脚本、抓包、深链样本、录屏
- 任何只能依赖客户端策略防守的约束，均需单独记为服务端补防项

## 执行顺序

1. 符号与路由面梳理
2. 越狱/Frida/Pinning 绕过
3. Keychain/UserDefaults/缓存提取
4. Agent 历史、草稿、任务、图片载荷操纵

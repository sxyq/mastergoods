# Zhihuiji iOS

智慧记 iOS 原生端，基于 `SwiftUI + async/await + URLSession`。

## 当前目标

- 复用现有后端 API，不改 `web/`、`master-goods-android/`、`src/`
- UI 风格对齐 Android 现有移动端设计系统，而不是 Web PC
- 大 ID 一律按 `String` 处理，避免精度丢失
- 首批闭环优先：登录、首页、销售、采购、商品、库存、财务、报表、AI、员工管理

## Android 视觉对齐规则

- 主品牌色：`#005BBF`
- 顶层背景：浅蓝到白的渐变底，叠加柔和 Aurora 光斑
- 主要容器：半透明白色玻璃卡片，14-16pt 圆角，细白描边，轻阴影
- 强操作按钮：亮蓝到主蓝横向渐变
- 文本层级：标题紧凑、正文克制、金额与 KPI 更粗更亮
- 页面结构沿用 Android 移动语义：顶部工具栏 + 内容流 + 底部 Tab

## 当前实施顺序

1. App / Session / Router / API 基础层
2. Design Tokens / 通用组件
3. 登录与权限壳
4. Dashboard / 销售 / 采购 / 商品首批页面
5. 报表 / AI / 设置与员工管理
6. SSE、测试、Xcode 工程收口

## 约束

- 未经允许不提交、不推送
- 不把供应商渠道密钥、模型 URL 逻辑放到 iOS 端
- 真实接口契约以当前后端 Controller + DTO 与 Web `client.ts/contracts.ts` 为准

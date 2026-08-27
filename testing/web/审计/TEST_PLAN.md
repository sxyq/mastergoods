# Web 审计测试执行手册

## Objective

按页面、组件、Store、API 适配器和工具函数审查 Web 端的权限边界、输入处理、后端契约、重复逻辑和可维护性。

## Scope

- `Code/frontend/web/src/app`
- `Code/frontend/web/src/entities`
- `Code/frontend/web/src/features`
- `Code/frontend/web/src/pages`
- `Code/frontend/web/src/shared`

## Audit Items

1. 路由参数使用 `readQueryId`，实体 ID 不转为不安全的 JavaScript `Number`。
2. 用户输入进入 HTML 时经过 `escapeHtml`，链接和富文本遵守白名单。
3. 客户端权限判断与服务端权限保护保持一致，不能只依赖按钮隐藏。
4. Agent 请求的取消、重试、SSE 终止和局部错误状态有明确处理。
5. API 请求、响应 envelope、分页字段和错误码与后端 DTO 一致。
6. 页面、Store 和共享工具不重复实现已有业务格式化与状态映射。

## Evidence

记录文件路径、符号、问题描述、风险、修复建议、复核人和 `result`。本手册仅为规划，不代表审计已执行。

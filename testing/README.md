# 测试目录规划

测试资料按端和测试类别维护。规划文档、执行结果、日志、原始证据和脚本分开管理，不把历史结果当作当前结论。

## 目录入口

| 端 | 入口 | 分类形态 |
| --- | --- | --- |
| 后端 | `testing/后端/README.md` | 中文分类目录，包含分类总台账和各类测试手册 |
| Android | `testing/安卓/` | 保留现有规划目录与分类空目录 |
| iOS | `testing/ios/README.md` | 与后端一致的中文分类目录 |
| Web | `testing/web/README.md` | 与后端一致的中文分类目录 |

## iOS 与 Web 统一分类

```text
testing/<端>/
├── README.md
├── 测试分类总台账.csv
├── 测试分类说明.md
├── 功能测试/TEST_PLAN.md
├── 单元测试/TEST_PLAN.md
├── 性能测试/TEST_PLAN.md
├── 审计/TEST_PLAN.md
└── 破坏性逆向安全测试/TEST_PLAN.md
```

当前 iOS 与 Web 只保留规划文档，尚未执行的项目不填写通过结果。实际执行时，日志、报告、原始证据和脚本放到对应类别目录下，并使用统一字段记录：`test_id`、`category_id`、`category_name`、环境、账号/store、前置状态、操作、预期、实际、证据路径、清理动作和 `result`。

结果值只使用 `Passed`、`Failed`、`Blocked`、`Deferred`。Token、Cookie、密码、私钥、完整认证载荷和模型密钥不得进入测试资料或 Git。

本次调整只涉及 iOS/Web 测试目录和规划索引，未修改业务源码、客户端测试源码、后端实现、数据库或部署配置。

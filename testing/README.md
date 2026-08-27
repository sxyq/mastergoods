# 测试目录规划

本目录只保留测试规划入口和按类别划分的空执行目录。历史报告、执行台账、原始日志、测试脚本和旧证据已清理，不作为当前结论来源。

## 三端规划入口

| 端 | 规划目录 | 规划内容 |
| --- | --- | --- |
| Android | `testing/安卓/plans/` | 功能、单元、性能、安全测试规划及分类说明 |
| iOS | `testing/ios/plans/` | 功能、单元、性能、安全测试规划 |
| Web | `testing/web/plans/` | 功能、单元、性能、安全测试规划 |

规划文档可以描述测试范围、步骤、输入、预期、边界、验收条件和环境要求，但不保存本轮执行结果。

## 测试类别目录

Android、iOS、Web 均使用相同的类别目录：

```text
client/
contract/
data/
functional/
integration/
observability/
performance/
reliability/
security/
unit/
```

每个类别下预留以下目录，当前只保留 `.gitkeep`：

```text
<category>/
├── artifacts/   # 原始证据
├── logs/        # 命令、请求和运行日志
├── reports/     # 阶段报告和汇总结果
└── scripts/     # 该类别的测试脚本
```

执行测试时，资料必须放入对应端和类别目录，不在端目录根部创建台账、报告或脚本。所有敏感信息必须脱敏，禁止保存 Token、Cookie、密码、私钥、完整认证载荷和模型密钥。

## 结果记录规则

重新执行测试时，每条记录至少包含：

- `test_id`
- `category_id`
- `wave_id`
- 环境、账号/store 和前置状态
- 操作、预期和实际结果
- 证据路径
- 清理动作
- `result`

结果值只使用 `Passed`、`Failed`、`Blocked`、`Deferred`。未执行项目不得写成通过；设备、服务或数据条件不足时，应记录阻塞原因。

## 当前状态

- Android、iOS、Web 业务源码未因本次清理修改。
- Android、iOS、Web 业务测试源码未因本次清理修改。
- 本轮未执行测试、构建、浏览器操作、设备操作或真实接口调用。
- 后端测试目录、公共测试脚本、数据库和业务数据不在本次三端清理范围内。

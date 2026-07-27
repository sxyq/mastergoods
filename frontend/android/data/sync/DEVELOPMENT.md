# data/sync 当前边界

当前已承接 `/v1` 基础手动同步和 `/v2` health/cursor/pull/ack/upload/import-jobs 及库存读模型。

## 尚未完成

- 后台同步调度、离线回写和冲突处理。
- 完整 upload 语义与客户端顺序联调。
- 服务器与 Android 真机同步闭环证据。

## 约束

- 同步只应用服务端真实返回，不能创建长期种子数据。
- 游标只有在本地应用成功后才能 ack。

## 验收入口

- 源码：`data/sync/src/main/`
- 测试台账：`testing/安卓/`、`testing/后端/`

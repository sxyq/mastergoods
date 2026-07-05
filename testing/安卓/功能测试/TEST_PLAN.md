# 安卓功能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

将 Android 功能测试文档细化成真机联调手册，固定设备矩阵、Wave 批次、live data 复用策略、补数规则和证据要求，后续可直接照文档执行。

## Current Baseline

可直接复用的 154 live 数据：

- 已存在账号与 session
- 已存在 `1` 个 store 与 `1` 条 store membership
- 已存在 Agent 会话、消息、审计、草稿数据

当前业务主表为空，因此：

- 登录、会话恢复、首页权限分流、Agent 列表/历史/草稿/审计类测试直接复用现有数据
- 商品、客户、供应商、订单、财务、库存、媒体相关场景需要最小夹具

## Environment Matrix

固定设备矩阵：

1. 1 台真实 Android 手机
2. 1 个 API 34 模拟器
3. 1 个小屏配置

环境目标：

- local backend
- deployed 154 backend

推荐命令：

```bash
cd master-goods-android
./gradlew connectedDebugAndroidTest
adb devices
```

## Execution Waves

### Wave 0

1. 确认设备在线
2. 确认目标后端可访问
3. 确认账号可登录
4. 确认当前 Agent 历史与草稿可加载
5. 记录业务主表空状态

### Wave 1

直接复用 live 数据：

- 登录
- session 恢复
- 首页权限分流
- Agent 会话列表
- Agent 历史恢复
- Agent 草稿/审计

### Wave 2

最小夹具补数后执行：

- 商品
- 客户
- 供应商
- 订单
- 财务
- 库存
- 媒体

### Wave 3

高复杂与稳定性：

- Agent 收流失败重试
- 长对话切换
- 图片上传与预览稳定性
- 小屏与真机差异复核

## Per-Category Execution Rules

### 登录与 session 恢复

- 前置：现有账号可用
- 页面入口：登录页 / 启动页
- 步骤模板：登录 -> 杀进程/重开 -> 校验 session 恢复
- 失败是否阻塞：是，阻塞所有后续业务验证

### 首页权限分流

- 前置：store context 可用
- 页面入口：首页
- 步骤模板：登录后进入首页 -> 校验 tab / 权限入口 / 无权限态
- 失败是否阻塞：是

### Agent 会话列表 / 历史 / 草稿 / 审计

- 前置：复用现有 Agent 数据
- 页面入口：Agent 页
- 步骤模板：
  - 打开 Agent 页
  - 加载列表
  - 切换历史会话
  - 查看消息
  - 查看草稿或审计相关信息
- 失败是否阻塞：阻塞 Agent 主链路

### Agent 收流与取消

- 前置：可新建或复用会话
- 页面入口：Agent 页
- 步骤模板：
  - 发送问题
  - 记录首条 delta
  - 执行取消
  - 校验最终状态
- 失败是否阻塞：是

### 商品 / 客户 / 供应商

- 前置：按场景创建最小夹具
- 页面入口：对应列表与编辑页
- 步骤模板：create -> list refresh -> detail -> update
- 失败是否阻塞：阻塞对应业务域，不阻塞 Agent

### 订单 / 财务 / 库存

- 前置：依赖上游最小夹具
- 页面入口：对应列表与详情页
- 步骤模板：create -> status transition -> refresh -> detail verification
- 失败是否阻塞：阻塞对应业务域

### 媒体

- 前置：最小上传夹具
- 页面入口：上传入口 / Agent 图片入口
- 步骤模板：选择文件 -> 上传 -> 预览/回显 -> 删除或释放
- 失败是否阻塞：阻塞多模态与媒体相关场景

## Evidence Template

- `test_id`
- `category_id`
- `wave_id`
- `env`
- `account/store`
- `pre_state`
- `actions`
- `expected`
- `actual`
- `artifacts`
- `cleanup`
- `result`

功能测试额外要求：

- `device_id`
- `build_variant`
- `screen_recording`
- `screenshot_set`
- `logcat_excerpt`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- 设备未连接
- 后端不可访问
- 登录失败
- 数据前置缺失且无法补最小夹具
- 真机与模拟器都无法复现核心路径

## Exit Criteria

1. Wave 1 所有安卓主路径都可直接执行并留证。
2. Agent 主链路完成真机验证。
3. Wave 2 每个业务域都明确最小夹具和清理动作。
4. 每个失败场景都能落在 `Blocked` 或明确失败，而不是模糊通过。

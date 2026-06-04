# B11 Android 真机现场摘要

- 日期: 2026-06-04
- 设备 serial: `d715a3a4`
- 设备型号: `25010PN30C`
- Android 版本: `16` (`sdk=36`)
- 安装包: `:app:installDebug`
- 启动入口: `com.zhihuiji.app/.MainActivity`

## 代码与构建结论

- `:core:datastore:testDebugUnitTest` 通过
- `:core:network:testDebugUnitTest` 通过
- `:app:compileDebugKotlin` 通过
- `:app:installDebug` 通过
- 本轮新增必要客户端修复:
  - `core/network/NetworkModule.kt` 不再只改 `scheme/host/port`，现在会同时保留目标 baseUrl 的路径前缀
  - 已补单测覆盖 `http://117.72.79.106/zhihuiji/v1/` 路径重写

## 现场结论

- 设备在线、安装成功、冷启动成功
- 现有会话恢复失败: 冷启动落在登录页，没有直接回到主流程
- 设备内实际 baseUrl 已确认是 `http://117.72.79.106/zhihuiji/v1/`
- 显式登录成功:
  - 先验证已有 demo/test 账号不存在，`13800138111/123456` 与 `13800138000/123456` 都返回 `account not found`
  - 使用邀请码 `021218` 通过后端注册新账号 `13800138115`
  - App 内再用 `13800138115/123456` 登录成功
- 登录后首页可进入，无 crash

## 关键证据

- 登录页截图: `docs/acceptance-evidence/b11/screenshots/launch.png`
- 设置页截图: `docs/acceptance-evidence/b11/screenshots/settings.png`
- 登录后首页截图: `docs/acceptance-evidence/b11/screenshots/login-success-2.png`
- 冷启动日志: `docs/acceptance-evidence/b11/android/logcat-launch.txt`
- 首次错误登录验证日志: `docs/acceptance-evidence/b11/android/logcat-login-demo.txt`
- 登录成功日志: `docs/acceptance-evidence/b11/android/logcat-login-success-2.txt`
- 设置页 UI dump: `docs/acceptance-evidence/b11/android/ui-settings.xml`

## 当前通过项

- 登录页可正常输入与提交
- `117` 调试地址真实生效，不再回落到 `https://api.zhihuiji.com/v1/`
- 登录请求真实打到 `http://117.72.79.106/zhihuiji/v1/auth/login`
- 设置页可进入，并显示:
  - 手机号 `13800138115`
  - 服务器地址 `http://117.72.79.106/zhihuiji/v1/`
  - 同步健康状态 `未知`
  - 导入任务空态
- 首页可进入，并诚实显示“数据加载存在缺口”

## 当前阻塞项

- `117` 后端当前缺少多条 App 依赖的 `/v2` 接口，导致登录后只能进入“壳层可用、数据缺口明显”的状态
- 已确认的 404:
  - `GET /zhihuiji/v1/v2/sale-orders`
  - `GET /zhihuiji/v1/v2/accounts`
  - `GET /zhihuiji/v1/v2/products/low-stock`
  - `GET /zhihuiji/v1/v2/sync/health`
  - `GET /zhihuiji/v1/v2/import-jobs`
- 因此本轮无法把商品/客户/供应商/销售/采购/付款/财务/报表/Agent/同步判定为“通过”
- 设置页里的 `clientId` 当前仍显示 `-`，说明设备侧 client id 尚未在这条现场链路里成功生成并展示

## 不能宣称完成的边界

- 不能宣称 `117` 已具备完整 `/v2` 联调能力
- 不能宣称主流程 smoke 已全部通过
- 不能宣称手动同步 `pull -> apply -> ack(next_cursor)` 已在真机上形成完整可解释日志链
- 不能把本轮 debug 真机验证升级为 release 安全验收或发布完成

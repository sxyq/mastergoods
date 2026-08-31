# Android App 观察记录

- status: `Blocked`
- device: `emulator-5554` / `Zhihuiji_API34` / Android API 34
- package: `com.zhihuiji.app` / `versionName=1.0.0`
- 清理：`pm clear com.zhihuiji.app` 返回 `Success`。
- 启动：`am start -n com.zhihuiji.app/.MainActivity` 返回成功；等待启动后前台 Activity 为 `com.zhihuiji.app/.MainActivity`。
- 启动页：UI 树显示手机号输入框、密码输入框、登录按钮和注册入口；未见历史会话或 Agent 页面。
- 登录操作：从 App 登录页输入第一个指定账号标签对应的测试输入并点击登录；密码未写入本文件、截图或日志。
- 交互状态：点击期间显示加载状态并暂时禁用登录按钮；请求结束后仍停留在登录页，显示“请求参数未通过校验，请检查输入内容”。
- 网络结果：真实请求到达 `https://zhj-api.sxyq27.online/v1/auth/login`，HTTP 422。
- 崩溃：未观察到 App 崩溃；本轮没有 SSE、工具、草稿或业务写入。
- 证据：本目录已有 `00-launch-screen.png`、`00-ui.xml` 和 `00-ui-summary.txt`；本轮清理后启动页截图仅用于本机临时复核，未加入版本库。

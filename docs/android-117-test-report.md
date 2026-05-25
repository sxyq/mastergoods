# 安卓端连接 117 后端测试报告

测试时间：2026-05-25 21:36-21:55 CST

## 结论

- 安卓工程默认后端地址已确认是 `http://117.72.79.106/zhihuiji/v1/`。
- 117 公网后端连通性通过，`sync/health`、商品、报表等核心只读接口均返回 `code=0`。
- 117 服务器只读 SSH 验证通过，后端容器 `zhihuiji117-backend`、Postgres、Redis 均处于运行状态。
- 全模块 `testDebugUnitTest` 任务执行成功；本轮新增并通过 13 个核心单元测试。
- `assembleDebug` 构建成功。
- 真机性能采样未完成：执行时 ADB 设备从 `d715a3a4 device` 变为未连接，脚本停在 `waiting for device`，已记录原始日志。

## 代码与配置核查

| 项目 | 结果 |
| --- | --- |
| Android 默认后端 | `core/network/NetworkConfig.kt` = `http://117.72.79.106/zhihuiji/v1/` |
| 设置模块默认后端 | `core/datastore/SettingsStore.kt` = `http://117.72.79.106/zhihuiji/v1/` |
| 明文 HTTP | `app/src/main/AndroidManifest.xml` 已设置 `android:usesCleartextTraffic="true"` |
| Retrofit 基础路径 | `NetworkModule` 从 `SettingsStore.baseUrl` 初始化，并写回 `NetworkConfig.baseUrl` |

## 本轮新增测试

| 模块 | 文件 | 覆盖内容 |
| --- | --- | --- |
| `core/common` | `MoneyFormatterTest.kt` | 金额格式化、千分位、正负号 |
| `core/common` | `StatusLabelsTest.kt` | 销售单、付款单、资金流水、库存状态文案 |
| `core/common` | `ApiResponseExtTest.kt` | `requireData()` 成功路径和业务异常路径 |
| `core/network` | `NetworkConfigTest.kt` | 117 默认 baseUrl、尾斜杠规范化、超时预算 |
| `core/network` | `ZhihuijiApiContractTest.kt` | 认证、商品、订单、资金、报表、同步、Agent 关键 API 注解路径 |
| `core/model` | `SerializationContractTest.kt` | `AuthResult`、`ProductDto` 的 snake_case JSON 契约 |

## 单元测试结果

执行命令：

```bash
cd /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android
./gradlew testDebugUnitTest --console=plain
```

结果：

| 指标 | 数值 |
| --- | --- |
| XML 测试套件文件 | 6 |
| 测试用例 | 13 |
| failures | 0 |
| errors | 0 |
| skipped | 0 |
| Gradle 结果 | `BUILD SUCCESSFUL in 3s` |

说明：

- 本轮执行的是全模块 `testDebugUnitTest`，所有模块都参与任务图。
- 目前只有 `core/common`、`core/network`、`core/model` 存在真实 JVM 单元测试。
- 其他模块结果为 `NO-SOURCE`，代表这些模块暂未编写单元测试，不代表业务逻辑已达到测试全覆盖。

## 117 公网接口探测

公网基础地址：

```text
http://117.72.79.106/zhihuiji/v1/
```

探测结果：

| 接口 | HTTP | 业务结果 |
| --- | --- | --- |
| `GET /sync/health` | 200 | `code=0`，`status=ok` |
| `GET /products` | 200 | `code=0`，返回商品数据 |
| `GET /reports/sales-summary` | 200 | `code=0`，返回销售统计 |

关键返回样例：

```json
{"code":0,"message":"success","data":{"status":"ok","message":"sync service ready"}}
```

## 117 后端只读运行状态

SSH 只读检查：

| 项目 | 结果 |
| --- | --- |
| 主机名 | `lavm-i2arq41omj` |
| 后端容器 | `zhihuiji117-backend`，运行 27 小时 |
| Postgres | `zhihuiji117-postgres`，healthy |
| Redis | `zhihuiji117-redis`，healthy |
| Nginx | 80 端口监听 |
| 后端容器端口 | `0.0.0.0:18080->18080/tcp` |
| 容器内健康路径 | `http://127.0.0.1:18080/v1/sync/health` 返回 200 |

容器资源快照：

| 容器 | CPU | 内存 |
| --- | --- | --- |
| `zhihuiji117-backend` | 1.92% | 306.5 MiB / 1.918 GiB |
| `zhihuiji117-postgres` | 0.00% | 50.81 MiB / 1.918 GiB |
| `zhihuiji117-redis` | 2.91% | 7.773 MiB / 1.918 GiB |

## 117 接口性能采样

采样方式：

- 只读 GET 请求。
- 每个端点 5 次。
- `curl --max-time 15`。
- 统计 `http_code`、`time_total_ms`、响应体大小。

| 端点 | 次数 | 成功次数 | 平均耗时 | P95 | 最大耗时 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `sync/health` | 5 | 5 | 85.4 ms | 86.7 ms | 89.9 ms |
| `products` | 5 | 5 | 120.2 ms | 89.2 ms | 262.2 ms |
| `customers` | 5 | 5 | 124.5 ms | 91.5 ms | 269.2 ms |
| `suppliers` | 5 | 5 | 84.7 ms | 86.6 ms | 87.5 ms |
| `sale-orders` | 5 | 5 | 90.0 ms | 94.3 ms | 95.3 ms |
| `purchase-orders` | 5 | 5 | 288.9 ms | 90.6 ms | 1087.7 ms |
| `pay-orders` | 5 | 5 | 85.1 ms | 86.4 ms | 86.7 ms |
| `finance-records` | 5 | 5 | 288.6 ms | 91.9 ms | 1090.0 ms |
| `reports/sales-summary` | 5 | 5 | 91.1 ms | 93.6 ms | 93.7 ms |
| `reports/top-products` | 5 | 5 | 89.5 ms | 90.6 ms | 93.4 ms |
| `agent/workbench` | 5 | 5 | 340.5 ms | 105.7 ms | 1284.0 ms |

观察：

- 常规接口大多数在 80-125 ms 区间。
- `purchase-orders`、`finance-records`、`agent/workbench` 各出现一次约 1 秒级峰值，平均值被抬高。
- 5 次采样规模较小，后续需要用更长压测窗口确认 P95/P99。

## 真机性能采样状态

预期采集内容：

- `am start -W` 启动耗时。
- `uiautomator dump` UI 树。
- `screencap` 启动截图。
- `dumpsys meminfo` 内存。
- `dumpsys gfxinfo` 帧数据。
- `logcat -b crash` 崩溃日志。

实际结果：

- 采样前曾检测到设备：`d715a3a4 device usb:0-1 model:25010PN30C`。
- 采样执行时设备已从 ADB 列表消失。
- 脚本日志显示：`adb: device 'd715a3a4' not found`、`waiting for device`。
- 因此本轮没有形成有效的 app 侧帧率/内存性能指标。

## 构建结果

执行命令：

```bash
cd /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android
./gradlew assembleDebug --console=plain
```

结果：

```text
BUILD SUCCESSFUL in 2m 16s
```

## 证据文件

本轮原始证据位于：

```text
/Users/sunyiyang/Desktop/Project/master-goods/docs/android-117-test-artifacts/20260525-213631
```

| 文件 | 内容 |
| --- | --- |
| `server-117-probe.txt` | 117 公网端口和核心接口探测 |
| `server-117-latency.csv` | 117 只读接口逐次延迟数据 |
| `server-117-latency-summary.txt` | 117 只读接口延迟统计 |
| `server-117-ssh-readonly.txt` | 117 SSH 只读容器/端口状态 |
| `server-117-runtime-readonly.txt` | 117 Docker 资源快照 |
| `server-117-local-backend-probe.txt` | 117 容器内后端健康探测 |
| `unit-testDebugUnitTest.log` | 全模块单元测试 Gradle 日志 |
| `unit-test-summary.txt` | 单元测试 XML 汇总 |
| `assembleDebug.log` | Debug 构建日志 |
| `device-startup-run.log` | 真机采样失败日志 |

## 后续建议

1. 重新连接真机后补跑 app 侧性能采样，形成启动、内存、帧耗时、crash buffer 证据。
2. 为 `data/*` Repository 添加 fake API + fake DAO 单元测试，覆盖缓存回退、远程成功写入、远程失败 fallback。
3. 为 `feature/*` ViewModel 添加 coroutine test，覆盖加载、提交、错误态、状态切换。
4. 增加 `connectedDebugAndroidTest` UI 冒烟测试，用真机验证登录页、底部五栏、列表页、报表页、助手页基础可达。
5. 对 117 做 1-5 分钟持续只读压测，重点观察 `purchase-orders`、`finance-records`、`agent/workbench` 的 1 秒级峰值。

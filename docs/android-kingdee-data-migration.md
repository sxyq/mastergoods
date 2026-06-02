# 历史参考

> 这是历史迁移分析记录，不再作为新版主规范。
> 新版需求请以 `docs/spec/` 为准。

# 旧版智慧记数据迁移到当前 Android App

## 目标

将旧版 `com.kingdee.zhihuiji` 本地 SQLite 业务数据迁移到当前 `com.zhihuiji.app` 使用的 Room 数据库 `zhihuiji.db` 中。

当前迁移脚本位置：

- `/Users/sunyiyang/Desktop/Project/master-goods/tools/migrate_kingdee_zhihuiji.py`

## 已确认的数据来源

来源 APK：

- `/Users/sunyiyang/Downloads/智慧记进销存.apk`

来源包名：

- `com.kingdee.zhihuiji`

来源主业务库：

- `/Users/sunyiyang/Desktop/Project/master-goods/migration_source_zhihuiji/9ffd7446d3f1480197908a113565d0ef.db`

## 当前脚本迁移的内容

- 商品：`products -> products`
- 客户：`companies(tye=1) -> customers`
- 供应商：`companies(tye=2) -> suppliers`
- 销售单：`sales -> sale_orders`
- 销售明细：`saleitems -> sale_order_items`
- 采购单：`purs -> purchase_orders`
- 付款单：从 `purs.pay_amt` 反推，生成 `pay_orders`
- 资金流水：`funds -> finance_records`

## 当前脚本不迁移的内容

- WebView 缓存
- SharedPreferences 配置
- 图片文件
- agent 通知
- sync 游标

## 迁移命令

仅生成目标数据库：

```bash
python3 /Users/sunyiyang/Desktop/Project/master-goods/tools/migrate_kingdee_zhihuiji.py
```

生成并直接推送到 rooted 真机：

```bash
python3 /Users/sunyiyang/Desktop/Project/master-goods/tools/migrate_kingdee_zhihuiji.py \
  --deploy \
  --device-serial 50f87ee9
```

## 设备部署逻辑

脚本会执行以下动作：

1. 强制停止 `com.zhihuiji.app`
2. 通过 `adb push` 将新数据库放到 `/data/local/tmp/`
3. 进入 root shell
4. 备份旧的 `zhihuiji.db`
5. 将迁移后的数据库复制到：

```text
/data/data/com.zhihuiji.app/databases/zhihuiji.db
```

6. 修复 owner 和权限

## 迁移映射说明

### 销售单

- `subtotalAmount` 使用旧库 `bill_amt`
- `totalAmount` 优先使用 `disc_amt`，并叠加运费/扣减
- `discountAmount = subtotalAmount - totalAmount`
- `paidAmount` 使用旧库 `pay_amt`
- 旧库 `tye=2` 视为退货单，保留负数金额和负数数量，并在备注中追加标记

### 采购单

- `totalAmount` 按旧库 `disc_amt/bill_amt + express_amt - deduction_amt` 计算
- 当前目标库没有采购明细表，所以只迁移采购单表头摘要

### 付款单

- 旧系统没有单独的付款单表
- 当前脚本通过 `purs.pay_amt` 反推一批已付款记录
- `referenceNo` 保存原采购单号

### 资金流水

- `amount = abs(in_amt - out_amt)`
- `type` 通过净额正负映射为收入或支出
- `category` 根据 `code` 前缀和 `tye` 推断，如 `SKD/收款`、`SZD/支出`、`CZD/储值`

## 风险说明

- 当前迁移是“本地数据库迁移”，不是“后端正式导入”
- 如果 app 后续登录线上账号并执行在线刷新，部分列表数据可能被服务器结果覆盖
- 如需长期保留旧数据，建议后续补一个“导入到后端”链路，而不是只落本地 Room

## 与服务器账号导入的边界

当前后端虽然已经具备账号注册、登录、会话能力，但业务表尚未完整按 `user_id` 隔离。

这意味着：

- 目前这份文档描述的是“旧版数据导入 Android 本地数据库”
- 还不是“导入到某个服务器账号私有空间”
- 如果要做到“为指定账号导入并持久化到服务器”，需要先补后端多账号业务隔离

## 2026-06-01 实测结果

真机：

- serial: `50f87ee9`
- package: `com.zhihuiji.app`

本次迁移实际生成并写入的记录数：

- 商品：`693`
- 客户：`440`
- 供应商：`1`
- 销售单：`3846`
- 销售明细：`11284`
- 采购单：`7`
- 付款单：`6`
- 资金流水：`2756`

结构校验：

- `room_master_table.identity_hash = e896ad50def3e9d177c893c4a4038e29`

设备校验：

- 从真机回读的 `zhihuiji.db` 与本地生成文件 `SHA-256` 完全一致
- `MainActivity` 已成功启动并驻留前台
- 启动日志中未出现 `Room` schema mismatch、`SQLiteException`、`FATAL EXCEPTION`

说明：

- 当前部署脚本已优先使用 `run-as com.zhihuiji.app` 将数据库复制进 app 自身沙箱
- rooted `su` 路径保留为回退方案，仅在 `run-as` 不可用时使用

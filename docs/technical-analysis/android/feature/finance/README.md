# Android feature/finance 模块分析

- 对应源码目录：`master-goods-android/feature/finance`
- 关键源码：
  - `FinanceRecordListScreen.kt`
  - `FinanceRecordEditorSheet.kt`
  - `FinanceViewModel.kt`

## 模块定位

`feature/finance` 当前只是轻量流水入口。  
新版里，它要逐步演化成财务域前台，承接：

- 账户与余额
- 收支流水
- 转账
- 找零/零钱
- 项目维度
- 单据资金关联

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 资金流水列表与新增 | 新版已做 | 旧版财务域更厚 | 支撑当前流水业务链 | 页面、弹窗、ViewModel 已存在 | 闭环已走通 |
| 账户/转账/找零/项目等场景 | 旧版存在新版未做 | 旧版有更完整财务体系 | 新版财务页要覆盖更厚场景 | 当前只覆盖轻量流水新增 | 依赖后端扩域 |
| `/v2` owner-aware 财务页 | 需重构 | 旧版无统一 owner | 按 owner 和新版接口重做状态管理 | 当前仍依赖 `/v1` | 后端先行 |

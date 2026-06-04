# 智慧记新版文档中心

这里是当前项目的正式文档入口。

## 阅读顺序

1. [40-batch-development-master-plan.md](./spec/40-batch-development-master-plan.md)
2. [00-product-overview.md](./spec/00-product-overview.md)
3. [01-status-taxonomy.md](./spec/01-status-taxonomy.md)
4. [02-domain-model-overview.md](./spec/02-domain-model-overview.md)
5. [10-auth-and-tenant.md](./spec/10-auth-and-tenant.md)
6. 业务域 spec：`20 ~ 32`

## 文档分层

- `docs/spec/`：新版正式规范中心，后续开发以此为准
- `docs/legacy/`：历史审计、迁移、分析记录，仅作参考
- `docs/design-mockups/`：Android 统一视觉真源，但不替代 `docs/spec/` 的需求规范

## 当前总控入口

- [40-batch-development-master-plan.md](./spec/40-batch-development-master-plan.md)
  - 这是后续批次推进的唯一总控文档
  - 后端、安卓数据层、安卓 feature、安卓 UI、联调与验收都要在这里持续打标

## 状态字段

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 说明

- 会员体系当前不纳入新版范围，已在 spec 中标记为 `新版需要去掉`
- 旧文档不删除，但不再作为主规范
- 后续每完成一个批次或一组代码改动，除更新对应 spec / technical-analysis 文档外，还必须回写 `40-batch-development-master-plan.md`

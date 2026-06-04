# 27 媒体与附件域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| media_assets | 待验证 | 旧版有图片资源 | 媒体资源表 | 已新增 `media_assets`、`MediaAssetEntity/Repository`、`V2MediaService/Controller` 与 `/v2/media/assets/*` | 已补 service/controller 定向回归，仍待真实上传链与安卓联调 |
| media_bindings | 待验证 | 旧版支持资源挂接 | 绑定关系表 | 已新增 `media_bindings`、`MediaBindingEntity/Repository`、`V2MediaService/Controller` 与 `/v2/media/bindings/*` | 已补 owner 维度绑定校验与定向回归，仍待真实附件场景联调；V14 迁移已补 `ON DELETE CASCADE`，会话删除时关联绑定自动级联 |

# 2026-07-03 154 后端备份存在性确认

## 结论

- `154` 主机上已确认存在智慧记后端运行实例与历史备份
- 这次确认只用于“备份是否存在”的只读核验，不代表已经执行恢复演练

## 运行态

- 在线容器：
  - `zhihuiji154-backend`
  - `zhihuiji154-postgres`
  - `zhihuiji154-redis`

## 已确认的备份/归档路径

- `/opt/zhihuiji-backend`
- `/root/backup`
- `/root/backup/zhihuiji`
- `/root/backup/zhihuiji-backend-20260615-163807.tgz`
- `/root/backup/zhihuiji/opt-zhihuiji-backend-20260630-081412.tgz`
- `/root/backup/zhihuiji/zhihuiji154-backend.image.20260630-081412.json`
- `/root/backup/zhihuiji/zhihuiji154-backend.inspect.20260630-081412.json`
- `/root/zhihuiji-backend-0.1.0-20260630.jar`
- `/root/zhihuiji-backend-20260616.tgz`
- `/root/zhihuiji-backend-deploy-20260630.tgz`
- `/root/zhihuiji154_bundle.tar.gz`

## 边界

- 当前仓库内没有恢复演练日志，因此不能把“备份存在”升级成“备份可恢复已验证”
- `124` 本轮有限只读搜索未看到同等级的智慧记后端备份目录，因此本证据页仅确认 `154`

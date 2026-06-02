# technical-analysis 总索引

> 历史说明：`docs/technical-analysis/` 现在承担“按真实代码目录展开的实现分析文档”角色。  
> 新版需求边界、领域模型与演进策略以 [docs/spec](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec) 为主规范。  
> 旧的风险盘点与修复结论不再作为唯一事实来源，阅读时请以当前源码和本目录 README 为准。

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 阅读顺序

1. 先看 [docs/spec/00-product-overview.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/00-product-overview.md)
2. 再看 [docs/spec/02-domain-model-overview.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec/02-domain-model-overview.md)
3. 然后按代码目录阅读本目录：
   - [android/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/README.md)
   - [server/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/README.md)

## 目录结构

### Android

- [android/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/README.md)
- [android/app/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/app/README.md)
- [android/backdrop/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/backdrop/README.md)
- [android/core/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/core/README.md)
- [android/data/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/data/README.md)
- [android/feature/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/android/feature/README.md)

### Server

- [server/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/README.md)
- [server/api/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/api/README.md)
- [server/infrastructure/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/infrastructure/README.md)
- [server/entity/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/entity/README.md)
- [server/repository/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/repository/README.md)
- [server/service/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/service/README.md)
- [server/resources/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/technical-analysis/server/resources/README.md)

## 本轮重建原则

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `technical-analysis` 目录 | 新版已做 | 旧文档以问题清单为主 | 变成按真实代码目录组织的分析入口 | 已按 android/server 重建 | 后续持续细化到字段和接口级 |
| Android README 覆盖率 | 新版已做 | 仅部分模块有说明 | 与 `master-goods-android` 目录一一对应 | 已补齐目录级 README | 子模块继续细化 |
| Server README 覆盖率 | 新版已做 | 只覆盖局部子目录 | 与 `src/main/java` 和 `src/main/resources` 对齐 | 已补齐目录级 README | 以 `/v2` 改造计划为主线 |
| 旧的“问题总清单”写法 | 新版需要去掉 | 以静态风险枚举为主 | 改成“当前实现 + spec 差距 + 下一步” | 本目录已切换 | 历史问题仅作参考 |

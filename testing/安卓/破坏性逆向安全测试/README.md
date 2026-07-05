# 安卓破坏性逆向安全测试

本目录用于记录 Android APK/运行时的破坏性逆向安全测试，不与常规单元/功能/性能测试混账。

路由策略：

- 总编排：`attack-chain`
- 主执行：`apk-reverse`
- 动态插桩：`mobile-reverse`
- 接口联动：`api-security`

目录内容：

- `TEST_PLAN.md`：测试范围、证据标准、执行顺序
- `reverse_attack_matrix.csv`：破坏性逆向用例总台账
- `scripts/`：后续放置解包、重签、Hook、抓包、回放脚本

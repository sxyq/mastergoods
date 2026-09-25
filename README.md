# master-goods — 重构中

本项目正在**从零重新实现**。当前分支不包含任何生产代码。

## 历史完整实现

旧的完整实现（Spring Boot 后端、Android / iOS / Web 客户端、Agent 运行时、数据库迁移、测试与运行证据）已完整封存，随时可查：

```bash
git show archive/pre-domain-rewrite:<path>
git grep <symbol> archive/pre-domain-rewrite
```

- 分支：`archive/pre-domain-rewrite`
- 提交：`19283a4dcd1b9bfc16d01859416547b82d76c06d`
- 标签：`pre-domain-rewrite-2026-09-25`

## 当前 rewrite 分支只包含

```text
master-goods/
├── .gitignore
├── AGENTS.md
├── README.md
├── reference/
│   └── ui-android/          独立 Android UI 参考工程（仅设计参考）
│       ├── README.md
│       ├── UI-DESIGN-SPEC.md
│       ├── screenshots/
│       └── app/             最小 Compose 参考页面 UIReferenceScreen
└── docs/
    ├── rewrite/             本轮重构文档
    │   ├── REWRITE-CLEAN-SLATE-001A-INVENTORY.md
    │   └── LEGACY-DATA-COMPATIBILITY.md
    └── agent/               Agent 设计文档与索引

data/database/               本地 Legacy 数据库样本（不进 Git）
```

- **新业务代码尚未开始。**
- `reference/ui-android` 是历史 UI 视觉语言参考，**不属于生产源码**，新生产代码不得依赖它。
- Agent 设计文档在 `docs/agent/`；旧 Agent 实现与测试运行证据只存在于 archive 分支。
- 旧数据的迁移方向与样本校验值见 `docs/rewrite/LEGACY-DATA-COMPATIBILITY.md`。

## 下一步

新的产品需求、领域模型、数据库设计与客户端实现将在后续阶段启动，统一进入新的 `Code/` 结构。

# Legacy Data Compatibility

日期：2026-09-25
分支：rewrite/business-v3
性质：兼容设计基础文档。后续产品需求文档同样放在 `docs/rewrite/`。

---

## A. 兼容目标

本次重写对旧数据的承诺是：

```
COMPATIBLE WITH LEGACY DATA
```

而不是 `COMPATIBLE WITH LEGACY DATABASE STRUCTURE`。

含义：旧数据库的**数据内容**要能迁移到新系统；旧数据库的**表结构**不保留、不复刻。新系统使用重新设计的领域模型与表结构。

---

## B. 固定迁移方向

```
Legacy Database
      ↓
  Exporter
      ↓
Transform / Clean
      ↓
New Import Package
      ↓
  New System
```

该方向固定，不提供反向同步、不提供新库回写旧库、不做双写。

---

## C. 本地样本（实测）

以下为本机 `data/database/` 下样本文件的实测结果（`stat -f%z` 取 size，`shasum -a 256` 取 sha256，2026-09-25 实测）：

| path | size (bytes) | sha256 |
|---|---:|---|
| `data/database/migration_source_zhihuiji/data.db` | 512000 | `c16a2f4cf0bb2ecd6438826ed31c988da1b80711661a129dd5b05cfaf79e5ace` |
| `data/database/migration_source_zhihuiji/demo.db` | 512000 | `0c6cb7ce2b34c8fd423f241966b12cea2cf3e9b47d4122351c2baf5a040c9e15` |
| `data/database/migration_source_zhihuiji/9ffd7446d3f1480197908a113565d0ef.db` | 15110144 | `7bf0b67d66e2430fde3137f57bbd060a1f78c3d3623702a15b49570078a4c117` |
| `data/database/migration_output/zhihuiji.db` | 1961984 | `15a4f236ac98b0b73c60de5f5ee3266694a205f14c6b6ae46939b2ae5b775aaa` |
| `data/database/migration_output/zhihuiji.device.db` | 1961984 | `15a4f236ac98b0b73c60de5f5ee3266694a205f14c6b6ae46939b2ae5b775aaa` |

补充事实：

- `zhihuiji.db` 与 `zhihuiji.device.db` 的 size 与 sha256 完全相同，当前是同一份内容的两个文件。
- 这些样本文件**不进 Git**：`git ls-files data/database` 返回 0 条。

---

## D. 已知事实

- `data.db` 是老 SQLite ERP schema。用 `sqlite3 -readonly` 实测：非 `sqlite_%` 系统表共 **70** 个（与此前「约 70」的结论一致）。
- `zhihuiji.db` 是后续简化的 Room 数据库。同法实测：非 `sqlite_%` 系统表共 **11** 个，明显小于 `data.db`，符合「简化后的 Room 结构」这一定位。

---

## E. 原则

1. 不把旧库的 70 张表重新复制到新数据库。
2. 旧 DB 只作为 `Legacy Source`——迁移管线的输入，不是新系统运行时依赖。
3. 新系统的表结构由新领域设计独立决定，与旧表结构无对应承诺。

---

## F. 数据价值方向

后续需求分析的重点方向（高价值）：

- 商品、客户、供应商
- 销售历史、采购历史
- 收付款、账户
- 库存、往来欠款

默认低价值（默认不迁移，除非后续另有决定）：

- 打印记录
- UI layout
- cache
- 旧 sync internal state

**以上只是当前兼容设计基础；最终 MUST / SHOULD / OPTIONAL / IGNORE 由后续产品需求阶段决定。**

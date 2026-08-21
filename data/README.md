# 本地数据库资料

本目录只保存本地迁移和数据库资料，不承载后端生产源码。

| 子目录 | 用途 | 版本管理 |
|---|---|---|
| `data/database/migration_source_zhihuiji/` | 迁移源 SQLite 数据库 | 本地资料，默认忽略 |
| `data/database/migration_output/` | 迁移生成的 SQLite 数据库和备份 | 本地资料，默认忽略 |
| `data/media/` | 本地媒体存储目录 | 按实际资源决定 |
| `data/research/` | 研究数据和实验输入 | 按数据敏感性和实际用途决定 |

迁移脚本默认路径已统一为 `data/database/`。数据库文件不得放回源码目录或仓库根目录。

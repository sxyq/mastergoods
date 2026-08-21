# Agent Observability

独立的服务器端 Agent 评测观测页。页面只读取 `public/data/agent-evaluation.json`，不调用生产接口，也不包含模型密钥。

## 更新真实数据

在仓库根目录执行：

```bash
cd Code/frontend/agent-observability
npm run build-data
python3 -m http.server 4173
```

打开 <http://127.0.0.1:4173>。

数据构建脚本默认读取：

- `testing/.artifacts/2026-08-02-server-agent-eval/mg-deepseek-all-tools-20260802/`
- `testing/.artifacts/2026-08-02-server-agent-eval/wave1-deepseek-performance-complete-20260802T201300+0800/`
- `testing/.artifacts/2026-08-02-server-agent-eval/wave1-deepseek-live-20260802T194300+0800/`

Token 数量来自 provider 日志中的真实 `prompt` / `completion` 记录。缓存命中和当前请求的精确流式 Token/s 如果 provider 没有上报，会显示为“未上报”，不会用 0 或估算值冒充真实指标。

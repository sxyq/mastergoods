# 2026-06-30 Android 真机首页/一级页签性能采样（15:56）

## 元数据

- 时间：2026-06-30 15:56 CST
- 执行人：Codex
- 设备：`d715a3a4` / `25010PN30C`
- 包名：`com.zhihuiji.app`
- 采样方式：
  - `adb shell dumpsys gfxinfo com.zhihuiji.app`
  - `adb shell dumpsys gfxinfo com.zhihuiji.app framestats`
  - `adb shell dumpsys meminfo com.zhihuiji.app`

## 采样流

1. `gfxinfo reset`
2. 启动 App
3. 依次点击底部一级页签
   - `单据`
   - `档案`
   - `报表`
   - `助手`
4. 导出 `gfxinfo` 与 `meminfo`

## gfxinfo 摘要

- `Total frames rendered: 5243`
- `Janky frames: 266 (5.07%)`
- `50th percentile: 10ms`
- `90th percentile: 26ms`
- `95th percentile: 34ms`
- `99th percentile: 73ms`
- `Number Slow UI thread: 248`
- `Number Slow issue draw commands: 199`
- `GPU 50th percentile: 5ms`
- `GPU 90th percentile: 13ms`
- `GPU 95th percentile: 18ms`
- `GPU 99th percentile: 24ms`

## meminfo 摘要

- `TOTAL PSS: 235671 KB`
- `TOTAL RSS: 374632 KB`
- `Native Heap PSS: 33615 KB`
- `Dalvik Heap PSS: 15533 KB`
- `Graphics: 51040 KB`
- `Views: 14`
- `Activities: 2`

## 保守解释

- 这是一份真机基础采样，不是完整 Perfetto 根因分析
- 可以证明当前首页/一级页签流不是“完全无性能证据”
- 但还不能替代：
  - 大列表滚动
  - 同步大批量数据
  - 媒体上传链
  - AI 流式会话
  - 大单据编辑
  这些更重场景的专项性能验证

## 严格结论

- B11 性能项已从“零证据”推进到“真机基础采样已存在”
- 发布级性能验收仍未完成

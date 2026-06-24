<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

interface PlanModule {
  key: string
  title: string
  status: 'done' | 'in-progress' | 'planned'
  progress: number
  description: string
  route?: string
  routeLabel?: string
}

interface PlanMilestone {
  phase: string
  title: string
  items: string[]
  status: 'done' | 'in-progress' | 'planned'
}

const modules: PlanModule[] = [
  {
    key: 'backend-v2',
    title: '后端 V2 API',
    status: 'in-progress',
    progress: 85,
    description: 'V2 控制器主体完成，媒体上传/分页/导入恢复已补齐，V2 Auth 待实施。',
    route: '/settings/database',
    routeLabel: '查看数据库健康',
  },
  {
    key: 'android',
    title: 'Android App',
    status: 'in-progress',
    progress: 90,
    description: '28 模块主体完成，V1 死代码已清理，SSE 重连已补齐，Finance V2 迁移待后端。',
    route: '/references/mobile/P01-documents-center',
    routeLabel: '查看移动端参考',
  },
  {
    key: 'ios',
    title: 'iOS App',
    status: 'in-progress',
    progress: 70,
    description: 'API Client 完整，主体功能可用，缺注册/客户独立模块/Agent 工作台等专页。',
  },
  {
    key: 'web',
    title: 'Web 管理端',
    status: 'in-progress',
    progress: 88,
    description: 'PC 页面主体完成，死代码已清理，搜索/表单/草稿管理已补齐。',
    route: '/dashboard',
    routeLabel: '进入经营看板',
  },
]

const milestones: PlanMilestone[] = [
  {
    phase: 'Phase 1',
    title: 'P0 阻塞项',
    items: [
      '后端 AI 流式取消语义对齐',
      'iOS 相机权限声明',
      'Web logout 调用后端 API',
    ],
    status: 'done',
  },
  {
    phase: 'Phase 2',
    title: 'P1 重要项',
    items: [
      '后端 CORS/分页/N+1/导入恢复/媒体上传',
      'Android V1 死代码清理/SSE 重连',
      'Web 死代码/硬编码/搜索/表单/草稿',
    ],
    status: 'done',
  },
  {
    phase: 'Phase 3',
    title: 'P2 次要项',
    items: [
      '后端 V2 Auth 实现',
      'Redis 使用决策',
      'iOS 功能补齐（12 项）',
      '跨端 V2 迁移（Finance/Report）',
    ],
    status: 'in-progress',
  },
]

const statusLabels: Record<PlanModule['status'], string> = {
  done: '已完成',
  'in-progress': '进行中',
  planned: '待规划',
}

const statusTones: Record<PlanModule['status'], string> = {
  done: 'var(--success, #18a34a)',
  'in-progress': 'var(--warning, #f5a623)',
  planned: 'var(--info, #3b82f6)',
}

const overallProgress = Math.round(modules.reduce((sum, m) => sum + m.progress, 0) / modules.length)

function navigate(route?: string) {
  if (route) router.push(route)
}
</script>

<template>
  <section class="planning-page">
    <header class="planning-header">
      <div>
        <p class="eyebrow">Planning</p>
        <h2>产品规划概览</h2>
        <p class="header-desc">智慧记全项目四端进度汇总，基于 2026-06-23 审查结果。</p>
      </div>
      <div class="overall-progress">
        <div class="progress-ring">
          <span>{{ overallProgress }}%</span>
        </div>
        <small>整体完成度</small>
      </div>
    </header>

    <section class="modules-grid">
      <article
        v-for="mod in modules"
        :key="mod.key"
        class="module-card"
        :class="{ clickable: mod.route }"
        @click="navigate(mod.route)"
      >
        <div class="module-head">
          <h3>{{ mod.title }}</h3>
          <span class="status-chip" :style="{ backgroundColor: statusTones[mod.status] }">
            {{ statusLabels[mod.status] }}
          </span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${mod.progress}%`, backgroundColor: statusTones[mod.status] }" />
        </div>
        <p class="module-desc">{{ mod.description }}</p>
        <button v-if="mod.route && mod.routeLabel" type="button" class="module-link">
          {{ mod.routeLabel }}
        </button>
      </article>
    </section>

    <section class="milestones">
      <h3>里程碑</h3>
      <div class="milestone-list">
        <article
          v-for="milestone in milestones"
          :key="milestone.phase"
          class="milestone-card"
          :data-status="milestone.status"
        >
          <div class="milestone-head">
            <span class="phase-tag">{{ milestone.phase }}</span>
            <span class="status-chip" :style="{ backgroundColor: statusTones[milestone.status] }">
              {{ statusLabels[milestone.status] }}
            </span>
          </div>
          <h4>{{ milestone.title }}</h4>
          <ul>
            <li v-for="item in milestone.items" :key="item">{{ item }}</li>
          </ul>
        </article>
      </div>
    </section>

    <section class="next-steps">
      <h3>下一步建议</h3>
      <ol>
        <li>后端 V2 Auth 实现，迁移 /v1/auth/* 到 /v2/auth/*</li>
        <li>后端补 V2 finance-records 端点，解除 Android FinanceViewModel 迁移阻塞</li>
        <li>iOS 补齐客户/供应商独立模块和 Agent 工作台专页</li>
        <li>后端 Redis 决策：移除冗余依赖或实现缓存逻辑</li>
      </ol>
    </section>
  </section>
</template>

<style scoped>
.planning-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.planning-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.planning-header h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.header-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.overall-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.progress-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: conic-gradient(var(--warning, #f5a623) 0deg, rgba(255,255,255,0.08) 0deg);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.progress-ring::before {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: var(--bg-card, #1a1a2e);
}

.progress-ring span {
  position: relative;
  font-size: 16px;
  font-weight: 700;
}

.overall-progress small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.module-card {
  padding: 18px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
}

.module-card.clickable {
  cursor: pointer;
  transition: border-color 0.2s;
}

.module-card.clickable:hover {
  border-color: rgba(255, 255, 255, 0.25);
}

.module-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.module-head h3 {
  font-size: 16px;
  font-weight: 600;
}

.status-chip {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  color: #fff;
  white-space: nowrap;
}

.progress-bar {
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.module-desc {
  font-size: 13px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 12px;
}

.module-link {
  font-size: 12px;
  color: var(--primary, #3b82f6);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.milestones {
  margin-bottom: 32px;
}

.milestones h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.milestone-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.milestone-card {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
}

.milestone-card[data-status="done"] {
  border-color: rgba(24, 163, 74, 0.3);
}

.milestone-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.phase-tag {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgba(255, 255, 255, 0.5);
}

.milestone-card h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

.milestone-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.milestone-card li {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  padding-left: 12px;
  position: relative;
}

.milestone-card li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: rgba(255, 255, 255, 0.3);
}

.next-steps h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.next-steps ol {
  padding-left: 20px;
}

.next-steps li {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.8;
}
</style>

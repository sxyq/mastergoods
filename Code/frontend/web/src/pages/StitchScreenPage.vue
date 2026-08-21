<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { StitchScreen } from '@/app/router/stitch-screens'
import { roleLabels } from '@/entities/auth/roles'
import { buildPageModel } from '@/entities/screen/page-models'
import { loadLiveScreenData, type ScreenLiveRow } from '@/entities/screen/live-screen-data'
import { useSession } from '@/app/stores/session'

const route = useRoute()
const session = useSession()
const screen = computed(() => route.meta.screen as StitchScreen | undefined)
const allowed = computed(() => session.hasPermission(screen.value?.permission))
const pageModel = computed(() => (screen.value ? buildPageModel(screen.value) : undefined))
const liveData = ref<Awaited<ReturnType<typeof loadLiveScreenData>>>(null)
const liveLoading = ref(false)
const liveError = ref('')
const activeStatus = ref('全部')
const searchKeyword = ref('')
const selectedRowIndex = ref(0)
const actionLog = ref('等待业务操作')
const formValues = ref<Record<string, string>>({})
const writePermissions = computed(() => (screen.value?.permission ?? []).filter((permission) => permission.endsWith(':write')))
const canManageUsers = computed(() => session.hasPermission(['users:manage']))
const sourceLabel = computed(() => {
  if (session.source.value === 'api') return 'Real Backend API / owner_user_id'
  if (screen.value?.source === 'mcp-desktop') return 'Stitch MCP Desktop'
  if (screen.value?.source === 'pc-planned') return 'PC Planned From Android'
  return 'Local Mobile Reference'
})
const canWrite = computed(() => writePermissions.value.length > 0 && session.hasPermission(writePermissions.value))
const displayMetrics = computed(() => liveData.value?.metrics ?? pageModel.value?.metrics ?? [])
const displayColumns = computed(() => pageModel.value?.table.columns ?? [])
type IndexedScreenLiveRow = ScreenLiveRow & {
  searchText: string
}

const displayRows = computed<IndexedScreenLiveRow[]>(() => {
  if (liveData.value?.rows) {
    return liveData.value.rows.map((row) => ({
      ...row,
      searchText: row.cells.join(' ').toLowerCase(),
    }))
  }
  return (pageModel.value?.table.rows ?? []).map((cells) => ({
    cells,
    statusTokens: [],
    searchText: cells.join(' ').toLowerCase(),
  }))
})
const displaySummary = computed(() => liveData.value?.summary ?? pageModel.value?.summary ?? [])
const visibleRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const rows: IndexedScreenLiveRow[] = []
  for (const row of displayRows.value) {
    if (keyword && !row.searchText.includes(keyword)) continue
    if (activeStatus.value !== '全部' && !matchesStatus(row, activeStatus.value)) continue
    rows.push(row)
  }
  return rows
})
const selectedRow = computed(() => visibleRows.value[selectedRowIndex.value]?.cells ?? visibleRows.value[0]?.cells)
const currentPageLabel = computed(() => {
  const total = visibleRows.value.length
  if (total === 0) return '0 条记录'
  return `当前显示 1-${total} 条 / 共 ${displayRows.value.length} 条`
})

watch(() => route.fullPath, () => {
  activeStatus.value = '全部'
  searchKeyword.value = ''
  selectedRowIndex.value = 0
  actionLog.value = '等待业务操作'
  formValues.value = {}
})

watch(visibleRows, (rows) => {
  if (rows.length === 0) {
    selectedRowIndex.value = 0
    return
  }
  if (selectedRowIndex.value > rows.length - 1) {
    selectedRowIndex.value = 0
  }
})

watch(selectedRow, (row) => {
  const model = pageModel.value
  if (!model || !row) return
  const next: Record<string, string> = {}
  model.formSections.forEach((section) => {
    section.fields.forEach((field, fieldIndex) => {
      if (field.includes('单据') && row[0]) {
        next[field] = row[0]
      } else if (fieldIndex < row.length && row[fieldIndex]) {
        next[field] = row[fieldIndex]
      } else {
        next[field] = ''
      }
    })
  })
  formValues.value = next
})

watch(
  [() => screen.value?.route, () => session.source.value, () => session.token.value, searchKeyword],
  async ([routePath, source, token]) => {
    if (!routePath || source !== 'api' || !token) {
      liveData.value = null
      liveError.value = ''
      return
    }
    liveLoading.value = true
    liveError.value = ''
    try {
      liveData.value = await loadLiveScreenData(routePath, token, searchKeyword.value)
    } catch (error) {
      liveData.value = null
      liveError.value = error instanceof Error ? error.message : '真实数据加载失败'
    } finally {
      liveLoading.value = false
    }
  },
  { immediate: true },
)

function selectStatus(status: string) {
  activeStatus.value = status
  selectedRowIndex.value = 0
}

function selectRow(index: number) {
  selectedRowIndex.value = index
}

function runAction(action: string) {
  const model = pageModel.value
  if (!model) return
  const readonlyAction = ['查看', '打印', '导出单据', '导出', '流水追踪', '查看运行审计'].some((item) => action.includes(item))
  if (!readonlyAction && !canWrite.value && !canManageUsers.value) {
    actionLog.value = `${roleLabels[session.role.value]} 无法执行「${action}」`
    return
  }
  if (action === '保存草稿') {
    const filledFields = Object.entries(formValues.value).filter(([, value]) => value.trim() !== '')
    if (filledFields.length === 0) {
      actionLog.value = '表单为空，无法保存草稿'
      return
    }
    actionLog.value = `已保存草稿（${filledFields.length} 个字段）：${filledFields.map(([key, value]) => `${key}=${value}`).join('，')}`
    return
  }
  const target = selectedRow.value?.[0] ?? model.title
  actionLog.value = `已在 ${model.title} 对「${target}」执行：${action}`
}

function matchesStatus(row: IndexedScreenLiveRow, status: string) {
  if (row.statusTokens.length > 0) {
    return row.statusTokens.includes(status)
  }
  return row.searchText.includes(status.replace(/^待/, '').replace(/^已/, ''))
}
</script>

<template>
  <section v-if="screen" class="screen-page">
    <div v-if="!allowed" class="access-denied">
      <h2>当前角色不可访问</h2>
      <p>{{ roleLabels[session.role.value] }} 没有访问「{{ screen.title }}」所需权限。</p>
    </div>

    <template v-else-if="pageModel">
      <section class="screen-hero">
        <div>
          <p class="eyebrow">{{ sourceLabel }} / {{ screen.module }}</p>
          <h2>{{ pageModel.title }}</h2>
          <p>{{ pageModel.description }}</p>
        </div>
        <div class="api-note">
          <strong>权限</strong>
          <span>{{ screen.permission.join(' / ') }}</span>
        </div>
      </section>

      <section class="workbench-grid">
        <article class="panel operations-panel">
          <div class="panel-head">
            <div>
          <p class="eyebrow">业务工作台</p>
              <h3>{{ pageModel.primaryAction }}</h3>
            </div>
            <button type="button" :disabled="!canWrite && !canManageUsers" @click="runAction(pageModel.primaryAction)">
              {{ pageModel.primaryAction }}
            </button>
          </div>

          <div class="metrics-grid compact">
            <article v-for="metric in displayMetrics" :key="metric.label" class="metric-card">
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.detail }}</p>
            </article>
          </div>

          <div class="status-tabs">
            <button
              v-for="status in pageModel.statusTabs"
              :key="status"
              type="button"
              :class="{ active: activeStatus === status }"
              @click="selectStatus(status)"
            >
              {{ status }}
            </button>
          </div>

          <div class="filter-bar">
            <label class="search-box">
              <span>搜索</span>
              <input v-model="searchKeyword" :placeholder="pageModel.filters[0] ?? '关键词'" />
            </label>
            <button v-for="filter in pageModel.filters.slice(1)" :key="filter" type="button">{{ filter }}</button>
          </div>

          <div v-if="session.source.value === 'api' && liveLoading" class="form-success">正在从真实后端加载当前页面数据...</div>
          <div v-if="session.source.value === 'api' && liveError" class="form-error">{{ liveError }}</div>

          <div class="table-shell">
            <table>
              <thead>
                <tr>
                  <th v-for="column in displayColumns" :key="column">{{ column }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, rowIndex) in visibleRows"
                  :key="rowIndex"
                  :class="{ selected: rowIndex === selectedRowIndex }"
                  @click="selectRow(rowIndex)"
                >
                  <td v-for="(cell, cellIndex) in row.cells" :key="`${rowIndex}-${cellIndex}`">{{ cell }}</td>
                </tr>
                <tr v-if="visibleRows.length === 0">
                  <td :colspan="displayColumns.length" class="empty-cell">没有匹配的数据</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="workbench-footer">
            <span>{{ currentPageLabel }}</span>
            <div>
              <button
                v-for="action in pageModel.secondaryActions"
                :key="action"
                type="button"
                class="ghost-action"
                @click="runAction(action)"
              >
                {{ action }}
              </button>
            </div>
          </div>

          <div class="action-log">{{ actionLog }}</div>
        </article>

        <aside class="panel contract-panel">
          <p class="eyebrow">接口与数据库</p>
          <h3>页面接口契约</h3>
          <div class="contract-list">
            <article v-for="contract in pageModel.contracts.slice(0, 7)" :key="`${contract.method}-${contract.path}`">
              <span>{{ contract.method }}</span>
              <strong>{{ contract.path }}</strong>
              <p>{{ contract.purpose }}</p>
            </article>
          </div>
          <div class="table-tags">
            <span v-for="table in pageModel.databaseTables" :key="table">{{ table }}</span>
          </div>
        </aside>
      </section>

      <section class="form-summary-grid">
        <article class="panel form-panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">PC 表单区</p>
              <h3>{{ screen.route.includes('edit') || screen.route.includes('adjust') ? '新建/编辑业务单据' : '详情与快速处理' }}</h3>
            </div>
            <button type="button" :disabled="!canWrite && !canManageUsers" @click="runAction('保存草稿')">保存草稿</button>
          </div>
          <div class="form-section-grid">
            <fieldset v-for="section in pageModel.formSections" :key="section.title">
              <legend>{{ section.title }}</legend>
              <label v-for="field in section.fields" :key="field">
                <span>{{ field }}</span>
                <input v-model="formValues[field]" :placeholder="field" />
              </label>
            </fieldset>
          </div>
        </article>

        <aside class="panel summary-panel">
          <p class="eyebrow">业务汇总</p>
          <h3>当前处理摘要</h3>
          <dl>
            <div v-for="item in displaySummary" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
          <div class="selected-record">
            <span>选中记录</span>
            <strong>{{ selectedRow?.[0] ?? '暂无' }}</strong>
            <small>{{ selectedRow?.slice(1, 4).join(' / ') }}</small>
          </div>
        </aside>
      </section>

      <section class="preview-layout">
        <article v-if="screen.imagePath" class="design-preview">
          <img :src="screen.imagePath" :alt="screen.title" />
        </article>
        <article v-else class="design-preview empty-preview">
          <strong>该 Stitch 屏幕只有 HTML 规划文件</strong>
          <p>可通过右侧实现说明继续拆分为 UI 页面和接口任务。</p>
        </article>
        <aside class="implementation-card">
          <h3>PC 化实施说明</h3>
          <ul>
            <li>桌面屏作为主实现来源；未出桌面稿的页面先按安卓/移动端业务能力升格为 PC 待设计页。</li>
            <li>列表页采用筛选栏、表格、批量操作和详情入口。</li>
            <li>新建/编辑页采用 PC 表单、明细表和权限控制按钮。</li>
            <li>所有写操作按店长、店长助理和员工角色启用或禁用。</li>
            <li>数据库连接、导入和备份恢复只允许店长（总）进入。</li>
          </ul>
        </aside>
      </section>
    </template>
  </section>
</template>

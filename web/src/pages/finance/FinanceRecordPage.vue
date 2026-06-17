<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  fetchAccounts,
  fetchFinanceRecords,
  type AccountRecord,
  type FinanceRecord,
} from '@/shared/api/client'
import {
  FINANCE_EXPENSE,
  FINANCE_INCOME,
  financeMethodLabel,
  financeTypeLabel,
  formatCurrency,
  formatDateTime,
} from '@/shared/utils/business'

const route = useRoute()
const router = useRouter()
const session = useSession()

const records = ref<FinanceRecord[]>([])
const accounts = ref<AccountRecord[]>([])
const loading = ref(false)
const error = ref('')
const searchKeyword = ref('')
const typeFilter = ref('all')
const selectedRecordId = ref<number | null>(null)

const queryRecordId = computed(() => {
  const raw = route.query.id
  const first = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(first)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const selectedRecord = computed(() => records.value.find((item) => item.id === selectedRecordId.value) ?? records.value[0] ?? null)
const totalIncome = computed(() => records.value.filter((item) => item.type === FINANCE_INCOME).reduce((sum, item) => sum + item.amount, 0))
const totalExpense = computed(() => records.value.filter((item) => item.type === FINANCE_EXPENSE).reduce((sum, item) => sum + item.amount, 0))

watch(
  [() => session.source.value, () => session.token.value, searchKeyword, typeFilter, queryRecordId],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      records.value = []
      accounts.value = []
      error.value = ''
      return
    }
    await loadPage()
  },
  { immediate: true },
)

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  try {
    const [nextRecords, nextAccounts] = await Promise.all([
      fetchFinanceRecords(session.token.value, {
        keyword: searchKeyword.value.trim() || undefined,
        type: typeFilter.value === 'all' ? undefined : Number(typeFilter.value),
        page: 0,
        size: 200,
      }),
      fetchAccounts(session.token.value),
    ])
    records.value = nextRecords
    accounts.value = nextAccounts
    selectedRecordId.value = queryRecordId.value ?? nextRecords[0]?.id ?? null
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '资金流水加载失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">资金流水 / Finance Records</p>
        <h2>财务流水专页</h2>
        <p>对齐安卓端资金流水页，读取真实财务记录与资金账户，并承接日常支出录入入口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" :disabled="!isApiSource" @click="router.push('/finance/daily-expense')">记录支出</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后读取资金流水。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="form-success">正在加载资金流水...</p>

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="green">
        <span>收入合计</span>
        <strong>{{ formatCurrency(totalIncome) }}</strong>
        <p>{{ records.filter((item) => item.type === FINANCE_INCOME).length }} 条收入记录</p>
      </article>
      <article class="metric-card" data-tone="red">
        <span>支出合计</span>
        <strong>{{ formatCurrency(totalExpense) }}</strong>
        <p>{{ records.filter((item) => item.type === FINANCE_EXPENSE).length }} 条支出记录</p>
      </article>
      <article class="metric-card" data-tone="blue">
        <span>资金账户</span>
        <strong>{{ accounts.length }}</strong>
        <p>账户总余额 {{ formatCurrency(accounts.reduce((sum, item) => sum + item.balance, 0)) }}</p>
      </article>
    </section>

    <section class="panel">
      <div class="business-toolbar">
        <label class="search-box">
          <span>搜索流水</span>
          <input v-model="searchKeyword" placeholder="流水号 / 分类 / 往来方" />
        </label>
        <label class="compact-field">
          <span>方向</span>
          <select v-model="typeFilter">
            <option value="all">全部</option>
            <option value="1">收入</option>
            <option value="2">支出</option>
          </select>
        </label>
      </div>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>流水号</th>
                <th>方向</th>
                <th>分类</th>
                <th>往来方</th>
                <th>金额</th>
                <th>方式</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="record in records"
                :key="record.id"
                :class="{ selected: record.id === selectedRecord?.id }"
                @click="selectedRecordId = record.id"
              >
                <td>{{ record.recordNo }}</td>
                <td>{{ financeTypeLabel(record.type) }}</td>
                <td>{{ record.category || '--' }}</td>
                <td>{{ record.partnerName || '--' }}</td>
                <td>{{ formatCurrency(record.amount) }}</td>
                <td>{{ financeMethodLabel(record.method) }}</td>
                <td>{{ formatDateTime(record.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && records.length === 0">
                <td colspan="7" class="empty-cell">暂无资金流水</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">流水详情</p>
        <h3>{{ selectedRecord?.recordNo || '请选择流水记录' }}</h3>

        <div v-if="selectedRecord" class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>方向</dt>
                <dd>{{ financeTypeLabel(selectedRecord.type) }}</dd>
              </div>
              <div>
                <dt>分类</dt>
                <dd>{{ selectedRecord.category || '--' }}</dd>
              </div>
              <div>
                <dt>往来方</dt>
                <dd>{{ selectedRecord.partnerName || '--' }}</dd>
              </div>
              <div>
                <dt>金额</dt>
                <dd>{{ formatCurrency(selectedRecord.amount) }}</dd>
              </div>
              <div>
                <dt>支付方式</dt>
                <dd>{{ financeMethodLabel(selectedRecord.method) }}</dd>
              </div>
              <div>
                <dt>备注</dt>
                <dd>{{ selectedRecord.notes || '--' }}</dd>
              </div>
            </dl>
          </article>

          <article class="detail-card">
            <p class="eyebrow">账户概览</p>
            <div class="mini-list">
              <div v-for="account in accounts.slice(0, 6)" :key="account.id">
                <strong>{{ account.name }}</strong>
                <span>{{ formatCurrency(account.balance) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-preview">
          <strong>暂无可查看流水</strong>
          <p>请先选择一条资金流水记录。</p>
        </div>
      </aside>
    </section>
  </section>
</template>

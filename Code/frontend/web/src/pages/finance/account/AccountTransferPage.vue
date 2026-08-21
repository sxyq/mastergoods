<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useSession } from '@/app/stores/session'
import {
  createAccountTransfer,
  fetchAccounts,
  fetchAccountTransfers,
  type AccountRecord,
  type AccountTransferPayload,
  type AccountTransferRecord,
} from '@/shared/api/client'
import { sameEntityId } from '@/shared/utils/id'
import {
  accountTransferStatusLabel,
  formatCurrency,
  formatDateTime,
} from '@/shared/utils/business'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

const session = useSession()

const transfers = ref<AccountTransferRecord[]>([])
const accounts = ref<AccountRecord[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  fromAccountId: '',
  toAccountId: '',
  amount: '',
  fee: '',
  notes: '',
})

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['finance:write']))

const summary = computed(() => transfers.value.reduce((acc, item) => {
  acc.totalAmount += item.amount
  return acc
}, { totalAmount: 0 }))

const parsedAmount = computed(() => Number(form.amount || 0))
const sameAccount = computed(() => sameEntityId(form.fromAccountId, form.toAccountId))
const canSubmit = computed(() => isApiSource.value
  && canWrite.value
  && !saving.value
  && form.fromAccountId !== ''
  && form.toAccountId !== ''
  && !sameAccount.value
  && parsedAmount.value > 0)

watch(
  [() => session.source.value, () => session.token.value],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      transfers.value = []
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
  success.value = ''
  try {
    const [nextTransfers, nextAccounts] = await Promise.all([
      fetchAccountTransfers(session.token.value),
      fetchAccounts(session.token.value),
    ])
    transfers.value = nextTransfers
    accounts.value = nextAccounts
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '账户转账数据加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.fromAccountId = ''
  form.toAccountId = ''
  form.amount = ''
  form.fee = ''
  form.notes = ''
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload: AccountTransferPayload = {
      fromAccountId: form.fromAccountId,
      toAccountId: form.toAccountId,
      amount: parsedAmount.value,
      fee: form.fee.trim() ? Number(form.fee || 0) : null,
      notes: form.notes.trim() || null,
    }
    const created = await createAccountTransfer(session.token.value, payload)
    success.value = `已创建转账单 ${created.transferNo}`
    resetForm()
    await loadPage()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '账户转账创建失败'
  } finally {
    saving.value = false
  }
}

async function retryPage() {
  await loadPage()
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">账户转账 / Transfers</p>
        <h2>账户转账</h2>
        <p>记录资金账户之间的转账流水，真实调用 `/v2/account-transfers` 接口。</p>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后创建账户转账。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
      action-label="重新加载"
      @action="retryPage"
    />
    <PageStatusBanner v-else-if="success" tone="success" title="操作成功" :message="success" />
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载转账记录..." />

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>转账笔数</span>
        <strong>{{ transfers.length }}</strong>
        <p>当前数据域</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>转账总额</span>
        <strong>{{ formatCurrency(summary.totalAmount) }}</strong>
        <p>列表汇总金额</p>
      </article>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">转账列表</p>
            <h3>账户转账记录</h3>
          </div>
          <span class="session-source">{{ loading ? '加载中...' : `${transfers.length} 笔` }}</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>转账单号</th>
                <th>转出账户</th>
                <th>转入账户</th>
                <th>金额</th>
                <th>手续费</th>
                <th>状态</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="transfer in transfers" :key="transfer.id">
                <td>{{ transfer.transferNo }}</td>
                <td>{{ transfer.fromAccountName || '--' }}</td>
                <td>{{ transfer.toAccountName || '--' }}</td>
                <td>{{ formatCurrency(transfer.amount) }}</td>
                <td>{{ transfer.fee != null ? formatCurrency(transfer.fee) : '--' }}</td>
                <td>{{ accountTransferStatusLabel(transfer.status) }}</td>
                <td>{{ formatDateTime(transfer.createdAt) }}</td>
              </tr>
              <tr v-if="!loading && transfers.length === 0">
                <td colspan="7" class="empty-cell">暂无转账记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">新建转账</p>
            <h3>录入转账信息</h3>
          </div>
          <span class="session-source">转账金额 {{ formatCurrency(parsedAmount) }}</span>
        </div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>转出账户</span>
            <select v-model="form.fromAccountId" :disabled="!canWrite">
              <option value="">请选择转出账户</option>
              <option v-for="account in accounts" :key="account.id" :value="String(account.id)">
                {{ account.name }} / {{ formatCurrency(account.balance) }}
              </option>
            </select>
          </label>
          <label>
            <span>转入账户</span>
            <select v-model="form.toAccountId" :disabled="!canWrite">
              <option value="">请选择转入账户</option>
              <option v-for="account in accounts" :key="account.id" :value="String(account.id)">
                {{ account.name }} / {{ formatCurrency(account.balance) }}
              </option>
            </select>
          </label>
          <p v-if="sameAccount && form.fromAccountId" class="form-error">转出账户与转入账户不能相同</p>
          <label>
            <span>转账金额</span>
            <input v-model="form.amount" type="number" min="0" step="0.01" placeholder="0.00" :disabled="!canWrite" />
          </label>
          <label>
            <span>手续费</span>
            <input v-model="form.fee" type="number" min="0" step="0.01" placeholder="可选" :disabled="!canWrite" />
          </label>
          <label class="wide-field">
            <span>备注说明</span>
            <textarea v-model="form.notes" rows="4" placeholder="补充转账用途、银行流水号等" :disabled="!canWrite"></textarea>
          </label>
          <div class="form-actions wide-field">
            <button type="submit" :disabled="!canSubmit">{{ saving ? '保存中...' : '创建转账' }}</button>
            <button type="button" class="ghost-action" :disabled="saving" @click="resetForm">重置</button>
          </div>
        </form>
      </aside>
    </section>
  </section>
</template>

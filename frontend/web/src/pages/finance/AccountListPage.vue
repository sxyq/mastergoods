<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useSession } from '@/app/stores/session'
import {
  createAccount,
  deleteAccount,
  fetchAccounts,
  updateAccount,
  type AccountRecord,
  type AccountWritePayload,
} from '@/shared/api/client'
import { sameEntityId, type EntityId } from '@/shared/utils/id'
import {
  ACCOUNT_ACTIVE,
  ACCOUNT_DISABLED,
  ACCOUNT_TYPE_ALIPAY,
  ACCOUNT_TYPE_BANK,
  ACCOUNT_TYPE_CASH,
  ACCOUNT_TYPE_WECHAT,
  accountStatusLabel,
  accountTypeLabel,
  formatCurrency,
  formatDateTime,
} from '@/shared/utils/business'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

const session = useSession()

const accounts = ref<AccountRecord[]>([])
const editingId = ref<EntityId | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  code: '',
  name: '',
  type: String(ACCOUNT_TYPE_CASH),
  balance: '0',
  isDefault: false,
  status: String(ACCOUNT_ACTIVE),
  notes: '',
})

const accountTypeOptions = [
  [ACCOUNT_TYPE_CASH, '现金'],
  [ACCOUNT_TYPE_BANK, '银行'],
  [ACCOUNT_TYPE_ALIPAY, '支付宝'],
  [ACCOUNT_TYPE_WECHAT, '微信'],
] as const

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['finance:write']))
const accountIndex = computed(() => new Map(accounts.value.map((item) => [String(item.id), item] as const)))
const editingAccount = computed(() => (editingId.value ? accountIndex.value.get(String(editingId.value)) ?? null : null))

const summary = computed(() => accounts.value.reduce((acc, item) => {
  acc.totalBalance += item.balance
  if (item.status === ACCOUNT_ACTIVE) acc.activeCount += 1
  if (item.isDefault) acc.defaultCount += 1
  return acc
}, { totalBalance: 0, activeCount: 0, defaultCount: 0 }))

const isEditMode = computed(() => editingId.value != null)
const canSubmit = computed(() => isApiSource.value && canWrite.value && !saving.value && form.code.trim().length > 0 && form.name.trim().length > 0)

watch(
  [() => session.source.value, () => session.token.value],
  async () => {
    if (!isApiSource.value || !session.token.value) {
      accounts.value = []
      error.value = ''
      return
    }
    await loadAccounts()
  },
  { immediate: true },
)

async function loadAccounts() {
  if (!session.token.value) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    accounts.value = await fetchAccounts(session.token.value)
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '资金账户加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.code = ''
  form.name = ''
  form.type = String(ACCOUNT_TYPE_CASH)
  form.balance = '0'
  form.isDefault = false
  form.status = String(ACCOUNT_ACTIVE)
  form.notes = ''
}

function editAccount(account: AccountRecord) {
  editingId.value = account.id
  form.code = account.code
  form.name = account.name
  form.type = String(account.type)
  form.balance = String(account.balance)
  form.isDefault = account.isDefault
  form.status = String(account.status)
  form.notes = account.notes || ''
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload: AccountWritePayload = {
      code: form.code.trim(),
      name: form.name.trim(),
      type: Number(form.type),
      balance: Number(form.balance || 0),
      isDefault: form.isDefault,
      status: Number(form.status),
      notes: form.notes.trim() || null,
    }
    if (editingId.value) {
      const updated = await updateAccount(session.token.value, editingId.value, payload)
      success.value = `账户「${updated.name}」已更新`
    } else {
      const created = await createAccount(session.token.value, payload)
      success.value = `账户「${created.name}」已创建`
    }
    resetForm()
    await loadAccounts()
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '账户保存失败'
  } finally {
    saving.value = false
  }
}

async function removeAccount(account: AccountRecord) {
  if (!session.token.value) return
  if (!window.confirm(`确认删除账户「${account.name}」？`)) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    await deleteAccount(session.token.value, account.id)
    if (sameEntityId(editingId.value, account.id)) resetForm()
    success.value = `账户「${account.name}」已删除`
    await loadAccounts()
  } catch (removeErr) {
    error.value = removeErr instanceof Error ? removeErr.message : '账户删除失败'
  } finally {
    saving.value = false
  }
}

async function retryPage() {
  await loadAccounts()
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">资金账户 / Accounts</p>
        <h2>资金账户</h2>
        <p>维护现金、银行、支付宝、微信等资金账户，真实调用 `/v2/accounts` 接口。</p>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后读取和写入资金账户。"
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
    <PageStatusBanner v-if="loading" tone="info" title="正在同步" message="正在加载资金账户..." />

    <section class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>账户总数</span>
        <strong>{{ accounts.length }}</strong>
        <p>当前数据域</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>启用账户</span>
        <strong>{{ summary.activeCount }}</strong>
        <p>状态为启用</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>账户总余额</span>
        <strong>{{ formatCurrency(summary.totalBalance) }}</strong>
        <p>默认账户 {{ summary.defaultCount }} 个</p>
      </article>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">账户列表</p>
            <h3>资金账户</h3>
          </div>
          <span class="session-source">{{ loading ? '加载中...' : `${accounts.length} 个账户` }}</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>账户编码</th>
                <th>账户名称</th>
                <th>类型</th>
                <th>余额</th>
                <th>默认</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="account in accounts"
                :key="account.id"
                :class="{ selected: sameEntityId(account.id, editingId) }"
                @click="editAccount(account)"
              >
                <td>{{ account.code }}</td>
                <td>{{ account.name }}</td>
                <td>{{ accountTypeLabel(account.type) }}</td>
                <td>{{ formatCurrency(account.balance) }}</td>
                <td>{{ account.isDefault ? '✓' : '—' }}</td>
                <td>{{ accountStatusLabel(account.status) }}</td>
                <td>
                  <div class="table-actions">
                    <button type="button" class="ghost-action" @click.stop="editAccount(account)">编辑</button>
                    <button type="button" class="ghost-action" :disabled="!canWrite || saving" @click.stop="removeAccount(account)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && accounts.length === 0">
                <td colspan="7" class="empty-cell">暂无资金账户</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ isEditMode ? '编辑账户' : '新建账户' }}</p>
            <h3>{{ isEditMode ? '修改账户信息' : '录入账户信息' }}</h3>
          </div>
          <button v-if="isEditMode" type="button" class="ghost-action" @click="resetForm">新建空白</button>
        </div>

        <p v-if="editingAccount" class="muted session-source">
          创建于 {{ formatDateTime(editingAccount.createdAt) }} / 更新于 {{ formatDateTime(editingAccount.updatedAt) }}
        </p>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>账户编码</span>
            <input v-model="form.code" placeholder="例如 ACC-001" :disabled="!canWrite" />
          </label>
          <label>
            <span>账户名称</span>
            <input v-model="form.name" placeholder="例如 工商银行" :disabled="!canWrite" />
          </label>
          <label>
            <span>账户类型</span>
            <select v-model="form.type" :disabled="!canWrite">
              <option v-for="[value, label] in accountTypeOptions" :key="value" :value="String(value)">{{ label }}</option>
            </select>
          </label>
          <label>
            <span>期初余额</span>
            <input v-model="form.balance" type="number" min="0" step="0.01" :disabled="!canWrite" />
          </label>
          <label>
            <span>状态</span>
            <select v-model="form.status" :disabled="!canWrite">
              <option :value="String(ACCOUNT_ACTIVE)">启用</option>
              <option :value="String(ACCOUNT_DISABLED)">停用</option>
            </select>
          </label>
          <label class="wide-field">
            <span>默认账户</span>
            <input v-model="form.isDefault" type="checkbox" :disabled="!canWrite" />
          </label>
          <label class="wide-field">
            <span>备注说明</span>
            <textarea v-model="form.notes" rows="4" placeholder="补充开户行、卡号尾号等信息" :disabled="!canWrite"></textarea>
          </label>
          <div class="form-actions wide-field">
            <button type="submit" :disabled="!canSubmit">{{ saving ? '保存中...' : (isEditMode ? '保存更新' : '新增账户') }}</button>
            <button v-if="isEditMode" type="button" class="ghost-action" :disabled="saving" @click="resetForm">取消编辑</button>
          </div>
        </form>
      </aside>
    </section>
  </section>
</template>

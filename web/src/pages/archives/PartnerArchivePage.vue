<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  createCustomer,
  createCustomerGroup,
  createSupplier,
  createSupplierGroup,
  deleteCustomer,
  deleteCustomerGroup,
  deleteSupplier,
  deleteSupplierGroup,
  fetchCustomerGroups,
  fetchCustomers,
  fetchSupplierGroups,
  fetchSuppliers,
  updateCustomer,
  updateCustomerGroup,
  updateSupplier,
  updateSupplierGroup,
  type CustomerRecord,
  type CustomerWritePayload,
  type PartnerGroupPayload,
  type PartnerGroupRecord,
  type SupplierRecord,
  type SupplierWritePayload,
} from '@/shared/api/client'
import { useSession } from '@/app/stores/session'
import { formatCurrency, formatDateTime } from '@/shared/utils/business'
import type { EntityId } from '@/shared/utils/id'

type PartnerKind = 'customer' | 'supplier'
type DirectoryRecord = CustomerRecord | SupplierRecord

const route = useRoute()
const session = useSession()
const kind = computed<PartnerKind>(() => (route.path.includes('/customers') ? 'customer' : 'supplier'))
const isCustomer = computed(() => kind.value === 'customer')
const title = computed(() => (isCustomer.value ? '客户档案' : '供应商档案'))
const intro = computed(() => (isCustomer.value
  ? '客户档案承接销售单、应收款和客户分组，已经接入真实客户主数据 CRUD。'
  : '供应商档案承接采购单、应付款和供应商分组，已经接入真实供应商主数据 CRUD。'))
const groupTitle = computed(() => (isCustomer.value ? '客户分组' : '供应商分组'))
const amountLabel = computed(() => (isCustomer.value ? '应收余额' : '应付余额'))
const partnerLabel = computed(() => (isCustomer.value ? '客户' : '供应商'))
const statusLabel = computed(() => (isCustomer.value ? '待跟进客户' : '待跟进供应商'))
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => {
  if (session.hasPermission(['archives:write'])) return true
  return isCustomer.value
    ? session.hasPermission(['sales:write'])
    : session.hasPermission(['purchase:write'])
})

const records = ref<DirectoryRecord[]>([])
const groups = ref<PartnerGroupRecord[]>([])
const loading = ref(false)
const savingRecord = ref(false)
const savingGroup = ref(false)
const loadError = ref('')
const success = ref('')
const searchKeyword = ref('')
const statusFilter = ref<'all' | '1' | '0'>('all')
const groupFilter = ref<string>('all')
const editingRecordId = ref<EntityId | null>(null)
const editingGroupId = ref<EntityId | null>(null)

const recordForm = reactive({
  name: '',
  phone: '',
  level: 1,
  groupId: '',
  primaryContactName: '',
  primaryContactPhone: '',
  address: '',
  notes: '',
  balance: '0',
  status: '1',
})

const groupForm = reactive({
  name: '',
  sortOrder: '0',
  status: '1',
})

watch(
  [kind, () => session.token.value, isApiSource],
  async () => {
    resetForms()
    resetFeedback()
    if (!isApiSource.value) {
      records.value = []
      groups.value = []
      return
    }
    await loadPage()
  },
  { immediate: true },
)

const recordSummary = computed(() => records.value.reduce(
  (summary, item) => {
    if (item.status === 1) summary.activeRecordCount += 1
    if ((item.balance || 0) > 0) summary.pendingCount += 1
    summary.totalBalance += item.balance || 0
    return summary
  },
  {
    activeRecordCount: 0,
    totalBalance: 0,
    pendingCount: 0,
  },
))
const activeRecordCount = computed(() => recordSummary.value.activeRecordCount)
const totalBalance = computed(() => recordSummary.value.totalBalance)
const pendingCount = computed(() => recordSummary.value.pendingCount)
const sortedGroups = computed(() => [...groups.value].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)))
const topGroups = computed(() => sortedGroups.value.slice(0, 6))

async function loadPage() {
  if (!session.token.value) return
  loading.value = true
    resetFeedback()
    try {
    const status = statusFilter.value === 'all' ? undefined : Number(statusFilter.value)
    const groupId = groupFilter.value === 'all' ? undefined : groupFilter.value
    const [nextGroups, nextRecords] = await Promise.all([
      isCustomer.value ? fetchCustomerGroups(session.token.value) : fetchSupplierGroups(session.token.value),
      isCustomer.value
        ? fetchCustomers(session.token.value, { keyword: searchKeyword.value.trim() || undefined, status, groupId, page: 0, size: 200 })
        : fetchSuppliers(session.token.value, { keyword: searchKeyword.value.trim() || undefined, status, groupId, page: 0, size: 200 }),
    ])
    groups.value = nextGroups
    records.value = nextRecords
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : `${partnerLabel.value}档案加载失败`
  } finally {
    loading.value = false
  }
}

async function submitRecord() {
  if (!session.token.value) return
  savingRecord.value = true
  resetFeedback()
  try {
    if (isCustomer.value) {
      const payload = buildCustomerPayload()
      if (editingRecordId.value) {
        await updateCustomer(session.token.value, editingRecordId.value, payload)
        success.value = `客户「${recordForm.name.trim()}」已更新`
      } else {
        await createCustomer(session.token.value, payload)
        success.value = `客户「${recordForm.name.trim()}」已创建`
      }
    } else {
      const payload = buildSupplierPayload()
      if (editingRecordId.value) {
        await updateSupplier(session.token.value, editingRecordId.value, payload)
        success.value = `供应商「${recordForm.name.trim()}」已更新`
      } else {
        await createSupplier(session.token.value, payload)
        success.value = `供应商「${recordForm.name.trim()}」已创建`
      }
    }
    resetRecordForm()
    await loadPage()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : `${partnerLabel.value}保存失败`
  } finally {
    savingRecord.value = false
  }
}

async function removeRecord(record: DirectoryRecord) {
  if (!session.token.value) return
  savingRecord.value = true
  resetFeedback()
  try {
    if (isCustomer.value) {
      await deleteCustomer(session.token.value, record.id)
    } else {
      await deleteSupplier(session.token.value, record.id)
    }
    if (editingRecordId.value === record.id) {
      resetRecordForm()
    }
    success.value = `${partnerLabel.value}「${record.name}」已删除`
    await loadPage()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : `${partnerLabel.value}删除失败`
  } finally {
    savingRecord.value = false
  }
}

function editRecord(record: DirectoryRecord) {
  editingRecordId.value = record.id
  recordForm.name = record.name
  recordForm.phone = record.phone
  recordForm.level = 'level' in record ? record.level : 1
  recordForm.groupId = record.groupId == null ? '' : String(record.groupId)
  recordForm.primaryContactName = record.primaryContactName || ''
  recordForm.primaryContactPhone = record.primaryContactPhone || ''
  recordForm.address = record.address || ''
  recordForm.notes = record.notes || ''
  recordForm.balance = String(record.balance || 0)
  recordForm.status = String(record.status)
}

async function submitGroup() {
  if (!session.token.value) return
  savingGroup.value = true
  resetFeedback()
  const payload: PartnerGroupPayload = {
    name: groupForm.name.trim(),
    sortOrder: Number(groupForm.sortOrder || 0),
    status: Number(groupForm.status),
  }
  try {
    if (isCustomer.value) {
      if (editingGroupId.value) {
        await updateCustomerGroup(session.token.value, editingGroupId.value, payload)
        success.value = `客户分组「${payload.name}」已更新`
      } else {
        await createCustomerGroup(session.token.value, payload)
        success.value = `客户分组「${payload.name}」已创建`
      }
    } else if (editingGroupId.value) {
      await updateSupplierGroup(session.token.value, editingGroupId.value, payload)
      success.value = `供应商分组「${payload.name}」已更新`
    } else {
      await createSupplierGroup(session.token.value, payload)
      success.value = `供应商分组「${payload.name}」已创建`
    }
    resetGroupForm()
    await loadPage()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : `${groupTitle.value}保存失败`
  } finally {
    savingGroup.value = false
  }
}

function editGroup(group: PartnerGroupRecord) {
  editingGroupId.value = group.id
  groupForm.name = group.name
  groupForm.sortOrder = String(group.sortOrder)
  groupForm.status = String(group.status)
}

async function removeGroup(group: PartnerGroupRecord) {
  if (!session.token.value) return
  savingGroup.value = true
  resetFeedback()
  try {
    if (isCustomer.value) {
      await deleteCustomerGroup(session.token.value, group.id)
    } else {
      await deleteSupplierGroup(session.token.value, group.id)
    }
    if (editingGroupId.value === group.id) {
      resetGroupForm()
    }
    success.value = `${groupTitle.value}「${group.name}」已删除`
    await loadPage()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : `${groupTitle.value}删除失败`
  } finally {
    savingGroup.value = false
  }
}

function resetForms() {
  resetRecordForm()
  resetGroupForm()
}

function resetRecordForm() {
  editingRecordId.value = null
  recordForm.name = ''
  recordForm.phone = ''
  recordForm.level = 1
  recordForm.groupId = ''
  recordForm.primaryContactName = ''
  recordForm.primaryContactPhone = ''
  recordForm.address = ''
  recordForm.notes = ''
  recordForm.balance = '0'
  recordForm.status = '1'
}

function resetGroupForm() {
  editingGroupId.value = null
  groupForm.name = ''
  groupForm.sortOrder = '0'
  groupForm.status = '1'
}

function resetFeedback() {
  loadError.value = ''
  success.value = ''
}

function buildCustomerPayload(): CustomerWritePayload {
  return {
    name: recordForm.name.trim(),
    phone: recordForm.phone.trim(),
    level: Number(recordForm.level || 1),
    groupId: recordForm.groupId || null,
    primaryContactName: nullableText(recordForm.primaryContactName),
    primaryContactPhone: nullableText(recordForm.primaryContactPhone),
    address: nullableText(recordForm.address),
    notes: nullableText(recordForm.notes),
    balance: Number(recordForm.balance || 0),
    status: Number(recordForm.status),
  }
}

function buildSupplierPayload(): SupplierWritePayload {
  return {
    name: recordForm.name.trim(),
    phone: recordForm.phone.trim(),
    groupId: recordForm.groupId || null,
    primaryContactName: nullableText(recordForm.primaryContactName),
    primaryContactPhone: nullableText(recordForm.primaryContactPhone),
    address: nullableText(recordForm.address),
    notes: nullableText(recordForm.notes),
    balance: Number(recordForm.balance || 0),
    status: Number(recordForm.status),
  }
}

function nullableText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : null
}
</script>

<template>
  <section class="partner-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">Archive Console / Real Backend</p>
        <h2>{{ title }}</h2>
        <p>{{ intro }}</p>
      </div>
      <div class="api-note">
        <strong>数据范围</strong>
        <span>{{ isApiSource ? '当前 owner_user_id 真数据域' : '请先登录真实 owner 账号' }}</span>
      </div>
    </section>

    <div class="metrics-grid compact">
      <article class="metric-card" data-tone="blue">
        <span>{{ partnerLabel }}总数</span>
        <strong>{{ records.length }}</strong>
        <p>当前查询结果</p>
      </article>
      <article class="metric-card" data-tone="green">
        <span>启用{{ partnerLabel }}</span>
        <strong>{{ activeRecordCount }}</strong>
        <p>状态为启用</p>
      </article>
      <article class="metric-card" data-tone="orange">
        <span>{{ amountLabel }}</span>
        <strong>{{ formatCurrency(totalBalance) }}</strong>
        <p>{{ statusLabel }} {{ pendingCount }} 个</p>
      </article>
    </div>

    <div v-if="!isApiSource" class="panel">
      <p class="eyebrow">API Required</p>
      <h2>请切到真实后端模式</h2>
      <p class="muted">这个专页已经接入真实 {{ partnerLabel }} 与分组接口。当前如果还是演示模式，不会写入真实主数据。</p>
    </div>

    <template v-else>
      <div v-if="loadError" class="form-error">{{ loadError }}</div>
      <div v-if="success" class="form-success">{{ success }}</div>

      <section class="partner-toolbar">
        <label class="search-box">
          <span>搜索</span>
          <input v-model="searchKeyword" :placeholder="`${partnerLabel}名称 / 手机号`" @keyup.enter="loadPage" />
        </label>
        <label class="compact-field">
          <span>状态</span>
          <select v-model="statusFilter">
            <option value="all">全部</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </label>
        <label class="compact-field">
          <span>{{ groupTitle }}</span>
          <select v-model="groupFilter">
            <option value="all">全部分组</option>
            <option v-for="group in sortedGroups" :key="group.id" :value="String(group.id)">{{ group.name }}</option>
          </select>
        </label>
        <button type="button" class="ghost-action" :disabled="loading" @click="loadPage">
          {{ loading ? '加载中' : '刷新列表' }}
        </button>
      </section>

      <div v-if="topGroups.length > 0" class="table-tags">
        <span v-for="group in topGroups" :key="group.id">{{ group.name }}</span>
      </div>

      <section class="partner-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ editingRecordId ? `编辑${partnerLabel}` : `新建${partnerLabel}` }}</p>
              <h3>{{ title }}维护</h3>
            </div>
            <button type="button" class="ghost-action" @click="resetRecordForm">新建空白</button>
          </div>

          <form class="partner-form" @submit.prevent="submitRecord">
            <label>
              {{ partnerLabel }}名称
              <input v-model="recordForm.name" :disabled="savingRecord || !canWrite" />
            </label>
            <label>
              手机号
              <input v-model="recordForm.phone" :disabled="savingRecord || !canWrite" />
            </label>
            <label v-if="isCustomer">
              客户等级
              <input v-model.number="recordForm.level" type="number" min="1" max="9" :disabled="savingRecord || !canWrite" />
            </label>
            <label>
              {{ groupTitle }}
              <select v-model="recordForm.groupId" :disabled="savingRecord || !canWrite">
                <option value="">未分组</option>
                <option v-for="group in sortedGroups" :key="group.id" :value="String(group.id)">{{ group.name }}</option>
              </select>
            </label>
            <label>
              主联系人
              <input v-model="recordForm.primaryContactName" :disabled="savingRecord || !canWrite" />
            </label>
            <label>
              联系人手机号
              <input v-model="recordForm.primaryContactPhone" :disabled="savingRecord || !canWrite" />
            </label>
            <label>
              {{ amountLabel }}
              <input v-model="recordForm.balance" type="number" min="0" step="0.01" :disabled="savingRecord || !canWrite" />
            </label>
            <label>
              状态
              <select v-model="recordForm.status" :disabled="savingRecord || !canWrite">
                <option value="1">启用</option>
                <option value="0">停用</option>
              </select>
            </label>
            <label class="wide-field">
              地址
              <input v-model="recordForm.address" :disabled="savingRecord || !canWrite" />
            </label>
            <label class="wide-field">
              备注
              <textarea v-model="recordForm.notes" rows="4" :disabled="savingRecord || !canWrite"></textarea>
            </label>
            <div class="form-actions">
              <button type="submit" :disabled="savingRecord || !canWrite">
                {{ savingRecord ? '保存中' : editingRecordId ? `更新${partnerLabel}` : `创建${partnerLabel}` }}
              </button>
              <button type="button" class="ghost-action" @click="resetRecordForm">取消编辑</button>
            </div>
          </form>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ groupTitle }}</p>
              <h3>{{ groupTitle }}维护</h3>
            </div>
            <button type="button" class="ghost-action" @click="resetGroupForm">新建分组</button>
          </div>

          <form class="partner-form partner-form-groups" @submit.prevent="submitGroup">
            <label>
              分组名称
              <input v-model="groupForm.name" :disabled="savingGroup || !canWrite" />
            </label>
            <label>
              排序
              <input v-model="groupForm.sortOrder" type="number" min="0" :disabled="savingGroup || !canWrite" />
            </label>
            <label>
              状态
              <select v-model="groupForm.status" :disabled="savingGroup || !canWrite">
                <option value="1">启用</option>
                <option value="0">停用</option>
              </select>
            </label>
            <div class="form-actions">
              <button type="submit" :disabled="savingGroup || !canWrite">
                {{ savingGroup ? '保存中' : editingGroupId ? '更新分组' : '创建分组' }}
              </button>
            </div>
          </form>

          <div class="table-shell group-table">
            <table>
              <thead>
                <tr>
                  <th>分组</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>更新时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="group in sortedGroups" :key="group.id">
                  <td>{{ group.name }}</td>
                  <td>{{ group.sortOrder }}</td>
                  <td>{{ group.status === 1 ? '启用' : '停用' }}</td>
                  <td>{{ formatDateTime(group.updatedAt) }}</td>
                  <td>
                    <div class="table-actions">
                      <button type="button" class="ghost-action" @click="editGroup(group)">编辑</button>
                      <button type="button" class="ghost-action" :disabled="savingGroup || !canWrite" @click="removeGroup(group)">删除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="sortedGroups.length === 0">
                  <td colspan="5" class="empty-cell">当前还没有分组</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ title }}</p>
            <h2>{{ title }}列表</h2>
          </div>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>{{ partnerLabel }}名称</th>
                <th>手机号</th>
                <th>{{ groupTitle }}</th>
                <th v-if="isCustomer">等级</th>
                <th>主联系人</th>
                <th>{{ amountLabel }}</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records" :key="record.id">
                <td>{{ record.name }}</td>
                <td>{{ record.phone }}</td>
                <td>{{ record.groupName || '未分组' }}</td>
                <td v-if="isCustomer">{{ 'level' in record ? record.level : '--' }}</td>
                <td>{{ record.primaryContactName || '--' }}</td>
                <td>{{ formatCurrency(record.balance) }}</td>
                <td>{{ record.status === 1 ? '启用' : '停用' }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="table-actions">
                    <button type="button" class="ghost-action" @click="editRecord(record)">编辑</button>
                    <button type="button" class="ghost-action" :disabled="savingRecord || !canWrite" @click="removeRecord(record)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="records.length === 0">
                <td :colspan="isCustomer ? 9 : 8" class="empty-cell">当前没有匹配的{{ partnerLabel }}档案</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>

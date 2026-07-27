<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import {
  createCustomerContact,
  createSupplierContact,
  deleteCustomerContact,
  deleteSupplierContact,
  fetchCustomerContacts,
  fetchCustomers,
  fetchSupplierContacts,
  fetchSuppliers,
  updateCustomerContact,
  updateSupplierContact,
  type ContactRecord,
  type ContactWritePayload,
  type CustomerRecord,
  type SupplierRecord,
} from '@/shared/api/client'
import { readQueryId, sameEntityId, type EntityId } from '@/shared/utils/id'
import PageStatusBanner from '@/shared/ui/PageStatusBanner.vue'

type ContactKind = 'customer' | 'supplier'
type PartnerRecord = CustomerRecord | SupplierRecord

const route = useRoute()
const router = useRouter()
const session = useSession()

const kind = computed<ContactKind>(() => (route.query.kind === 'supplier' ? 'supplier' : 'customer'))
const isCustomer = computed(() => kind.value === 'customer')
const queryPartnerId = computed(() => readQueryId(route.query.partnerId))

const contacts = ref<ContactRecord[]>([])
const partners = ref<PartnerRecord[]>([])
const selectedPartnerId = ref<EntityId | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')
const editingId = ref<EntityId | null>(null)

const form = reactive({
  partnerId: '',
  name: '',
  phone: '',
  title: '',
  isPrimary: false,
})

const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['archives:write']))
const partnerLabel = computed(() => (isCustomer.value ? '客户' : '供应商'))
const titleText = computed(() => (isCustomer.value ? '客户联系人' : '供应商联系人'))
const isEditMode = computed(() => editingId.value != null)
const canSubmit = computed(() => isApiSource.value
  && canWrite.value
  && !saving.value
  && form.partnerId !== ''
  && form.name.trim().length > 0)

const selectedPartnerIdProxy = computed({
  get: () => (selectedPartnerId.value ? String(selectedPartnerId.value) : ''),
  set: (value: string) => {
    selectedPartnerId.value = value || null
  },
})

watch(
  [kind, () => session.source.value, () => session.token.value],
  async () => {
    resetForm()
    if (!isApiSource.value || !session.token.value) {
      contacts.value = []
      partners.value = []
      selectedPartnerId.value = null
      error.value = ''
      return
    }
    await loadPartners()
    const validQuery = queryPartnerId.value && partners.value.some((p) => sameEntityId(p.id, queryPartnerId.value))
      ? queryPartnerId.value
      : null
    selectedPartnerId.value = validQuery ?? partners.value[0]?.id ?? null
  },
  { immediate: true },
)

watch([selectedPartnerId, () => session.token.value], async () => {
  if (!isApiSource.value || !session.token.value) return
  await loadContacts()
})

async function loadPartners() {
  if (!session.token.value) return
  try {
    partners.value = isCustomer.value
      ? await fetchCustomers(session.token.value, { page: 0, size: 200 })
      : await fetchSuppliers(session.token.value, { page: 0, size: 200 })
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : `${partnerLabel.value}列表加载失败`
  }
}

async function loadContacts() {
  if (!session.token.value) return
  if (!selectedPartnerId.value) {
    contacts.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    contacts.value = isCustomer.value
      ? await fetchCustomerContacts(session.token.value, selectedPartnerId.value)
      : await fetchSupplierContacts(session.token.value, selectedPartnerId.value)
  } catch (loadErr) {
    error.value = loadErr instanceof Error ? loadErr.message : '联系人加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.partnerId = selectedPartnerId.value ? String(selectedPartnerId.value) : ''
  form.name = ''
  form.phone = ''
  form.title = ''
  form.isPrimary = false
}

function editContact(contact: ContactRecord) {
  editingId.value = contact.id
  form.partnerId = String(contact.partnerId)
  form.name = contact.name
  form.phone = contact.phone || ''
  form.title = contact.title || ''
  form.isPrimary = contact.isPrimary
}

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  const targetPartnerId = form.partnerId
  try {
    const payload: ContactWritePayload = {
      partnerId: targetPartnerId,
      name: form.name.trim(),
      phone: form.phone.trim() || null,
      title: form.title.trim() || null,
      isPrimary: form.isPrimary,
    }
    if (editingId.value) {
      if (isCustomer.value) {
        await updateCustomerContact(session.token.value, editingId.value, payload)
      } else {
        await updateSupplierContact(session.token.value, editingId.value, payload)
      }
      success.value = `联系人「${payload.name}」已更新`
    } else {
      if (isCustomer.value) {
        await createCustomerContact(session.token.value, payload)
      } else {
        await createSupplierContact(session.token.value, payload)
      }
      success.value = `联系人「${payload.name}」已创建`
    }
    resetForm()
    if (!sameEntityId(selectedPartnerId.value, targetPartnerId)) {
      selectedPartnerId.value = targetPartnerId
    } else {
      await loadContacts()
    }
  } catch (submitErr) {
    error.value = submitErr instanceof Error ? submitErr.message : '联系人保存失败'
  } finally {
    saving.value = false
  }
}

async function removeContact(contact: ContactRecord) {
  if (!session.token.value) return
  if (!window.confirm(`确认删除联系人「${contact.name}」？`)) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    if (isCustomer.value) {
      await deleteCustomerContact(session.token.value, contact.id)
    } else {
      await deleteSupplierContact(session.token.value, contact.id)
    }
    if (sameEntityId(editingId.value, contact.id)) resetForm()
    success.value = `联系人「${contact.name}」已删除`
    await loadContacts()
  } catch (removeErr) {
    error.value = removeErr instanceof Error ? removeErr.message : '联系人删除失败'
  } finally {
    saving.value = false
  }
}

function switchKind(nextKind: ContactKind) {
  router.push({ path: '/archives/contacts', query: { kind: nextKind } })
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">联系人 / Contacts</p>
        <h2>{{ titleText }}</h2>
        <p>维护{{ partnerLabel }}联系人信息，真实调用 `/v2/customer-contacts` 与 `/v2/supplier-contacts` 接口。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" :class="{ active: isCustomer }" @click="switchKind('customer')">客户联系人</button>
        <button type="button" class="ghost-action" :class="{ active: !isCustomer }" @click="switchKind('supplier')">供应商联系人</button>
      </div>
    </section>

    <PageStatusBanner
      v-if="!isApiSource"
      tone="warning"
      title="演示模式"
      message="当前是演示模式。这一页只在真实登录后读取和写入联系人。"
    />
    <PageStatusBanner
      v-else-if="error"
      tone="error"
      title="页面加载异常"
      :message="error"
    />
    <PageStatusBanner v-else-if="success" tone="success" title="操作成功" :message="success" />

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">{{ partnerLabel }}选择</p>
          <h3>选择{{ partnerLabel }}查看联系人</h3>
        </div>
      </div>
      <label class="wide-field">
        <span>{{ partnerLabel }}</span>
        <select v-model="selectedPartnerIdProxy">
          <option value="">请选择{{ partnerLabel }}</option>
          <option v-for="partner in partners" :key="partner.id" :value="String(partner.id)">
            {{ partner.name }}{{ partner.phone ? ` / ${partner.phone}` : '' }}
          </option>
        </select>
      </label>
    </section>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">联系人列表</p>
            <h3>{{ titleText }}</h3>
          </div>
          <span class="session-source">{{ loading ? '加载中...' : `${contacts.length} 个联系人` }}</span>
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>电话</th>
                <th>职务</th>
                <th>主要联系人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="contact in contacts"
                :key="contact.id"
                :class="{ selected: sameEntityId(contact.id, editingId) }"
                @click="editContact(contact)"
              >
                <td>{{ contact.name }}</td>
                <td>{{ contact.phone || '--' }}</td>
                <td>{{ contact.title || '--' }}</td>
                <td>{{ contact.isPrimary ? '✓' : '—' }}</td>
                <td>
                  <div class="table-actions">
                    <button type="button" class="ghost-action" @click.stop="editContact(contact)">编辑</button>
                    <button type="button" class="ghost-action" :disabled="!canWrite || saving" @click.stop="removeContact(contact)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && contacts.length === 0">
                <td colspan="5" class="empty-cell">{{ selectedPartnerId ? `当前${partnerLabel}暂无联系人` : `请选择${partnerLabel}` }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="panel detail-panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ isEditMode ? '编辑联系人' : '新建联系人' }}</p>
            <h3>{{ isEditMode ? '修改联系人信息' : '录入联系人信息' }}</h3>
          </div>
          <button v-if="isEditMode" type="button" class="ghost-action" @click="resetForm">新建空白</button>
        </div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label>
            <span>{{ partnerLabel }}</span>
            <select v-model="form.partnerId" :disabled="!canWrite">
              <option value="">请选择{{ partnerLabel }}</option>
              <option v-for="partner in partners" :key="partner.id" :value="String(partner.id)">{{ partner.name }}</option>
            </select>
          </label>
          <label>
            <span>姓名</span>
            <input v-model="form.name" placeholder="联系人姓名" :disabled="!canWrite" />
          </label>
          <label>
            <span>电话</span>
            <input v-model="form.phone" placeholder="联系电话" :disabled="!canWrite" />
          </label>
          <label>
            <span>职务</span>
            <input v-model="form.title" placeholder="例如 采购经理" :disabled="!canWrite" />
          </label>
          <label class="wide-field">
            <span>主要联系人</span>
            <input v-model="form.isPrimary" type="checkbox" :disabled="!canWrite" />
          </label>
          <div class="form-actions wide-field">
            <button type="submit" :disabled="!canSubmit">{{ saving ? '保存中...' : (isEditMode ? '保存更新' : '新增联系人') }}</button>
            <button v-if="isEditMode" type="button" class="ghost-action" :disabled="saving" @click="resetForm">取消编辑</button>
          </div>
        </form>
      </aside>
    </section>
  </section>
</template>

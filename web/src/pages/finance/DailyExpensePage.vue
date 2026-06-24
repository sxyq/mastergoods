<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/app/stores/session'
import { createFinanceRecord } from '@/shared/api/client'
import {
  FINANCE_EXPENSE,
  METHOD_ALIPAY,
  METHOD_BANK,
  METHOD_CASH,
  METHOD_OTHER,
  METHOD_WECHAT,
  formatCurrency,
} from '@/shared/utils/business'

const router = useRouter()
const session = useSession()

const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  amount: '',
  category: '房租',
  partnerName: '',
  method: String(METHOD_CASH),
  notes: '',
})

const categoryOptions = ['房租', '水电', '工资', '办公', '营销', '物流', '餐饮', '其他'] as const
const methodOptions = [
  [METHOD_CASH, '现金'],
  [METHOD_WECHAT, '微信'],
  [METHOD_ALIPAY, '支付宝'],
  [METHOD_BANK, '银行卡'],
  [METHOD_OTHER, '其他'],
] as const
const isApiSource = computed(() => session.source.value === 'api' && Boolean(session.token.value))
const canWrite = computed(() => session.hasPermission(['finance:write']))
const parsedAmount = computed(() => Number(form.amount || 0))
const canSubmit = computed(() => canWrite.value && isApiSource.value && !saving.value && parsedAmount.value > 0 && form.category.trim().length > 0)
const selectedMethodLabel = computed(() => methodOptions.find(([value]) => String(value) === form.method)?.[1] || '--')

async function submitForm() {
  if (!session.token.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const record = await createFinanceRecord(session.token.value, {
      type: FINANCE_EXPENSE,
      category: form.category.trim(),
      partnerName: form.partnerName.trim() || null,
      amount: parsedAmount.value,
      method: Number(form.method),
      notes: form.notes.trim() || null,
    })
    success.value = `已记录支出 ${record.recordNo}`
    await router.push({ path: '/finance/records/detail', query: { id: String(record.id) } })
  } catch (saveErr) {
    error.value = saveErr instanceof Error ? saveErr.message : '日常支出记录失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="business-page">
    <section class="screen-hero">
      <div>
        <p class="eyebrow">日常支出 / Daily Expense</p>
        <h2>日常支出专页</h2>
        <p>对齐安卓端日常支出录入，写入真实 `POST /v1/finance-records` 支出流水。</p>
      </div>
      <div class="hero-actions">
        <button type="button" class="ghost-action" @click="router.push('/finance/records/detail')">返回财务流水</button>
      </div>
    </section>

    <p v-if="!isApiSource" class="form-error">当前是演示模式。这一页只在真实登录后保存支出记录。</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="success" class="form-success">{{ success }}</p>

    <section class="business-split">
      <article class="panel">
        <div class="panel-head">
          <div>
            <p class="eyebrow">支出表单</p>
            <h3>录入支出信息</h3>
          </div>
          <span class="session-source">金额 {{ formatCurrency(parsedAmount) }}</span>
        </div>

        <form class="partner-form product-form" @submit.prevent="submitForm">
          <label class="wide-field">
            <span>支出金额</span>
            <input v-model="form.amount" type="number" min="0" step="0.01" placeholder="0.00" />
          </label>
          <label>
            <span>支出分类</span>
            <select v-model="form.category">
              <option v-for="option in categoryOptions" :key="option" :value="option">{{ option }}</option>
            </select>
          </label>
          <label>
            <span>支付方式</span>
            <select v-model="form.method">
              <option v-for="[value, label] in methodOptions" :key="value" :value="String(value)">{{ label }}</option>
            </select>
          </label>
          <label class="wide-field">
            <span>往来对象</span>
            <input v-model="form.partnerName" placeholder="房东 / 物流商 / 服务商，可选" />
          </label>
          <label class="wide-field">
            <span>备注说明</span>
            <textarea v-model="form.notes" rows="4" placeholder="补充说明支出用途、票据情况等" />
          </label>

          <div class="form-actions wide-field">
            <button type="submit" :disabled="!canSubmit">{{ saving ? '保存中...' : '记录支出' }}</button>
          </div>
        </form>
      </article>

      <aside class="panel detail-panel">
        <p class="eyebrow">当前摘要</p>
        <h3>支出预览</h3>
        <div class="detail-stack">
          <article class="detail-card">
            <dl class="detail-list">
              <div>
                <dt>支出金额</dt>
                <dd>{{ formatCurrency(parsedAmount) }}</dd>
              </div>
              <div>
                <dt>支出分类</dt>
                <dd>{{ form.category }}</dd>
              </div>
              <div>
                <dt>支付方式</dt>
                <dd>{{ selectedMethodLabel }}</dd>
              </div>
              <div>
                <dt>往来对象</dt>
                <dd>{{ form.partnerName || '--' }}</dd>
              </div>
            </dl>
          </article>
          <article class="detail-card">
            <strong>实现说明</strong>
            <p class="muted">附件上传与票据图片仍待媒体接口接入；当前优先完成真实支出流水写入。</p>
          </article>
        </div>
      </aside>
    </section>
  </section>
</template>

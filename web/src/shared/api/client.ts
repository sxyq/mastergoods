import type { Permission, StoreRole } from '@/entities/auth/roles'
import type { EntityId } from '@/shared/utils/id'
import { camelize } from '@/shared/utils/camelize'
import { API_BASE_URL } from '@/shared/api/config'

interface AuthRuntimeConfig {
  getRefreshToken: () => string
  onAuthRefreshed: (payload: AuthPayload) => void
  onAuthExpired: () => void
}

const authRuntime: Partial<AuthRuntimeConfig> = {}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export class ApiError extends Error {
  status: number
  code: number

  constructor(message: string, status: number, code = -1) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export interface AuthPayload {
  userId: number
  token: string
  refreshToken: string
  expiresIn: number
}

export interface UserProfile {
  id: number
  phone: string
  nickname: string
  status: number
}

export interface CurrentStoreProfile {
  storeId: EntityId
  storeName: string
  ownerUserId: number
  currentUserId: number
  currentUserName: string
  currentUserPhone: string
  role: StoreRole
  title: string
  status: 0 | 1
  permissions: Permission[]
  memberCount: number
  enabledMemberCount: number
  disabledMemberCount: number
}

export interface StoreMemberRecord {
  userId: number
  phone: string
  nickname: string
  role: StoreRole
  title: string
  status: 0 | 1
  permissions: Permission[]
  createdAt: number
  updatedAt: number
  activeSessions: number
  storeId: EntityId
  storeName: string
}

export interface StoreMemberCreatePayload {
  phone: string
  password: string
  nickname: string
  role: Exclude<StoreRole, 'OWNER'>
  title?: string | null
  status?: 0 | 1
}

export interface StoreMemberUpdatePayload {
  nickname?: string
  password?: string | null
  role?: StoreRole
  title?: string | null
  status?: 0 | 1
  keepSessions?: boolean
}

export interface AdminSummary {
  userCount: number
  productCount: number
  customerCount: number
  supplierCount: number
  saleOrderCount: number
  purchaseOrderCount: number
  agentTaskCount: number
  unreadNotificationCount: number
}

export interface AdminUser {
  id: number
  phone: string
  nickname: string
  status: number
  createdAt: number
  updatedAt: number
  activeSessions: number
}

export interface AdminCreateUserPayload {
  phone: string
  password: string
  nickname: string
  status?: number
}

export interface AdminUpdateUserPayload {
  nickname?: string
  status?: number
  password?: string
  keepSessions?: boolean
}

export interface PartnerRecord {
  id: number
  name: string
  phone: string
  groupId: number | null
  groupName: string | null
  primaryContactName: string | null
  primaryContactPhone: string | null
  address: string | null
  notes: string | null
  balance: number
  status: number
  createdAt: number
  updatedAt: number
}

export interface PartnerGroupRecord {
  id: number
  partnerType: string
  name: string
  status: number
  sortOrder: number
  createdAt: number
  updatedAt: number
}

export interface PartnerGroupPayload {
  name: string
  status?: number
  sortOrder?: number
}

export interface CustomerRecord extends PartnerRecord {
  level: number
}

export interface SupplierRecord extends PartnerRecord {}

export interface CustomerWritePayload {
  name: string
  phone: string
  level: number
  groupId?: number | null
  primaryContactName?: string | null
  primaryContactPhone?: string | null
  address?: string | null
  notes?: string | null
  balance?: number
  status?: number
}

export interface SupplierWritePayload {
  name: string
  phone: string
  groupId?: number | null
  primaryContactName?: string | null
  primaryContactPhone?: string | null
  address?: string | null
  notes?: string | null
  balance?: number
  status?: number
}

export interface SaleOrderItem {
  id: EntityId
  orderId: EntityId
  productId: number
  productCode: string
  productName: string
  customerId: number | null
  customerName: string | null
  quantity: number
  unitPrice: number
  amount: number
  createdAt: number
}

export interface SaleOrder {
  id: EntityId
  orderNo: string
  customerId: number | null
  customerName: string | null
  items: SaleOrderItem[]
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  paidAmount: number
  notes: string | null
  status: number
  createdAt: number
  updatedAt: number
}

export interface SaleOrderCreateItemPayload {
  productId?: number | null
  quantity: number
  unitPrice: number
}

export interface SaleOrderCreatePayload {
  customerId?: number | null
  customerName?: string | null
  items: SaleOrderCreateItemPayload[]
  notes?: string | null
  discountAmount?: number | null
}

export interface SaleOrderUpdateDraftPayload {
  discountAmount?: number | null
  notes?: string | null
  items: SaleOrderCreateItemPayload[]
}

export interface SaleOrderConfirmPayload {
  notes?: string | null
}

export interface SaleOrderPaymentPayload {
  amount: number
  method: number
  referenceNo?: string | null
}

export interface PaymentRecord {
  id: EntityId
  orderId: EntityId
  amount: number
  method: number
  referenceNo: string | null
  type: number | null
  createdAt: number
}

export interface PayOrder {
  id: EntityId
  orderNo: string
  supplierId: number | null
  supplierName: string | null
  amount: number
  method: number
  referenceNo: string | null
  notes: string | null
  accountId: number | null
  status: number
  createdAt: number
  updatedAt: number
}

export interface PayOrderCreatePayload {
  supplierId?: number | null
  supplierName?: string | null
  amount: number
  method: number
  referenceNo?: string | null
  notes?: string | null
  accountId?: number | null
  status?: number | null
}

export interface PurchaseOrderItem {
  id: EntityId
  orderId: EntityId
  productId: number | null
  productCode: string | null
  productName: string | null
  quantity: number
  unitCost: number
  amount: number
  createdAt: number
}

export interface PurchaseOrder {
  id: EntityId
  orderNo: string
  supplierId: number | null
  supplierName: string | null
  items: PurchaseOrderItem[]
  totalAmount: number
  paidAmount: number
  receivedAmount: number
  notes: string | null
  status: number
  createdAt: number
  updatedAt: number
}

export interface PurchaseOrderCreateItemPayload {
  productId?: number | null
  productCode?: string | null
  productName?: string | null
  quantity: number
  unitCost: number
}

export interface PurchaseOrderWritePayload {
  supplierId?: number | null
  supplierName?: string | null
  items: PurchaseOrderCreateItemPayload[]
  notes?: string | null
  status?: number | null
}

export interface ProductPriceLevelValue {
  levelId: number
  code: string
  name: string
  price: number
  status: number
  sortOrder: number
}

export interface ProductSupplierRelation {
  id: number
  productId: number
  supplierId: number
  supplierName: string
  supplierPhone: string | null
  isDefault: boolean
  purchasePriority: number | null
  lastPurchasePrice: number | null
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface ProductRecord {
  id: number
  code: string
  name: string
  categoryId: number | null
  categoryName: string | null
  unitId: number | null
  unitName: string | null
  salePrice: number
  purchasePrice: number
  priceLevels: ProductPriceLevelValue[]
  defaultSupplier: ProductSupplierRelation | null
  supplierRelations: ProductSupplierRelation[]
  stock: number
  safeStock: number
  status: number
  createdAt: number
  updatedAt: number
}

export interface ProductCategoryRecord {
  id: number
  name: string
  status: number
  sortOrder: number | null
  createdAt: number
  updatedAt: number
}

export interface ProductUnitRecord {
  id: number
  name: string
  status: number
  sortOrder: number | null
  createdAt: number
  updatedAt: number
}

export interface ProductPriceLevelWritePayload {
  levelId: number
  price: number
}

export interface ProductSupplierRelationWritePayload {
  productId: number
  supplierId: number
  isDefault?: boolean | null
  purchasePriority?: number | null
  lastPurchasePrice?: number | null
  notes?: string | null
}

export interface ProductWritePayload {
  code: string
  name: string
  categoryId: number
  unitId: number
  salePrice: number
  purchasePrice: number
  priceLevels?: ProductPriceLevelWritePayload[]
  supplierRelations?: ProductSupplierRelationWritePayload[]
  stock: number
  safeStock: number
  status: number
}

export interface SalesSummaryReport {
  startAt: number
  endAt: number
  totalSalesAmount: number
  totalPaidAmount: number
  totalRefundAmount: number
  totalUnpaidAmount: number
  totalOrderCount: number
}

export interface SalesTrendPoint {
  startAt: number
  endAt: number
  totalSalesAmount: number
  totalOrderCount: number
}

export interface RefundRecordReportItem {
  paymentId: EntityId
  orderId: EntityId
  orderNo: string
  customerName: string
  refundAmount: number
  method: number
  referenceNo: string | null
  createdAt: number
}

export interface StockOutRecordReportItem {
  orderId: EntityId
  orderNo: string
  customerId: number | null
  customerName: string | null
  productId: number
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  amount: number
  itemCreatedAt: number
  orderCreatedAt: number
}

export interface TopProductReportItem {
  productId: number
  productCode: string
  productName: string
  totalQuantity: number
  totalAmount: number
}

export interface ProfitByProductReportItem {
  productId: number
  productCode: string
  productName: string
  totalSalesAmount: number
  totalCostAmount: number
  totalProfitAmount: number
  profitRate: number
}

export interface ProfitByCustomerReportItem {
  customerId: number | null
  customerName: string | null
  totalSalesAmount: number
  totalCostAmount: number
  totalProfitAmount: number
  profitRate: number
}

export interface InventoryFlowReportItem {
  orderId: EntityId
  orderNo: string
  productId: number
  productCode: string
  productName: string
  quantity: number
  flowType: number
  flowTime: number
  customerName: string | null
  sourceType: number
  sourceLabel: string | null
  adjustReason: string | null
  operatorName: string | null
}

export interface CustomerSalesReportItem {
  customerId: number | null
  customerName: string | null
  totalOrders: number
  totalAmount: number
}

export interface CustomerReceivableReportItem {
  customerId: number
  customerName: string
  phone: string | null
  balance: number
}

export interface ProfitSummaryReport {
  startAt: number
  endAt: number
  estimatedCostAmount: number
  estimatedProfitAmount: number
  estimatedProfitRate: number
}

export interface CashflowSummaryReport {
  startAt: number
  endAt: number
  totalIncomeAmount: number
  totalExpenseAmount: number
  netCashFlow: number
  totalRecordCount: number
}

export interface ReconciliationSummaryReport {
  startAt: number
  endAt: number
  totalReceivableAmount: number
  totalPayableAmount: number
  totalReceivableCustomerCount: number
  totalPayableSupplierCount: number
  totalReceivedAmount: number
  totalPaidAmount: number
  netCashFlow: number
}

export interface LowStockReportItem {
  productId: number
  productCode: string
  productName: string
  stock: number
  safeStock: number
}

export interface InventoryLedgerEntry {
  id: EntityId
  productId: number
  productCode: string
  productName: string
  warehouseId: number | null
  quantityBefore: number
  quantityChange: number
  quantityAfter: number
  unitCost: number | null
  sourceType: string
  sourceId: EntityId | null
  sourceNo: string | null
  notes: string | null
  createdAt: number
}

export interface InventorySnapshot {
  id: number
  productId: number
  productCode: string
  productName: string
  warehouseId: number | null
  quantity: number
  unitCost: number | null
  totalValue: number | null
  snapshotDate: number
  createdAt: number
}

export interface FinanceRecord {
  id: number
  recordNo: string
  type: number
  category: string | null
  partnerName: string | null
  amount: number
  method: number | null
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface FinanceRecordCreatePayload {
  type: number
  category?: string | null
  partnerName?: string | null
  amount: number
  method?: number | null
  notes?: string | null
}

export interface AccountRecord {
  id: EntityId
  code: string
  name: string
  type: number
  balance: number
  isDefault: boolean
  status: number
  sortOrder: number | null
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface AccountWritePayload {
  code: string
  name: string
  type: number
  balance?: number | null
  isDefault?: boolean | null
  status?: number | null
  sortOrder?: number | null
  notes?: string | null
}

export interface AccountTransferRecord {
  id: EntityId
  transferNo: string
  fromAccountId: EntityId
  fromAccountName: string
  toAccountId: EntityId
  toAccountName: string
  amount: number
  fee: number | null
  status: number
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface AccountTransferPayload {
  fromAccountId: EntityId
  toAccountId: EntityId
  amount: number
  fee?: number | null
  notes?: string | null
}

export interface ContactRecord {
  id: EntityId
  partnerType: string
  partnerId: EntityId
  name: string
  phone: string | null
  title: string | null
  isPrimary: boolean
  createdAt: number
  updatedAt: number
}

export interface ContactWritePayload {
  partnerId: EntityId
  name: string
  phone?: string | null
  title?: string | null
  isPrimary?: boolean | null
}

export interface MediaAssetRecord {
  id: EntityId
  assetType: string
  storageProvider: string
  bucketName: string | null
  objectKey: string
  originalFileName: string
  mimeType: string
  sizeBytes: number
  checksum: string | null
  width: number | null
  height: number | null
  metadataJson: string | null
  createdAt: number
  updatedAt: number
}

export interface MediaBindingRecord {
  id: EntityId
  assetId: EntityId
  targetType: string
  targetId: EntityId
  sortOrder: number
  createdAt: number
}

export interface MediaBindingCreatePayload {
  assetId: EntityId
  targetType: string
  targetId: EntityId
  sortOrder?: number | null
}

export interface InventoryLedgerCreatePayload {
  productId: EntityId
  sourceType: string
  sourceId?: EntityId | null
  sourceNo?: string | null
  quantityChange: number
  unitCost?: number | null
  warehouseId?: number | null
  notes?: string | null
}

export interface InventorySnapshotCreatePayload {
  productId: number
  snapshotDate: number
  warehouseId?: number | null
}

export interface PurchaseReceiptItem {
  id: EntityId
  receiptId: EntityId
  productId: number | null
  productCode: string | null
  productName: string | null
  quantity: number
  unitCost: number | null
  amount: number
  createdAt: number
}

export interface PurchaseReceipt {
  id: EntityId
  receiptNo: string
  purchaseOrderId: EntityId | null
  supplierId: number | null
  supplierName: string | null
  items: PurchaseReceiptItem[]
  totalAmount: number
  status: number
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface PurchaseReceiptCreateItemPayload {
  productId?: number | null
  productCode?: string | null
  productName?: string | null
  quantity: number
  unitCost: number
}

export interface PurchaseReceiptWritePayload {
  purchaseOrderId?: EntityId | null
  supplierId?: number | null
  supplierName?: string | null
  items: PurchaseReceiptCreateItemPayload[]
  notes?: string | null
}

export interface PurchaseReturnItem {
  id: EntityId
  returnId: EntityId
  productId: number | null
  productCode: string | null
  productName: string | null
  quantity: number
  unitCost: number | null
  amount: number
  createdAt: number
}

export interface PurchaseReturnRefund {
  id: EntityId
  returnId: EntityId
  amount: number
  method: number
  referenceNo: string | null
  createdAt: number
}

export interface PurchaseReturn {
  id: EntityId
  returnNo: string
  purchaseOrderId: EntityId | null
  supplierId: number | null
  supplierName: string | null
  items: PurchaseReturnItem[]
  refunds: PurchaseReturnRefund[]
  totalAmount: number
  refundAmount: number
  status: number
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface PurchaseReturnCreateItemPayload {
  productId?: number | null
  productCode?: string | null
  productName?: string | null
  quantity: number
  unitCost?: number | null
}

export interface PurchaseReturnWritePayload {
  purchaseOrderId?: EntityId | null
  supplierId?: number | null
  supplierName?: string | null
  items: PurchaseReturnCreateItemPayload[]
  notes?: string | null
}

export interface PurchaseReturnUpdateDraftPayload {
  notes?: string | null
}

export interface PurchaseReturnRefundPayload {
  amount?: number | null
  method?: number | null
  referenceNo?: string | null
}

export interface SalesReturnItem {
  id: EntityId
  returnId: EntityId
  productId: number | null
  productCode: string | null
  productName: string | null
  quantity: number
  unitPrice: number | null
  amount: number
  createdAt: number
}

export interface SalesReturn {
  id: EntityId
  returnNo: string
  originalOrderId: EntityId | null
  customerId: number | null
  customerName: string | null
  items: SalesReturnItem[]
  totalAmount: number
  refundAmount: number
  status: number
  notes: string | null
  createdAt: number
  updatedAt: number
}

export interface SalesReturnCreateItemPayload {
  productId?: number | null
  quantity: number
  unitPrice?: number | null
}

export interface SalesReturnCreatePayload {
  originalOrderId?: EntityId | null
  customerId?: number | null
  customerName?: string | null
  items: SalesReturnCreateItemPayload[]
  notes?: string | null
}

export interface SalesReturnUpdateDraftPayload {
  notes?: string | null
}

export interface SalesReturnRefundPayload {
  amount?: number | null
  method?: number | null
  referenceNo?: string | null
}

export interface SyncHealth {
  status: string
  message: string
  ownerScoped: boolean
  serverTime: number
  supportedEntityTypes: string[]
  uploadableEntityTypes: string[]
}

export interface ImportJob {
  id: number
  clientId: string
  sourceType: string
  sourceUri: string | null
  sourceChecksum: string | null
  idempotencyKey: string | null
  status: string
  stage: string | null
  retryCount: number | null
  replayCursor: string | null
  summaryJson: string | null
  optionsJson: string | null
  failureCode: string | null
  failureMessage: string | null
  createdAt: number
  updatedAt: number
  startedAt: number | null
  finishedAt: number | null
  lastHeartbeatAt: number | null
}

export interface LegacySqliteImportPayload {
  legacyDbPath: string
  resetOwnedData?: boolean
}

export interface ImportResult {
  userId: number
  phone: string
  nickname: string
  legacyDbPath: string
  accounts: number
  customers: number
  suppliers: number
  products: number
  saleOrders: number
  saleOrderItems: number
  payments: number
  purchaseOrders: number
  purchaseOrderItems: number
  payOrders: number
  financeRecords: number
  inventorySnapshots: number
}

export interface AgentWorkbench {
  greeting: string
  kpiCards: Array<{
    label: string
    value: string
    trendDirection: string | null
    trendValue: string | null
    route: string | null
  }>
  quickQuestions: string[]
  recentConversations: Array<{
    id: number
    title: string
    lastMessageAt: number
    messageCount: number
  }>
  pendingDrafts: Array<{
    id: number
    draftType: string
    title: string
    createdAt: number
  }>
  riskAlerts: Array<{
    level: string
    title: string
    description: string
  }>
  todaySummary: string
  status: string
  dataPolicy: string
  capabilities: Array<{
    id: string
    title: string
    description: string
  }>
  warnings: string[]
}

export interface AgentTask {
  id: number
  taskType: string
  title: string
  triggerSource: string
  status: string
  statusLabel: string
  progress: number | null
  inputText: string | null
  resultJson: string | null
  createdAt: number
  updatedAt: number
  completedAt: number | null
}

export interface AgentNotification {
  id: number
  taskId: number | null
  title: string
  body: string
  level: string
  isRead: boolean
  isDelivered: boolean
  createdAt: number
}

export interface AgentConversation {
  id: EntityId
  title: string
  status: string
  latestSummary: string | null
  createdAt: number
  updatedAt: number
  lastMessageAt: number | null
}

export interface AgentConversationCreatePayload {
  title: string
  status?: string | null
}

export interface AgentConversationUpdatePayload {
  title?: string | null
  status?: string | null
}

export interface AgentMessage {
  id: EntityId
  conversationId: EntityId
  role: string
  messageType: string
  content: string
  structuredDataJson: string | null
  createdAt: number
}

export interface AgentMessageCreatePayload {
  role: string
  messageType: string
  content: string
  structuredDataJson?: string | null
}

export interface AgentDraft {
  id: EntityId
  conversationId: EntityId | null
  draftType: string
  title: string
  contentJson: string
  status: string
  createdAt: number
  updatedAt: number
}

export interface AgentDraftCreatePayload {
  conversationId?: EntityId | null
  draftType: string
  title: string
  contentJson: string
  status?: string | null
}

export interface AgentDraftUpdatePayload {
  conversationId?: EntityId | null
  draftType: string
  title: string
  contentJson: string
  status?: string | null
}

export interface AgentRunCancelResult {
  runId: string
  status: string
  cancelled: boolean
}

export interface AgentRunAuditEvent {
  eventId: string | null
  seq: number | null
  eventType: string
  payload: unknown
  createdAt: number
}

export interface AgentRunAudit {
  runId: string
  ownerUserId: number | null
  conversationId: EntityId | null
  status: string
  mode: string | null
  llmStatus: string | null
  planSource: string | null
  toolCount: number | null
  eventCount: number | null
  auditWriteDroppedCount: number | null
  auditWriteFailedCount: number | null
  auditLossy: boolean | null
  emittedEventCount: number | null
  warnings: string[]
  auditId: string | null
  traceId: string | null
  errorCode: string | null
  errorMessage: string | null
  startedAt: number | null
  completedAt: number | null
  updatedAt: number | null
  events: AgentRunAuditEvent[]
}

export interface AgentResultBlock {
  blockType: string
  title: string | null
  data: unknown
}

export interface AgentToolCall {
  toolCallId: string | null
  toolName: string
  status: string
  inputSummary: string | null
  queryWindow: unknown
  returnedCount: number | null
  totalCount: number | null
  limit: number | null
  isTruncated: boolean | null
  durationMs: number | null
  resultSummary: string | null
  errorCode: string | null
  errorMessage: string | null
}

export interface AgentEvidenceRef {
  evidenceId: string | null
  toolCallId: string | null
  toolName: string | null
  label: string
  value: string
  queryWindow: unknown
  isTruncated: boolean | null
}

export interface AgentPerformanceSummary {
  startedAt: number | null
  completedAt: number | null
  durationMs: number | null
  toolDurationMs: number | null
  modelDurationMs: number | null
}

export interface AgentObservability {
  requestId: string | null
  correlationId: string | null
  traceId: string | null
  auditId: string | null
  logRef: string | null
}

export interface AgentChatPayload {
  conversationId?: EntityId | null
  message: string
  stream?: boolean
}

export interface AgentChatResponse {
  runId: string
  conversationId: EntityId
  answer: string
  blocks: AgentResultBlock[]
  draftId: EntityId | null
  safetyPassed: boolean
  safetyReason: string | null
  mode: string | null
  llmStatus: string | null
  planSource: string | null
  planSummary: string | null
  toolCalls: AgentToolCall[]
  evidenceRefs: AgentEvidenceRef[]
  resultBlocks: AgentResultBlock[]
  performanceSummary: AgentPerformanceSummary | null
  auditId: string | null
  traceId: string | null
  observability: AgentObservability | null
}

export function configureAuthRuntime(config: AuthRuntimeConfig) {
  authRuntime.getRefreshToken = config.getRefreshToken
  authRuntime.onAuthRefreshed = config.onAuthRefreshed
  authRuntime.onAuthExpired = config.onAuthExpired
}

export async function login(phone: string, password: string) {
  return request<AuthPayload>('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  })
}

async function refreshAuth(refreshToken: string) {
  return request<AuthPayload>('/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
}

export async function logout(token?: string) {
  return request<void>('/v1/auth/logout', {
    method: 'POST',
    headers: token ? authHeaders(token) : undefined,
  })
}

export async function fetchCurrentUser(token: string) {
  return request<UserProfile>('/v1/auth/users/me', {
    headers: authHeaders(token),
  })
}

export async function fetchCurrentStore(token: string) {
  return request<CurrentStoreProfile>('/v2/stores/current', {
    headers: authHeaders(token),
  })
}

export async function fetchStoreMembers(token: string) {
  return request<StoreMemberRecord[]>('/v2/stores/current/members', {
    headers: authHeaders(token),
  })
}

export async function createStoreMember(token: string, payload: StoreMemberCreatePayload) {
  return request<StoreMemberRecord>('/v2/stores/current/members', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function updateStoreMember(token: string, userId: number, payload: StoreMemberUpdatePayload) {
  return request<StoreMemberRecord>(`/v2/stores/current/members/${userId}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({
      ...payload,
      keepSessions: payload.keepSessions ?? false,
    }),
  })
}

export async function fetchSaleOrders(token: string, params: {
  keyword?: string
  status?: number
  minTotalAmount?: number
  maxTotalAmount?: number
  createdAfter?: number
  createdBefore?: number
  productKeyword?: string
  paymentStatus?: number
  page?: number
  size?: number
} = {}) {
  return request<SaleOrder[]>(`/v2/sale-orders${buildQuery({
    keyword: params.keyword,
    status: params.status,
    min_total_amount: params.minTotalAmount,
    max_total_amount: params.maxTotalAmount,
    created_after: params.createdAfter,
    created_before: params.createdBefore,
    product_keyword: params.productKeyword,
    payment_status: params.paymentStatus,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSaleOrder(token: string, id: EntityId) {
  return request<SaleOrder>(`/v2/sale-orders/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createSaleOrder(token: string, payload: SaleOrderCreatePayload) {
  return request<SaleOrder>('/v2/sale-orders', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toSaleOrderCreateBody(payload)),
  })
}

export async function updateSaleOrderDraft(token: string, id: EntityId, payload: SaleOrderUpdateDraftPayload) {
  return request<SaleOrder>(`/v2/sale-orders/${id}/draft`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toSaleOrderDraftBody(payload)),
  })
}

export async function confirmSaleOrder(token: string, id: EntityId, payload: SaleOrderConfirmPayload = {}) {
  return request<SaleOrder>(`/v2/sale-orders/${id}/confirm`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ notes: payload.notes ?? null }),
  })
}

export async function fetchSaleOrderPayments(token: string, id: EntityId) {
  return request<PaymentRecord[]>(`/v2/sale-orders/${id}/payments`, {
    headers: authHeaders(token),
  })
}

export async function createSaleOrderPayment(token: string, id: EntityId, payload: SaleOrderPaymentPayload) {
  return request<PaymentRecord>(`/v2/sale-orders/${id}/payments`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      amount: payload.amount,
      method: payload.method,
      reference_no: payload.referenceNo ?? null,
    }),
  })
}

export async function cancelSaleOrder(token: string, id: EntityId) {
  return request<SaleOrder>(`/v2/sale-orders/${id}/cancel`, {
    method: 'PUT',
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseOrders(token: string, params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
} = {}) {
  return request<PurchaseOrder[]>(`/v2/purchase-orders${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseOrder(token: string, id: EntityId) {
  return request<PurchaseOrder>(`/v2/purchase-orders/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createPurchaseOrder(token: string, payload: PurchaseOrderWritePayload) {
  return request<PurchaseOrder>('/v2/purchase-orders', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toPurchaseOrderBody(payload)),
  })
}

export async function updatePurchaseOrder(token: string, id: EntityId, payload: PurchaseOrderWritePayload) {
  return request<PurchaseOrder>(`/v2/purchase-orders/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toPurchaseOrderBody(payload)),
  })
}

export async function fetchProducts(token: string, params: {
  keyword?: string
  status?: number
  categoryId?: number
  unitId?: number
  page?: number
  size?: number
} = {}) {
  return request<ProductRecord[]>(`/v2/products${buildQuery({
    keyword: params.keyword,
    status: params.status,
    category_id: params.categoryId,
    unit_id: params.unitId,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchProduct(token: string, id: EntityId) {
  return request<ProductRecord>(`/v2/products/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createProduct(token: string, payload: ProductWritePayload) {
  return request<ProductRecord>('/v2/products', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toProductWriteBody(payload)),
  })
}

export async function updateProduct(token: string, id: EntityId, payload: ProductWritePayload) {
  return request<ProductRecord>(`/v2/products/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toProductWriteBody(payload)),
  })
}

export async function deleteProduct(token: string, id: EntityId) {
  return request<void>(`/v2/products/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchProductCategories(token: string) {
  return request<ProductCategoryRecord[]>('/v2/product-categories', {
    headers: authHeaders(token),
  })
}

export async function fetchProductUnits(token: string) {
  return request<ProductUnitRecord[]>('/v2/product-units', {
    headers: authHeaders(token),
  })
}

export async function fetchCustomers(token: string, params: {
  keyword?: string
  status?: number
  groupId?: number
  page?: number
  size?: number
} = {}) {
  return request<CustomerRecord[]>(`/v2/customers${buildQuery({
    keyword: params.keyword,
    status: params.status,
    group_id: params.groupId,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createCustomer(token: string, payload: CustomerWritePayload) {
  return request<CustomerRecord>('/v2/customers', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function updateCustomer(token: string, id: number, payload: CustomerWritePayload) {
  return request<CustomerRecord>(`/v2/customers/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function deleteCustomer(token: string, id: number) {
  return request<void>(`/v2/customers/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchCustomerGroups(token: string) {
  return request<PartnerGroupRecord[]>('/v2/customer-groups', {
    headers: authHeaders(token),
  })
}

export async function createCustomerGroup(token: string, payload: PartnerGroupPayload) {
  return request<PartnerGroupRecord>('/v2/customer-groups', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function updateCustomerGroup(token: string, id: number, payload: PartnerGroupPayload) {
  return request<PartnerGroupRecord>(`/v2/customer-groups/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function deleteCustomerGroup(token: string, id: number) {
  return request<void>(`/v2/customer-groups/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchSuppliers(token: string, params: {
  keyword?: string
  status?: number
  groupId?: number
  page?: number
  size?: number
} = {}) {
  return request<SupplierRecord[]>(`/v2/suppliers${buildQuery({
    keyword: params.keyword,
    status: params.status,
    group_id: params.groupId,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createSupplier(token: string, payload: SupplierWritePayload) {
  return request<SupplierRecord>('/v2/suppliers', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function updateSupplier(token: string, id: number, payload: SupplierWritePayload) {
  return request<SupplierRecord>(`/v2/suppliers/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function deleteSupplier(token: string, id: number) {
  return request<void>(`/v2/suppliers/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchSupplierGroups(token: string) {
  return request<PartnerGroupRecord[]>('/v2/supplier-groups', {
    headers: authHeaders(token),
  })
}

export async function createSupplierGroup(token: string, payload: PartnerGroupPayload) {
  return request<PartnerGroupRecord>('/v2/supplier-groups', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function updateSupplierGroup(token: string, id: number, payload: PartnerGroupPayload) {
  return request<PartnerGroupRecord>(`/v2/supplier-groups/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function deleteSupplierGroup(token: string, id: number) {
  return request<void>(`/v2/supplier-groups/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchLowStockProducts(token: string, size?: number) {
  return request<ProductRecord[]>(`/v2/products/low-stock${buildQuery({ size })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSalesSummary(token: string, startAt: number, endAt: number) {
  return request<SalesSummaryReport>(`/v1/reports/sales-summary${buildQuery({ start_at: startAt, end_at: endAt })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSalesTrend(token: string, startAt: number, endAt: number, bucket = 'day') {
  return request<SalesTrendPoint[]>(`/v1/reports/sales-trend${buildQuery({ start_at: startAt, end_at: endAt, bucket })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchProfitSummary(token: string, startAt: number, endAt: number) {
  return request<ProfitSummaryReport>(`/v1/reports/profit-summary${buildQuery({ start_at: startAt, end_at: endAt })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchCashflowSummary(token: string, startAt: number, endAt: number) {
  return request<CashflowSummaryReport>(`/v1/reports/cashflow-summary${buildQuery({ start_at: startAt, end_at: endAt })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchReconciliationSummary(token: string, startAt: number, endAt: number) {
  return request<ReconciliationSummaryReport>(`/v1/reports/reconciliation-summary${buildQuery({ start_at: startAt, end_at: endAt })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchLowStockReport(token: string, limit = 10) {
  return request<LowStockReportItem[]>(`/v1/reports/low-stock-products${buildQuery({ limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchRefundRecords(token: string, startAt: number, endAt: number, limit = 10) {
  return request<RefundRecordReportItem[]>(`/v1/reports/refund-records${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchStockOutRecords(token: string, startAt: number, endAt: number, limit = 10) {
  return request<StockOutRecordReportItem[]>(`/v1/reports/stock-out-records${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchTopProducts(token: string, startAt: number, endAt: number, limit = 10) {
  return request<TopProductReportItem[]>(`/v1/reports/top-products${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchProfitByProducts(token: string, startAt: number, endAt: number, limit = 10) {
  return request<ProfitByProductReportItem[]>(`/v1/reports/profit-by-products${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchProfitByCustomers(token: string, startAt: number, endAt: number, limit = 10) {
  return request<ProfitByCustomerReportItem[]>(`/v1/reports/profit-by-customers${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchInventoryFlowReport(token: string, startAt: number, endAt: number, limit = 10) {
  return request<InventoryFlowReportItem[]>(`/v1/reports/inventory-flow${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchCustomerSalesReport(token: string, startAt: number, endAt: number, limit = 10) {
  return request<CustomerSalesReportItem[]>(`/v1/reports/customer-sales${buildQuery({ start_at: startAt, end_at: endAt, limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchTopReceivableCustomers(token: string, limit = 10) {
  return request<CustomerReceivableReportItem[]>(`/v1/reports/top-receivable-customers${buildQuery({ limit })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchInventoryLedger(token: string, params: {
  productId?: EntityId
  startAt?: number
  endAt?: number
} = {}) {
  return request<InventoryLedgerEntry[]>(`/v2/inventory/ledger${buildQuery({
    product_id: params.productId,
    startAt: params.startAt,
    endAt: params.endAt,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createInventoryLedgerEntry(token: string, payload: InventoryLedgerCreatePayload) {
  return request<InventoryLedgerEntry>('/v2/inventory/ledger', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      product_id: payload.productId,
      source_type: payload.sourceType,
      source_id: payload.sourceId ?? null,
      source_no: payload.sourceNo ?? null,
      quantity_change: payload.quantityChange,
      unit_cost: payload.unitCost ?? null,
      warehouse_id: payload.warehouseId ?? null,
      notes: payload.notes ?? null,
    }),
  })
}

export async function fetchInventorySnapshots(token: string, params: {
  snapshotDate?: number
  startDate?: number
  endDate?: number
} = {}) {
  return request<InventorySnapshot[]>(`/v2/inventory/snapshots${buildQuery({
    snapshotDate: params.snapshotDate,
    startDate: params.startDate,
    endDate: params.endDate,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createInventorySnapshot(token: string, payload: InventorySnapshotCreatePayload) {
  return request<InventorySnapshot>('/v2/inventory/snapshots', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      product_id: payload.productId,
      snapshot_date: payload.snapshotDate,
      warehouse_id: payload.warehouseId ?? null,
    }),
  })
}

export async function fetchFinanceRecords(token: string, params: {
  keyword?: string
  type?: number
  createdAfter?: number
  createdBefore?: number
  page?: number
  size?: number
} = {}) {
  return request<FinanceRecord[]>(`/v1/finance-records${buildQuery({
    keyword: params.keyword,
    type: params.type,
    created_after: params.createdAfter,
    created_before: params.createdBefore,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createFinanceRecord(token: string, payload: FinanceRecordCreatePayload) {
  return request<FinanceRecord>('/v1/finance-records', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      type: payload.type,
      category: payload.category ?? null,
      partnerName: payload.partnerName ?? null,
      amount: payload.amount,
      method: payload.method ?? null,
      notes: payload.notes ?? null,
    }),
  })
}

export async function fetchAccounts(token: string) {
  return request<AccountRecord[]>('/v2/accounts', {
    headers: authHeaders(token),
  })
}

export async function fetchAccount(token: string, id: EntityId) {
  return request<AccountRecord>(`/v2/accounts/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createAccount(token: string, payload: AccountWritePayload) {
  return request<AccountRecord>('/v2/accounts', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toAccountWriteBody(payload)),
  })
}

export async function updateAccount(token: string, id: EntityId, payload: AccountWritePayload) {
  return request<AccountRecord>(`/v2/accounts/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toAccountWriteBody(payload)),
  })
}

export async function deleteAccount(token: string, id: EntityId) {
  return request<void>(`/v2/accounts/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchAccountTransfers(token: string) {
  return request<AccountTransferRecord[]>('/v2/account-transfers', {
    headers: authHeaders(token),
  })
}

export async function fetchAccountTransfer(token: string, id: EntityId) {
  return request<AccountTransferRecord>(`/v2/account-transfers/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createAccountTransfer(token: string, payload: AccountTransferPayload) {
  return request<AccountTransferRecord>('/v2/account-transfers', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toAccountTransferBody(payload)),
  })
}

export async function fetchCustomerContacts(token: string, customerId: EntityId) {
  return request<ContactRecord[]>(`/v2/customer-contacts${buildQuery({ customer_id: customerId })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSupplierContacts(token: string, supplierId: EntityId) {
  return request<ContactRecord[]>(`/v2/supplier-contacts${buildQuery({ supplier_id: supplierId })}`, {
    headers: authHeaders(token),
  })
}

export async function createCustomerContact(token: string, payload: ContactWritePayload) {
  return request<ContactRecord>('/v2/customer-contacts', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toContactWriteBody(payload)),
  })
}

export async function updateCustomerContact(token: string, id: EntityId, payload: ContactWritePayload) {
  return request<ContactRecord>(`/v2/customer-contacts/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toContactWriteBody(payload)),
  })
}

export async function deleteCustomerContact(token: string, id: EntityId) {
  return request<void>(`/v2/customer-contacts/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function createSupplierContact(token: string, payload: ContactWritePayload) {
  return request<ContactRecord>('/v2/supplier-contacts', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toContactWriteBody(payload)),
  })
}

export async function updateSupplierContact(token: string, id: EntityId, payload: ContactWritePayload) {
  return request<ContactRecord>(`/v2/supplier-contacts/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(toContactWriteBody(payload)),
  })
}

export async function deleteSupplierContact(token: string, id: EntityId) {
  return request<void>(`/v2/supplier-contacts/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchMediaBindings(token: string, targetType: string, targetId: EntityId) {
  return request<MediaBindingRecord[]>(`/v2/media/bindings${buildQuery({ target_type: targetType, target_id: targetId })}`, {
    headers: authHeaders(token),
  })
}

export async function createMediaBinding(token: string, payload: MediaBindingCreatePayload) {
  return request<MediaBindingRecord>('/v2/media/bindings', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      asset_id: payload.assetId,
      target_type: payload.targetType,
      target_id: payload.targetId,
      sort_order: payload.sortOrder ?? null,
    }),
  })
}

export async function deleteMediaBinding(token: string, id: EntityId) {
  return request<void>(`/v2/media/bindings/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function uploadMediaAsset(token: string, file: File, assetType: string = 'product_image') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('asset_type', assetType)
  return request<MediaAssetRecord>('/v2/media/assets/upload', {
    method: 'POST',
    headers: authHeaders(token),
    body: formData,
  })
}

export async function deleteMediaAsset(token: string, id: EntityId) {
  return request<void>(`/v2/media/assets/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export function mediaAssetContentUrl(id: EntityId): string {
  return `${API_BASE_URL}/v2/media/assets/${id}/content`
}

export async function fetchPurchaseReceipts(token: string, params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
  } = {}) {
  return request<PurchaseReceipt[]>(`/v2/purchase-receipts${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseReceiptsByOrder(token: string, orderId: EntityId) {
  return request<PurchaseReceipt[]>(`/v2/purchase-receipts/by-order/${orderId}`, {
    headers: authHeaders(token),
  })
}

export async function createPurchaseReceipt(token: string, payload: PurchaseReceiptWritePayload) {
  return request<PurchaseReceipt>('/v2/purchase-receipts', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toPurchaseReceiptBody(payload)),
  })
}

export async function confirmPurchaseReceipt(token: string, id: EntityId) {
  return request<PurchaseReceipt>(`/v2/purchase-receipts/${id}/confirm`, {
    method: 'PUT',
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseReturns(token: string, params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
} = {}) {
  return request<PurchaseReturn[]>(`/v2/purchase-returns${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseReturn(token: string, id: EntityId) {
  return request<PurchaseReturn>(`/v2/purchase-returns/${id}`, {
    headers: authHeaders(token),
  })
}

export async function fetchPurchaseReturnsByOrder(token: string, orderId: EntityId) {
  return request<PurchaseReturn[]>(`/v2/purchase-returns/by-order/${orderId}`, {
    headers: authHeaders(token),
  })
}

export async function createPurchaseReturn(token: string, payload: PurchaseReturnWritePayload) {
  return request<PurchaseReturn>('/v2/purchase-returns', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toPurchaseReturnBody(payload)),
  })
}

export async function updatePurchaseReturnDraft(token: string, id: EntityId, payload: PurchaseReturnUpdateDraftPayload) {
  return request<PurchaseReturn>(`/v2/purchase-returns/${id}/draft`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ notes: payload.notes ?? null }),
  })
}

export async function confirmPurchaseReturn(token: string, id: EntityId, payload: PurchaseReturnUpdateDraftPayload = {}) {
  return request<PurchaseReturn>(`/v2/purchase-returns/${id}/confirm`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ notes: payload.notes ?? null }),
  })
}

export async function addPurchaseReturnRefund(token: string, id: EntityId, payload: PurchaseReturnRefundPayload) {
  return request<PurchaseReturn>(`/v2/purchase-returns/${id}/refunds`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      amount: payload.amount ?? null,
      method: payload.method ?? null,
      reference_no: payload.referenceNo ?? null,
    }),
  })
}

export async function cancelPurchaseReturn(token: string, id: EntityId) {
  return request<PurchaseReturn>(`/v2/purchase-returns/${id}/cancel`, {
    method: 'PUT',
    headers: authHeaders(token),
  })
}

export async function fetchSalesReturns(token: string, params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
} = {}) {
  return request<SalesReturn[]>(`/v2/sales-returns${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSalesReturn(token: string, id: EntityId) {
  return request<SalesReturn>(`/v2/sales-returns/${id}`, {
    headers: authHeaders(token),
  })
}

export async function fetchSalesReturnsByOrder(token: string, orderId: EntityId) {
  return request<SalesReturn[]>(`/v2/sales-returns/by-order/${orderId}`, {
    headers: authHeaders(token),
  })
}

export async function createSalesReturn(token: string, payload: SalesReturnCreatePayload) {
  return request<SalesReturn>('/v2/sales-returns', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toSalesReturnBody(payload)),
  })
}

export async function updateSalesReturnDraft(token: string, id: EntityId, payload: SalesReturnUpdateDraftPayload) {
  return request<SalesReturn>(`/v2/sales-returns/${id}/draft`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ notes: payload.notes ?? null }),
  })
}

export async function confirmSalesReturn(token: string, id: EntityId, payload: SalesReturnUpdateDraftPayload = {}) {
  return request<SalesReturn>(`/v2/sales-returns/${id}/confirm`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ notes: payload.notes ?? null }),
  })
}

export async function addSalesReturnRefund(token: string, id: EntityId, payload: SalesReturnRefundPayload) {
  return request<SalesReturn>(`/v2/sales-returns/${id}/refunds`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      amount: payload.amount ?? null,
      method: payload.method ?? null,
      reference_no: payload.referenceNo ?? null,
    }),
  })
}

export async function cancelSalesReturn(token: string, id: EntityId) {
  return request<SalesReturn>(`/v2/sales-returns/${id}/cancel`, {
    method: 'PUT',
    headers: authHeaders(token),
  })
}

export async function fetchPayOrders(token: string, params: {
  keyword?: string
  status?: number
  createdAfter?: number
  createdBefore?: number
  page?: number
  size?: number
} = {}) {
  return request<PayOrder[]>(`/v2/pay-orders${buildQuery({
    keyword: params.keyword,
    status: params.status,
    created_after: params.createdAfter,
    created_before: params.createdBefore,
    page: params.page,
    size: params.size,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function fetchPayOrder(token: string, id: EntityId) {
  return request<PayOrder>(`/v2/pay-orders/${id}`, {
    headers: authHeaders(token),
  })
}

export async function createPayOrder(token: string, payload: PayOrderCreatePayload) {
  return request<PayOrder>('/v2/pay-orders', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(toPayOrderBody(payload)),
  })
}

export async function updatePayOrderStatus(token: string, id: EntityId, status: number) {
  return request<PayOrder>(`/v2/pay-orders/${id}/status`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ status }),
  })
}

export async function fetchSyncHealth(token: string) {
  return request<SyncHealth>('/v2/sync/health', {
    headers: authHeaders(token),
  })
}

export async function fetchImportJobs(token: string, status?: string) {
  return request<ImportJob[]>(`/v2/import-jobs${buildQuery({ status })}`, {
    headers: authHeaders(token),
  })
}

export async function importLegacySqlite(token: string, payload: LegacySqliteImportPayload) {
  return request<ImportResult>('/v2/import-jobs/legacy-sqlite', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  })
}

export async function fetchAgentWorkbench(token: string) {
  return request<AgentWorkbench>('/v2/agent/workbench', {
    headers: authHeaders(token),
  })
}

export async function fetchAgentConversations(token: string, params: { page?: number; limit?: number } = {}) {
  return request<AgentConversation[]>(`/v2/agent/conversations${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function createAgentConversation(token: string, payload: AgentConversationCreatePayload) {
  return request<AgentConversation>('/v2/agent/conversations', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      title: payload.title,
      status: payload.status ?? null,
    }),
  })
}

export async function deleteAgentConversation(token: string, id: EntityId) {
  return request<void>(`/v2/agent/conversations/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function fetchAgentMessages(token: string, conversationId: EntityId, params: { page?: number; limit?: number } = {}) {
  return request<AgentMessage[]>(`/v2/agent/conversations/${conversationId}/messages${buildQuery(params)}`, {
    headers: authHeaders(token),
  })
}

export async function fetchAgentDrafts(token: string, params: { conversationId?: EntityId; page?: number; limit?: number } = {}) {
  return request<AgentDraft[]>(`/v2/agent/drafts${buildQuery({
    conversation_id: params.conversationId,
    page: params.page,
    limit: params.limit,
  })}`, {
    headers: authHeaders(token),
  })
}

export async function createAgentDraft(token: string, payload: AgentDraftCreatePayload) {
  return request<AgentDraft>('/v2/agent/drafts', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({
      conversation_id: payload.conversationId ?? null,
      draft_type: payload.draftType,
      title: payload.title,
      content_json: payload.contentJson,
      status: payload.status ?? null,
    }),
  })
}

export async function updateAgentDraft(token: string, id: EntityId, payload: AgentDraftUpdatePayload) {
  return request<AgentDraft>(`/v2/agent/drafts/${id}`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({
      conversation_id: payload.conversationId ?? null,
      draft_type: payload.draftType,
      title: payload.title,
      content_json: payload.contentJson,
      status: payload.status ?? null,
    }),
  })
}

export async function deleteAgentDraft(token: string, id: EntityId) {
  return request<void>(`/v2/agent/drafts/${id}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  })
}

export async function confirmAgentDraft(token: string, id: EntityId) {
  return request<AgentDraft>(`/v2/agent/drafts/${id}/confirm`, {
    method: 'POST',
    headers: authHeaders(token),
  })
}

export async function cancelAgentDraftAction(token: string, id: EntityId) {
  return request<AgentDraft>(`/v2/agent/drafts/${id}/cancel`, {
    method: 'POST',
    headers: authHeaders(token),
  })
}

export async function fetchAgentTasks(token: string) {
  return request<AgentTask[]>('/v2/agent/tasks', {
    headers: authHeaders(token),
  })
}

export async function fetchAgentNotifications(token: string, unreadOnly = false) {
  return request<AgentNotification[]>(`/v2/agent/notifications${buildQuery({ unread_only: unreadOnly })}`, {
    headers: authHeaders(token),
  })
}

export async function markAgentNotificationRead(token: string, id: EntityId) {
  return request<AgentNotification>(`/v2/agent/notifications/${id}/read`, {
    method: 'POST',
    headers: authHeaders(token),
  })
}

export async function cancelAgentRun(token: string, runId: string) {
  return request<AgentRunCancelResult>(`/v2/agent/runs/${runId}/cancel`, {
    method: 'POST',
    headers: authHeaders(token),
  })
}

export async function fetchAgentRunAudit(token: string, runId: string) {
  return request<AgentRunAudit>(`/v2/agent/runs/${runId}/audit`, {
    headers: authHeaders(token),
  })
}

async function request<T>(path: string, init: RequestInit = {}, hasRetriedAuth = false): Promise<T> {
  const requestHeaders = headersToRecord(init.headers)
  const headers = buildHeaders(requestHeaders, init.body)
  const hasAuthHeader = hasAuthorization(requestHeaders)
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })

  const rawText = await response.text()
  const payload = rawText ? safeParse<T>(rawText) : null

  if (response.status === 401 && !hasRetriedAuth && hasAuthHeader) {
    const refreshedToken = await tryRefreshAccessToken()
    if (refreshedToken) {
      return request<T>(path, withAuthorization(init, refreshedToken), true)
    }
    emitApiAuthEvent(401)
  } else if (response.status === 403 && hasAuthHeader) {
    emitApiAuthEvent(403)
  }

  if (!response.ok || !payload || payload.code !== 0) {
    const message = payload?.message || `request failed: ${response.status}`
    throw new ApiError(message, response.status, payload?.code ?? -1)
  }

  return camelize(payload.data) as T
}

function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
  }
}

function buildHeaders(headers: Record<string, string>, body?: BodyInit | null) {
  if (body instanceof FormData) {
    return headers
  }
  if (body != null && !hasContentType(headers)) {
    return { ...headers, 'Content-Type': 'application/json' }
  }
  return headers
}

function headersToRecord(headers?: HeadersInit): Record<string, string> {
  if (!headers) return {}
  if (headers instanceof Headers) {
    const record: Record<string, string> = {}
    headers.forEach((value, key) => {
      record[key] = value
    })
    return record
  }
  if (Array.isArray(headers)) {
    return Object.fromEntries(headers) as Record<string, string>
  }
  return headers as Record<string, string>
}

function hasAuthorization(headers: Record<string, string>) {
  return typeof headers.Authorization === 'string' || typeof headers.authorization === 'string'
}

function hasContentType(headers: Record<string, string>) {
  return Object.keys(headers).some(key => key.toLowerCase() === 'content-type')
}

function withAuthorization(init: RequestInit, token: string): RequestInit {
  const headers = headersToRecord(init.headers)
  headers.Authorization = `Bearer ${token}`
  return {
    ...init,
    headers,
  }
}

async function tryRefreshAccessToken() {
  const refreshToken = authRuntime.getRefreshToken?.()
  if (!refreshToken || !authRuntime.onAuthRefreshed) {
    authRuntime.onAuthExpired?.()
    return null
  }
  try {
    const refreshed = await refreshAuth(refreshToken)
    authRuntime.onAuthRefreshed(refreshed)
    return refreshed.token
  } catch {
    authRuntime.onAuthExpired?.()
    return null
  }
}

export function emitApiAuthEvent(status: 401 | 403) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent('zhihuiji:web:api-auth', { detail: { status } }))
}

function safeParse<T>(rawText: string) {
  try {
    return JSON.parse(preserveUnsafeIntegers(rawText)) as ApiResponse<T>
  } catch {
    return null
  }
}

export function preserveUnsafeIntegers(rawText: string) {
  const chunks: string[] = []
  let inString = false
  let isEscaped = false

  for (let index = 0; index < rawText.length; index += 1) {
    const char = rawText[index]

    if (inString) {
      chunks.push(char)
      if (isEscaped) {
        isEscaped = false
      } else if (char === '\\') {
        isEscaped = true
      } else if (char === '"') {
        inString = false
      }
      continue
    }

    if (char === '"') {
      inString = true
      chunks.push(char)
      continue
    }

    if (char === '-' || isDigit(char)) {
      let cursor = index + 1
      while (cursor < rawText.length && isNumberTokenChar(rawText[cursor])) {
        cursor += 1
      }

      const token = rawText.slice(index, cursor)
      chunks.push(shouldPreserveInteger(token) ? `"${token}"` : token)
      index = cursor - 1
      continue
    }

    chunks.push(char)
  }

  return chunks.join('')
}

function shouldPreserveInteger(token: string) {
  if (!/^-?\d+$/.test(token)) {
    return false
  }

  const normalized = token.startsWith('-') ? token.slice(1) : token
  if (normalized.length < 16) {
    return false
  }

  const value = BigInt(token)
  return value > BigInt(Number.MAX_SAFE_INTEGER) || value < BigInt(Number.MIN_SAFE_INTEGER)
}

function isDigit(char: string) {
  return char >= '0' && char <= '9'
}

function isNumberTokenChar(char: string) {
  return isDigit(char) || char === 'e' || char === 'E' || char === '+' || char === '-' || char === '.'
}

function buildQuery(params: Record<string, unknown>) {
  const search = new URLSearchParams()
  for (const key in params) {
    if (!Object.prototype.hasOwnProperty.call(params, key)) continue
    const value = params[key]
    if (value === undefined || value === null || value === '') continue
    search.set(key, String(value))
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

function mapArray<T, U>(items: readonly T[] | null | undefined, mapper: (item: T) => U): U[] {
  return (items ?? []).map(mapper)
}

function toProductWriteBody(payload: ProductWritePayload) {
  return {
    code: payload.code,
    name: payload.name,
    category_id: payload.categoryId,
    unit_id: payload.unitId,
    sale_price: payload.salePrice,
    purchase_price: payload.purchasePrice,
    price_levels: mapArray(payload.priceLevels, (item) => ({
      level_id: item.levelId,
      price: item.price,
    })),
    supplier_relations: mapArray(payload.supplierRelations, (item) => ({
      product_id: item.productId,
      supplier_id: item.supplierId,
      is_default: item.isDefault ?? false,
      purchase_priority: item.purchasePriority ?? null,
      last_purchase_price: item.lastPurchasePrice ?? null,
      notes: item.notes ?? null,
    })),
    stock: payload.stock,
    safe_stock: payload.safeStock,
    status: payload.status,
  }
}

function toSaleOrderCreateBody(payload: SaleOrderCreatePayload) {
  return {
    customer_id: payload.customerId ?? null,
    customer_name: payload.customerName ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      quantity: item.quantity,
      unit_price: item.unitPrice,
    })),
    notes: payload.notes ?? null,
    discount_amount: payload.discountAmount ?? null,
  }
}

function toSaleOrderDraftBody(payload: SaleOrderUpdateDraftPayload) {
  return {
    discount_amount: payload.discountAmount ?? null,
    notes: payload.notes ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      quantity: item.quantity,
      unit_price: item.unitPrice,
    })),
  }
}

function toPurchaseOrderBody(payload: PurchaseOrderWritePayload) {
  return {
    supplier_id: payload.supplierId ?? null,
    supplier_name: payload.supplierName ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      product_code: item.productCode ?? null,
      product_name: item.productName ?? null,
      quantity: item.quantity,
      unit_cost: item.unitCost,
    })),
    notes: payload.notes ?? null,
    status: payload.status ?? null,
  }
}

function toPurchaseReceiptBody(payload: PurchaseReceiptWritePayload) {
  return {
    purchase_order_id: payload.purchaseOrderId ?? null,
    supplier_id: payload.supplierId ?? null,
    supplier_name: payload.supplierName ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      product_code: item.productCode ?? null,
      product_name: item.productName ?? null,
      quantity: item.quantity,
      unit_cost: item.unitCost,
    })),
    notes: payload.notes ?? null,
  }
}

function toAccountWriteBody(payload: AccountWritePayload) {
  return {
    code: payload.code,
    name: payload.name,
    type: payload.type,
    balance: payload.balance ?? null,
    is_default: payload.isDefault ?? null,
    status: payload.status ?? null,
    sort_order: payload.sortOrder ?? null,
    notes: payload.notes ?? null,
  }
}

function toAccountTransferBody(payload: AccountTransferPayload) {
  return {
    from_account_id: payload.fromAccountId,
    to_account_id: payload.toAccountId,
    amount: payload.amount,
    fee: payload.fee ?? null,
    notes: payload.notes ?? null,
  }
}

function toContactWriteBody(payload: ContactWritePayload) {
  return {
    partner_id: payload.partnerId,
    name: payload.name,
    phone: payload.phone ?? null,
    title: payload.title ?? null,
    is_primary: payload.isPrimary ?? null,
  }
}

function toPurchaseReturnBody(payload: PurchaseReturnWritePayload) {
  return {
    purchase_order_id: payload.purchaseOrderId ?? null,
    supplier_id: payload.supplierId ?? null,
    supplier_name: payload.supplierName ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      product_code: item.productCode ?? null,
      product_name: item.productName ?? null,
      quantity: item.quantity,
      unit_cost: item.unitCost ?? null,
    })),
    notes: payload.notes ?? null,
  }
}

function toSalesReturnBody(payload: SalesReturnCreatePayload) {
  return {
    original_order_id: payload.originalOrderId ?? null,
    customer_id: payload.customerId ?? null,
    customer_name: payload.customerName ?? null,
    items: mapArray(payload.items, (item) => ({
      product_id: item.productId ?? null,
      quantity: item.quantity,
      unit_price: item.unitPrice ?? null,
    })),
    notes: payload.notes ?? null,
  }
}

function toPayOrderBody(payload: PayOrderCreatePayload) {
  return {
    supplier_id: payload.supplierId ?? null,
    supplier_name: payload.supplierName ?? null,
    amount: payload.amount,
    method: payload.method,
    reference_no: payload.referenceNo ?? null,
    notes: payload.notes ?? null,
    account_id: payload.accountId ?? null,
    status: payload.status ?? null,
  }
}

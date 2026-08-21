export type StoreRole = 'OWNER' | 'MANAGER' | 'SALES' | 'PURCHASING' | 'WAREHOUSE' | 'FINANCE' | 'ASSISTANT'

export type Permission =
  | 'dashboard:view'
  | 'sales:view'
  | 'sales:write'
  | 'purchase:view'
  | 'purchase:write'
  | 'archives:view'
  | 'archives:write'
  | 'inventory:view'
  | 'inventory:write'
  | 'finance:view'
  | 'finance:write'
  | 'reports:view'
  | 'agent:view'
  | 'agent:write'
  | 'database:manage'
  | 'settings:manage'
  | 'users:manage'

export interface StoreMember {
  id: string
  name: string
  role: StoreRole
  phone: string
  storeId: string
  storeName: string
  status: 0 | 1
  title: string
}

export const roleLabels: Record<StoreRole, string> = {
  OWNER: '店长（总）',
  MANAGER: '店长助理',
  SALES: '销售员工',
  PURCHASING: '采购员工',
  WAREHOUSE: '仓库员工',
  FINANCE: '财务员工',
  ASSISTANT: 'AI/只读助理',
}

export const roleDescriptions: Record<StoreRole, string> = {
  OWNER: '拥有店铺全部业务、数据库、用户和权限管理能力。',
  MANAGER: '可管理日常业务和报表，但不能修改数据库连接和超级权限。',
  SALES: '负责销售开单、客户、收款和销售相关查询。',
  PURCHASING: '负责采购开单、供应商、采购收货和付款申请。',
  WAREHOUSE: '负责库存流水、库存调整、盘点和低库存处理。',
  FINANCE: '负责资金流水、账户、收付款、现金流和对账报表。',
  ASSISTANT: '可使用 AI 助手和只读查看授权数据。',
}

export const rolePermissions: Record<StoreRole, Permission[]> = {
  OWNER: [
    'dashboard:view',
    'sales:view',
    'sales:write',
    'purchase:view',
    'purchase:write',
    'archives:view',
    'archives:write',
    'inventory:view',
    'inventory:write',
    'finance:view',
    'finance:write',
    'reports:view',
    'agent:view',
    'agent:write',
    'database:manage',
    'settings:manage',
    'users:manage',
  ],
  MANAGER: [
    'dashboard:view',
    'sales:view',
    'sales:write',
    'purchase:view',
    'purchase:write',
    'archives:view',
    'archives:write',
    'inventory:view',
    'inventory:write',
    'finance:view',
    'reports:view',
    'agent:view',
    'agent:write',
    'users:manage',
  ],
  SALES: ['dashboard:view', 'sales:view', 'sales:write', 'archives:view', 'finance:view', 'agent:view'],
  PURCHASING: ['dashboard:view', 'purchase:view', 'purchase:write', 'archives:view', 'finance:view', 'agent:view'],
  WAREHOUSE: ['dashboard:view', 'archives:view', 'inventory:view', 'inventory:write', 'agent:view'],
  FINANCE: ['dashboard:view', 'finance:view', 'finance:write', 'reports:view', 'sales:view', 'purchase:view', 'agent:view'],
  ASSISTANT: ['dashboard:view', 'reports:view', 'agent:view'],
}

export const rolePermissionSets: Record<StoreRole, ReadonlySet<Permission>> = {
  OWNER: new Set(rolePermissions.OWNER),
  MANAGER: new Set(rolePermissions.MANAGER),
  SALES: new Set(rolePermissions.SALES),
  PURCHASING: new Set(rolePermissions.PURCHASING),
  WAREHOUSE: new Set(rolePermissions.WAREHOUSE),
  FINANCE: new Set(rolePermissions.FINANCE),
  ASSISTANT: new Set(rolePermissions.ASSISTANT),
}

export function canAccess(role: StoreRole, required?: readonly Permission[]) {
  if (!required || required.length === 0) return true
  const granted = rolePermissionSets[role]
  return required.every((permission) => granted.has(permission))
}

export const demoMembers: StoreMember[] = [
  { id: 'u-owner', name: '老板', role: 'OWNER', phone: '13800000001', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '总店店长' },
  { id: 'u-manager', name: '店助小周', role: 'MANAGER', phone: '13800000002', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '店长助理' },
  { id: 'u-sales', name: '销售小林', role: 'SALES', phone: '13800000003', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '销售开单' },
  { id: 'u-purchasing', name: '采购小许', role: 'PURCHASING', phone: '13800000004', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '采购补货' },
  { id: 'u-warehouse', name: '仓管阿陈', role: 'WAREHOUSE', phone: '13800000005', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '库存盘点' },
  { id: 'u-finance', name: '周会计', role: 'FINANCE', phone: '13800000006', storeId: 'store-main', storeName: '智慧记总店', status: 1, title: '资金对账' },
  { id: 'u-assistant', name: '只读助理', role: 'ASSISTANT', phone: '13800000007', storeId: 'store-main', storeName: '智慧记总店', status: 0, title: '停用账号' },
]

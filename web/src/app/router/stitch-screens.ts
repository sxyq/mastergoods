import type { Permission } from '@/entities/auth/roles'

export interface StitchScreen {
  order: string
  id: string
  title: string
  route: string
  module: string
  permission: Permission[]
  imagePath: string
  htmlPath: string
  source: 'mcp-desktop' | 'pc-planned' | 'local-mobile-reference'
  width?: number
  height?: number
  priority?: 'primary' | 'planned' | 'reference'
  permissionMode?: 'all' | 'any'
}

const mobileReferenceRoot = '/stitch_exports/visual-design_system_framework_14840154594131085259'
const pcMcpRoot = '/stitch_exports/zhihuiji_web_pc_admin_mcp_17989845462303116064'

export const pcDesktopScreens: StitchScreen[] = [
  pcScreen('05', 'fd733d8a7ada48cea2f3f567417ce9e2', '经营首页 Dashboard', '/dashboard', '首页', ['dashboard:view'], 2560, 2048),
  plannedPcScreen('P01', 'documents-center', '单据中心', '/documents', '单据', ['sales:view', 'purchase:view', 'inventory:view', 'finance:view'], mobileImage('10', '851bba4950bc4f1385ade2cce0668d2f'), 'any'),
  pcScreen('10', '778c9991ab44444d978da4b2a28bd2a3', '销售单列表', '/documents/sales', '销售', ['sales:view'], 2560, 2048),
  pcScreen('09', '6d40c074a1284479a453f5a32a603618', '销售单新建/编辑', '/documents/sales/edit', '销售', ['sales:write'], 2560, 2048),
  pcScreen('08', 'a3f41bc2a6e5494f8538d16bb739d8e4', '销售单详情', '/documents/sales/detail', '销售', ['sales:view'], 2560, 2048),
  plannedPcScreen('P02', 'sales-payment', '销售收款', '/documents/sales/payment', '销售', ['sales:write', 'finance:write'], mobileImage('13', '3f9b69dd02ad4f6cb092ed93b91f77af'), 'any'),
  plannedPcScreen('P03', 'sales-returns', '销售退货', '/documents/sales-returns', '销售', ['sales:view'], mobileImage('04', 'c9feeec36d6742f8adf33a5c8ee2168b')),
  pcScreen('06', '595ffe36f46b4d478103fd4b63280706', '采购单列表', '/documents/purchases', '采购', ['purchase:view'], 2560, 2048),
  pcScreen('04', 'debb39738a0d4034affe3dbb4dd048de', '采购单新建/编辑', '/documents/purchases/edit', '采购', ['purchase:write'], 2560, 2048),
  pcScreen('01', '73ac0e15f5df49249ac2197064adeb46', '采购单详情', '/documents/purchases/detail', '采购', ['purchase:view'], 2560, 2352),
  plannedPcScreen('P04', 'purchase-receipts', '采购入库', '/documents/purchase-receipts', '采购', ['purchase:write', 'inventory:write'], mobileImage('18', '8c0d2379aca145efbc7997554ea63c36'), 'any'),
  plannedPcScreen('P05', 'purchase-returns', '采购退货', '/documents/purchase-returns', '采购', ['purchase:view'], mobileImage('25', '546176a5ad574e5282033dee6b8148d4')),
  plannedPcScreen('P06', 'pay-order-detail', '付款单详情', '/documents/pay-orders/detail', '财务', ['finance:view'], mobileImage('05', '624955bd91ed45fa9d4093c2fe7259fa')),
  pcScreen('07', 'a5aaaa8f7f04438d84d1fdc749d6a5cb', '商品列表', '/archives/products', '档案', ['archives:view'], 2560, 2048),
  plannedPcScreen('P07', 'product-edit', '商品编辑', '/archives/products/edit', '档案', ['archives:write'], mobileImage('11', '71bd8f9c60234565aadd7fd505d9bc16')),
  plannedPcScreen('P08', 'suppliers', '供应商档案', '/archives/suppliers', '档案', ['purchase:view'], mobileImage('02', '670a28679b57420d8c75e670f964b58c')),
  plannedPcScreen('P09', 'customers', '客户档案', '/archives/customers', '档案', ['sales:view'], mobileImage('03', 'd750377ad8d04dbfb8e502c0092798fc')),
  plannedPcScreen('P10', 'inventory-adjust', '库存调整', '/inventory/adjust', '库存', ['inventory:write'], mobileImage('12', '13620e8ea5ec47a08a93ee4ec4c6c331')),
  plannedPcScreen('P11', 'inventory-ledger', '商品库存流水', '/inventory/product-ledger', '库存', ['inventory:view'], mobileImage('24', 'd97470c943fa4c79980bd71c9e412727')),
  plannedPcScreen('P12', 'inventory-snapshots', '库存盘点', '/inventory/snapshots', '库存', ['inventory:view'], mobileImage('31', 'a952abd4ba2a411aa3f5372b44ee932b')),
  plannedPcScreen('P13', 'finance-record-detail', '资金流水详情', '/finance/records/detail', '财务', ['finance:view'], mobileImage('16', 'b70cadecc87e49d583075e0b7a71b38b')),
  plannedPcScreen('P14', 'daily-expense', '日常支出', '/finance/daily-expense', '财务', ['finance:write'], mobileImage('29', '5119f452001d4b5794457c2d99bce892')),
  plannedPcScreen('P15', 'reports', '经营报表', '/reports', '报表', ['reports:view'], mobileImage('28', '32d2d1d993a84090b927b119cceb6675')),
  plannedPcScreen('P16', 'agent', 'AI 智能助手', '/agent', 'AI', ['agent:view'], mobileImage('32', 'bb4cfaeb86aa4862ba26a7eca264b4e6')),
  plannedPcScreen('P17', 'settings', '系统设置', '/settings', '系统', ['users:manage', 'database:manage', 'settings:manage'], mobileImage('01', '67429855c01c456d984034f3cb0b8ec4'), 'any'),
  pcScreen('02', 'e8d81fc900d54197ba485cb076d1205c', '智慧记 Web PC 管理端产品规划', '/planning', '系统', ['settings:manage'], 0, 0),
]

export const mobileReferenceScreens: StitchScreen[] = [
  mobileScreen('01', '67429855c01c456d984034f3cb0b8ec4', '系统设置', '/references/mobile/settings', '移动参考', ['settings:manage']),
  mobileScreen('02', '670a28679b57420d8c75e670f964b58c', '供应商档案', '/references/mobile/archives/suppliers', '移动参考', ['purchase:view']),
  mobileScreen('03', 'd750377ad8d04dbfb8e502c0092798fc', '客户档案', '/references/mobile/archives/customers', '移动参考', ['sales:view']),
  mobileScreen('04', 'c9feeec36d6742f8adf33a5c8ee2168b', '销售退货', '/references/mobile/documents/sales-returns', '移动参考', ['sales:view']),
  mobileScreen('05', '624955bd91ed45fa9d4093c2fe7259fa', '付款单详情', '/references/mobile/documents/pay-orders/detail', '移动参考', ['finance:view']),
  mobileScreen('06', '790e9c9b67f74e29a312d5f9f333873c', 'AI 助手思考中', '/references/mobile/agent/thinking', '移动参考', ['agent:view']),
  mobileScreen('07', 'faf71221e71e4b43a37192508eecfe0d', '商品列表', '/references/mobile/archives/products', '移动参考', ['archives:view']),
  mobileScreen('08', '018b7e292a0c488fb689a5d279dafb6b', '经营报表', '/references/mobile/reports', '移动参考', ['reports:view']),
  mobileScreen('09', '4664bc10c2db4ff7beecb0cb710f5c51', 'AI 深度思考', '/references/mobile/agent/deep-thinking', '移动参考', ['agent:view']),
  mobileScreen('10', '851bba4950bc4f1385ade2cce0668d2f', '单据中心', '/references/mobile/documents', '移动参考', ['sales:view', 'purchase:view']),
  mobileScreen('11', '71bd8f9c60234565aadd7fd505d9bc16', '商品编辑', '/references/mobile/archives/products/edit', '移动参考', ['archives:write']),
  mobileScreen('12', '13620e8ea5ec47a08a93ee4ec4c6c331', '库存调整', '/references/mobile/inventory/adjust', '移动参考', ['inventory:write']),
  mobileScreen('13', '3f9b69dd02ad4f6cb092ed93b91f77af', '销售收款', '/references/mobile/documents/sales/payment', '移动参考', ['sales:write', 'finance:write']),
  mobileScreen('14', '0f8c2bf610ac442f93e70547fcd2539e', '采购开单', '/references/mobile/documents/purchases/edit', '移动参考', ['purchase:write']),
  mobileScreen('15', '11484f8c3688487085ddd485eaca5daa', '首页经营总览', '/references/mobile/dashboard', '移动参考', ['dashboard:view']),
  mobileScreen('16', 'b70cadecc87e49d583075e0b7a71b38b', '资金流水详情', '/references/mobile/finance/records/detail', '移动参考', ['finance:view']),
  mobileScreen('17', '20cc45c3ab5940ff81061dc21851f0c0', '供应商对账', '/references/mobile/archives/suppliers/statement-light', '移动参考', ['finance:view']),
  mobileScreen('18', '8c0d2379aca145efbc7997554ea63c36', '采购入库', '/references/mobile/documents/purchase-receipts', '移动参考', ['purchase:write', 'inventory:write']),
  mobileScreen('19', '124304d91cd44e088d3227e90f35d1ae', '商品库存流水', '/references/mobile/inventory/product-ledger-light', '移动参考', ['inventory:view']),
  mobileScreen('20', '2f7065105f7e4b519a479d6d8d7a60b3', '销售单详情', '/references/mobile/documents/sales/detail-light', '移动参考', ['sales:view']),
  mobileScreen('21', '5aafbb8f938e42fe9021390d45c30d42', '销售单详情极光版', '/references/mobile/documents/sales/detail', '移动参考', ['sales:view']),
  mobileScreen('22', '6565096a23a94309b0d1e37126ed35b1', '资金流水详情极光版', '/references/mobile/finance/records/detail-aurora', '移动参考', ['finance:view']),
  mobileScreen('23', '151d6ada46844fc385e54f5f4e597104', '供应商对账极光版', '/references/mobile/archives/suppliers/statement', '移动参考', ['finance:view']),
  mobileScreen('24', 'd97470c943fa4c79980bd71c9e412727', '商品库存流水极光版', '/references/mobile/inventory/product-ledger', '移动参考', ['inventory:view']),
  mobileScreen('25', '546176a5ad574e5282033dee6b8148d4', '采购退货', '/references/mobile/documents/purchase-returns', '移动参考', ['purchase:view']),
  mobileScreen('26', '7e06d3383247490c9dd95123f62f96cf', '供应商往来详情', '/references/mobile/archives/suppliers/current-account', '移动参考', ['finance:view']),
  mobileScreen('27', 'da13b081b480497791f2cefa3d030214', '采购退货动态交互', '/references/mobile/documents/purchase-returns/interactive', '移动参考', ['purchase:write']),
  mobileScreen('28', '32d2d1d993a84090b927b119cceb6675', '经营报表亮色版', '/references/mobile/reports/light', '移动参考', ['reports:view']),
  mobileScreen('29', '5119f452001d4b5794457c2d99bce892', '日常支出', '/references/mobile/finance/daily-expense', '移动参考', ['finance:write']),
  mobileScreen('30', 'a194c0a633854700b6603964da79caca', '首页经营总览亮色版', '/references/mobile/dashboard/light', '移动参考', ['dashboard:view']),
  mobileScreen('31', 'a952abd4ba2a411aa3f5372b44ee932b', '库存盘点', '/references/mobile/inventory/snapshots', '移动参考', ['inventory:view']),
  mobileScreen('32', 'bb4cfaeb86aa4862ba26a7eca264b4e6', 'AI 智能助手', '/references/mobile/agent', '移动参考', ['agent:view']),
]

export const stitchScreens: StitchScreen[] = [...pcDesktopScreens, ...mobileReferenceScreens]

function pcScreen(
  order: string,
  id: string,
  title: string,
  route: string,
  module: string,
  permission: Permission[],
  width: number,
  height: number,
): StitchScreen {
  return {
    order,
    id,
    title,
    route,
    module,
    permission,
    source: 'mcp-desktop',
    priority: 'primary',
    width,
    height,
    imagePath: width > 0 ? `${pcMcpRoot}/images/${order}_${id}.png` : '',
    htmlPath: `${pcMcpRoot}/html/${order}_${id}.html`,
  }
}

function plannedPcScreen(
  order: string,
  id: string,
  title: string,
  route: string,
  module: string,
  permission: Permission[],
  referenceImagePath: string,
  permissionMode: 'all' | 'any' = 'all',
): StitchScreen {
  return {
    order,
    id,
    title,
    route,
    module,
    permission,
    source: 'pc-planned',
    priority: 'planned',
    permissionMode,
    imagePath: referenceImagePath,
    htmlPath: '',
  }
}

function mobileScreen(order: string, id: string, title: string, route: string, module: string, permission: Permission[]): StitchScreen {
  return {
    order,
    id,
    title,
    route,
    module,
    permission,
    source: 'local-mobile-reference',
    priority: 'reference',
    imagePath: `${mobileReferenceRoot}/images/${order}_${id}.png`,
    htmlPath: `${mobileReferenceRoot}/html/${order}_${id}.html`,
  }
}

function mobileImage(order: string, id: string) {
  return `${mobileReferenceRoot}/images/${order}_${id}.png`
}

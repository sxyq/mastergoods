import type { StitchScreen } from '@/app/router/stitch-screens'
import { contractsForRoute, type ApiContract } from '@/shared/api/contracts'

export interface PageMetric {
  label: string
  value: string
  detail: string
}

export interface PageTable {
  columns: string[]
  rows: string[][]
}

export interface PageFormSection {
  title: string
  fields: string[]
}

export interface PageSummaryItem {
  label: string
  value: string
}

export interface PageModel {
  title: string
  description: string
  primaryAction: string
  secondaryActions: string[]
  statusTabs: string[]
  metrics: PageMetric[]
  filters: string[]
  table: PageTable
  formSections: PageFormSection[]
  summary: PageSummaryItem[]
  contracts: ApiContract[]
  databaseTables: string[]
}

const baseMetrics: PageMetric[] = [
  { label: '今日处理', value: '126', detail: '对齐后端业务单据统计' },
  { label: '待跟进', value: '18', detail: '需要员工角色处理' },
  { label: '异常预警', value: '5', detail: '库存、资金或同步异常' },
]

export function buildPageModel(screen: StitchScreen): PageModel {
  const route = screen.route
  const contracts = contractsForRoute(route)
  const databaseTables = collectDatabaseTables(contracts)

  if (route.includes('/sales')) {
    return {
      title: screen.title,
      description: '销售开单、销售详情、销售收款和销售退货统一进入 PC 单据工作台。',
      primaryAction: route.includes('detail') ? '审核/确认销售单' : '新建销售单',
      secondaryActions: ['审核', '编辑', '收款', '打印', '导出单据'],
      statusTabs: ['全部', '待审核', '待出库', '待结算', '已完成', '已作废'],
      metrics: [
        { label: '今日销售额', value: '¥24,580', detail: '+12.5%' },
        { label: '待收款', value: '¥5,230', detail: '3 笔待催收' },
        { label: '销售退货', value: '2 单', detail: '需复核库存' },
      ],
      filters: ['单据编号', '客户名称', '创建日期', '收款状态', '出库状态'],
      table: {
        columns: ['单据编号', '客户名称', '销售金额', '已付金额', '出库状态', '收款状态', '操作'],
        rows: [
          ['XS-20231024-001', '深圳市创科未来实业有限公司', '¥45,600.00', '¥45,600.00', '已出库', '已结清', '查看 / 打印'],
          ['XS-20231024-002', '广州汇智商贸行', '¥12,850.00', '¥0.00', '未出库', '未收款', '审核 / 编辑'],
          ['XS-20231024-003', '北京东方建材批发部', '¥89,000.00', '¥30,000.00', '部分出库', '部分收款', '收款'],
        ],
      },
      formSections: [
        { title: '客户与日期', fields: ['客户名称', '销售日期', '经办人', '备注'] },
        { title: '商品明细', fields: ['商品名称/编码', '规格', '数量', '单价', '折扣', '合计'] },
        { title: '收款与出库', fields: ['收款账户', '本次收款', '出库仓库', '物流信息'] },
      ],
      summary: [
        { label: '共 128 条记录', value: '当前显示 1-3 条' },
        { label: '待审核', value: '12' },
        { label: '待结算金额', value: '¥71,850.00' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/purchase') || route.includes('/purchases')) {
    return {
      title: screen.title,
      description: '采购开单、采购入库、供应商付款与采购退货统一进入采购工作台。',
      primaryAction: screen.route.includes('detail') ? '生成入库/付款' : '新建采购单',
      secondaryActions: ['导入外部单据', '暂存为草稿', '直接入库', '提交审批'],
      statusTabs: ['全部', '草稿', '待审批', '待入库', '部分入库', '待付款', '已完成'],
      metrics: [
        { label: '待入库', value: '9 单', detail: '采购员工处理' },
        { label: '本周采购', value: '¥38,920', detail: '环比 +8%' },
        { label: '待付款', value: '¥16,740', detail: '财务复核' },
      ],
      filters: ['采购单号', '供应商', '采购日期', '入库状态', '付款状态'],
      table: {
        columns: ['采购单号', '供应商', '商品数', '采购金额', '入库状态', '付款状态', '负责人'],
        rows: [
          ['PO-20260610-006', '华东供货商', '12', '¥12,800', '待入库', '未付款', '采购员工'],
          ['PO-20260609-011', '北区纸品', '6', '¥6,430', '部分入库', '部分付款', '仓库员工'],
          ['PO-20260608-004', '鑫源贸易', '18', '¥20,160', '已入库', '待付款', '财务员工'],
        ],
      },
      formSections: [
        { title: '基本信息', fields: ['供应商', '采购日期', '预计到货时间', '收货仓库', '采购员', '备注'] },
        { title: '商品明细', fields: ['商品名称/编码', '规格', '单位', '数量', '单价(含税)', '折扣', '总额'] },
        { title: '附加费用与附件', fields: ['运费', '装卸费', '其他费用', 'PDF/JPG/PNG 附件'] },
      ],
      summary: [
        { label: '商品总数', value: '60 件' },
        { label: '附加费用', value: '¥0.00' },
        { label: '本单应付总额', value: '¥109,750.00' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/archives')) {
    if (route.includes('/archives/customers')) {
      return {
        title: screen.title,
        description: '客户档案承接销售单、应收款与客户分组，是销售与财务联动的主数据入口。',
        primaryAction: '新增客户',
        secondaryActions: ['批量导入', '导出客户', '客户分组', '催收跟进'],
        statusTabs: ['全部', '启用', '停用', '待跟进', '最近更新'],
        metrics: [
          { label: '客户档案', value: '326', detail: '示例数据' },
          { label: '客户应收', value: '¥42,680', detail: '需要销售/财务跟进' },
          { label: '客户分组', value: '8 组', detail: '已做客户分层' },
        ],
        filters: ['客户名称/手机号', '客户分组', '客户等级', '状态', '更新时间'],
        table: {
          columns: ['客户名称', '手机号', '分组', '主联系人', '应收余额', '状态', '更新时间'],
          rows: [
            ['深圳创科未来', '13800138000', '重点客户', '张经理', '¥18,600', '启用', '今天 09:30'],
            ['广州汇智商贸', '13800138001', '普通客户', '李会计', '¥4,500', '启用', '昨天 18:20'],
          ],
        },
        formSections: [
          { title: '客户资料', fields: ['客户名称', '手机号', '等级', '客户分组', '主联系人'] },
          { title: '经营往来', fields: ['应收余额', '地址', '备注', '跟进记录'] },
        ],
        summary: [
          { label: '客户总数', value: '326' },
          { label: '应收余额', value: '¥42,680' },
          { label: '待跟进客户', value: '18' },
        ],
        contracts,
        databaseTables,
      }
    }

    if (route.includes('/archives/suppliers')) {
      return {
        title: screen.title,
        description: '供应商档案关联采购、付款和对账，是采购与财务联动的主数据入口。',
        primaryAction: '新增供应商',
        secondaryActions: ['批量导入', '导出供应商', '供应商分组', '对账跟进'],
        statusTabs: ['全部', '启用', '停用', '待跟进', '最近更新'],
        metrics: [
          { label: '供应商档案', value: '86', detail: '示例数据' },
          { label: '供应商应付', value: '¥31,240', detail: '需要采购/财务对账' },
          { label: '供应商分组', value: '5 组', detail: '按渠道/品类维护' },
        ],
        filters: ['供应商名称/手机号', '供应商分组', '状态', '更新时间', '应付金额'],
        table: {
          columns: ['供应商名称', '手机号', '分组', '主联系人', '应付余额', '状态', '更新时间'],
          rows: [
            ['华东供货商', '13800138100', '核心供货', '王经理', '¥12,800', '启用', '今天 10:10'],
            ['北区纸品', '13800138101', '办公耗材', '刘主管', '¥6,430', '启用', '昨天 17:50'],
          ],
        },
        formSections: [
          { title: '供应商资料', fields: ['供应商名称', '手机号', '供应商分组', '主联系人', '地址'] },
          { title: '经营往来', fields: ['应付余额', '备注', '对账周期', '结算偏好'] },
        ],
        summary: [
          { label: '供应商总数', value: '86' },
          { label: '应付余额', value: '¥31,240' },
          { label: '待跟进供应商', value: '12' },
        ],
        contracts,
        databaseTables,
      }
    }

    return {
      title: screen.title,
      description: '商品、客户、供应商和价格资料作为所有销售采购单据的主数据来源。',
      primaryAction: '新增档案',
      secondaryActions: ['批量导入', '导出', '分类维护', '停用/启用'],
      statusTabs: ['全部', '启用', '停用', '低库存', '最近更新'],
      metrics: [
        { label: '商品档案', value: '1,284', detail: '含库存预警' },
        { label: '客户', value: '326', detail: '52 个活跃客户' },
        { label: '供应商', value: '86', detail: '12 个待对账' },
      ],
      filters: ['商品搜索', '全部分类', '办公用品', '数码电子', '食品生鲜', '状态'],
      table: {
        columns: ['图片', '商品编码', '商品名称', '规格型号', '单位', '零售价', '进货价', '当前库存', '操作'],
        rows: [
          ['有图', 'SP-001001', '得力 A4 打印纸', '70g 500张/包', '包', '¥25.00', '¥18.50', '350', '编辑 / 详情 / 流水'],
          ['有图', 'SP-001002', '罗技 M185 无线鼠标', '黑色 2.4G', '个', '¥69.00', '¥45.00', '5', '编辑 / 详情 / 流水'],
          ['无图', 'SP-002015', '农夫山泉 饮用天然水', '550ml*24瓶/箱', '箱', '¥35.00', '¥24.00', '120', '编辑 / 详情 / 流水'],
        ],
      },
      formSections: [
        { title: '基础资料', fields: ['商品编码', '商品名称', '分类', '规格型号', '单位', '条码'] },
        { title: '价格与库存', fields: ['零售价', '进货价', '当前库存', '库存预警线', '默认供应商'] },
      ],
      summary: [
        { label: '共 128 条数据', value: '10 条/页' },
        { label: '低库存商品', value: '12' },
        { label: '可售库存金额', value: '¥186,420.00' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/inventory')) {
    return {
      title: screen.title,
      description: '库存流水、库存调整、入库出库和盘点都需要记录来源单据与经办员工。',
      primaryAction: '发起库存处理',
      secondaryActions: ['库存盘点', '低库存筛选', '流水追踪', '导出库存表'],
      statusTabs: ['全部', '入库', '出库', '调整', '盘点', '低库存'],
      metrics: [
        { label: '低库存', value: '12 件', detail: '需要补货' },
        { label: '今日出库', value: '48 笔', detail: '销售确认产生' },
        { label: '今日入库', value: '21 笔', detail: '采购收货产生' },
      ],
      filters: ['商品', '仓库', '流水类型', '来源单据', '日期'],
      table: {
        columns: ['商品', '类型', '数量', '来源', '经办角色', '时间'],
        rows: [
          ['A4 打印纸', '销售出库', '-8 箱', 'SO-20260610-001', '销售员工', '10:14'],
          ['签字笔', '采购入库', '+240 支', 'PO-20260609-011', '仓库员工', '09:32'],
          ['票据夹', '库存调整', '+3 个', '盘点差异', '店长助理', '昨天'],
        ],
      },
      formSections: [
        { title: '库存处理', fields: ['商品', '仓库', '处理类型', '调整数量', '来源单据', '备注'] },
        { title: '盘点结果', fields: ['账面库存', '实盘库存', '差异数量', '经办员工'] },
      ],
      summary: [
        { label: '今日流水', value: '69 笔' },
        { label: '库存异常', value: '5' },
        { label: '待盘点', value: '8 类商品' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/finance')) {
    return {
      title: screen.title,
      description: '资金流水、账户、收付款和单据关联只允许财务或店长处理写操作。',
      primaryAction: '登记资金流水',
      secondaryActions: ['账户切换', '收付款核销', '供应商对账', '现金流导出'],
      statusTabs: ['全部', '收入', '支出', '待核销', '已核销', '异常'],
      metrics: [
        { label: '账户余额', value: '¥168,420', detail: '3 个资金账户' },
        { label: '待付款', value: '¥16,740', detail: '采购相关' },
        { label: '待收款', value: '¥5,230', detail: '销售相关' },
      ],
      filters: ['流水号', '账户', '收支类型', '关联单据', '日期'],
      table: {
        columns: ['流水号', '方向', '分类', '往来方', '金额', '收支方式', '时间'],
        rows: [
          ['FR-20260610-012', '收入', '销售收款', '深圳创科未来', '¥4,280', '工商银行', '今天 10:20'],
          ['FR-20260610-009', '支出', '日常支出', '门店运营', '¥380', '现金账户', '今天 09:45'],
          ['FR-20260609-021', '支出', '采购付款', '北区纸品', '¥6,430', '支付宝', '昨天 17:10'],
        ],
      },
      formSections: [
        { title: '资金信息', fields: ['账户', '收支方向', '金额', '业务类型', '关联单据'] },
        { title: '核销信息', fields: ['客户/供应商', '应收应付', '本次核销', '经办人'] },
      ],
      summary: [
        { label: '账户余额', value: '¥168,420' },
        { label: '待收款', value: '¥5,230' },
        { label: '待付款', value: '¥16,740' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/pay-orders')) {
    return {
      title: screen.title,
      description: '付款单详情联动采购、账户和资金流水，适合财务复核与单据追踪。',
      primaryAction: '查看付款链路',
      secondaryActions: ['账户切换', '供应商对账', '关联采购单', '导出流水'],
      statusTabs: ['全部', '收入', '支出', '待核销', '已核销', '异常'],
      metrics: [
        { label: '付款相关流水', value: '28 笔', detail: '含采购付款与退款' },
        { label: '待核销', value: '¥8,230', detail: '需要财务确认' },
        { label: '账户余额', value: '¥168,420', detail: '示例数据' },
      ],
      filters: ['流水号', '方向', '分类', '往来方', '日期'],
      table: {
        columns: ['流水号', '方向', '分类', '往来方', '金额', '收支方式', '时间'],
        rows: [
          ['FR-20260610-012', '支出', '采购付款', '华东供货商', '¥4,280', '工商银行', '今天 10:20'],
          ['FR-20260610-009', '支出', '日常支出', '门店运营', '¥380', '现金账户', '今天 09:45'],
        ],
      },
      formSections: [
        { title: '付款信息', fields: ['供应商', '付款账户', '金额', '关联采购单', '备注'] },
        { title: '核销信息', fields: ['应付金额', '已付金额', '本次核销', '经办人'] },
      ],
      summary: [
        { label: '付款相关流水', value: '28 笔' },
        { label: '待核销金额', value: '¥8,230' },
        { label: '关联采购单', value: '12 单' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/agent')) {
    return {
      title: screen.title,
      description: 'AI 助手只能读取当前店铺授权数据，写入建议草稿和运行审计。',
      primaryAction: '发起 AI 查询',
      secondaryActions: ['选择数据范围', '查看运行审计', '保存草稿', '导出结论'],
      statusTabs: ['全部', '思考中', '已完成', '待确认', '已审计'],
      metrics: [
        { label: '本周会话', value: '42', detail: '含经营分析' },
        { label: '待确认建议', value: '7', detail: '需要人工审核' },
        { label: '审计记录', value: '128', detail: '可追踪来源' },
      ],
      filters: ['问题类型', '数据范围', '创建人', '运行状态'],
      table: {
        columns: ['会话', '问题', '数据范围', '状态', '发起人', '时间'],
        rows: [
          ['AI-20260610-008', '低库存补货建议', '库存/采购', '已完成', '店长（总）', '11:05'],
          ['AI-20260610-006', '本周现金流风险', '财务', '思考中', '财务员工', '10:36'],
          ['AI-20260609-019', '销售利润异常', '销售/商品', '已审计', '店长助理', '昨天'],
        ],
      },
      formSections: [
        { title: '提问配置', fields: ['问题类型', '数据范围', '时间范围', '输出格式'] },
        { title: '审计追踪', fields: ['运行 ID', '事件数', '引用数据', '人工确认'] },
      ],
      summary: [
        { label: '可读取范围', value: '当前店铺授权数据' },
        { label: '待确认建议', value: '7' },
        { label: '审计记录', value: '128' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/reports')) {
    return {
      title: screen.title,
      description: '经营报表汇总销售、采购、库存和现金流，按店铺权限提供可视化经营分析。',
      primaryAction: '生成经营报表',
      secondaryActions: ['时间范围', '利润分析', '现金流', '导出报表'],
      statusTabs: ['全部', '销售', '采购', '库存', '现金流', '利润'],
      metrics: [
        { label: '本月销售', value: '¥328,600', detail: '同比 +18%' },
        { label: '毛利率', value: '31.2%', detail: '较上月 +2.1%' },
        { label: '库存周转', value: '18 天', detail: '健康区间' },
      ],
      filters: ['日期范围', '报表类型', '业务模块', '经办角色'],
      table: {
        columns: ['报表', '覆盖模块', '关键指标', '权限', '更新时间'],
        rows: [
          ['销售利润分析', '销售/商品', '销售额、毛利、退货', 'reports:view', '今天 10:20'],
          ['采购与应付', '采购/财务', '采购额、待付款', 'finance:view', '今天 09:40'],
          ['库存风险', '库存/采购', '低库存、周转天数', 'inventory:view', '昨天 18:00'],
        ],
      },
      formSections: [
        { title: '报表条件', fields: ['日期范围', '报表类型', '业务模块', '经办角色'] },
        { title: '输出设置', fields: ['图表维度', '导出格式', '权限范围'] },
      ],
      summary: [
        { label: '本月销售', value: '¥328,600' },
        { label: '毛利率', value: '31.2%' },
        { label: '库存周转', value: '18 天' },
      ],
      contracts,
      databaseTables,
    }
  }

  if (route.includes('/settings')) {
    return {
      title: screen.title,
      description: '系统设置聚合店铺资料、员工角色、数据库连接、旧库导入、同步健康和安全配置。',
      primaryAction: '检查系统状态',
      secondaryActions: ['角色权限', '数据库连接', '旧库导入', '同步健康'],
      statusTabs: ['全部', '正常', '待配置', '仅店长', '审计记录'],
      metrics: [
        { label: '员工角色', value: '7 类', detail: '总店长与员工权限' },
        { label: '同步实体', value: '28 类', detail: '与安卓端保持一致' },
        { label: '数据库状态', value: 'OK', detail: '支持健康检查' },
      ],
      filters: ['设置类型', '权限', '状态', '更新时间'],
      table: {
        columns: ['设置项', '接口', '数据表', '权限', '状态'],
        rows: [
          ['角色权限', 'owner_user_id（后端当前模型）', 'users / sessions', 'users:manage', '等待真实成员接口'],
          ['数据库健康', '/v2/sync/health', 'sync_cursors / import_jobs', 'database:manage', '已接真实接口'],
          ['旧库导入', '/v2/import-jobs/legacy-sqlite', 'import_jobs', 'database:manage', '已接真实接口'],
        ],
      },
      formSections: [
        { title: '店铺设置', fields: ['店铺名称', '店长账号', '员工角色', '权限模板'] },
        { title: '数据设置', fields: ['数据库健康', '旧库导入', '备份恢复', '同步状态'] },
      ],
      summary: [
        { label: '店长（总）', value: '1 人' },
        { label: '员工角色', value: '6 类' },
        { label: '数据库权限', value: '仅店长' },
      ],
      contracts,
      databaseTables,
    }
  }

  return {
    title: screen.title,
    description: '智慧记 PC 管理端需要围绕店铺数据、员工角色、单据流转和数据库连接统一建设。',
    primaryAction: '进入业务处理',
    secondaryActions: ['查看报表', '同步状态', '角色权限', '数据库健康检查'],
    statusTabs: ['全部', '待处理', '执行中', '已完成', '异常'],
    metrics: baseMetrics,
    filters: ['关键词', '状态', '角色', '日期'],
    table: {
      columns: ['模块', '核心数据', '主接口', '权限', '状态'],
      rows: [
        ['销售', '销售单/收款/退货', '/v2/sale-orders', 'sales:view/write', '已接入契约'],
        ['采购', '采购单/入库/付款', '/v2/purchase-orders', 'purchase:view/write', '已接入契约'],
        ['权限', 'owner / demo RBAC', '后端暂缺真实成员接口', 'users:manage', '前端规划已完成'],
      ],
    },
    formSections: [
      { title: '业务配置', fields: ['模块', '权限', '数据范围', '负责人'] },
    ],
    summary: [
      { label: '模块入口', value: '26 个 PC 页面' },
      { label: '接口契约', value: `${contracts.length} 个` },
      { label: '数据库表', value: `${databaseTables.length} 类` },
    ],
    contracts,
    databaseTables,
  }
}

function collectDatabaseTables(contracts: ApiContract[]): string[] {
  const tables: string[] = []
  const seen = new Set<string>()
  for (const contract of contracts) {
    for (const table of contract.tables) {
      if (seen.has(table)) continue
      seen.add(table)
      tables.push(table)
      if (tables.length === 10) {
        return tables
      }
    }
  }
  return tables
}

package com.zhihuiji.backend.application.service.v2.agent.component;

import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 给模型使用的工具说明和运行提示。
 *
 * <p>工具类名和数据库字段名适合代码，不适合直接指导模型理解用户话术。这里保留稳定的
 * 工具名，同时为每个工具补充一条日常说法，降低模型把一个问题拆错工具的概率。
 * 这些内容只发送给模型，不会作为用户可见的运行过程渲染。
 */
public final class AgentPromptCatalog {
    private static final String TOOL_SELECTION_GUIDANCE =
        "工具选择要克制：用户只问一个主题时，默认只选一个最直接的工具；只有用户明确要求同时、分别、放一起、对比或串起来，"
            + "或者一个工具明确无法覆盖问题时，才选多个。一个专门的汇总、全链路或概览工具能够覆盖整件事时，优先只选它。"
            + "工具描述里提到的相关数据不等于用户要查，不要顺手补查账户、付款、现金流、趋势、库存或报表。已有足够事实时停止调用。"
            + "首轮对单一主题最多发起一个工具调用；不要把一个工具的返回字段拆成多个邻近查询。"
            + "只有用户明确提出两个或以上独立数据来源时，才可以每个来源调用一个工具。\n";

    private static final String INVENTORY_RESTOCK_GUIDANCE =
        "用户同时说‘库存和补货’、‘库存现状和哪些要补’或同义表达时，这是两个明确的数据目标：必须分别调用 inventory_panorama_lookup 和 smart_restock_lookup；"
            + "先取得真实库存与补货结果，再由你自主判断是否需要调用 result_visualization。只问库存全貌时不要调用 smart_restock_lookup，只问补货建议时不要调用 inventory_panorama_lookup。\n";

    private static final Set<String> WRITE_WORDS = Set.of(
        "新增", "添加", "创建", "新建", "加一个", "建个", "记一笔", "录入", "登记", "开一单", "开一张", "开张",
        "保存"
    );

    private static final Map<String, String> USER_LANGUAGE_HINTS = Map.ofEntries(
        entry("account_balance_lookup", "我现在有几个账户，里面还剩多少钱"),
        entry("account_health_lookup", "最近资金账户有没有异常"),
        entry("account_transfer_lookup", "账户之间最近转过哪些钱"),
        entry("anomaly_alert_lookup", "最近生意有没有不对劲的地方，帮我扫一遍"),
        entry("cash_change_lookup", "最近的钱都是怎么进进出出的"),
        entry("cashflow_summary_lookup", "最近现金流怎么样，收入支出和结余算一下"),
        entry("cross_analysis_lookup", "销售、采购和库存放一起看看有什么关系"),
        entry("customer_profile_lookup", "客户整体情况怎么样，哪些人值得跟进"),
        entry("customer_receivable_lookup", "哪些客户还欠我钱，先催谁"),
        entry("data_export_tool", "销售数据能导出哪些，先给我看看"),
        entry("finance_record_lookup", "最近收入支出流水按类别理一下"),
        entry("generate_poster_prompt", "拿这个商品帮我想一段海报文案和画面描述；需要真实 product_id，没给编号时先查商品，查到后继续调用这个工具，不要直接自己编写"),
        entry("import_job_lookup", "之前的数据导入到哪一步了，有没有失败的"),
        entry("inventory_adjustment_lookup", "最近库存改过什么，盘盈盘亏也看看"),
        entry("inventory_ledger_lookup", "库存出入库流水和来源给我列一下"),
        entry("inventory_low_stock_lookup", "哪些商品快没货了"),
        entry("inventory_panorama_lookup", "库存、销量、周转和补货建议一起看看"),
        entry("inventory_snapshot_lookup", "之前的库存盘点和快照还有吗"),
        entry("partner_contact_lookup", "客户和供应商的联系人找一下"),
        entry("partner_group_lookup", "客户和供应商分组现在是什么情况"),
        entry("pay_order_lookup", "最近给供应商付了哪些款，状态怎么样"),
        entry("payment_lookup", "最近收款和付款记录理一下"),
        entry("product_catalog_lookup", "现在有哪些商品，各自库存和价格是多少"),
        entry("product_category_lookup", "商品分类怎么分的，每类有多少"),
        entry("product_price_level_lookup", "商品价格等级有哪些"),
        entry("product_supplier_relation_lookup", "这些商品从哪家供应商进，最近价多少；不指定商品时可以查当前账户全部供货关系"),
        entry("purchase_order_lookup", "最近采购了什么，货到了没有"),
        entry("purchase_receipt_lookup", "最近哪些采购货已经入库"),
        entry("purchase_return_lookup", "最近退给供应商的货有哪些"),
        entry("purchase_tracking_lookup", "把采购、入库和退货这条线串起来看看"),
        entry("receivable_payable_lookup", "客户欠我的和我欠供应商的分别算一下"),
        entry("report_query", "这个月生意怎么样，给我看个经营汇总"),
        entry("result_visualization", "已经拿到真实查询结果后，由你判断表格、图、统计卡、趋势或排行是否更容易让用户理解；不需要展示时不要调用"),
        entry("sale_order_lookup", "最近卖出去的单子、客户和收款情况看看；没有具体单号时也用这个"),
        entry("sales_full_chain_lookup", "有具体销售单号或客户名时，把这张销售单的收款和退货关联记录串起来看"),
        entry("sales_overview_lookup", "最近一周销售和回款整体怎么样"),
        entry("sales_return_lookup", "最近有哪些销售退货，状态给我看看"),
        entry("sales_trend_lookup", "最近一个月每天卖得怎么样，按天看趋势"),
        entry("smart_restock_lookup", "哪些东西该补货，按着急程度排一下"),
        entry("store_info_lookup", "当前门店信息和成员数量看一下"),
        entry("supplier_directory_lookup", "现有供应商都有哪些，编号和联系方式给我看一下"),
        entry("supplier_payable_lookup", "我还欠哪些供应商钱"),
        entry("supplier_statement_lookup", "帮我和供应商对一下账，把余额、采购、入库、退货和付款一起核对"),
        entry("sync_status_lookup", "数据同步现在正常吗"),
        entry("customer_directory_lookup", "现有客户都有哪些，编号和联系方式给我看一下"),
        entry("create_account_transfer", "把两个账户之间的钱转一下，先给我确认"),
        entry("create_customer", "帮我加一个客户，先把要保存的内容给我看看"),
        entry("create_finance_record", "记一笔收入或支出，先做成草稿"),
        entry("create_inventory_adjustment", "这个商品库存改几件，先做调整草稿"),
        entry("create_inventory_count_draft", "按实际盘点数量做个盘点草稿"),
        entry("create_pay_order", "给供应商记一笔付款，但先别直接付"),
        entry("create_product", "帮我加个商品，先生成草稿"),
        entry("create_purchase_order", "向供应商买点货，先做采购单给我确认"),
        entry("create_purchase_receipt", "这批货先做个入库草稿"),
        entry("create_purchase_return", "这批采购货退几件，先给我看退货草稿"),
        entry("create_sale_order", "给客户开一单，先别直接保存"),
        entry("create_sales_return", "这张销售单退几件，先做草稿"),
        entry("create_supplier", "帮我加个供应商，先看看要保存什么"),
        entry("image_generate", "生成一张图片或商品海报，先把生图草稿给我确认"),
        entry("media_upload_tool", "我有个文件，先生成上传意图，不要直接上传")
    );

    /**
     * Clarifies overlapping tools for providers that tend to over-call when a
     * broad registry is visible. This is guidance only; the model still emits
     * the native call and the server never selects a business tool by keyword.
     */
    private static final Map<String, String> TOOL_DISAMBIGUATION_HINTS = Map.ofEntries(
        entry("account_balance_lookup", "只查账户数量和当前余额；不要因为出现状态、异常或转账而顺手调用它"),
        entry("account_health_lookup", "已经包含账户余额、近期收支、转账与账户异常概览；账户健康问题优先只调用它，不要再补查账户余额、转账或经营异常"),
        entry("account_transfer_lookup", "只查账户之间的转账记录；不要因为用户提到余额或账户状态而补查其他账户工具"),
        entry("anomaly_alert_lookup", "只用于跨销售、库存和客户欠款的经营异常扫描；资金账户自身状态用 account_health_lookup，不要两者同时调用"),
        entry("cash_change_lookup", "只查资金变动明细；不要因为结果包含收支而再调用 finance_record_lookup、payment_lookup 或 cashflow_summary_lookup"),
        entry("cashflow_summary_lookup", "直接返回现金流汇总；不要为了补充明细再调用 cash_change_lookup、finance_record_lookup 或 payment_lookup"),
        entry("customer_profile_lookup", "已经包含客户余额、订单、收款和退货画像；用户问客户整体情况时优先只调用它，不要再调用客户目录或应收工具"),
        entry("inventory_low_stock_lookup", "只查低库存或快缺货商品；补货数量和紧急度用 smart_restock_lookup，库存全貌用 inventory_panorama_lookup，不要无条件同时调用"),
        entry("inventory_panorama_lookup", "查询库存现状、安全库存、销量和周转，并可附带参考补货量；用户同时问库存和补货时仍要和 smart_restock_lookup 一起调用，不要把本工具当成补货建议工具"),
        entry("smart_restock_lookup", "只查补货建议、建议数量和紧急度；只要用户问补货、该补多少或哪些要马上补，就必须调用本工具"),
        entry("product_catalog_lookup", "查询商品及其库存、价格和类别字段；只问分类时使用 product_category_lookup，不要因为类别字段存在而重复调用分类工具"),
        entry("product_supplier_relation_lookup", "直接查询商品和供应商的供货关系；product_id 是可选过滤条件，不需要先调用 product_catalog_lookup 来补商品列表"),
        entry("payment_lookup", "只查收款和付款记录；供应商付款单详情用 pay_order_lookup，现金变动或现金流汇总不要作为补充调用"),
        entry("purchase_tracking_lookup", "已经覆盖采购单、入库和退货的完整链路；用户明确要串联过程时优先只调用它，不要再拆调用组成工具"),
        entry("sales_full_chain_lookup", "只处理指定销售单或客户关键词的完整链路；用户没有提供具体对象、只问最近销售单列表时使用 sale_order_lookup，不要调用本工具"),
        entry("sales_overview_lookup", "直接返回近期销售和回款概览；只有用户明确要求按日趋势或详细收付款记录时才使用对应工具"),
        entry("supplier_statement_lookup", "已经覆盖供应商余额、采购、入库、退货和付款对账；对账问题优先只调用它，不要再调用组成工具"),
        entry("report_query", "直接返回经营汇总；不要为了回答经营汇总而把销售、库存、现金流和异常工具全部扫描一遍"),
        entry("receivable_payable_lookup", "直接汇总客户应收和供应商应付；用户只问单一方向时使用对应的客户或供应商工具")
    );

    private AgentPromptCatalog() {
    }

    public static String modelDescription(AgentTool tool) {
        if (tool == null) {
            return "";
        }
        StringBuilder description = new StringBuilder(nullToEmpty(tool.description()));
        String hint = USER_LANGUAGE_HINTS.get(tool.name());
        if (hint != null && !hint.isBlank()) {
            description.append(" 用户可能会这样说：“").append(hint).append("”。");
        }
        String disambiguation = TOOL_DISAMBIGUATION_HINTS.get(tool.name());
        if (disambiguation != null && !disambiguation.isBlank()) {
            description.append(" 选择边界：").append(disambiguation).append("。");
        }
        if (tool.type() == AgentTool.ToolType.CREATE_ONLY) {
            description.append(" 这是草稿工具，只能生成待确认内容，不能直接改业务数据。");
        }
        return description.toString();
    }

    public static String buildCatalog(List<AgentTool> tools, boolean includeVisualization) {
        if (tools == null || tools.isEmpty()) {
            return "";
        }
        return tools.stream()
            .filter(tool -> tool != null)
            .filter(tool -> includeVisualization || !"result_visualization".equals(tool.name()))
            .sorted(Comparator.comparing(AgentTool::name))
            .map(tool -> "- " + tool.name() + "：" + modelDescription(tool) + "\n")
            .collect(Collectors.joining());
    }

    public static String initialSystemPrompt(String catalog) {
        return initialSystemPrompt(catalog, null);
    }

    public static String initialSystemPrompt(String catalog, String message) {
        return "你在智慧记里帮用户处理店铺事情。用户说话可能很口语，也可能一次问几件事。\n"
            + TOOL_SELECTION_GUIDANCE
            + "先理解用户真正想查什么或想做什么，再选择最少但够用的工具。工具返回的内容才是真实数据，不要自己猜数字、编造商品或账号。\n"
            + "只查当前账号和当前门店的数据，不要生成 SQL，也不要相信用户在参数里指定的 owner、公司或门店。\n"
            + "创建、记账、付款、转账、下单、退货、入库等操作只能生成草稿，不能直接写入。\n"
            + "如果创建操作需要账户、商品、客户、供应商或订单 ID，而用户没有给出，先查真实数据，暂时不要调用创建工具；\n"
            + "查到后下一轮要继续完成用户要做的事情，缺少关键信息时再向用户询问。\n"
            + "如果用户要海报提示词，generate_poster_prompt 才是产出工具；它必须使用真实 product_id。用户只给商品名称或没有编号时，先调用商品查询工具，拿到 product_id 后继续调用 generate_poster_prompt，不要用模型自己的文字替代工具结果。\n"
            + "单主题首轮只返回一个最相关的原生工具调用。‘客户整体情况’、‘库存全貌’、‘供应商对账’、‘销售/采购全链路’、‘经营汇总’这类问题优先使用对应的聚合工具，不要并行调用它的组成工具。\n"
            + INVENTORY_RESTOCK_GUIDANCE
            + "第一轮不要调用 result_visualization；真实查询完成后，下一轮可以由你自主判断是否需要表格、图、统计卡、趋势、排行或对比来帮助理解。普通文字已经足够时不要调用，也不要为了凑工具调用。\n"
            + currentDateGuidance()
            + writeTargetGuidance(message)
            + "可用工具如下：\n"
            + catalog;
    }

    public static String reactSystemPrompt(String catalog, boolean writeIntent, int iteration) {
        return reactSystemPrompt(catalog, writeIntent, iteration, null);
    }

    public static String reactSystemPrompt(String catalog, boolean writeIntent, int iteration, String message) {
        String writeInstruction = writeIntent
            ? "用户这句话带有要新增、保存、记账、下单、付款、转账或退货的意思。先从真实结果中取出 ID、名称、数量和金额等字段，再由你自主选择对应草稿工具继续完成；不要停在‘我查到了’，也不要调用无关的查询工具。若真实结果仍缺必填字段，只查询能补齐该字段的工具。\n"
            : "用户当前没有明确要求写入，不要主动选择创建类工具。\n";
        String visualizationInstruction = requestsVisualization(message)
            ? "用户明确要求图表、表格或趋势展示。只要上一轮已经返回相关真实查询结果，必须调用 result_visualization 决定展示方式；不要用普通文字替代用户明确要求的展示，也不要在没有真实查询结果时调用。\n"
            : "只有用户明确要求图表、表格、排行、统计卡或其他结构化展示时，才可以自主判断是否调用 result_visualization；普通文字已经足够时不要调用。只说看看、汇总、说说、一起分析或串起来，不代表需要展示工具，也不能在没有真实查询结果时调用。\n";
        return "你在智慧记里接着处理用户刚才的问题。请先读完已经执行工具返回的真实结果，再决定下一步。\n"
            + TOOL_SELECTION_GUIDANCE
            + "不要重复已经完成且没有新条件的查询。\n"
            + INVENTORY_RESTOCK_GUIDANCE
            + writeInstruction
            + visualizationInstruction
            + "result_visualization 只决定展示方式，不能生成数据，mode 只能是 auto、table、chart、kpi、timeline。\n"
            + currentDateGuidance()
            + "写入请求请根据用户目标、工具描述和已经查到的真实参数自主选择对应的草稿工具；不要因为某个关键词就调用无关工具。\n"
            + "不生成 SQL，不访问其他账号，不把猜测写成事实。当前迭代：" + iteration + "。\n"
            + writeTargetGuidance(message)
            + "当前可用工具：\n"
            + catalog;
    }

    /**
     * 给模型提供语义导航，但不替代原生工具选择。
     * 当用户已经给出创建工具的必填字段时，明确哪些字段不需要额外查询，
     * 避免模型为了补充可选信息连续调用无关的只读工具并耗尽 ReAct 轮次。
     */
    public static String writeTargetGuidance(String message) {
        String target = targetWriteTool(message);
        if (target == null) {
            return "";
        }
        if ("create_finance_record".equals(target)) {
            return "当前写入目标与 create_finance_record 最匹配。用户已经提供收入/支出类型和金额时，type、amount 已足够生成资金流水草稿；account_id 是可选展示字段，不要先为了补账户 ID 调用 account_balance_lookup、cash_change_lookup、account_transfer_lookup 或 account_health_lookup。请仍通过原生工具调用确认最终选择。\n";
        }
        if ("create_customer".equals(target)) {
            return "当前写入目标与 create_customer 最匹配。用户已经提供客户名称和手机号时，name、phone 已足够生成客户草稿；group_id 是可选字段，只有用户明确指定客户分组或要求查分组时才调用 partner_group_lookup。请仍通过原生工具调用确认最终选择。\n";
        }
        return "当前写入目标与 " + target + " 最匹配。若该工具的必填参数已经出现在用户问题或真实工具结果中，下一轮必须继续由你通过原生 Function Calling 自主决定是否调用它；不要停在‘我查到了’，也不要声称系统没有这个工具。只有真实结果仍缺少必填参数时，才继续查询能补齐这些参数的真实数据。\n";
    }

    public static boolean hasWriteIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return WRITE_WORDS.stream().anyMatch(message::contains)
            || hasOrderWriteIntent(message)
            || hasExplicitWriteRequest(message)
            || targetWriteTool(message) != null;
    }

    private static boolean hasOrderWriteIntent(String message) {
        return message.contains("我要下单")
            || message.contains("帮我下单")
            || message.contains("请下单")
            || message.contains("下单买")
            || message.contains("下单购买")
            || message.contains("向供应商下单");
    }

    /**
     * 只有用户明确要求结构化展示时才允许模型选择展示工具。
     * “看看、汇总、分析”本身只是查询意图，不会打开图表或表格。
     */
    public static boolean requestsVisualization(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("图表")
            || message.contains("趋势图")
            || message.contains("统计图")
            || message.contains("柱状图")
            || message.contains("折线图")
            || message.contains("饼图")
            || message.contains("表格")
            || message.contains("排行")
            || message.contains("统计卡")
            || message.contains("可视化")
            || message.contains("画一张")
            || message.contains("用图")
            || message.contains("用合适的方式展示")
            || message.contains("展示成图")
            || message.contains("展示成表");
    }

    /**
     * 只在用户明确要求多个数据来源时允许 ReAct 续轮补查。
     * “看看、汇总、分析”本身不表示需要把所有相关工具都扫一遍。
     */
    public static boolean requestsMultipleSources(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("放一起")
            || message.contains("一起看")
            || message.contains("一起算")
            || message.contains("分别")
            || message.contains("同时")
            || message.contains("串起来")
            || message.contains("对比")
            || message.contains("比较")
            || message.contains("都算进去")
            || message.contains("销售和现金流")
            || message.contains("库存和补货")
            || message.contains("客户欠款和供应商应付款");
    }

    /**
     * 模型不能可靠地从 provider 默认上下文推断业务日期，日期必须由服务端明确注入。
     */
    private static String currentDateGuidance() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        return "当前业务日期是 " + today + "。用户说‘本月、最近、今天、这周’时不要猜年份，也不要自行填写 period/start_date/end_date；"
            + "只有用户明确给出具体日期或月份时才传时间参数，省略时由工具使用当前业务日期。\n";
    }

    public static String targetWriteTool(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        if (!hasExplicitWriteRequest(message)) {
            return null;
        }
        boolean createRequest = containsCreateVerb(message);
        if (hasImageGenerateIntent(message)) {
            return "image_generate";
        }
        if (message.contains("采购") && message.contains("退")) {
            return "create_purchase_return";
        }
        if (message.contains("销售") && message.contains("退")) {
            return "create_sales_return";
        }
        if (message.contains("入库")) {
            return "create_purchase_receipt";
        }
        if (message.contains("采购") || message.contains("向现有供应商买")) {
            return "create_purchase_order";
        }
        if (message.contains("开一单") || message.contains("销售草稿") || message.contains("销售单")) {
            return "create_sale_order";
        }
        if ((message.contains("转账") || message.contains("转到") || message.contains("转入"))
            && (createRequest || message.contains("转到") || message.contains("转入"))) {
            return "create_account_transfer";
        }
        if (message.contains("付款") && (createRequest || message.contains("先别直接付款") || message.contains("付款草稿"))) {
            return "create_pay_order";
        }
        if (message.contains("库存") && (message.contains("调整草稿")
            || message.contains("库存要加") || message.contains("库存要减")
            || message.contains("库存加") || message.contains("库存减"))) {
            return "create_inventory_adjustment";
        }
        if (message.contains("盘点") && (message.contains("草稿") || message.contains("按现在库存"))) {
            return "create_inventory_count_draft";
        }
        if (message.contains("商品") && (createRequest || message.contains("商品草稿"))) {
            return "create_product";
        }
        if (message.contains("客户") && (createRequest || message.contains("客户草稿"))) {
            return "create_customer";
        }
        if (message.contains("供应商") && (createRequest || message.contains("供应商草稿"))) {
            return "create_supplier";
        }
        if ((message.contains("收入") || message.contains("支出") || message.contains("记一笔"))
            && (createRequest || message.contains("记一笔"))) {
            return "create_finance_record";
        }
        if (message.contains("上传") && (createRequest || message.contains("上传意图") || message.contains("上传草稿"))) {
            return "media_upload_tool";
        }
        return null;
    }

    /**
     * Returns a read-only target when ordinary wording has one unambiguous
     * business meaning that the model has occasionally missed.
     */
    public static String targetReadTool(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        boolean supplierMentioned = message.contains("供应商") || message.contains("供货商");
        boolean statementRequest = message.contains("对账")
            || (message.contains("核对") && message.contains("账"))
            || message.contains("对一下账");
        return supplierMentioned && statementRequest ? "supplier_statement_lookup" : null;
    }

    private static boolean containsCreateVerb(String message) {
        return message.contains("新增")
            || message.contains("添加")
            || message.contains("创建")
            || message.contains("新建")
            || message.contains("加一个")
            || message.contains("加个")
            || message.contains("建一个")
            || message.contains("建个")
            || message.contains("开一张")
            || message.contains("开张")
            || message.contains("生成草稿")
            || message.contains("做成草稿")
            || message.contains("先做")
            || message.contains("先生成")
            || message.contains("记一笔");
    }

    private static boolean hasExplicitWriteRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (containsCreateVerb(message) || message.contains("草稿") || message.contains("保存")) {
            return true;
        }
        if (message.contains("向现有供应商买") || message.contains("我要采购")) {
            return true;
        }
        if (message.contains("把") && message.contains("转到")) {
            return true;
        }
        if (message.contains("把") && (message.contains("库存加") || message.contains("库存减"))) {
            return true;
        }
        if (message.contains("把") && message.contains("做入库")) {
            return true;
        }
        return containsExplicitReturnOperation(message) || hasImageGenerateIntent(message);
    }

    private static boolean hasImageGenerateIntent(String message) {
        if (message == null || message.isBlank() || requestsVisualization(message)) {
            return false;
        }
        if (message.contains("提示词") || message.contains("文案")) {
            return false;
        }
        boolean imageSubject = message.contains("图片")
            || message.contains("图像")
            || message.contains("商品图")
            || message.contains("主图")
            || message.contains("配图")
            || message.contains("插画")
            || message.contains("海报");
        boolean generationVerb = message.contains("生成")
            || message.contains("生图")
            || message.contains("绘制")
            || message.contains("画一张")
            || message.contains("做一张");
        return imageSubject && generationVerb;
    }

    private static boolean containsExplicitReturnOperation(String message) {
        if (!message.contains("退")) {
            return false;
        }
        // “销售单、收款和退货串起来看看” is a read request. A return write
        // needs an explicit quantity or an imperative return phrase.
        boolean hasQuantity = message.matches(
            ".*退\\s*(?:\\d+(?:\\.\\d+)?|一|两|三|四|五|六|七|八|九|十)\\s*(?:件|个|条)?.*"
        );
        boolean hasImperativeTarget = message.matches(
            ".*(?:把|将).*(?:销售单|采购单|采购来的货).*(?:退掉|退回|退还).*"
        );
        return hasQuantity || hasImperativeTarget;
    }

    public static String userHint(String toolName) {
        return USER_LANGUAGE_HINTS.get(toolName);
    }

    public static Set<String> knownToolNames() {
        return USER_LANGUAGE_HINTS.keySet();
    }

    private static Entry<String, String> entry(String name, String hint) {
        return Map.entry(name, hint);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

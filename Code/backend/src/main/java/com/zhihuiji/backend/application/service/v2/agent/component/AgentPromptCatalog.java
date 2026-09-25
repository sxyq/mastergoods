// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent.component;

import com.zhihuiji.backend.application.service.v2.agent.tool.AgentTool;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
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

    private static final Set<String> WRITE_WORDS = Set.of(
        "新增", "添加", "创建", "新建", "加一个", "建个", "录入", "登记", "开一张", "开张",
        "保存", "上传", "生成"
    );
    private static final Pattern NEGATED_WRITE_INTENT = Pattern.compile(
        "(?:不要|不需要|无需|禁止|不执行|不进行|不做|仅查询|只读)[^。；;，,]{0,12}"
            + "(?:新增|添加|创建|新建|加一个|加个|建一个|建个|开一张|开张|生成草稿|生成|保存|上传)"
    );
    private static final Pattern POSITIVE_WRITE_INTENT = Pattern.compile(
        "(?:请|帮我|我要|需要|给我|把)[^。；;，,]{0,20}"
            + "(?:新增|添加|创建|新建|加一个|加个|建一个|建个|开一张|开张|生成草稿|生成|保存|上传)"
    );

    private static final Map<String, String> USER_LANGUAGE_HINTS = Map.ofEntries(
        entry("data_export_tool", "销售数据能导出哪些，先给我看看"),
        entry("import_job_lookup", "之前的数据导入到哪一步了，有没有失败的"),
        entry("result_visualization", "已经拿到真实查询结果后，由你判断表格、图、统计卡、趋势或排行是否更容易让用户理解；不需要展示时不要调用"),
        entry("store_info_lookup", "当前门店信息和成员数量看一下"),
        entry("sync_status_lookup", "数据同步现在正常吗"),
        entry("image_generate", "生成一张图片或商品海报，先把生图草稿给我确认"),
        entry("media_upload_tool", "我有个文件，先生成上传意图，不要直接上传")
    );

    /**
     * Clarifies overlapping tools for providers that tend to over-call when a
     * broad registry is visible. This is guidance only; the model still emits
     * the native call and the server never selects a business tool by keyword.
     */
    // 旧领域工具的消歧提示已删除；新领域工具接入后在此补充。
    private static final Map<String, String> TOOL_DISAMBIGUATION_HINTS = Map.of();

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
            + "如果创建操作需要实体 ID，而用户没有给出，先查真实数据，暂时不要调用创建工具；\n"
            + "查到后下一轮要继续完成用户要做的事情，缺少关键信息时再向用户询问。\n"
            + "单主题首轮只返回一个最相关的原生工具调用；已有覆盖整件事的聚合工具时优先只选它。\n"
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
        return "当前写入目标与 " + target + " 最匹配。若该工具的必填参数已经出现在用户问题或真实工具结果中，下一轮必须继续由你通过原生 Function Calling 自主决定是否调用它；不要停在‘我查到了’，也不要声称系统没有这个工具。只有真实结果仍缺少必填参数时，才继续查询能补齐这些参数的真实数据。\n";
    }

    public static boolean hasWriteIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (hasNegatedWriteIntent(message)) {
            return false;
        }
        return WRITE_WORDS.stream().anyMatch(message::contains)
            || hasExplicitWriteRequest(message)
            || targetWriteTool(message) != null;
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
            || message.contains("都算进去");
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
        // 旧领域的 create_* 草稿路由已删除；当前只保留基础设施写入工具。
        if (hasImageGenerateIntent(message)) {
            return "image_generate";
        }
        if (message.contains("上传") && (message.contains("上传意图") || message.contains("上传草稿") || containsCreateVerb(message))) {
            return "media_upload_tool";
        }
        return null;
    }

    /**
     * Returns a read-only target when ordinary wording has one unambiguous
     * business meaning that the model has occasionally missed.
     */
    public static String targetReadTool(String message) {
        // 旧领域的对账工具路由已删除；新领域读取工具接入后在此补充。
        return null;
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
        if (hasNegatedWriteIntent(message)) {
            return false;
        }
        if (containsCreateVerb(message) || message.contains("草稿") || message.contains("保存")) {
            return true;
        }
        if (message.contains("把") && message.contains("转到")) {
            return true;
        }
        return hasImageGenerateIntent(message);
    }

    private static boolean hasNegatedWriteIntent(String message) {
        return NEGATED_WRITE_INTENT.matcher(message).find()
            && !POSITIVE_WRITE_INTENT.matcher(message).find();
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

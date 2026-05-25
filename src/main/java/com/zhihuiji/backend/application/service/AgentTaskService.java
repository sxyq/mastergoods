package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.api.dto.report.ReportDto;
import com.zhihuiji.backend.domain.entity.AgentNotificationEntity;
import com.zhihuiji.backend.domain.entity.AgentTaskEntity;
import com.zhihuiji.backend.domain.entity.ProductEntity;
import com.zhihuiji.backend.domain.entity.SaleOrderEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentTaskService {
    private static final Logger log = LoggerFactory.getLogger(AgentTaskService.class);
    private static final String STATUS_QUEUED = "queued";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String SOURCE_USER = "user";
    private static final String SOURCE_SCHEDULER = "scheduler";
    private static final String TASK_ANOMALY = "anomaly_watch";
    private static final String TASK_RECONCILIATION = "reconciliation_deep_dive";
    private static final String TASK_REPORT = "sales_report_deep_dive";
    private static final String TASK_QUESTION = "question_deep_dive";
    private static final String TASK_DRAFT = "operation_draft_deep_dive";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final AgentTaskRepository agentTaskRepository;
    private final AgentNotificationRepository notificationRepository;
    private final AgentService agentService;
    private final LlmDrivenAgentService llmDrivenAgentService;
    private final AgentLlmService agentLlmService;
    private final ReportService reportService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ObjectMapper objectMapper;
    private final Executor agentTaskExecutor;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public AgentTaskService(
        AgentTaskRepository agentTaskRepository,
        AgentNotificationRepository notificationRepository,
        AgentService agentService,
        LlmDrivenAgentService llmDrivenAgentService,
        AgentLlmService agentLlmService,
        ReportService reportService,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        SaleOrderRepository saleOrderRepository,
        ObjectMapper objectMapper,
        @Qualifier("agentTaskExecutor") Executor agentTaskExecutor
    ) {
        this.agentTaskRepository = agentTaskRepository;
        this.notificationRepository = notificationRepository;
        this.agentService = agentService;
        this.llmDrivenAgentService = llmDrivenAgentService;
        this.agentLlmService = agentLlmService;
        this.reportService = reportService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.objectMapper = objectMapper;
        this.agentTaskExecutor = agentTaskExecutor;
    }

    public AgentDto.AgentTaskSummaryDto submitTask(String taskType, String title, String input) {
        AgentTaskEntity task = new AgentTaskEntity();
        long now = System.currentTimeMillis();
        task.setTaskType(normalizeTaskType(taskType));
        task.setTitle(StringUtils.hasText(title) ? title.trim() : defaultTitle(task.getTaskType()));
        task.setTriggerSource(SOURCE_USER);
        task.setStatus(STATUS_QUEUED);
        task.setProgress(5);
        task.setInputText(input);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        AgentTaskEntity saved = agentTaskRepository.save(task);
        agentTaskExecutor.execute(() -> executeTask(saved.getId()));
        return toSummary(saved);
    }

    public List<AgentDto.AgentTaskSummaryDto> listTasks() {
        return agentTaskRepository.findTop20ByOrderByCreatedAtDesc().stream()
            .map(this::toSummary)
            .toList();
    }

    public AgentDto.AgentTaskDetailDto getTask(Long taskId) {
        AgentTaskEntity task = agentTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("agent task not found"));
        return new AgentDto.AgentTaskDetailDto(
            toSummary(task),
            task.getInputText(),
            parseResult(task.getResultJson())
        );
    }

    public List<AgentDto.AgentNotificationDto> listNotifications(boolean unreadOnly, boolean undeliveredOnly) {
        List<AgentNotificationEntity> rows;
        if (unreadOnly && undeliveredOnly) {
            rows = notificationRepository.findTop30ByIsReadFalseAndIsDeliveredFalseOrderByCreatedAtDesc();
        } else if (unreadOnly) {
            rows = notificationRepository.findTop30ByIsReadFalseOrderByCreatedAtDesc();
        } else if (undeliveredOnly) {
            rows = notificationRepository.findTop30ByIsDeliveredFalseOrderByCreatedAtDesc();
        } else {
            rows = notificationRepository.findTop30ByOrderByCreatedAtDesc();
        }
        return rows.stream().map(this::toNotificationDto).toList();
    }

    public AgentDto.AgentNotificationDto markNotificationRead(Long notificationId) {
        AgentNotificationEntity entity = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        entity.setIsRead(true);
        return toNotificationDto(notificationRepository.save(entity));
    }

    public AgentDto.AgentNotificationDto markNotificationDelivered(Long notificationId) {
        AgentNotificationEntity entity = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        entity.setIsDelivered(true);
        return toNotificationDto(notificationRepository.save(entity));
    }

    public SseEmitter subscribeNotifications() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("agent-notification-stream-ready"));
            emitter.send(SseEmitter.event().name("heartbeat").data(System.currentTimeMillis()));
        } catch (Exception ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @Scheduled(fixedDelayString = "${agent.task.anomaly-interval-ms:900000}")
    public void runScheduledAnomalyWatch() {
        Optional<AgentTaskEntity> active = agentTaskRepository.findFirstByTaskTypeAndStatusInOrderByCreatedAtDesc(
            TASK_ANOMALY,
            Set.of(STATUS_QUEUED, STATUS_RUNNING)
        );
        if (active.isPresent()) {
            return;
        }
        AgentTaskEntity task = new AgentTaskEntity();
        long now = System.currentTimeMillis();
        task.setTaskType(TASK_ANOMALY);
        task.setTitle("定时异常巡检");
        task.setTriggerSource(SOURCE_SCHEDULER);
        task.setStatus(STATUS_QUEUED);
        task.setProgress(5);
        task.setInputText("system scheduled anomaly watch");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        AgentTaskEntity saved = agentTaskRepository.save(task);
        agentTaskExecutor.execute(() -> executeTask(saved.getId()));
    }

    private void executeTask(Long taskId) {
        AgentTaskEntity task = agentTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        try {
            task.setStatus(STATUS_RUNNING);
            task.setProgress(20);
            task.setUpdatedAt(System.currentTimeMillis());
            agentTaskRepository.save(task);

            AgentDto.AgentTaskResultDto result = buildTaskResult(task);
            task.setResultJson(toJson(result));
            task.setStatus(STATUS_COMPLETED);
            task.setProgress(100);
            task.setCompletedAt(System.currentTimeMillis());
            task.setUpdatedAt(System.currentTimeMillis());
            agentTaskRepository.save(task);
            publishNotification(task, result);
        } catch (Exception ex) {
            log.warn("agent task {} failed: {}", taskId, ex.getMessage());
            task.setStatus(STATUS_FAILED);
            task.setProgress(100);
            task.setUpdatedAt(System.currentTimeMillis());
            task.setCompletedAt(System.currentTimeMillis());
            task.setResultJson(toJson(failureResult(task, ex)));
            agentTaskRepository.save(task);
            publishNotification(task, failureResult(task, ex));
        }
    }

    private AgentDto.AgentTaskResultDto buildTaskResult(AgentTaskEntity task) {
        return switch (task.getTaskType()) {
            case TASK_RECONCILIATION -> buildReconciliationResult(task.getInputText());
            case TASK_REPORT -> buildSalesReportResult(task.getInputText());
            case TASK_QUESTION -> buildQuestionResult(task.getInputText());
            case TASK_DRAFT -> buildDraftResult(task.getInputText());
            case TASK_ANOMALY -> buildAnomalyResult();
            default -> buildQuestionResult(task.getInputText());
        };
    }

    private AgentDto.AgentTaskResultDto buildAnomalyResult() {
        AgentDto.AlertDashboardDto alerts = agentService.getAlerts(8, 15);
        AgentDto.ReconciliationFollowupDto reconciliation = agentService.getReconciliationFollowup(6, 15);
        AgentDto.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        List<ReportDto.LowStockProductReportDto> lowStocks = reportService.lowStockProducts(6);
        AgentDto.OperationDraftDto draft = llmDrivenAgentService.draftOperation(
            lowStocks.isEmpty()
                ? "根据当前经营风险生成一条采购草稿"
                : "给供应商A入库 " + Math.max(1, Math.round(lowStocks.get(0).safeStock() - lowStocks.get(0).stock())) + " 个 " + lowStocks.get(0).productName()
        );

        TaskContext context = new TaskContext(
            "异常巡检",
            "服务端定时运行的异常巡检",
            alerts,
            reconciliation,
            insight,
            lowStocks,
            List.of(),
            List.of(),
            draft
        );
        AgentDto.AgentTaskResultDto fallback = new AgentDto.AgentTaskResultDto(
            "异常巡检完成",
            "服务端定时任务",
            "已完成库存、账龄和退款风险巡检，建议优先处理高严重度预警。",
            List.of(
                new AgentDto.AgentTaskMetricDto("高风险提醒", String.valueOf(alerts.alerts().stream().filter(a -> "high".equals(a.severity())).count()), "", "high"),
                new AgentDto.AgentTaskMetricDto("低库存商品", String.valueOf(lowStocks.size()), "", "medium"),
                new AgentDto.AgentTaskMetricDto("待催收金额", formatMoney(reconciliation.totalReceivable()), "", "medium")
            ),
            List.of(
                new AgentDto.AgentTaskSectionDto("处理顺序", insight.narrative(), alerts.alerts().stream().limit(4).map(AgentDto.AlertDto::recommendedAction).toList())
            ),
            List.of(
                new AgentDto.AgentTaskTableDto(
                    "重点预警",
                    List.of("类型", "标题", "对象", "建议"),
                    alerts.alerts().stream().limit(6)
                        .map(alert -> List.of(alert.type(), alert.title(), safeText(alert.entityName(), "-"), alert.recommendedAction()))
                        .toList()
                )
            ),
            List.of(
                new AgentDto.AgentTaskChartDto(
                    "低库存缺口",
                    "bar",
                    lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::productName).toList(),
                    List.of(
                        new AgentDto.AgentTaskChartSeriesDto("当前库存", lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::stock).toList()),
                        new AgentDto.AgentTaskChartSeriesDto("安全库存", lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::safeStock).toList())
                    )
                )
            ),
            insight.suggestedActions(),
            draft,
            List.of()
        );
        fallback = ensureRenderBlocks(fallback);
        return llmTaskResult("异常巡检分析师", """
            生成一份详细的仓储异常巡检报告。
            必须返回严格 JSON，字段:
            title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, renderBlocks.
            charts 内 chartType 仅可用 line, bar, area。
            renderBlocks 内 type 仅可用 hero, metric_grid, bullet_list, table, chart, draft。
            所有事实必须只来自上下文。
            Context:
            %s
            """.formatted(toJson(context)), fallback);
    }

    private AgentDto.AgentTaskResultDto buildReconciliationResult(String input) {
        AgentDto.ReconciliationFollowupDto reconciliation = agentService.getReconciliationFollowup(8, 15);
        AgentDto.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        TaskContext context = new TaskContext(
            "对账催办深度分析",
            safeText(input, "分析应收应付与异常账龄"),
            new AgentDto.AlertDashboardDto(List.of()),
            reconciliation,
            insight,
            List.of(),
            List.of(),
            List.of(),
            null
        );
        AgentDto.AgentTaskResultDto fallback = new AgentDto.AgentTaskResultDto(
            "对账催办分析",
            "应收、应付与账龄风险",
            "已汇总待催收客户、待付款供应商和异常账龄订单。",
            List.of(
                new AgentDto.AgentTaskMetricDto("待催收总额", formatMoney(reconciliation.totalReceivable()), "", "high"),
                new AgentDto.AgentTaskMetricDto("待付款总额", formatMoney(reconciliation.totalPayable()), "", "medium"),
                new AgentDto.AgentTaskMetricDto("异常账龄数", String.valueOf(reconciliation.agingRisks().size()), "", "medium")
            ),
            List.of(
                new AgentDto.AgentTaskSectionDto(
                    "核心结论",
                    "当前对账风险主要集中在高余额客户和长账龄订单。",
                    reconciliation.agingRisks().stream().limit(4).map(AgentDto.AgingRiskDto::suggestedAction).toList()
                )
            ),
            List.of(
                new AgentDto.AgentTaskTableDto(
                    "待催收客户",
                    List.of("客户", "电话", "应收"),
                    reconciliation.receivableCustomers().stream()
                        .limit(6)
                        .map(item -> List.of(item.name(), safeText(item.phone(), "-"), formatMoney(item.amount())))
                        .toList()
                ),
                new AgentDto.AgentTaskTableDto(
                    "异常账龄",
                    List.of("对象", "单号", "天数", "金额"),
                    reconciliation.agingRisks().stream()
                        .limit(6)
                        .map(item -> List.of(item.name(), item.orderNo(), String.valueOf(item.ageDays()), formatMoney(item.amount())))
                        .toList()
                )
            ),
            List.of(
                new AgentDto.AgentTaskChartDto(
                    "应收与应付对比",
                    "bar",
                    List.of("应收", "应付", "净现金流"),
                    List.of(new AgentDto.AgentTaskChartSeriesDto(
                        "金额",
                        List.of(reconciliation.totalReceivable(), reconciliation.totalPayable(), reconciliation.netCashFlow())
                    ))
                )
            ),
            List.of("优先催收前两名客户。", "对长账龄订单建立逐单跟踪。"),
            null,
            List.of()
        );
        fallback = ensureRenderBlocks(fallback);
        return llmTaskResult("对账分析师", """
            生成一份详细的应收应付催办分析。
            必须返回严格 JSON，字段:
            title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, renderBlocks.
            charts 内 chartType 仅可用 line, bar, area。
            Context:
            %s
            """.formatted(toJson(context)), fallback);
    }

    private AgentDto.AgentTaskResultDto buildSalesReportResult(String input) {
        long now = System.currentTimeMillis();
        long start = startOfDay(now - 6 * DAY_MS);
        long prevStart = start - 7 * DAY_MS;
        AgentDto.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        List<ReportDto.TopSellingProductReportDto> topProducts = reportService.topProducts(start, now, 6);
        List<ReportDto.CustomerSalesReportDto> topCustomers = reportService.customerSales(start, now, 6);
        List<Double> salesSeries = dailySalesSeries(start, now);
        List<Double> previousSeries = dailySalesSeries(prevStart, start - 1);
        TaskContext context = new TaskContext(
            "销售趋势复盘",
            safeText(input, "复盘近 7 天销售表现"),
            new AgentDto.AlertDashboardDto(List.of()),
            agentService.getReconciliationFollowup(6, 15),
            insight,
            List.of(),
            topProducts,
            topCustomers,
            null
        );
        AgentDto.AgentTaskResultDto fallback = new AgentDto.AgentTaskResultDto(
            "销售趋势复盘",
            "近 7 天销售与客户贡献",
            insight.narrative(),
            List.of(
                new AgentDto.AgentTaskMetricDto("本期销售额", formatMoney(insight.currentSales()), formatPercent(insight.salesChangeRate()), "high"),
                new AgentDto.AgentTaskMetricDto("主力商品", insight.leadingProductName(), formatMoney(insight.leadingProductAmount()), "medium"),
                new AgentDto.AgentTaskMetricDto("主力客户", insight.leadingCustomerName(), formatMoney(insight.leadingCustomerAmount()), "medium")
            ),
            List.of(
                new AgentDto.AgentTaskSectionDto("趋势判断", insight.narrative(), insight.highlights())
            ),
            List.of(
                new AgentDto.AgentTaskTableDto(
                    "主力商品",
                    List.of("商品", "数量", "金额"),
                    topProducts.stream()
                        .map(item -> List.of(item.productName(), formatNumber(item.totalQuantity()), formatMoney(item.totalAmount())))
                        .toList()
                ),
                new AgentDto.AgentTaskTableDto(
                    "客户贡献",
                    List.of("客户", "订单数", "金额"),
                    topCustomers.stream()
                        .map(item -> List.of(item.customerName(), String.valueOf(item.totalOrders()), formatMoney(item.totalAmount())))
                        .toList()
                )
            ),
            List.of(
                new AgentDto.AgentTaskChartDto(
                    "近 7 天销售走势",
                    "line",
                    dayLabels(start, 7),
                    List.of(
                        new AgentDto.AgentTaskChartSeriesDto("本期", salesSeries),
                        new AgentDto.AgentTaskChartSeriesDto("上期", previousSeries)
                    )
                )
            ),
            insight.suggestedActions(),
            null,
            List.of()
        );
        fallback = ensureRenderBlocks(fallback);
        return llmTaskResult("销售经营分析师", """
            生成一份详细的销售趋势复盘报告。
            必须返回严格 JSON，字段:
            title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, renderBlocks.
            charts 内 chartType 仅可用 line, bar, area。
            Context:
            %s
            """.formatted(toJson(context)), fallback);
    }

    private AgentDto.AgentTaskResultDto buildQuestionResult(String input) {
        AgentDto.AgentAnswerDto answer = llmDrivenAgentService.answerQuestion(safeText(input, "当前最值得关注的经营问题是什么"));
        AgentDto.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        TaskContext context = new TaskContext(
            "经营问题深度分析",
            safeText(input, ""),
            agentService.getAlerts(6, 15),
            agentService.getReconciliationFollowup(6, 15),
            insight,
            reportService.lowStockProducts(6),
            reportService.topProducts(startOfDay(System.currentTimeMillis()), System.currentTimeMillis(), 6),
            reportService.customerSales(startOfDay(System.currentTimeMillis()), System.currentTimeMillis(), 6),
            null
        );
        AgentDto.AgentTaskResultDto fallback = new AgentDto.AgentTaskResultDto(
            "经营问题分析",
            answer.intent(),
            answer.answer(),
            List.of(new AgentDto.AgentTaskMetricDto("问题类型", answer.intent(), "", "medium")),
            List.of(new AgentDto.AgentTaskSectionDto("结论", answer.answer(), answer.highlights())),
            answer.columns().isEmpty() ? List.of() : List.of(new AgentDto.AgentTaskTableDto("数据明细", answer.columns(), answer.rows())),
            List.of(),
            answer.suggestedActions(),
            null,
            List.of()
        );
        fallback = ensureRenderBlocks(fallback);
        return llmTaskResult("经营分析专家", """
            对用户的问题做一份详细业务分析。
            必须返回严格 JSON，字段:
            title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, renderBlocks.
            charts 内 chartType 仅可用 line, bar, area。
            Context:
            %s
            """.formatted(toJson(context)), fallback);
    }

    private AgentDto.AgentTaskResultDto buildDraftResult(String input) {
        AgentDto.OperationDraftDto draft = llmDrivenAgentService.draftOperation(safeText(input, "根据当前数据生成一条采购草稿"));
        TaskContext context = new TaskContext(
            "单据草稿深度分析",
            safeText(input, ""),
            agentService.getAlerts(6, 15),
            agentService.getReconciliationFollowup(6, 15),
            llmDrivenAgentService.getReportInsight(7),
            reportService.lowStockProducts(6),
            List.of(),
            List.of(),
            draft
        );
        AgentDto.AgentTaskResultDto fallback = new AgentDto.AgentTaskResultDto(
            "单据草稿分析",
            draft.operationType(),
            draft.summary(),
            List.of(
                new AgentDto.AgentTaskMetricDto("可提交", draft.canSubmit() ? "是" : "否", "", draft.canSubmit() ? "success" : "high"),
                new AgentDto.AgentTaskMetricDto("明细数量", String.valueOf(draft.items().size()), "", "medium")
            ),
            List.of(
                new AgentDto.AgentTaskSectionDto("草稿说明", draft.summary(), draft.suggestedActions())
            ),
            draft.items().isEmpty()
                ? List.of()
                : List.of(new AgentDto.AgentTaskTableDto(
                    "草稿明细",
                    List.of("商品", "编码", "数量", "单价", "金额", "库存"),
                    draft.items().stream()
                        .map(item -> List.of(
                            item.productName(),
                            item.productCode(),
                            formatNumber(item.quantity()),
                            formatMoney(item.unitPrice()),
                            formatMoney(item.amount()),
                            formatNumber(item.currentStock())
                        ))
                        .toList()
                )),
            draft.items().isEmpty()
                ? List.of()
                : List.of(new AgentDto.AgentTaskChartDto(
                    "草稿商品库存对比",
                    "bar",
                    draft.items().stream().map(AgentDto.OperationDraftItemDto::productName).toList(),
                    List.of(
                        new AgentDto.AgentTaskChartSeriesDto("拟操作数量", draft.items().stream().map(AgentDto.OperationDraftItemDto::quantity).toList()),
                        new AgentDto.AgentTaskChartSeriesDto("当前库存", draft.items().stream().map(AgentDto.OperationDraftItemDto::currentStock).toList())
                    )
                )),
            draft.warnings().isEmpty() ? draft.suggestedActions() : draft.warnings(),
            draft,
            List.of()
        );
        fallback = ensureRenderBlocks(fallback);
        return llmTaskResult("单据编排分析师", """
            对草稿指令做详细分析。
            必须返回严格 JSON，字段:
            title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, renderBlocks.
            charts 内 chartType 仅可用 line, bar, area。
            Context:
            %s
            """.formatted(toJson(context)), fallback);
    }

    private AgentDto.AgentTaskResultDto llmTaskResult(String systemPrompt, String userPrompt, AgentDto.AgentTaskResultDto fallback) {
        return agentLlmService.requestStructuredJson(systemPrompt, userPrompt)
            .map(node -> mergeTaskResult(node, fallback))
            .map(this::ensureRenderBlocks)
            .orElse(fallback);
    }

    private AgentDto.AgentTaskResultDto mergeTaskResult(JsonNode node, AgentDto.AgentTaskResultDto fallback) {
        return new AgentDto.AgentTaskResultDto(
            readText(node, "title", fallback.title()),
            readText(node, "subtitle", fallback.subtitle()),
            readText(node, "summary", fallback.summary()),
            readMetrics(node.path("metrics"), fallback.metrics()),
            readSections(node.path("sections"), fallback.sections()),
            readTables(node.path("tables"), fallback.tables()),
            readCharts(node.path("charts"), fallback.charts()),
            readStringList(node.path("suggestedActions"), fallback.suggestedActions()),
            fallback.draft(),
            readRenderBlocks(node.path("renderBlocks"), fallback.renderBlocks())
        );
    }

    private void publishNotification(AgentTaskEntity task, AgentDto.AgentTaskResultDto result) {
        AgentNotificationEntity notification = new AgentNotificationEntity();
        notification.setTaskId(task.getId());
        notification.setTitle(task.getTitle());
        notification.setBody(safeText(result.summary(), "任务已完成"));
        notification.setLevel(STATUS_FAILED.equals(task.getStatus()) ? "error" : TASK_ANOMALY.equals(task.getTaskType()) ? "warning" : "info");
        notification.setIsRead(false);
        notification.setIsDelivered(false);
        notification.setCreatedAt(System.currentTimeMillis());
        AgentNotificationEntity saved = notificationRepository.save(notification);
        AgentDto.AgentNotificationDto payload = toNotificationDto(saved);
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("agent-notification").data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        });
    }

    private AgentDto.AgentTaskResultDto failureResult(AgentTaskEntity task, Exception ex) {
        return new AgentDto.AgentTaskResultDto(
            task.getTitle(),
            "执行失败",
            safeText(ex.getMessage(), "任务执行失败"),
            List.of(),
            List.of(new AgentDto.AgentTaskSectionDto("失败原因", safeText(ex.getMessage(), "未知错误"), List.of())),
            List.of(),
            List.of(),
            List.of("稍后重试，或缩小分析范围。"),
            null,
            List.of()
        );
    }

    private AgentDto.AgentTaskSummaryDto toSummary(AgentTaskEntity entity) {
        return new AgentDto.AgentTaskSummaryDto(
            entity.getId(),
            entity.getTaskType(),
            entity.getTitle(),
            entity.getStatus(),
            entity.getTriggerSource(),
            entity.getProgress() == null ? 0 : entity.getProgress(),
            safeLong(entity.getCreatedAt()),
            safeLong(entity.getUpdatedAt()),
            entity.getCompletedAt()
        );
    }

    private AgentDto.AgentNotificationDto toNotificationDto(AgentNotificationEntity entity) {
        return new AgentDto.AgentNotificationDto(
            entity.getId(),
            entity.getTitle(),
            entity.getBody(),
            entity.getLevel(),
            entity.getTaskId(),
            Boolean.TRUE.equals(entity.getIsRead()),
            Boolean.TRUE.equals(entity.getIsDelivered()),
            safeLong(entity.getCreatedAt())
        );
    }

    private AgentDto.AgentTaskResultDto parseResult(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, AgentDto.AgentTaskResultDto.class);
        } catch (Exception ex) {
            return new AgentDto.AgentTaskResultDto(
                "结果解析失败",
                "",
                raw,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of()
            );
        }
    }

    private List<Double> dailySalesSeries(long startAt, long endAt) {
        List<Double> values = new ArrayList<>();
        long cursor = startAt;
        while (cursor <= endAt) {
            long next = Math.min(endAt, cursor + DAY_MS - 1);
            values.add(reportService.salesSummary(cursor, next).totalSalesAmount());
            cursor += DAY_MS;
        }
        return values;
    }

    private List<String> dayLabels(long startAt, int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<String> labels = new ArrayList<>();
        long cursor = startAt;
        for (int i = 0; i < days; i++) {
            labels.add(Instant.ofEpochMilli(cursor).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter));
            cursor += DAY_MS;
        }
        return labels;
    }

    private long startOfDay(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    private String normalizeTaskType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return TASK_REPORT;
        }
        return switch (raw.trim()) {
            case TASK_ANOMALY, TASK_RECONCILIATION, TASK_REPORT, TASK_QUESTION, TASK_DRAFT -> raw.trim();
            default -> TASK_REPORT;
        };
    }

    private String defaultTitle(String taskType) {
        return switch (taskType) {
            case TASK_ANOMALY -> "异常巡检";
            case TASK_RECONCILIATION -> "对账催办分析";
            case TASK_QUESTION -> "经营问题分析";
            case TASK_DRAFT -> "单据草稿分析";
            default -> "销售趋势复盘";
        };
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asText("");
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String text = item.asText("").trim();
            if (StringUtils.hasText(text)) {
                values.add(text);
            }
        });
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentDto.AgentTaskMetricDto> readMetrics(JsonNode node, List<AgentDto.AgentTaskMetricDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentTaskMetricDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentDto.AgentTaskMetricDto(
            readText(item, "label", ""),
            readText(item, "value", ""),
            readText(item, "delta", ""),
            readText(item, "emphasis", "medium")
        )));
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentDto.AgentTaskSectionDto> readSections(JsonNode node, List<AgentDto.AgentTaskSectionDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentTaskSectionDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentDto.AgentTaskSectionDto(
            readText(item, "title", ""),
            readText(item, "narrative", ""),
            readStringList(item.path("bullets"), List.of())
        )));
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentDto.AgentTaskTableDto> readTables(JsonNode node, List<AgentDto.AgentTaskTableDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentTaskTableDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentDto.AgentTaskTableDto(
            readText(item, "title", ""),
            readStringList(item.path("columns"), List.of()),
            readRows(item.path("rows"))
        )));
        return values.isEmpty() ? fallback : values;
    }

    private List<List<String>> readRows(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        node.forEach(rowNode -> {
            List<String> row = new ArrayList<>();
            rowNode.forEach(cell -> row.add(cell.asText("")));
            rows.add(row);
        });
        return rows;
    }

    private List<AgentDto.AgentTaskChartDto> readCharts(JsonNode node, List<AgentDto.AgentTaskChartDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentTaskChartDto> values = new ArrayList<>();
        node.forEach(item -> {
            List<AgentDto.AgentTaskChartSeriesDto> series = new ArrayList<>();
            item.path("series").forEach(seriesNode -> {
                List<Double> numeric = new ArrayList<>();
                seriesNode.path("values").forEach(v -> numeric.add(v.asDouble(0.0)));
                series.add(new AgentDto.AgentTaskChartSeriesDto(
                    readText(seriesNode, "name", "series"),
                    numeric
                ));
            });
            values.add(new AgentDto.AgentTaskChartDto(
                readText(item, "title", ""),
                readText(item, "chartType", "bar"),
                readStringList(item.path("categories"), List.of()),
                series
            ));
        });
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentDto.AgentRenderBlockDto> readRenderBlocks(
        JsonNode node,
        List<AgentDto.AgentRenderBlockDto> fallback
    ) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentDto.AgentRenderBlockDto> blocks = new ArrayList<>();
        node.forEach(item -> blocks.add(new AgentDto.AgentRenderBlockDto(
            readText(item, "type", ""),
            readText(item, "title", ""),
            readText(item, "subtitle", ""),
            readText(item, "tone", "primary"),
            readText(item, "text", ""),
            readStringList(item.path("bullets"), List.of()),
            readMetrics(item.path("metrics"), List.of()),
            readBlockTable(item.path("table")),
            readBlockChart(item.path("chart")),
            fallback.isEmpty() ? null : fallback.stream().map(AgentDto.AgentRenderBlockDto::draft).filter(java.util.Objects::nonNull).findFirst().orElse(null)
        )));
        return blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList().isEmpty()
            ? fallback
            : blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList();
    }

    private AgentDto.AgentTaskTableDto readBlockTable(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        return new AgentDto.AgentTaskTableDto(
            readText(node, "title", ""),
            readStringList(node.path("columns"), List.of()),
            readRows(node.path("rows"))
        );
    }

    private AgentDto.AgentTaskChartDto readBlockChart(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        List<AgentDto.AgentTaskChartSeriesDto> series = new ArrayList<>();
        node.path("series").forEach(seriesNode -> {
            List<Double> numeric = new ArrayList<>();
            seriesNode.path("values").forEach(v -> numeric.add(v.asDouble(0.0)));
            series.add(new AgentDto.AgentTaskChartSeriesDto(
                readText(seriesNode, "name", "series"),
                numeric
            ));
        });
        return new AgentDto.AgentTaskChartDto(
            readText(node, "title", ""),
            readText(node, "chartType", "bar"),
            readStringList(node.path("categories"), List.of()),
            series
        );
    }

    private AgentDto.AgentTaskResultDto ensureRenderBlocks(AgentDto.AgentTaskResultDto result) {
        if (result.renderBlocks() != null && !result.renderBlocks().isEmpty()) {
            return result;
        }
        return new AgentDto.AgentTaskResultDto(
            result.title(),
            result.subtitle(),
            result.summary(),
            result.metrics(),
            result.sections(),
            result.tables(),
            result.charts(),
            result.suggestedActions(),
            result.draft(),
            buildRenderBlocks(result)
        );
    }

    private List<AgentDto.AgentRenderBlockDto> buildRenderBlocks(AgentDto.AgentTaskResultDto result) {
        List<AgentDto.AgentRenderBlockDto> blocks = new ArrayList<>();
        blocks.add(new AgentDto.AgentRenderBlockDto(
            "hero",
            result.title(),
            result.subtitle(),
            "primary",
            result.summary(),
            result.suggestedActions().stream().limit(4).toList(),
            List.of(),
            null,
            null,
            null
        ));
        if (!result.metrics().isEmpty()) {
            blocks.add(new AgentDto.AgentRenderBlockDto(
                "metric_grid",
                "关键指标",
                "由 Agent 输出的优先指标",
                "primary",
                null,
                List.of(),
                result.metrics(),
                null,
                null,
                null
            ));
        }
        result.sections().forEach(section -> blocks.add(new AgentDto.AgentRenderBlockDto(
            "bullet_list",
            section.title(),
            "",
            "primary",
            section.narrative(),
            section.bullets(),
            List.of(),
            null,
            null,
            null
        )));
        result.tables().forEach(table -> blocks.add(new AgentDto.AgentRenderBlockDto(
            "table",
            table.title(),
            "",
            "primary",
            null,
            List.of(),
            List.of(),
            table,
            null,
            null
        )));
        result.charts().forEach(chart -> blocks.add(new AgentDto.AgentRenderBlockDto(
            "chart",
            chart.title(),
            chart.chartType(),
            "primary",
            null,
            List.of(),
            List.of(),
            null,
            chart,
            null
        )));
        if (result.draft() != null) {
            blocks.add(new AgentDto.AgentRenderBlockDto(
                "draft",
                result.draft().summary(),
                result.draft().operationType(),
                result.draft().canSubmit() ? "success" : "warning",
                result.draft().notes(),
                result.draft().warnings().isEmpty() ? result.draft().suggestedActions() : result.draft().warnings(),
                List.of(),
                null,
                null,
                result.draft()
            ));
        }
        return blocks;
    }

    private String formatMoney(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record TaskContext(
        String taskTitle,
        String taskIntent,
        AgentDto.AlertDashboardDto alerts,
        AgentDto.ReconciliationFollowupDto reconciliation,
        AgentDto.ReportInsightDto insight,
        List<ReportDto.LowStockProductReportDto> lowStockProducts,
        List<ReportDto.TopSellingProductReportDto> topProducts,
        List<ReportDto.CustomerSalesReportDto> topCustomers,
        AgentDto.OperationDraftDto draft
    ) {}
}

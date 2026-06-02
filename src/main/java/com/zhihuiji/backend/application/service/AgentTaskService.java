package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.api.dto.agent.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final ExecutorService agentTaskExecutor;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final CurrentOwnerService currentOwnerService;
    private static final long TASK_TIMEOUT_SECONDS = 120L;

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
        @Qualifier("agentTaskExecutor") ExecutorService agentTaskExecutor,
        CurrentOwnerService currentOwnerService
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
        this.currentOwnerService = currentOwnerService;
    }

    public AgentTaskDtos.AgentTaskSummaryDto submitTask(String taskType, String title, String input) {
        AgentTaskEntity task = new AgentTaskEntity();
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        task.setOwnerUserId(ownerUserId);
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
        agentTaskExecutor.submit(() -> executeTask(saved.getId(), ownerUserId));
        return toSummary(saved);
    }

    public List<AgentTaskDtos.AgentTaskSummaryDto> listTasks() {
        return agentTaskRepository.findTop20ByOwnerUserIdOrderByCreatedAtDesc(currentOwnerService.requireCurrentOwnerUserId()).stream()
            .map(this::toSummary)
            .toList();
    }

    public AgentTaskDtos.AgentTaskDetailDto getTask(Long taskId) {
        AgentTaskEntity task = agentTaskRepository.findByIdAndOwnerUserId(taskId, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("agent task not found"));
        return new AgentTaskDtos.AgentTaskDetailDto(
            toSummary(task),
            task.getInputText(),
            parseResult(task.getResultJson())
        );
    }

    public List<AgentTaskDtos.AgentNotificationDto> listNotifications(boolean unreadOnly, boolean undeliveredOnly) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<AgentNotificationEntity> rows;
        if (unreadOnly && undeliveredOnly) {
            rows = notificationRepository.findTop30ByOwnerUserIdAndIsReadFalseAndIsDeliveredFalseOrderByCreatedAtDesc(ownerUserId);
        } else if (unreadOnly) {
            rows = notificationRepository.findTop30ByOwnerUserIdAndIsReadFalseOrderByCreatedAtDesc(ownerUserId);
        } else if (undeliveredOnly) {
            rows = notificationRepository.findTop30ByOwnerUserIdAndIsDeliveredFalseOrderByCreatedAtDesc(ownerUserId);
        } else {
            rows = notificationRepository.findTop30ByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        }
        return rows.stream().map(this::toNotificationDto).toList();
    }

    public AgentTaskDtos.AgentNotificationDto markNotificationRead(Long notificationId) {
        AgentNotificationEntity entity = notificationRepository.findByIdAndOwnerUserId(notificationId, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        entity.setIsRead(true);
        return toNotificationDto(notificationRepository.save(entity));
    }

    public AgentTaskDtos.AgentNotificationDto markNotificationDelivered(Long notificationId) {
        AgentNotificationEntity entity = notificationRepository.findByIdAndOwnerUserId(notificationId, currentOwnerService.requireCurrentOwnerUserId())
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
        Long ownerUserId = currentOwnerService.requireLegacyOwnerUserId();
        Optional<AgentTaskEntity> active = agentTaskRepository.findFirstByOwnerUserIdAndTaskTypeAndStatusInOrderByCreatedAtDesc(
            ownerUserId,
            TASK_ANOMALY,
            Set.of(STATUS_QUEUED, STATUS_RUNNING)
        );
        if (active.isPresent()) {
            return;
        }
        AgentTaskEntity task = new AgentTaskEntity();
        task.setOwnerUserId(ownerUserId);
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
        agentTaskExecutor.submit(() -> {
            try {
                agentTaskExecutor.submit(() -> executeTask(saved.getId(), ownerUserId))
                    .get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException tex) {
                markTaskFailed(saved.getId(), ownerUserId, "任务执行超时(" + TASK_TIMEOUT_SECONDS + "s)");
            } catch (Exception ex) {
                markTaskFailed(saved.getId(), ownerUserId, ex.getMessage());
            }
        });
    }

    private void markTaskFailed(Long taskId, Long ownerUserId, String reason) {
        AgentTaskEntity task = agentTaskRepository.findByIdAndOwnerUserId(taskId, ownerUserId).orElse(null);
        if (task != null && !STATUS_COMPLETED.equals(task.getStatus()) && !STATUS_FAILED.equals(task.getStatus())) {
            task.setStatus(STATUS_FAILED);
            task.setProgress(100);
            task.setCompletedAt(System.currentTimeMillis());
            task.setUpdatedAt(System.currentTimeMillis());
            task.setResultJson(toJson(failureResult(task, new RuntimeException(reason))));
            agentTaskRepository.save(task);
        }
    }

    private void executeTask(Long taskId) {
        executeTask(taskId, currentOwnerService.requireCurrentOwnerUserId());
    }

    private void executeTask(Long taskId, Long ownerUserId) {
        AgentTaskEntity task = agentTaskRepository.findByIdAndOwnerUserId(taskId, ownerUserId).orElse(null);
        if (task == null) {
            return;
        }
        try {
            task.setStatus(STATUS_RUNNING);
            task.setProgress(20);
            task.setUpdatedAt(System.currentTimeMillis());
            agentTaskRepository.save(task);

            AgentTaskDtos.AgentTaskResultDto result = buildTaskResult(task);
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

    private AgentTaskDtos.AgentTaskResultDto buildTaskResult(AgentTaskEntity task) {
        return switch (task.getTaskType()) {
            case TASK_RECONCILIATION -> buildReconciliationResult(task.getInputText());
            case TASK_REPORT -> buildSalesReportResult(task.getInputText());
            case TASK_QUESTION -> buildQuestionResult(task.getInputText());
            case TASK_DRAFT -> buildDraftResult(task.getInputText());
            case TASK_ANOMALY -> buildAnomalyResult();
            default -> buildQuestionResult(task.getInputText());
        };
    }

    private AgentTaskDtos.AgentTaskResultDto buildAnomalyResult() {
        AlertDtos.AlertDashboardDto alerts = agentService.getAlerts(8, 15);
        ReconciliationDtos.ReconciliationFollowupDto reconciliation = agentService.getReconciliationFollowup(6, 15);
        ReconciliationDtos.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        List<ReportDto.LowStockProductReportDto> lowStocks = reportService.lowStockProducts(6);
        OperationDraftDtos.OperationDraftDto draft = llmDrivenAgentService.draftOperation(
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
        AgentTaskDtos.AgentTaskResultDto fallback = new AgentTaskDtos.AgentTaskResultDto(
            "异常巡检完成",
            "服务端定时任务",
            "已完成库存、账龄和退款风险巡检，建议优先处理高严重度预警。",
            List.of(
                new AgentTaskDtos.AgentTaskMetricDto("高风险提醒", String.valueOf(alerts.alerts().stream().filter(a -> "high".equals(a.severity())).count()), "", "high"),
                new AgentTaskDtos.AgentTaskMetricDto("低库存商品", String.valueOf(lowStocks.size()), "", "medium"),
                new AgentTaskDtos.AgentTaskMetricDto("待催收金额", formatMoney(reconciliation.totalReceivable()), "", "medium")
            ),
            List.of(
                new AgentTaskDtos.AgentTaskSectionDto("处理顺序", insight.narrative(), alerts.alerts().stream().limit(4).map(AlertDtos.AlertDto::recommendedAction).toList())
            ),
            List.of(
                new AgentTaskDtos.AgentTaskTableDto(
                    "重点预警",
                    List.of("类型", "标题", "对象", "建议"),
                    alerts.alerts().stream().limit(6)
                        .map(alert -> List.of(alert.type(), alert.title(), safeText(alert.entityName(), "-"), alert.recommendedAction()))
                        .toList()
                )
            ),
            List.of(
                new AgentTaskDtos.AgentTaskChartDto(
                    "低库存缺口",
                    "bar",
                    lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::productName).toList(),
                    List.of(
                        new AgentTaskDtos.AgentTaskChartSeriesDto("当前库存", lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::stock).toList()),
                        new AgentTaskDtos.AgentTaskChartSeriesDto("安全库存", lowStocks.stream().limit(6).map(ReportDto.LowStockProductReportDto::safeStock).toList())
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

    private AgentTaskDtos.AgentTaskResultDto buildReconciliationResult(String input) {
        ReconciliationDtos.ReconciliationFollowupDto reconciliation = agentService.getReconciliationFollowup(8, 15);
        ReconciliationDtos.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        TaskContext context = new TaskContext(
            "对账催办深度分析",
            safeText(input, "分析应收应付与异常账龄"),
            new AlertDtos.AlertDashboardDto(List.of()),
            reconciliation,
            insight,
            List.of(),
            List.of(),
            List.of(),
            null
        );
        AgentTaskDtos.AgentTaskResultDto fallback = new AgentTaskDtos.AgentTaskResultDto(
            "对账催办分析",
            "应收、应付与账龄风险",
            "已汇总待催收客户、待付款供应商和异常账龄订单。",
            List.of(
                new AgentTaskDtos.AgentTaskMetricDto("待催收总额", formatMoney(reconciliation.totalReceivable()), "", "high"),
                new AgentTaskDtos.AgentTaskMetricDto("待付款总额", formatMoney(reconciliation.totalPayable()), "", "medium"),
                new AgentTaskDtos.AgentTaskMetricDto("异常账龄数", String.valueOf(reconciliation.agingRisks().size()), "", "medium")
            ),
            List.of(
                new AgentTaskDtos.AgentTaskSectionDto(
                    "核心结论",
                    "当前对账风险主要集中在高余额客户和长账龄订单。",
                    reconciliation.agingRisks().stream().limit(4).map(ReconciliationDtos.AgingRiskDto::suggestedAction).toList()
                )
            ),
            List.of(
                new AgentTaskDtos.AgentTaskTableDto(
                    "待催收客户",
                    List.of("客户", "电话", "应收"),
                    reconciliation.receivableCustomers().stream()
                        .limit(6)
                        .map(item -> List.of(item.name(), safeText(item.phone(), "-"), formatMoney(item.amount())))
                        .toList()
                ),
                new AgentTaskDtos.AgentTaskTableDto(
                    "异常账龄",
                    List.of("对象", "单号", "天数", "金额"),
                    reconciliation.agingRisks().stream()
                        .limit(6)
                        .map(item -> List.of(item.name(), item.orderNo(), String.valueOf(item.ageDays()), formatMoney(item.amount())))
                        .toList()
                )
            ),
            List.of(
                new AgentTaskDtos.AgentTaskChartDto(
                    "应收与应付对比",
                    "bar",
                    List.of("应收", "应付", "净现金流"),
                    List.of(new AgentTaskDtos.AgentTaskChartSeriesDto(
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

    private AgentTaskDtos.AgentTaskResultDto buildSalesReportResult(String input) {
        long now = System.currentTimeMillis();
        long start = startOfDay(now - 6 * DAY_MS);
        long prevStart = start - 7 * DAY_MS;
        ReconciliationDtos.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
        List<ReportDto.TopSellingProductReportDto> topProducts = reportService.topProducts(start, now, 6);
        List<ReportDto.CustomerSalesReportDto> topCustomers = reportService.customerSales(start, now, 6);
        List<Double> salesSeries = dailySalesSeries(start, now);
        List<Double> previousSeries = dailySalesSeries(prevStart, start - 1);
        TaskContext context = new TaskContext(
            "销售趋势复盘",
            safeText(input, "复盘近 7 天销售表现"),
            new AlertDtos.AlertDashboardDto(List.of()),
            agentService.getReconciliationFollowup(6, 15),
            insight,
            List.of(),
            topProducts,
            topCustomers,
            null
        );
        AgentTaskDtos.AgentTaskResultDto fallback = new AgentTaskDtos.AgentTaskResultDto(
            "销售趋势复盘",
            "近 7 天销售与客户贡献",
            insight.narrative(),
            List.of(
                new AgentTaskDtos.AgentTaskMetricDto("本期销售额", formatMoney(insight.currentSales()), formatPercent(insight.salesChangeRate()), "high"),
                new AgentTaskDtos.AgentTaskMetricDto("主力商品", insight.leadingProductName(), formatMoney(insight.leadingProductAmount()), "medium"),
                new AgentTaskDtos.AgentTaskMetricDto("主力客户", insight.leadingCustomerName(), formatMoney(insight.leadingCustomerAmount()), "medium")
            ),
            List.of(
                new AgentTaskDtos.AgentTaskSectionDto("趋势判断", insight.narrative(), insight.highlights())
            ),
            List.of(
                new AgentTaskDtos.AgentTaskTableDto(
                    "主力商品",
                    List.of("商品", "数量", "金额"),
                    topProducts.stream()
                        .map(item -> List.of(item.productName(), formatNumber(item.totalQuantity()), formatMoney(item.totalAmount())))
                        .toList()
                ),
                new AgentTaskDtos.AgentTaskTableDto(
                    "客户贡献",
                    List.of("客户", "订单数", "金额"),
                    topCustomers.stream()
                        .map(item -> List.of(item.customerName(), String.valueOf(item.totalOrders()), formatMoney(item.totalAmount())))
                        .toList()
                )
            ),
            List.of(
                new AgentTaskDtos.AgentTaskChartDto(
                    "近 7 天销售走势",
                    "line",
                    dayLabels(start, 7),
                    List.of(
                        new AgentTaskDtos.AgentTaskChartSeriesDto("本期", salesSeries),
                        new AgentTaskDtos.AgentTaskChartSeriesDto("上期", previousSeries)
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

    private AgentTaskDtos.AgentTaskResultDto buildQuestionResult(String input) {
        AnswerDtos.AgentAnswerDto answer = llmDrivenAgentService.answerQuestion(safeText(input, "当前最值得关注的经营问题是什么"));
        ReconciliationDtos.ReportInsightDto insight = llmDrivenAgentService.getReportInsight(7);
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
        AgentTaskDtos.AgentTaskResultDto fallback = new AgentTaskDtos.AgentTaskResultDto(
            "经营问题分析",
            answer.intent(),
            answer.answer(),
            List.of(new AgentTaskDtos.AgentTaskMetricDto("问题类型", answer.intent(), "", "medium")),
            List.of(new AgentTaskDtos.AgentTaskSectionDto("结论", answer.answer(), answer.highlights())),
            answer.columns().isEmpty() ? List.of() : List.of(new AgentTaskDtos.AgentTaskTableDto("数据明细", answer.columns(), answer.rows())),
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

    private AgentTaskDtos.AgentTaskResultDto buildDraftResult(String input) {
        OperationDraftDtos.OperationDraftDto draft = llmDrivenAgentService.draftOperation(safeText(input, "根据当前数据生成一条采购草稿"));
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
        AgentTaskDtos.AgentTaskResultDto fallback = new AgentTaskDtos.AgentTaskResultDto(
            "单据草稿分析",
            draft.operationType(),
            draft.summary(),
            List.of(
                new AgentTaskDtos.AgentTaskMetricDto("可提交", draft.canSubmit() ? "是" : "否", "", draft.canSubmit() ? "success" : "high"),
                new AgentTaskDtos.AgentTaskMetricDto("明细数量", String.valueOf(draft.items().size()), "", "medium")
            ),
            List.of(
                new AgentTaskDtos.AgentTaskSectionDto("草稿说明", draft.summary(), draft.suggestedActions())
            ),
            draft.items().isEmpty()
                ? List.of()
                : List.of(new AgentTaskDtos.AgentTaskTableDto(
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
                : List.of(new AgentTaskDtos.AgentTaskChartDto(
                    "草稿商品库存对比",
                    "bar",
                    draft.items().stream().map(OperationDraftDtos.OperationDraftItemDto::productName).toList(),
                    List.of(
                        new AgentTaskDtos.AgentTaskChartSeriesDto("拟操作数量", draft.items().stream().map(OperationDraftDtos.OperationDraftItemDto::quantity).toList()),
                        new AgentTaskDtos.AgentTaskChartSeriesDto("当前库存", draft.items().stream().map(OperationDraftDtos.OperationDraftItemDto::currentStock).toList())
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

    private AgentTaskDtos.AgentTaskResultDto llmTaskResult(String systemPrompt, String userPrompt, AgentTaskDtos.AgentTaskResultDto fallback) {
        return agentLlmService.requestStructuredJson(systemPrompt, userPrompt)
            .map(node -> mergeTaskResult(node, fallback))
            .map(this::ensureRenderBlocks)
            .orElse(fallback);
    }

    private AgentTaskDtos.AgentTaskResultDto mergeTaskResult(JsonNode node, AgentTaskDtos.AgentTaskResultDto fallback) {
        return new AgentTaskDtos.AgentTaskResultDto(
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

    private void publishNotification(AgentTaskEntity task, AgentTaskDtos.AgentTaskResultDto result) {
        AgentNotificationEntity notification = new AgentNotificationEntity();
        notification.setOwnerUserId(task.getOwnerUserId());
        notification.setTaskId(task.getId());
        notification.setTitle(task.getTitle());
        notification.setBody(safeText(result.summary(), "任务已完成"));
        notification.setLevel(STATUS_FAILED.equals(task.getStatus()) ? "error" : TASK_ANOMALY.equals(task.getTaskType()) ? "warning" : "info");
        notification.setIsRead(false);
        notification.setIsDelivered(false);
        notification.setCreatedAt(System.currentTimeMillis());
        AgentNotificationEntity saved = notificationRepository.save(notification);
        AgentTaskDtos.AgentNotificationDto payload = toNotificationDto(saved);
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("agent-notification").data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        });
    }

    private AgentTaskDtos.AgentTaskResultDto failureResult(AgentTaskEntity task, Exception ex) {
        return new AgentTaskDtos.AgentTaskResultDto(
            task.getTitle(),
            "执行失败",
            safeText(ex.getMessage(), "任务执行失败"),
            List.of(),
            List.of(new AgentTaskDtos.AgentTaskSectionDto("失败原因", safeText(ex.getMessage(), "未知错误"), List.of())),
            List.of(),
            List.of(),
            List.of("稍后重试，或缩小分析范围。"),
            null,
            List.of()
        );
    }

    private AgentTaskDtos.AgentTaskSummaryDto toSummary(AgentTaskEntity entity) {
        return new AgentTaskDtos.AgentTaskSummaryDto(
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

    private AgentTaskDtos.AgentNotificationDto toNotificationDto(AgentNotificationEntity entity) {
        return new AgentTaskDtos.AgentNotificationDto(
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

    private AgentTaskDtos.AgentTaskResultDto parseResult(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, AgentTaskDtos.AgentTaskResultDto.class);
        } catch (Exception ex) {
            return new AgentTaskDtos.AgentTaskResultDto(
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

    private List<AgentTaskDtos.AgentTaskMetricDto> readMetrics(JsonNode node, List<AgentTaskDtos.AgentTaskMetricDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentTaskDtos.AgentTaskMetricDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentTaskDtos.AgentTaskMetricDto(
            readText(item, "label", ""),
            readText(item, "value", ""),
            readText(item, "delta", ""),
            readText(item, "emphasis", "medium")
        )));
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentTaskDtos.AgentTaskSectionDto> readSections(JsonNode node, List<AgentTaskDtos.AgentTaskSectionDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentTaskDtos.AgentTaskSectionDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentTaskDtos.AgentTaskSectionDto(
            readText(item, "title", ""),
            readText(item, "narrative", ""),
            readStringList(item.path("bullets"), List.of())
        )));
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentTaskDtos.AgentTaskTableDto> readTables(JsonNode node, List<AgentTaskDtos.AgentTaskTableDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentTaskDtos.AgentTaskTableDto> values = new ArrayList<>();
        node.forEach(item -> values.add(new AgentTaskDtos.AgentTaskTableDto(
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

    private List<AgentTaskDtos.AgentTaskChartDto> readCharts(JsonNode node, List<AgentTaskDtos.AgentTaskChartDto> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentTaskDtos.AgentTaskChartDto> values = new ArrayList<>();
        node.forEach(item -> {
            List<AgentTaskDtos.AgentTaskChartSeriesDto> series = new ArrayList<>();
            item.path("series").forEach(seriesNode -> {
                List<Double> numeric = new ArrayList<>();
                seriesNode.path("values").forEach(v -> numeric.add(v.asDouble(0.0)));
                series.add(new AgentTaskDtos.AgentTaskChartSeriesDto(
                    readText(seriesNode, "name", "series"),
                    numeric
                ));
            });
            values.add(new AgentTaskDtos.AgentTaskChartDto(
                readText(item, "title", ""),
                readText(item, "chartType", "bar"),
                readStringList(item.path("categories"), List.of()),
                series
            ));
        });
        return values.isEmpty() ? fallback : values;
    }

    private List<AgentTaskDtos.AgentRenderBlockDto> readRenderBlocks(
        JsonNode node,
        List<AgentTaskDtos.AgentRenderBlockDto> fallback
    ) {
        if (!node.isArray()) {
            return fallback;
        }
        List<AgentTaskDtos.AgentRenderBlockDto> blocks = new ArrayList<>();
        node.forEach(item -> blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
            readText(item, "type", ""),
            readText(item, "title", ""),
            readText(item, "subtitle", ""),
            readText(item, "tone", "primary"),
            readText(item, "text", ""),
            readStringList(item.path("bullets"), List.of()),
            readMetrics(item.path("metrics"), List.of()),
            readBlockTable(item.path("table")),
            readBlockChart(item.path("chart")),
            fallback.isEmpty() ? null : fallback.stream().map(AgentTaskDtos.AgentRenderBlockDto::draft).filter(java.util.Objects::nonNull).findFirst().orElse(null)
        )));
        return blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList().isEmpty()
            ? fallback
            : blocks.stream().filter(block -> StringUtils.hasText(block.type())).toList();
    }

    private AgentTaskDtos.AgentTaskTableDto readBlockTable(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        return new AgentTaskDtos.AgentTaskTableDto(
            readText(node, "title", ""),
            readStringList(node.path("columns"), List.of()),
            readRows(node.path("rows"))
        );
    }

    private AgentTaskDtos.AgentTaskChartDto readBlockChart(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        List<AgentTaskDtos.AgentTaskChartSeriesDto> series = new ArrayList<>();
        node.path("series").forEach(seriesNode -> {
            List<Double> numeric = new ArrayList<>();
            seriesNode.path("values").forEach(v -> numeric.add(v.asDouble(0.0)));
            series.add(new AgentTaskDtos.AgentTaskChartSeriesDto(
                readText(seriesNode, "name", "series"),
                numeric
            ));
        });
        return new AgentTaskDtos.AgentTaskChartDto(
            readText(node, "title", ""),
            readText(node, "chartType", "bar"),
            readStringList(node.path("categories"), List.of()),
            series
        );
    }

    private AgentTaskDtos.AgentTaskResultDto ensureRenderBlocks(AgentTaskDtos.AgentTaskResultDto result) {
        if (result.renderBlocks() != null && !result.renderBlocks().isEmpty()) {
            return result;
        }
        return new AgentTaskDtos.AgentTaskResultDto(
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

    private List<AgentTaskDtos.AgentRenderBlockDto> buildRenderBlocks(AgentTaskDtos.AgentTaskResultDto result) {
        List<AgentTaskDtos.AgentRenderBlockDto> blocks = new ArrayList<>();
        blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
            blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
        result.sections().forEach(section -> blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
        result.tables().forEach(table -> blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
        result.charts().forEach(chart -> blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
            blocks.add(new AgentTaskDtos.AgentRenderBlockDto(
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
        AlertDtos.AlertDashboardDto alerts,
        ReconciliationDtos.ReconciliationFollowupDto reconciliation,
        ReconciliationDtos.ReportInsightDto insight,
        List<ReportDto.LowStockProductReportDto> lowStockProducts,
        List<ReportDto.TopSellingProductReportDto> topProducts,
        List<ReportDto.CustomerSalesReportDto> topCustomers,
        OperationDraftDtos.OperationDraftDto draft
    ) {}
}

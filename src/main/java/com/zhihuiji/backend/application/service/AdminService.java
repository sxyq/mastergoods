package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.zhihuiji.backend.api.dto.agent.AgentDto;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile("local")
public class AdminService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AgentNotificationRepository agentNotificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoDataService demoDataService;
    private final LlmDrivenAgentService llmDrivenAgentService;
    private final AgentTaskService agentTaskService;

    public AdminService(
        UserRepository userRepository,
        SessionRepository sessionRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        SaleOrderRepository saleOrderRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        AgentTaskRepository agentTaskRepository,
        AgentNotificationRepository agentNotificationRepository,
        PasswordEncoder passwordEncoder,
        DemoDataService demoDataService,
        LlmDrivenAgentService llmDrivenAgentService,
        AgentTaskService agentTaskService
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agentNotificationRepository = agentNotificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoDataService = demoDataService;
        this.llmDrivenAgentService = llmDrivenAgentService;
        this.agentTaskService = agentTaskService;
    }

    public AdminSummary summary() {
        return new AdminSummary(
            userRepository.count(),
            productRepository.count(),
            customerRepository.count(),
            supplierRepository.count(),
            saleOrderRepository.count(),
            purchaseOrderRepository.count(),
            agentTaskRepository.count(),
            agentNotificationRepository.findTop30ByIsReadFalseOrderByCreatedAtDesc().size()
        );
    }

    public List<UserItem> listUsers(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
            .filter(user -> normalized.isBlank()
                || user.getPhone().toLowerCase(Locale.ROOT).contains(normalized)
                || user.getNickname().toLowerCase(Locale.ROOT).contains(normalized))
            .sorted(Comparator.comparingLong(UserEntity::getCreatedAt).reversed())
            .map(this::toUserItem)
            .toList();
    }

    @Transactional
    public UserItem createUser(CreateUserRequest request) {
        validateCreateRequest(request);
        if (userRepository.findByPhone(request.phone().trim()).isPresent()) {
            throw new IllegalArgumentException("phone already registered");
        }
        long now = System.currentTimeMillis();
        UserEntity user = new UserEntity();
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setNickname(request.nickname().trim());
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return toUserItem(userRepository.save(user));
    }

    @Transactional
    public UserItem updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (StringUtils.hasText(request.nickname())) {
            user.setNickname(request.nickname().trim());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
            if (!request.keepSessions()) {
                sessionRepository.findAll().stream()
                    .filter(session -> userId.equals(session.getUserId()) && Boolean.TRUE.equals(session.getIsActive()))
                    .forEach(session -> session.setIsActive(false));
            }
        }
        user.setUpdatedAt(System.currentTimeMillis());
        return toUserItem(userRepository.save(user));
    }

    public DemoDataService.SeedResult seedDemoData(boolean reset) {
        return demoDataService.seed(reset);
    }

    public AgentSmokeResult runAgentSmoke() {
        AgentDto.AgentWorkbenchDto workbench = llmDrivenAgentService.getWorkbench(7, 6, 15);
        AgentDto.AgentAnswerDto answer = llmDrivenAgentService.answerQuestion("按紧急程度告诉我现在该先补哪些货，并说下原因。");
        AgentDto.OperationDraftDto draft = llmDrivenAgentService.draftOperation("帮我给 supplier-a 入库 20 个 sensor S7，单价 35。");
        AgentDto.AgentTaskSummaryDto task = agentTaskService.submitTask(
            "sales_report_deep_dive",
            "后台报表复盘 smoke",
            "请复盘近 7 天销售趋势、客户贡献、利润驱动和补货机会。"
        );
        AgentDto.AgentTaskDetailDto detail = waitForTask(task.id(), 5);
        String taskSummary = detail.result() == null
            ? "任务已提交，结果将在后台继续生成。"
            : detail.result().summary();
        return new AgentSmokeResult(
            workbench.reportInsight().narrative(),
            answer.answer(),
            draft.summary(),
            draft.canSubmit(),
            detail.task().status(),
            taskSummary
        );
    }

    private AgentDto.AgentTaskDetailDto waitForTask(Long taskId, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            AgentDto.AgentTaskDetailDto detail = agentTaskService.getTask(taskId);
            if ("completed".equals(detail.task().status()) || "failed".equals(detail.task().status())) {
                return detail;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return agentTaskService.getTask(taskId);
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(request.phone())) {
            throw new IllegalArgumentException("phone is required");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("password is required");
        }
        if (!StringUtils.hasText(request.nickname())) {
            throw new IllegalArgumentException("nickname is required");
        }
    }

    private UserItem toUserItem(UserEntity user) {
        long activeSessions = sessionRepository.findAll().stream()
            .filter(session -> user.getId().equals(session.getUserId()) && Boolean.TRUE.equals(session.getIsActive()))
            .count();
        return new UserItem(
            user.getId(),
            user.getPhone(),
            user.getNickname(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            activeSessions
        );
    }

    public record AdminSummary(
        long userCount,
        long productCount,
        long customerCount,
        long supplierCount,
        long saleOrderCount,
        long purchaseOrderCount,
        long agentTaskCount,
        long unreadNotificationCount
    ) {}

    public record UserItem(
        Long id,
        String phone,
        String nickname,
        Integer status,
        long createdAt,
        long updatedAt,
        long activeSessions
    ) {}

    public record CreateUserRequest(String phone, String password, String nickname, Integer status) {}

    public record UpdateUserRequest(
        String nickname,
        Integer status,
        String password,
        @JsonAlias("keepSessions") boolean keepSessions
    ) {}

    public record AgentSmokeResult(
        String workbenchNarrative,
        String answerSummary,
        String draftSummary,
        boolean draftCanSubmit,
        String taskStatus,
        String taskSummary
    ) {}
}

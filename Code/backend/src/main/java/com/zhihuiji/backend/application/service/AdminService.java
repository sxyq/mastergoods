package com.zhihuiji.backend.application.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.zhihuiji.backend.api.dto.agent.*;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentNotificationRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentTaskRepository;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import com.zhihuiji.backend.infrastructure.repository.PurchaseOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SaleOrderRepository;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        DemoDataService demoDataService
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
            agentNotificationRepository.countByIsReadFalse()
        );
    }

    public List<UserItem> listUsers(String keyword) {
        List<UserEntity> users = userRepository.searchByKeyword(keyword);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = new ArrayList<>(users.size());
        for (UserEntity user : users) {
            userIds.add(user.getId());
        }
        List<SessionRepository.ActiveSessionCount> activeCounts = sessionRepository.countActiveSessionsByUserIds(userIds);
        Map<Long, Long> activeSessionCounts = new HashMap<>(activeCounts.size());
        for (SessionRepository.ActiveSessionCount activeCount : activeCounts) {
            activeSessionCounts.put(
                activeCount.getUserId(),
                activeCount.getActiveCount() == null ? 0L : activeCount.getActiveCount()
            );
        }
        List<UserItem> results = new ArrayList<>(users.size());
        for (UserEntity user : users) {
            results.add(toUserItem(user, activeSessionCounts.getOrDefault(user.getId(), 0L)));
        }
        return results;
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
                sessionRepository.findByUserIdAndIsActiveTrue(userId)
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
        return new AgentSmokeResult(
            "Legacy admin smoke is disabled.",
            "No static answer is generated from this endpoint.",
            "No draft is generated from this endpoint.",
            false,
            "disabled",
            "Use authenticated /v2/agent/chat or /v2/agent/chat/stream with real owner-scoped data."
        );
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
        return toUserItem(user, sessionRepository.countByUserIdAndIsActiveTrue(user.getId()));
    }

    private UserItem toUserItem(UserEntity user, long activeSessions) {
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

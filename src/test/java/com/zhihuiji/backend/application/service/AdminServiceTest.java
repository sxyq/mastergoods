package com.zhihuiji.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.SessionEntity;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SaleOrderRepository saleOrderRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private AgentTaskRepository agentTaskRepository;
    @Mock
    private AgentNotificationRepository agentNotificationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DemoDataService demoDataService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(
            userRepository,
            sessionRepository,
            productRepository,
            customerRepository,
            supplierRepository,
            saleOrderRepository,
            purchaseOrderRepository,
            agentTaskRepository,
            agentNotificationRepository,
            passwordEncoder,
            demoDataService
        );
    }

    @Test
    void createUserRejectsDuplicatePhone() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(new UserEntity()));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> adminService.createUser(new AdminService.CreateUserRequest("13800138000", "123456", "管理员", 1))
        );

        assertEquals("phone already registered", ex.getMessage());
    }

    @Test
    void createUserTrimsInputAndDefaultsStatus() {
        when(userRepository.findByPhone("13800138001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("ENCODED");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            setId(entity, 9L);
            return entity;
        });
        when(sessionRepository.findAll()).thenReturn(List.of());

        AdminService.UserItem user = adminService.createUser(
            new AdminService.CreateUserRequest(" 13800138001 ", " 123456 ", " 仓库经理 ", null)
        );

        assertEquals(9L, user.id());
        assertEquals("13800138001", user.phone());
        assertEquals("仓库经理", user.nickname());
        assertEquals(1, user.status());
        assertEquals(0L, user.activeSessions());
    }

    @Test
    void updateUserInvalidatesSessionsWhenKeepSessionsDisabled() {
        UserEntity user = new UserEntity();
        setId(user, 5L);
        user.setPhone("13800138002");
        user.setNickname("旧昵称");
        user.setPasswordHash("OLD");
        user.setStatus(1);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());

        SessionEntity active = new SessionEntity();
        setId(active, 1L);
        active.setUserId(5L);
        active.setIsActive(true);
        active.setToken("token-a");
        active.setRefreshToken("refresh-a");
        active.setExpiresAt(System.currentTimeMillis() + 1000);
        active.setCreatedAt(System.currentTimeMillis());

        SessionEntity otherUser = new SessionEntity();
        setId(otherUser, 2L);
        otherUser.setUserId(6L);
        otherUser.setIsActive(true);
        otherUser.setToken("token-b");
        otherUser.setRefreshToken("refresh-b");
        otherUser.setExpiresAt(System.currentTimeMillis() + 1000);
        otherUser.setCreatedAt(System.currentTimeMillis());

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("654321")).thenReturn("ENCODED");
        when(sessionRepository.findAll()).thenReturn(List.of(active, otherUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminService.UserItem result = adminService.updateUser(
            5L,
            new AdminService.UpdateUserRequest("新昵称", 0, "654321", false)
        );

        assertEquals("新昵称", result.nickname());
        assertEquals(0, result.status());
        assertFalse(active.getIsActive());
        assertTrue(otherUser.getIsActive());
    }

    @Test
    void updateUserKeepsSessionsWhenRequested() {
        UserEntity user = new UserEntity();
        setId(user, 7L);
        user.setPhone("13800138003");
        user.setNickname("仓库值班");
        user.setPasswordHash("OLD");
        user.setStatus(1);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());

        SessionEntity active = new SessionEntity();
        setId(active, 3L);
        active.setUserId(7L);
        active.setIsActive(true);
        active.setToken("token-c");
        active.setRefreshToken("refresh-c");
        active.setExpiresAt(System.currentTimeMillis() + 1000);
        active.setCreatedAt(System.currentTimeMillis());

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("777777")).thenReturn("ENCODED-KEEP");
        when(sessionRepository.findAll()).thenReturn(List.of(active));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminService.UserItem result = adminService.updateUser(
            7L,
            new AdminService.UpdateUserRequest("仓库值班", 1, "777777", true)
        );

        assertEquals(1, result.status());
        assertTrue(active.getIsActive());
    }

    @Test
    void createUserRejectsMissingNickname() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> adminService.createUser(new AdminService.CreateUserRequest("13800138004", "123456", " ", 1))
        );

        assertEquals("nickname is required", ex.getMessage());
    }

    private void setId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void setId(SessionEntity session, Long id) {
        try {
            var field = SessionEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(session, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.store.V2StoreDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.SessionEntity;
import com.zhihuiji.backend.domain.entity.StoreEntity;
import com.zhihuiji.backend.domain.entity.StoreMembershipEntity;
import com.zhihuiji.backend.domain.entity.UserEntity;
import com.zhihuiji.backend.infrastructure.repository.SessionRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreMembershipRepository;
import com.zhihuiji.backend.infrastructure.repository.StoreRepository;
import com.zhihuiji.backend.infrastructure.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class V2StoreServiceTest {
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreMembershipRepository storeMembershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2StoreService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2StoreService(
            storeRepository,
            storeMembershipRepository,
            userRepository,
            sessionRepository,
            passwordEncoder,
            currentOwnerService
        );
    }

    @Test
    void listMembersCountsActiveSessionsWithOneGroupedQuery() {
        long ownerUserId = 9L;
        long managerUserId = 10L;
        long targetUserId = 11L;
        StoreEntity store = store(ownerUserId);
        UserEntity owner = user(ownerUserId, "店长", 1);
        UserEntity manager = user(managerUserId, "店长助理", 1);
        UserEntity targetUser = user(targetUserId, "销售", 1);
        StoreMembershipEntity ownerMembership = membership(ownerUserId, ownerUserId, "OWNER", 1);
        StoreMembershipEntity managerMembership = membership(ownerUserId, managerUserId, "MANAGER", 1);
        StoreMembershipEntity targetMembership = membership(ownerUserId, targetUserId, "SALES", 1);
        List<Long> memberUserIds = List.of(ownerUserId, managerUserId, targetUserId);

        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(ownerUserId);
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));
        when(storeRepository.findByOwnerUserId(ownerUserId)).thenReturn(Optional.of(store));
        when(storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, ownerUserId))
            .thenReturn(Optional.of(ownerMembership));
        when(storeMembershipRepository.findByOwnerUserIdOrderByCreatedAtAsc(ownerUserId))
            .thenReturn(List.of(ownerMembership, managerMembership, targetMembership));
        when(userRepository.findAllById(memberUserIds)).thenReturn(List.of(owner, manager, targetUser));
        when(sessionRepository.countActiveSessionsByUserIds(memberUserIds))
            .thenReturn(List.of(new ActiveCount(ownerUserId, 1L), new ActiveCount(managerUserId, 2L)));

        List<V2StoreDtos.MemberResponse> responses = service.listMembers();

        assertEquals(3, responses.size());
        assertEquals(1L, responses.get(0).activeSessions());
        assertEquals(2L, responses.get(1).activeSessions());
        assertEquals(0L, responses.get(2).activeSessions());
        verify(sessionRepository).countActiveSessionsByUserIds(memberUserIds);
        verify(sessionRepository, never()).countByUserIdAndIsActiveTrue(any());
    }

    @Test
    void disablingMemberInvalidatesOnlyActiveSessionsForThatUser() {
        long ownerUserId = 9L;
        long managerUserId = 10L;
        long targetUserId = 11L;
        StoreEntity store = store(ownerUserId);
        UserEntity owner = user(ownerUserId, "店长", 1);
        UserEntity targetUser = user(targetUserId, "销售", 1);
        StoreMembershipEntity ownerMembership = membership(ownerUserId, ownerUserId, "OWNER", 1);
        StoreMembershipEntity managerMembership = membership(ownerUserId, managerUserId, "MANAGER", 1);
        StoreMembershipEntity targetMembership = membership(ownerUserId, targetUserId, "SALES", 1);
        SessionEntity activeSession = session(targetUserId);

        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(ownerUserId);
        when(currentOwnerService.requireCurrentUserId()).thenReturn(managerUserId);
        when(userRepository.findById(ownerUserId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findByOwnerUserId(ownerUserId)).thenReturn(Optional.of(store));
        when(storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, ownerUserId))
            .thenReturn(Optional.of(ownerMembership));
        when(storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, managerUserId))
            .thenReturn(Optional.of(managerMembership));
        when(storeMembershipRepository.findByOwnerUserIdAndUserId(ownerUserId, targetUserId))
            .thenReturn(Optional.of(targetMembership));
        when(storeMembershipRepository.save(any(StoreMembershipEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findByUserIdAndIsActiveTrue(targetUserId)).thenReturn(List.of(activeSession));

        V2StoreDtos.MemberResponse response = service.updateMember(
            targetUserId,
            new V2StoreDtos.MemberUpdateRequest(null, null, null, null, 0, true)
        );

        assertEquals(0, response.status());
        assertFalse(activeSession.getIsActive());
        verify(sessionRepository).findByUserIdAndIsActiveTrue(targetUserId);
        verify(sessionRepository, never()).findAll();
        verify(sessionRepository).saveAll(List.of(activeSession));
    }

    private static StoreEntity store(Long ownerUserId) {
        StoreEntity store = new StoreEntity();
        setId(store, 21L);
        store.setOwnerUserId(ownerUserId);
        store.setStoreName("智慧记总店");
        store.setStatus(1);
        store.setCreatedAt(1L);
        store.setUpdatedAt(1L);
        return store;
    }

    private static UserEntity user(Long id, String nickname, int status) {
        UserEntity user = new UserEntity();
        setId(user, id);
        user.setPhone("13800000000");
        user.setPasswordHash("hash");
        user.setNickname(nickname);
        user.setStatus(status);
        user.setCreatedAt(1L);
        user.setUpdatedAt(1L);
        return user;
    }

    private static StoreMembershipEntity membership(Long ownerUserId, Long userId, String roleCode, int status) {
        StoreMembershipEntity membership = new StoreMembershipEntity();
        setId(membership, userId);
        membership.setOwnerUserId(ownerUserId);
        membership.setStoreId(21L);
        membership.setUserId(userId);
        membership.setRoleCode(roleCode);
        membership.setTitle(roleCode);
        membership.setStatus(status);
        membership.setCreatedAt(1L);
        membership.setUpdatedAt(1L);
        return membership;
    }

    private static SessionEntity session(Long userId) {
        SessionEntity session = new SessionEntity();
        setId(session, 31L);
        session.setUserId(userId);
        session.setToken("access-token");
        session.setRefreshToken("refresh-token");
        session.setExpiresAt(System.currentTimeMillis() + 60_000L);
        session.setIsActive(true);
        session.setCreatedAt(1L);
        return session;
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private record ActiveCount(Long userId, Long activeCount) implements SessionRepository.ActiveSessionCount {
        @Override
        public Long getUserId() {
            return userId;
        }

        @Override
        public Long getActiveCount() {
            return activeCount;
        }
    }
}

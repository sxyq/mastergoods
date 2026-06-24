package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private V2PartnerGroupService partnerGroupService;
    @Mock
    private V2PartnerContactService partnerContactService;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2CustomerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2CustomerService(customerRepository, partnerGroupService, partnerContactService, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void listUsesRepositorySearchAndBatchLoadsGroupNames() {
        CustomerEntity customer = customer(10L, "客户A", "13800000000", 3L, 1);
        when(customerRepository.search(1L, "abc", 1, 3L)).thenReturn(List.of(customer));
        when(partnerGroupService.getOwnedEntityMap(eq(PartnerTypes.CUSTOMER), org.mockito.ArgumentMatchers.anyCollection()))
            .thenReturn(Map.of(3L, partnerGroup(3L, "核心客户")));
        when(partnerGroupService.getOwnedEntity(PartnerTypes.CUSTOMER, 3L))
            .thenReturn(partnerGroup(3L, "核心客户"));

        List<V2PartnerDtos.CustomerResponse> responses = service.list(" abc ", 1, 3L);

        assertEquals(1, responses.size());
        assertEquals("客户A", responses.get(0).name());
        assertEquals("核心客户", responses.get(0).groupName());
        verify(customerRepository).search(1L, "abc", 1, 3L);
        ArgumentCaptor<Collection<Long>> groupIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(partnerGroupService).getOwnedEntityMap(eq(PartnerTypes.CUSTOMER), groupIdsCaptor.capture());
        assertEquals(Set.of(3L), Set.copyOf(groupIdsCaptor.getValue()));
    }

    private static CustomerEntity customer(Long id, String name, String phone, Long groupId, Integer status) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setPhone(phone);
        entity.setLevel(1);
        entity.setGroupId(groupId);
        entity.setBalance(0.0);
        entity.setStatus(status);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        return entity;
    }

    private static PartnerGroupEntity partnerGroup(Long id, String name) {
        PartnerGroupEntity entity = new PartnerGroupEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setPartnerType(PartnerTypes.CUSTOMER);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(1);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}

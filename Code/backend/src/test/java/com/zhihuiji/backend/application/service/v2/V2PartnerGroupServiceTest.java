package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerGroupRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PartnerGroupServiceTest {
    @Mock
    private PartnerGroupRepository partnerGroupRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2PartnerGroupService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2PartnerGroupService(partnerGroupRepository, customerRepository, supplierRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void getOwnedEntityMapReturnsOnlyRequestedGroups() {
        PartnerGroupEntity first = partnerGroup(1L, "客户A");
        PartnerGroupEntity second = partnerGroup(2L, "客户B");
        when(partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeAndIdIn(1L, PartnerTypes.CUSTOMER, Set.of(1L, 2L)))
            .thenReturn(List.of(first, second));

        Map<Long, PartnerGroupEntity> result = service.getOwnedEntityMap(PartnerTypes.CUSTOMER, Set.of(1L, 2L));

        assertEquals(Set.of(1L, 2L), result.keySet());
        assertEquals("客户A", result.get(1L).getName());
        assertEquals("客户B", result.get(2L).getName());
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

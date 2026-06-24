package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2SupplierServiceTest {
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private V2PartnerGroupService partnerGroupService;
    @Mock
    private V2PartnerContactService partnerContactService;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2SupplierService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2SupplierService(supplierRepository, partnerGroupService, partnerContactService, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void listUsesRepositorySearchAndBatchLoadsGroupNames() {
        SupplierEntity supplier = supplier(20L, "供应商A", "13900000000", 5L, 1);
        when(supplierRepository.search(1L, "abc", 0, 5L)).thenReturn(List.of(supplier));
        when(partnerGroupService.getOwnedEntityMap(eq(PartnerTypes.SUPPLIER), org.mockito.ArgumentMatchers.anyCollection()))
            .thenReturn(Map.of(5L, partnerGroup(5L, "核心供应商")));
        when(partnerGroupService.getOwnedEntity(PartnerTypes.SUPPLIER, 5L))
            .thenReturn(partnerGroup(5L, "核心供应商"));

        List<V2PartnerDtos.SupplierResponse> responses = service.list(" abc ", 0, 5L);

        assertEquals(1, responses.size());
        assertEquals("供应商A", responses.get(0).name());
        assertEquals("核心供应商", responses.get(0).groupName());
        verify(supplierRepository).search(1L, "abc", 0, 5L);
        ArgumentCaptor<Collection<Long>> groupIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(partnerGroupService).getOwnedEntityMap(eq(PartnerTypes.SUPPLIER), groupIdsCaptor.capture());
        assertEquals(Set.of(5L), Set.copyOf(groupIdsCaptor.getValue()));
    }

    private static SupplierEntity supplier(Long id, String name, String phone, Long groupId, Integer status) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setName(name);
        entity.setPhone(phone);
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
        entity.setPartnerType(PartnerTypes.SUPPLIER);
        entity.setName(name);
        entity.setStatus(1);
        entity.setSortOrder(1);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}

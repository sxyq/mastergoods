package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerContactRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2PartnerContactServiceTest {
    @Mock
    private PartnerContactRepository partnerContactRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;

    private V2PartnerContactService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2PartnerContactService(partnerContactRepository, customerRepository, supplierRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
    }

    @Test
    void createPrimaryContactRefreshesCustomerSummary() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(20L);
        customer.setOwnerUserId(1L);
        customer.setName("客户A");
        customer.setPhone("13800000000");
        customer.setStatus(1);
        customer.setLevel(1);
        customer.setBalance(0.0);
        customer.setSyncStatus(0);
        customer.setSyncVersion(1L);
        customer.setCreatedAt(1L);
        customer.setUpdatedAt(1L);
        when(customerRepository.findByIdAndOwnerUserId(20L, 1L)).thenReturn(Optional.of(customer));
        when(partnerContactRepository.findAllByOwnerUserIdAndPartnerTypeAndPartnerIdOrderByIsPrimaryDescCreatedAtAsc(1L, PartnerTypes.CUSTOMER, 20L))
            .thenReturn(List.of());
        when(partnerContactRepository.findByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrue(1L, PartnerTypes.CUSTOMER, 20L))
            .thenReturn(Optional.of(savedPrimary()));
        when(partnerContactRepository.save(any(PartnerContactEntity.class))).thenAnswer(invocation -> {
            PartnerContactEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(99L);
            }
            return entity;
        });
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        V2PartnerDtos.PartnerContactResponse response = service.create(
            PartnerTypes.CUSTOMER,
            new V2PartnerDtos.PartnerContactWriteRequest(20L, "张三", "13900000000", "采购", true)
        );

        assertEquals(99L, response.id());
        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository).save(captor.capture());
        assertEquals("张三", captor.getValue().getContactName());
        assertEquals("13900000000", captor.getValue().getContactPhone());
    }

    private static PartnerContactEntity savedPrimary() {
        PartnerContactEntity entity = new PartnerContactEntity();
        entity.setId(99L);
        entity.setOwnerUserId(1L);
        entity.setPartnerType(PartnerTypes.CUSTOMER);
        entity.setPartnerId(20L);
        entity.setName("张三");
        entity.setPhone("13900000000");
        entity.setTitle("采购");
        entity.setIsPrimary(true);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }
}

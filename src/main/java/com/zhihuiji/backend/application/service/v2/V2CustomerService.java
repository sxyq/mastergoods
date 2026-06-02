package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2CustomerService {
    private final CustomerRepository customerRepository;
    private final V2PartnerGroupService partnerGroupService;
    private final V2PartnerContactService partnerContactService;
    private final CurrentOwnerService currentOwnerService;

    public V2CustomerService(
        CustomerRepository customerRepository,
        V2PartnerGroupService partnerGroupService,
        V2PartnerContactService partnerContactService,
        CurrentOwnerService currentOwnerService
    ) {
        this.customerRepository = customerRepository;
        this.partnerGroupService = partnerGroupService;
        this.partnerContactService = partnerContactService;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2PartnerDtos.CustomerResponse> list(String keyword, Integer status, Long groupId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<CustomerEntity> customers = (keyword == null || keyword.isBlank())
            ? customerRepository.findAllByOwnerUserId(ownerUserId)
            : customerRepository.findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCase(
                ownerUserId,
                keyword.trim(),
                ownerUserId,
                keyword.trim()
            );
        return customers.stream()
            .filter(customer -> status == null || status.equals(customer.getStatus()))
            .filter(customer -> groupId == null || groupId.equals(customer.getGroupId()))
            .sorted(Comparator.comparing(CustomerEntity::getUpdatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    public V2PartnerDtos.CustomerResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2PartnerDtos.CustomerResponse create(V2PartnerDtos.CustomerWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (customerRepository.findByOwnerUserIdAndPhone(ownerUserId, normalizeRequired(request.phone(), "客户手机号不能为空")).isPresent()) {
            throw new IllegalArgumentException("手机号已存在");
        }
        PartnerGroupEntity group = request.groupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.CUSTOMER, request.groupId());
        long now = System.currentTimeMillis();
        CustomerEntity entity = new CustomerEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(normalizeRequired(request.name(), "客户名称不能为空"));
        entity.setPhone(normalizeRequired(request.phone(), "客户手机号不能为空"));
        entity.setLevel(request.level());
        entity.setGroupId(group == null ? null : group.getId());
        entity.setAddress(normalizeNullable(request.address()));
        entity.setNotes(normalizeNullable(request.notes()));
        entity.setContactName(normalizeNullable(request.primaryContactName()));
        entity.setContactPhone(normalizeNullable(request.primaryContactPhone()));
        entity.setBalance(normalizeBalance(request.balance()));
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        CustomerEntity saved = customerRepository.save(entity);
        partnerContactService.syncPrimarySummary(PartnerTypes.CUSTOMER, saved.getId(), saved.getContactName(), saved.getContactPhone());
        return toResponse(saved);
    }

    @Transactional
    public V2PartnerDtos.CustomerResponse update(Long id, V2PartnerDtos.CustomerWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        CustomerEntity entity = getOwnedEntity(id);
        customerRepository.findByOwnerUserIdAndPhone(ownerUserId, normalizeRequired(request.phone(), "客户手机号不能为空"))
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("手机号已存在");
            });
        PartnerGroupEntity group = request.groupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.CUSTOMER, request.groupId());
        entity.setName(normalizeRequired(request.name(), "客户名称不能为空"));
        entity.setPhone(normalizeRequired(request.phone(), "客户手机号不能为空"));
        entity.setLevel(request.level());
        entity.setGroupId(group == null ? null : group.getId());
        entity.setAddress(normalizeNullable(request.address()));
        entity.setNotes(normalizeNullable(request.notes()));
        entity.setContactName(normalizeNullable(request.primaryContactName()));
        entity.setContactPhone(normalizeNullable(request.primaryContactPhone()));
        entity.setBalance(normalizeBalance(request.balance()));
        entity.setStatus(normalizeStatus(request.status()));
        entity.setUpdatedAt(System.currentTimeMillis());
        entity.setSyncStatus(0);
        entity.setSyncVersion(entity.getSyncVersion() + 1);
        CustomerEntity saved = customerRepository.save(entity);
        partnerContactService.syncPrimarySummary(PartnerTypes.CUSTOMER, saved.getId(), saved.getContactName(), saved.getContactPhone());
        return toResponse(saved);
    }

    public void delete(Long id) {
        customerRepository.delete(getOwnedEntity(id));
    }

    public CustomerEntity getOwnedEntity(Long id) {
        return customerRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    private V2PartnerDtos.CustomerResponse toResponse(CustomerEntity entity) {
        PartnerGroupEntity group = entity.getGroupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.CUSTOMER, entity.getGroupId());
        return new V2PartnerDtos.CustomerResponse(
            entity.getId(),
            entity.getName(),
            entity.getPhone(),
            entity.getLevel(),
            entity.getGroupId(),
            group == null ? null : group.getName(),
            entity.getContactName(),
            entity.getContactPhone(),
            entity.getAddress(),
            entity.getNotes(),
            entity.getBalance(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Double normalizeBalance(Double value) {
        return value == null ? 0.0 : Math.max(0.0, value);
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("客户状态不合法");
        }
        return status;
    }
}

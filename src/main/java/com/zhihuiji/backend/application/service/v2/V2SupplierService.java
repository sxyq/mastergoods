package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2SupplierService {
    private final SupplierRepository supplierRepository;
    private final V2PartnerGroupService partnerGroupService;
    private final V2PartnerContactService partnerContactService;
    private final CurrentOwnerService currentOwnerService;

    public V2SupplierService(
        SupplierRepository supplierRepository,
        V2PartnerGroupService partnerGroupService,
        V2PartnerContactService partnerContactService,
        CurrentOwnerService currentOwnerService
    ) {
        this.supplierRepository = supplierRepository;
        this.partnerGroupService = partnerGroupService;
        this.partnerContactService = partnerContactService;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2PartnerDtos.SupplierResponse> list(String keyword, Integer status, Long groupId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        List<SupplierEntity> suppliers;
        if (hasKeyword && status != null) {
            suppliers = supplierRepository.findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCaseAndStatus(
                ownerUserId,
                keyword.trim(),
                ownerUserId,
                keyword.trim(),
                status
            );
        } else if (hasKeyword) {
            suppliers = supplierRepository.findByOwnerUserIdAndNameContainingIgnoreCaseOrOwnerUserIdAndPhoneContainingIgnoreCase(
                ownerUserId,
                keyword.trim(),
                ownerUserId,
                keyword.trim()
            );
        } else if (status != null) {
            suppliers = supplierRepository.findByOwnerUserIdAndStatus(ownerUserId, status);
        } else {
            suppliers = supplierRepository.findAllByOwnerUserId(ownerUserId);
        }
        return suppliers.stream()
            .filter(supplier -> groupId == null || groupId.equals(supplier.getGroupId()))
            .sorted(Comparator.comparing(SupplierEntity::getUpdatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    public V2PartnerDtos.SupplierResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2PartnerDtos.SupplierResponse create(V2PartnerDtos.SupplierWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String phone = normalizeRequired(request.phone(), "供应商手机号不能为空");
        if (supplierRepository.existsByOwnerUserIdAndPhone(ownerUserId, phone)) {
            throw new IllegalArgumentException("供应商手机号已存在");
        }
        PartnerGroupEntity group = request.groupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.SUPPLIER, request.groupId());
        long now = System.currentTimeMillis();
        SupplierEntity entity = new SupplierEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setName(normalizeRequired(request.name(), "供应商名称不能为空"));
        entity.setPhone(phone);
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
        SupplierEntity saved = supplierRepository.save(entity);
        partnerContactService.syncPrimarySummary(PartnerTypes.SUPPLIER, saved.getId(), saved.getContactName(), saved.getContactPhone());
        return toResponse(saved);
    }

    @Transactional
    public V2PartnerDtos.SupplierResponse update(Long id, V2PartnerDtos.SupplierWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        SupplierEntity entity = getOwnedEntity(id);
        String phone = normalizeRequired(request.phone(), "供应商手机号不能为空");
        if (supplierRepository.existsByOwnerUserIdAndPhoneAndIdNot(ownerUserId, phone, id)) {
            throw new IllegalArgumentException("供应商手机号已存在");
        }
        PartnerGroupEntity group = request.groupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.SUPPLIER, request.groupId());
        entity.setName(normalizeRequired(request.name(), "供应商名称不能为空"));
        entity.setPhone(phone);
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
        SupplierEntity saved = supplierRepository.save(entity);
        partnerContactService.syncPrimarySummary(PartnerTypes.SUPPLIER, saved.getId(), saved.getContactName(), saved.getContactPhone());
        return toResponse(saved);
    }

    public void delete(Long id) {
        supplierRepository.delete(getOwnedEntity(id));
    }

    public SupplierEntity getOwnedEntity(Long id) {
        return supplierRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    private V2PartnerDtos.SupplierResponse toResponse(SupplierEntity entity) {
        PartnerGroupEntity group = entity.getGroupId() == null ? null : partnerGroupService.getOwnedEntity(PartnerTypes.SUPPLIER, entity.getGroupId());
        return new V2PartnerDtos.SupplierResponse(
            entity.getId(),
            entity.getName(),
            entity.getPhone(),
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
            throw new IllegalArgumentException("供应商状态不合法");
        }
        return status;
    }
}

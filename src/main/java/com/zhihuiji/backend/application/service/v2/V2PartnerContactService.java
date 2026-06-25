package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.CustomerEntity;
import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import com.zhihuiji.backend.domain.entity.SupplierEntity;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerContactRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2PartnerContactService {
    private final PartnerContactRepository partnerContactRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2PartnerContactService(
        PartnerContactRepository partnerContactRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.partnerContactRepository = partnerContactRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2PartnerDtos.PartnerContactResponse> list(String partnerType, Long partnerId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPartnerType = requirePartnerType(partnerType);
        requirePartnerExists(ownerUserId, normalizedPartnerType, partnerId);
        List<PartnerContactEntity> contacts = partnerContactRepository
            .findAllByOwnerUserIdAndPartnerTypeAndPartnerIdOrderByIsPrimaryDescCreatedAtAsc(ownerUserId, normalizedPartnerType, partnerId);
        return contacts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public V2PartnerDtos.PartnerContactResponse create(String partnerType, V2PartnerDtos.PartnerContactWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPartnerType = requirePartnerType(partnerType);
        Long partnerId = requirePartnerExists(ownerUserId, normalizedPartnerType, request.partnerId());
        long now = System.currentTimeMillis();
        PartnerContactEntity entity = new PartnerContactEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setPartnerType(normalizedPartnerType);
        entity.setPartnerId(partnerId);
        entity.setName(normalizeRequired(request.name(), "联系人名称不能为空"));
        entity.setPhone(normalizeNullable(request.phone()));
        entity.setTitle(normalizeNullable(request.title()));
        entity.setIsPrimary(Boolean.TRUE.equals(request.isPrimary()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            clearPrimary(ownerUserId, normalizedPartnerType, partnerId, null);
        }
        PartnerContactEntity saved = partnerContactRepository.save(entity);
        refreshPrimarySummary(ownerUserId, normalizedPartnerType, partnerId);
        return toResponse(saved);
    }

    @Transactional
    public V2PartnerDtos.PartnerContactResponse update(String partnerType, Long id, V2PartnerDtos.PartnerContactWriteRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPartnerType = requirePartnerType(partnerType);
        PartnerContactEntity entity = getOwnedEntity(normalizedPartnerType, id);
        Long previousPartnerId = entity.getPartnerId();
        Long partnerId = requirePartnerExists(ownerUserId, normalizedPartnerType, request.partnerId());
        entity.setPartnerId(partnerId);
        entity.setName(normalizeRequired(request.name(), "联系人名称不能为空"));
        entity.setPhone(normalizeNullable(request.phone()));
        entity.setTitle(normalizeNullable(request.title()));
        entity.setIsPrimary(Boolean.TRUE.equals(request.isPrimary()));
        entity.setUpdatedAt(System.currentTimeMillis());
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            clearPrimary(ownerUserId, normalizedPartnerType, partnerId, entity.getId());
        }
        PartnerContactEntity saved = partnerContactRepository.save(entity);
        if (!previousPartnerId.equals(partnerId)) {
            refreshPrimarySummary(ownerUserId, normalizedPartnerType, previousPartnerId);
        }
        refreshPrimarySummary(ownerUserId, normalizedPartnerType, partnerId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String partnerType, Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPartnerType = requirePartnerType(partnerType);
        PartnerContactEntity entity = getOwnedEntity(normalizedPartnerType, id);
        Long partnerId = entity.getPartnerId();
        partnerContactRepository.delete(entity);
        refreshPrimarySummary(ownerUserId, normalizedPartnerType, partnerId);
    }

    @Transactional
    public void syncPrimarySummary(String partnerType, Long partnerId, String contactName, String contactPhone) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedPartnerType = requirePartnerType(partnerType);
        Long resolvedPartnerId = requirePartnerExists(ownerUserId, normalizedPartnerType, partnerId);
        String normalizedName = normalizeNullable(contactName);
        String normalizedPhone = normalizeNullable(contactPhone);
        PartnerContactEntity currentPrimary = partnerContactRepository
            .findByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrue(ownerUserId, normalizedPartnerType, resolvedPartnerId)
            .orElse(null);
        if (normalizedName == null) {
            if (currentPrimary != null) {
                currentPrimary.setIsPrimary(false);
                currentPrimary.setUpdatedAt(System.currentTimeMillis());
                partnerContactRepository.save(currentPrimary);
            }
            refreshPrimarySummary(ownerUserId, normalizedPartnerType, resolvedPartnerId);
            return;
        }
        long now = System.currentTimeMillis();
        if (currentPrimary == null) {
            PartnerContactEntity entity = new PartnerContactEntity();
            entity.setOwnerUserId(ownerUserId);
            entity.setPartnerType(normalizedPartnerType);
            entity.setPartnerId(resolvedPartnerId);
            entity.setName(normalizedName);
            entity.setPhone(normalizedPhone);
            entity.setTitle(null);
            entity.setIsPrimary(true);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            clearPrimary(ownerUserId, normalizedPartnerType, resolvedPartnerId, null);
            partnerContactRepository.save(entity);
        } else {
            currentPrimary.setName(normalizedName);
            currentPrimary.setPhone(normalizedPhone);
            currentPrimary.setIsPrimary(true);
            currentPrimary.setUpdatedAt(now);
            clearPrimary(ownerUserId, normalizedPartnerType, resolvedPartnerId, currentPrimary.getId());
            partnerContactRepository.save(currentPrimary);
        }
        refreshPrimarySummary(ownerUserId, normalizedPartnerType, resolvedPartnerId);
    }

    public PartnerContactEntity getOwnedEntity(String partnerType, Long id) {
        return partnerContactRepository.findByIdAndOwnerUserIdAndPartnerType(
                id,
                currentOwnerService.requireCurrentOwnerUserId(),
                requirePartnerType(partnerType)
            )
            .orElseThrow(() -> new IllegalArgumentException("联系人不存在"));
    }

    private void clearPrimary(Long ownerUserId, String partnerType, Long partnerId, Long keepId) {
        long now = System.currentTimeMillis();
        List<PartnerContactEntity> contacts = partnerContactRepository.findAllByOwnerUserIdAndPartnerTypeAndPartnerIdOrderByIsPrimaryDescCreatedAtAsc(ownerUserId, partnerType, partnerId);
        for (PartnerContactEntity contact : contacts) {
            if (contact.getId().equals(keepId) || !Boolean.TRUE.equals(contact.getIsPrimary())) {
                continue;
            }
            contact.setIsPrimary(false);
            contact.setUpdatedAt(now);
            partnerContactRepository.save(contact);
        }
    }

    private void refreshPrimarySummary(Long ownerUserId, String partnerType, Long partnerId) {
        PartnerContactEntity primary = partnerContactRepository
            .findByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrue(ownerUserId, partnerType, partnerId)
            .orElse(null);
        if (PartnerTypes.CUSTOMER.equals(partnerType)) {
            CustomerEntity customer = customerRepository.findByIdAndOwnerUserId(partnerId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
            customer.setContactName(primary == null ? null : primary.getName());
            customer.setContactPhone(primary == null ? null : primary.getPhone());
            customer.setUpdatedAt(System.currentTimeMillis());
            customerRepository.save(customer);
        } else {
            SupplierEntity supplier = supplierRepository.findByIdAndOwnerUserId(partnerId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
            supplier.setContactName(primary == null ? null : primary.getName());
            supplier.setContactPhone(primary == null ? null : primary.getPhone());
            supplier.setUpdatedAt(System.currentTimeMillis());
            supplierRepository.save(supplier);
        }
    }

    private Long requirePartnerExists(Long ownerUserId, String partnerType, Long partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("往来单位不能为空");
        }
        if (PartnerTypes.CUSTOMER.equals(partnerType)) {
            return customerRepository.findByIdAndOwnerUserId(partnerId, ownerUserId)
                .map(CustomerEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        }
        return supplierRepository.findByIdAndOwnerUserId(partnerId, ownerUserId)
            .map(SupplierEntity::getId)
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在"));
    }

    private V2PartnerDtos.PartnerContactResponse toResponse(PartnerContactEntity entity) {
        return new V2PartnerDtos.PartnerContactResponse(
            entity.getId(),
            entity.getPartnerType(),
            entity.getPartnerId(),
            entity.getName(),
            entity.getPhone(),
            entity.getTitle(),
            entity.getIsPrimary(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String requirePartnerType(String partnerType) {
        if (!PartnerTypes.isSupported(partnerType)) {
            throw new IllegalArgumentException("partner_type 不合法");
        }
        return partnerType;
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
}

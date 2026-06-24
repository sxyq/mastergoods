package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.PartnerTypes;
import com.zhihuiji.backend.api.dto.v2.partner.V2PartnerDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import java.util.Collection;
import java.util.LinkedHashMap;
import com.zhihuiji.backend.infrastructure.repository.CustomerRepository;
import com.zhihuiji.backend.infrastructure.repository.PartnerGroupRepository;
import com.zhihuiji.backend.infrastructure.repository.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class V2PartnerGroupService {
    private final PartnerGroupRepository partnerGroupRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2PartnerGroupService(
        PartnerGroupRepository partnerGroupRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.partnerGroupRepository = partnerGroupRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<V2PartnerDtos.PartnerGroupResponse> list(String partnerType) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<PartnerGroupEntity> rows = partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(ownerUserId, requirePartnerType(partnerType));
        List<V2PartnerDtos.PartnerGroupResponse> responses = new ArrayList<>(rows.size());
        for (PartnerGroupEntity row : rows) {
            responses.add(toResponse(row));
        }
        return responses;
    }

    public Map<Long, PartnerGroupEntity> getOwnedEntityMap(String partnerType, Collection<Long> ids) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, PartnerGroupEntity> groupsById = new LinkedHashMap<>(ids.size());
        partnerGroupRepository.findAllByOwnerUserIdAndPartnerTypeAndIdIn(ownerUserId, requirePartnerType(partnerType), ids)
            .forEach(group -> groupsById.put(group.getId(), group));
        return groupsById;
    }

    public PartnerGroupEntity getOwnedEntity(String partnerType, Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return partnerGroupRepository.findByIdAndOwnerUserIdAndPartnerType(id, ownerUserId, requirePartnerType(partnerType))
            .orElseThrow(() -> new IllegalArgumentException("分组不存在"));
    }

    public V2PartnerDtos.PartnerGroupResponse create(String partnerType, V2PartnerDtos.PartnerGroupWriteRequest request) {
        String normalizedPartnerType = requirePartnerType(partnerType);
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String name = normalizeRequired(request.name(), "分组名称不能为空");
        if (partnerGroupRepository.existsByOwnerUserIdAndPartnerTypeAndName(ownerUserId, normalizedPartnerType, name)) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        long now = System.currentTimeMillis();
        PartnerGroupEntity entity = new PartnerGroupEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setPartnerType(normalizedPartnerType);
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(partnerGroupRepository.save(entity));
    }

    public V2PartnerDtos.PartnerGroupResponse update(String partnerType, Long id, V2PartnerDtos.PartnerGroupWriteRequest request) {
        String normalizedPartnerType = requirePartnerType(partnerType);
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PartnerGroupEntity entity = getOwnedEntity(normalizedPartnerType, id);
        String name = normalizeRequired(request.name(), "分组名称不能为空");
        if (partnerGroupRepository.existsByOwnerUserIdAndPartnerTypeAndNameAndIdNot(ownerUserId, normalizedPartnerType, name, id)) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        entity.setName(name);
        entity.setStatus(normalizeStatus(request.status()));
        entity.setSortOrder(normalizeSortOrder(request.sortOrder()));
        entity.setUpdatedAt(System.currentTimeMillis());
        return toResponse(partnerGroupRepository.save(entity));
    }

    public void delete(String partnerType, Long id) {
        String normalizedPartnerType = requirePartnerType(partnerType);
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PartnerGroupEntity entity = getOwnedEntity(normalizedPartnerType, id);
        long usage = PartnerTypes.CUSTOMER.equals(normalizedPartnerType)
            ? customerRepository.countByOwnerUserIdAndGroupId(ownerUserId, id)
            : supplierRepository.countByOwnerUserIdAndGroupId(ownerUserId, id);
        if (usage > 0) {
            throw new IllegalArgumentException("分组已被往来单位引用，无法删除");
        }
        partnerGroupRepository.delete(entity);
    }

    private V2PartnerDtos.PartnerGroupResponse toResponse(PartnerGroupEntity entity) {
        return new V2PartnerDtos.PartnerGroupResponse(
            entity.getId(),
            entity.getPartnerType(),
            entity.getName(),
            entity.getStatus(),
            entity.getSortOrder(),
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

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("分组状态不合法");
        }
        return status;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}

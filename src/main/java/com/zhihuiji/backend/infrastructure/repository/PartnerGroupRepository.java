package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerGroupRepository extends JpaRepository<PartnerGroupEntity, Long> {
    List<PartnerGroupEntity> findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(Long ownerUserId, String partnerType);

    Optional<PartnerGroupEntity> findByIdAndOwnerUserIdAndPartnerType(Long id, Long ownerUserId, String partnerType);

    boolean existsByOwnerUserIdAndPartnerTypeAndName(Long ownerUserId, String partnerType, String name);

    boolean existsByOwnerUserIdAndPartnerTypeAndNameAndIdNot(Long ownerUserId, String partnerType, String name, Long id);
}

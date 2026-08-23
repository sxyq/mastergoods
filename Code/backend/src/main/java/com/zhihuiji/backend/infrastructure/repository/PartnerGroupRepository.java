package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PartnerGroupEntity;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerGroupRepository extends JpaRepository<PartnerGroupEntity, Long> {
    List<PartnerGroupEntity> findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(Long ownerUserId, String partnerType);
    List<PartnerGroupEntity> findAllByOwnerUserIdAndPartnerTypeOrderBySortOrderAscNameAsc(Long ownerUserId, String partnerType, Pageable pageable);

    List<PartnerGroupEntity> findAllByOwnerUserIdAndPartnerTypeAndIdIn(Long ownerUserId, String partnerType, Collection<Long> ids);

    @Query("SELECT e FROM PartnerGroupEntity e WHERE e.ownerUserId = :ownerUserId AND e.partnerType = :partnerType AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.sortOrder ASC, e.name ASC")
    List<PartnerGroupEntity> findChangedByOwnerUserIdAndPartnerType(@Param("ownerUserId") Long ownerUserId, @Param("partnerType") String partnerType, @Param("sinceTimestamp") Long sinceTimestamp);

    Optional<PartnerGroupEntity> findByIdAndOwnerUserIdAndPartnerType(Long id, Long ownerUserId, String partnerType);

    boolean existsByOwnerUserIdAndPartnerTypeAndName(Long ownerUserId, String partnerType, String name);

    boolean existsByOwnerUserIdAndPartnerTypeAndNameAndIdNot(Long ownerUserId, String partnerType, String name, Long id);
}

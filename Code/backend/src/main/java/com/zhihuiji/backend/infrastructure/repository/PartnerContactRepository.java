package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartnerContactRepository extends JpaRepository<PartnerContactEntity, Long> {
    List<PartnerContactEntity> findAllByOwnerUserIdAndPartnerTypeOrderByUpdatedAtAscIdAsc(
        Long ownerUserId,
        String partnerType
    );

    @Query("SELECT e FROM PartnerContactEntity e WHERE e.ownerUserId = :ownerUserId AND e.partnerType = :partnerType AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<PartnerContactEntity> findChangedByOwnerUserIdAndPartnerType(@Param("ownerUserId") Long ownerUserId, @Param("partnerType") String partnerType, @Param("sinceTimestamp") Long sinceTimestamp);

    List<PartnerContactEntity> findAllByOwnerUserIdAndPartnerTypeAndPartnerIdOrderByIsPrimaryDescCreatedAtAsc(
        Long ownerUserId,
        String partnerType,
        Long partnerId
    );

    Optional<PartnerContactEntity> findByIdAndOwnerUserIdAndPartnerType(Long id, Long ownerUserId, String partnerType);

    Optional<PartnerContactEntity> findByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrue(
        Long ownerUserId,
        String partnerType,
        Long partnerId
    );

    void deleteByOwnerUserIdAndPartnerTypeAndPartnerId(Long ownerUserId, String partnerType, Long partnerId);

    boolean existsByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrueAndIdNot(
        Long ownerUserId,
        String partnerType,
        Long partnerId,
        Long id
    );
}

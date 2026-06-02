package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PartnerContactEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerContactRepository extends JpaRepository<PartnerContactEntity, Long> {
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

    boolean existsByOwnerUserIdAndPartnerTypeAndPartnerIdAndIsPrimaryTrueAndIdNot(
        Long ownerUserId,
        String partnerType,
        Long partnerId,
        Long id
    );
}

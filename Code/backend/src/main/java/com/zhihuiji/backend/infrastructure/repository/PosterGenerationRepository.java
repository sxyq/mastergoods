package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.PosterGenerationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosterGenerationRepository extends JpaRepository<PosterGenerationEntity, Long> {

    List<PosterGenerationEntity> findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId);

    List<PosterGenerationEntity> findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);

    Optional<PosterGenerationEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

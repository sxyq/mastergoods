package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.AccountTransferEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransferRepository extends JpaRepository<AccountTransferEntity, Long> {
    List<AccountTransferEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<AccountTransferEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);
    Optional<AccountTransferEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    boolean existsByOwnerUserIdAndTransferNo(Long ownerUserId, String transferNo);
}

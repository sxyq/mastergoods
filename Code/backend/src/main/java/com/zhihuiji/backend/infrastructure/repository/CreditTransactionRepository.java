package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.CreditTransactionEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditTransactionRepository extends JpaRepository<CreditTransactionEntity, Long> {

    List<CreditTransactionEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<CreditTransactionEntity> findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);
}

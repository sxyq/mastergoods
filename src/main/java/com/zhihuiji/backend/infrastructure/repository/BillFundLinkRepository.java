package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillFundLinkRepository extends JpaRepository<BillFundLinkEntity, Long> {
    List<BillFundLinkEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);

    @Query("SELECT e FROM BillFundLinkEntity e WHERE e.ownerUserId = :ownerUserId AND COALESCE(e.updatedAt, e.createdAt) >= :sinceTimestamp ORDER BY e.updatedAt ASC, e.id ASC")
    List<BillFundLinkEntity> findChangedByOwnerUserId(@Param("ownerUserId") Long ownerUserId, @Param("sinceTimestamp") Long sinceTimestamp);
    List<BillFundLinkEntity> findAllByOwnerUserIdAndBillTypeAndBillIdOrderByCreatedAtDesc(Long ownerUserId, String billType, Long billId);
    List<BillFundLinkEntity> findAllByOwnerUserIdAndAccountIdOrderByCreatedAtDesc(Long ownerUserId, Long accountId);
    Optional<BillFundLinkEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    Optional<BillFundLinkEntity> findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(Long ownerUserId, String billType, Long billId, Integer linkType);
}

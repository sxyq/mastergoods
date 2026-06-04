package com.zhihuiji.backend.infrastructure.repository;

import com.zhihuiji.backend.domain.entity.BillFundLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillFundLinkRepository extends JpaRepository<BillFundLinkEntity, Long> {
    List<BillFundLinkEntity> findAllByOwnerUserIdOrderByUpdatedAtAscIdAsc(Long ownerUserId);
    List<BillFundLinkEntity> findAllByOwnerUserIdAndBillTypeAndBillIdOrderByCreatedAtDesc(Long ownerUserId, String billType, Long billId);
    List<BillFundLinkEntity> findAllByOwnerUserIdAndAccountIdOrderByCreatedAtDesc(Long ownerUserId, Long accountId);
    Optional<BillFundLinkEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    Optional<BillFundLinkEntity> findFirstByOwnerUserIdAndBillTypeAndBillIdAndLinkType(Long ownerUserId, String billType, Long billId, Integer linkType);
}

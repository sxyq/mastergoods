package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.api.dto.v2.poster.V2PosterDtos;
import com.zhihuiji.backend.domain.entity.CreditTransactionEntity;
import com.zhihuiji.backend.domain.entity.UserCreditEntity;
import com.zhihuiji.backend.infrastructure.repository.CreditTransactionRepository;
import com.zhihuiji.backend.infrastructure.repository.UserCreditRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分服务：查询余额/流水、扣减（乐观锁 @Version）、充值、退分。
 *
 * <p>多租户隔离：所有操作均按 ownerUserId 过滤。余额变更通过 {@link UserCreditEntity#getVersion()}
 * 的 JPA {@code @Version} 实现乐观锁，并发扣减冲突时由 JPA 抛出乐观锁异常并回滚。
 */
@Service
public class CreditService {

    public static final String TYPE_RECHARGE = "recharge";
    public static final String TYPE_CONSUME = "consume";
    public static final String TYPE_REFUND = "refund";

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final IdGenerator idGenerator;

    public CreditService(
        UserCreditRepository userCreditRepository,
        CreditTransactionRepository creditTransactionRepository,
        IdGenerator idGenerator
    ) {
        this.userCreditRepository = userCreditRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long ownerUserId) {
        return userCreditRepository.findByOwnerUserId(ownerUserId)
            .map(UserCreditEntity::getBalance)
            .orElse(ZERO);
    }

    @Transactional(readOnly = true)
    public V2PosterDtos.CreditBalanceResponse getBalanceSnapshot(Long ownerUserId) {
        UserCreditEntity credit = userCreditRepository.findByOwnerUserId(ownerUserId).orElse(null);
        if (credit == null) {
            return new V2PosterDtos.CreditBalanceResponse(ZERO, ZERO, ZERO);
        }
        return new V2PosterDtos.CreditBalanceResponse(
            credit.getBalance(),
            credit.getTotalRecharged(),
            credit.getTotalConsumed()
        );
    }

    @Transactional(readOnly = true)
    public List<CreditTransactionEntity> listTransactions(Long ownerUserId, Integer page, Integer limit) {
        return creditTransactionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
    }

    @Transactional
    public void deductCredits(Long ownerUserId, BigDecimal amount, String refType, Long refId, String note) {
        BigDecimal safeAmount = requirePositive(amount, "扣减积分必须大于 0");
        UserCreditEntity credit = getOrCreateCredit(ownerUserId);
        BigDecimal balanceBefore = credit.getBalance();
        if (balanceBefore.compareTo(safeAmount) < 0) {
            throw new BusinessException("积分余额不足");
        }
        BigDecimal balanceAfter = balanceBefore.subtract(safeAmount).setScale(2, RoundingMode.HALF_UP);
        credit.setBalance(balanceAfter);
        credit.setTotalConsumed(credit.getTotalConsumed().add(safeAmount).setScale(2, RoundingMode.HALF_UP));
        credit.setUpdatedAt(System.currentTimeMillis());
        userCreditRepository.save(credit);
        recordTransaction(ownerUserId, safeAmount.negate(), TYPE_CONSUME, refType, refId, balanceBefore, balanceAfter, note);
    }

    @Transactional
    public void rechargeCredits(Long ownerUserId, BigDecimal amount, String note) {
        BigDecimal safeAmount = requirePositive(amount, "充值积分必须大于 0");
        UserCreditEntity credit = getOrCreateCredit(ownerUserId);
        BigDecimal balanceBefore = credit.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(safeAmount).setScale(2, RoundingMode.HALF_UP);
        credit.setBalance(balanceAfter);
        credit.setTotalRecharged(credit.getTotalRecharged().add(safeAmount).setScale(2, RoundingMode.HALF_UP));
        credit.setUpdatedAt(System.currentTimeMillis());
        userCreditRepository.save(credit);
        recordTransaction(ownerUserId, safeAmount, TYPE_RECHARGE, null, null, balanceBefore, balanceAfter, note);
    }

    @Transactional
    public void refundCredits(Long ownerUserId, BigDecimal amount, String refType, Long refId, String note) {
        BigDecimal safeAmount = requirePositive(amount, "退分金额必须大于 0");
        UserCreditEntity credit = getOrCreateCredit(ownerUserId);
        BigDecimal balanceBefore = credit.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(safeAmount).setScale(2, RoundingMode.HALF_UP);
        credit.setBalance(balanceAfter);
        BigDecimal consumed = credit.getTotalConsumed();
        BigDecimal restored = consumed.subtract(safeAmount);
        credit.setTotalConsumed((restored.signum() < 0 ? ZERO : restored).setScale(2, RoundingMode.HALF_UP));
        credit.setUpdatedAt(System.currentTimeMillis());
        userCreditRepository.save(credit);
        recordTransaction(ownerUserId, safeAmount, TYPE_REFUND, refType, refId, balanceBefore, balanceAfter, note);
    }

    private UserCreditEntity getOrCreateCredit(Long ownerUserId) {
        return userCreditRepository.findByOwnerUserId(ownerUserId)
            .orElseGet(() -> {
                UserCreditEntity entity = new UserCreditEntity();
                entity.setId(idGenerator.nextId());
                entity.setOwnerUserId(ownerUserId);
                entity.setBalance(ZERO);
                entity.setTotalRecharged(ZERO);
                entity.setTotalConsumed(ZERO);
                entity.setVersion(0);
                entity.setUpdatedAt(System.currentTimeMillis());
                return userCreditRepository.save(entity);
            });
    }

    private void recordTransaction(
        Long ownerUserId,
        BigDecimal signedAmount,
        String type,
        String refType,
        Long refId,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String note
    ) {
        CreditTransactionEntity tx = new CreditTransactionEntity();
        tx.setId(idGenerator.nextId());
        tx.setOwnerUserId(ownerUserId);
        tx.setAmount(signedAmount.setScale(2, RoundingMode.HALF_UP));
        tx.setType(type);
        tx.setRefType(refType);
        tx.setRefId(refId);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setNote(note);
        tx.setCreatedAt(System.currentTimeMillis());
        creditTransactionRepository.save(tx);
    }

    private BigDecimal requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(message);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}

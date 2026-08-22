package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.zhihuiji.backend.api.common.IdGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceRecordService {
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    private final FinanceRecordRepository financeRecordRepository;
    private final CurrentOwnerService currentOwnerService;
    private final IdGenerator idGenerator;

    public FinanceRecordService(FinanceRecordRepository financeRecordRepository, CurrentOwnerService currentOwnerService, IdGenerator idGenerator) {
        this.financeRecordRepository = financeRecordRepository;
        this.currentOwnerService = currentOwnerService;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<FinanceRecordEntity> list(
        String keyword,
        Integer type,
        Long createdAfter,
        Long createdBefore
    ) {
        return financeRecordRepository.search(
            currentOwnerService.requireCurrentOwnerUserId(),
            normalizeKeyword(keyword),
            type,
            createdAfter,
            createdBefore
        );
    }

    @Transactional(readOnly = true)
    public List<FinanceRecordEntity> list(
        String keyword,
        Integer type,
        Long createdAfter,
        Long createdBefore,
        Pageable pageable
    ) {
        return financeRecordRepository.search(
            currentOwnerService.requireCurrentOwnerUserId(),
            normalizeKeyword(keyword),
            type,
            createdAfter,
            createdBefore,
            pageable
        );
    }

    @Transactional
    public FinanceRecordEntity create(CreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("资金流水参数不能为空");
        }
        String normalizedCategory = normalizeCategory(command.category());
        validateCreateCommand(command, normalizedCategory);

        long now = System.currentTimeMillis();
        FinanceRecordEntity entity = new FinanceRecordEntity();
        entity.setId(idGenerator.nextId());
        entity.setOwnerUserId(currentOwnerService.requireCurrentOwnerUserId());
        entity.setRecordNo(generateRecordNo());
        entity.setType(command.type());
        entity.setCategory(normalizedCategory);
        entity.setPartnerName(normalizeNullableText(command.partnerName()));
        entity.setAmount(command.amount());
        entity.setMethod(command.method() == null || command.method() <= 0 ? 1 : command.method());
        entity.setNotes(normalizeNullableText(command.notes()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setSyncStatus(0);
        entity.setSyncVersion(1L);
        return financeRecordRepository.save(entity);
    }

    private void validateCreateCommand(CreateCommand command, String normalizedCategory) {
        if (command.type() == null ||
            (command.type() != TYPE_INCOME && command.type() != TYPE_EXPENSE)) {
            throw new IllegalArgumentException("资金流水类型不合法");
        }
        if (command.amount() == null || command.amount() <= 0.0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (normalizedCategory.isEmpty()) {
            throw new IllegalArgumentException("分类不能为空");
        }
    }

    private String normalizeCategory(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private String generateRecordNo() {
        return "FR" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record CreateCommand(
        Integer type,
        String category,
        String partnerName,
        Double amount,
        Integer method,
        String notes
    ) {}
}

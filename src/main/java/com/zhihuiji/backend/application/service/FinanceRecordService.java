package com.zhihuiji.backend.application.service;

import com.zhihuiji.backend.domain.entity.FinanceRecordEntity;
import com.zhihuiji.backend.infrastructure.repository.FinanceRecordRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.zhihuiji.backend.api.common.IdGenerator;
import org.springframework.stereotype.Service;

@Service
public class FinanceRecordService {
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    private final FinanceRecordRepository financeRecordRepository;
    private final CurrentOwnerService currentOwnerService;

    public FinanceRecordService(FinanceRecordRepository financeRecordRepository, CurrentOwnerService currentOwnerService) {
        this.financeRecordRepository = financeRecordRepository;
        this.currentOwnerService = currentOwnerService;
    }

    public List<FinanceRecordEntity> list(
        String keyword,
        Integer type,
        Long createdAfter,
        Long createdBefore
    ) {
        return financeRecordRepository.search(
            currentOwnerService.requireCurrentOwnerUserId(),
            keyword,
            type,
            createdAfter,
            createdBefore
        );
    }

    public FinanceRecordEntity create(CreateCommand command) {
        validateCreateCommand(command);

        long now = System.currentTimeMillis();
        FinanceRecordEntity entity = new FinanceRecordEntity();
        entity.setId(IdGenerator.nextId());
        entity.setOwnerUserId(currentOwnerService.requireCurrentOwnerUserId());
        entity.setRecordNo(generateRecordNo(now));
        entity.setType(command.type());
        entity.setCategory(normalizeCategory(command.category()));
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

    private void validateCreateCommand(CreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("资金流水参数不能为空");
        }
        if (command.type() == null ||
            (command.type() != TYPE_INCOME && command.type() != TYPE_EXPENSE)) {
            throw new IllegalArgumentException("资金流水类型不合法");
        }
        if (command.amount() == null || command.amount() <= 0.0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        String category = normalizeCategory(command.category());
        if (category.isEmpty()) {
            throw new IllegalArgumentException("分类不能为空");
        }
    }

    private String normalizeCategory(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private boolean matchesKeyword(FinanceRecordEntity entity, String keyword) {
        return safe(entity.getRecordNo()).contains(keyword)
            || safe(entity.getCategory()).contains(keyword)
            || safe(entity.getPartnerName()).contains(keyword)
            || safe(entity.getNotes()).contains(keyword);
    }

    private String safe(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    private String generateRecordNo(long timestamp) {
        return "FR" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        return normalized.isEmpty() ? null : normalized;
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

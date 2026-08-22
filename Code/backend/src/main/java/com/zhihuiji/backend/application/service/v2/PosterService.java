package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.common.IdGenerator;
import com.zhihuiji.backend.domain.entity.PosterGenerationEntity;
import com.zhihuiji.backend.domain.entity.product.ProductEntity;
import com.zhihuiji.backend.infrastructure.repository.PosterGenerationRepository;
import com.zhihuiji.backend.infrastructure.repository.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 海报生成服务：生成（先扣分再调用外部 API，失败退分）、历史列表、详情、迭代（双参考图策略）。
 *
 * <p>多租户隔离：所有查询/写入均按 ownerUserId 过滤。外部图像生成 API 暂为占位实现，
 * 返回固定占位 URL，后续接入真实 API 时替换 {@link #callExternalImageApi}。
 *
 * <p>事务边界：{@code generate}/{@code iterate} 不使用单一长事务，避免外部 API 调用期间
 * 长时间持有数据库事务。扣分/退分各自通过 {@link CreditService} 的独立事务提交，
 * 生成记录的状态变更按步骤单独持久化，保证「失败退分」与失败记录可被观测。
 */
@Service
public class PosterService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";

    public static final String REF_TYPE_POSTER = "poster";

    private static final BigDecimal GENERATION_COST = BigDecimal.TEN;

    private final PosterGenerationRepository posterGenerationRepository;
    private final ProductRepository productRepository;
    private final CreditService creditService;
    private final IdGenerator idGenerator;

    public PosterService(
        PosterGenerationRepository posterGenerationRepository,
        ProductRepository productRepository,
        CreditService creditService,
        IdGenerator idGenerator
    ) {
        this.posterGenerationRepository = posterGenerationRepository;
        this.productRepository = productRepository;
        this.creditService = creditService;
        this.idGenerator = idGenerator;
    }

    public PosterGenerationEntity generate(
        Long ownerUserId,
        Long productId,
        String prompt,
        List<String> referenceAssetIds
    ) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException("海报生成提示词不能为空");
        }
        ProductEntity product = productRepository.findByIdAndOwnerUserId(productId, ownerUserId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        String referenceAssetIdsText = joinAssetIds(referenceAssetIds);

        PosterGenerationEntity generation = newGeneration(ownerUserId, product.getId(), prompt, referenceAssetIdsText, 1, null);
        generation.setCreditsCost(GENERATION_COST);
        generation = posterGenerationRepository.save(generation);

        try {
            creditService.deductCredits(ownerUserId, GENERATION_COST, REF_TYPE_POSTER, generation.getId(), "海报生成扣费");
        } catch (BusinessException ex) {
            markFailed(generation, BigDecimal.ZERO);
            throw ex;
        }

        try {
            String imageUrl = callExternalImageApi(generation.getId(), prompt, referenceAssetIdsText);
            generation.setResultImageUrl(imageUrl);
            generation.setStatus(STATUS_SUCCESS);
            return posterGenerationRepository.save(generation);
        } catch (RuntimeException ex) {
            markFailed(generation, GENERATION_COST);
            creditService.refundCredits(ownerUserId, GENERATION_COST, REF_TYPE_POSTER, generation.getId(), "海报生成失败退款");
            throw new BusinessException("海报生成服务暂未接入");
        }
    }

    public PosterGenerationEntity iterate(Long ownerUserId, Long parentGenerationId) {
        PosterGenerationEntity parent = posterGenerationRepository.findByIdAndOwnerUserId(parentGenerationId, ownerUserId)
            .orElseThrow(() -> new BusinessException("原海报生成记录不存在"));
        if (!STATUS_SUCCESS.equals(parent.getStatus())) {
            throw new BusinessException("仅可基于成功的海报进行迭代");
        }
        String combinedReferences = buildIterateReferences(parent);
        int nextIteration = (parent.getIteration() == null ? 1 : parent.getIteration()) + 1;

        PosterGenerationEntity generation = newGeneration(
            ownerUserId,
            parent.getProductId(),
            parent.getPromptText(),
            combinedReferences,
            nextIteration,
            parent.getId()
        );
        generation.setCreditsCost(GENERATION_COST);
        generation = posterGenerationRepository.save(generation);

        try {
            creditService.deductCredits(ownerUserId, GENERATION_COST, REF_TYPE_POSTER, generation.getId(), "海报迭代生成扣费");
        } catch (BusinessException ex) {
            markFailed(generation, BigDecimal.ZERO);
            throw ex;
        }

        try {
            String imageUrl = callExternalImageApi(generation.getId(), generation.getPromptText(), combinedReferences);
            generation.setResultImageUrl(imageUrl);
            generation.setStatus(STATUS_SUCCESS);
            return posterGenerationRepository.save(generation);
        } catch (RuntimeException ex) {
            markFailed(generation, GENERATION_COST);
            creditService.refundCredits(ownerUserId, GENERATION_COST, REF_TYPE_POSTER, generation.getId(), "海报迭代生成失败退款");
            throw new BusinessException("海报生成服务暂未接入");
        }
    }

    public List<PosterGenerationEntity> listGenerations(Long ownerUserId, Integer page, Integer limit) {
        return posterGenerationRepository.findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(ownerUserId);
    }

    public List<PosterGenerationEntity> listGenerations(Long ownerUserId, Pageable pageable) {
        return posterGenerationRepository.findAllByOwnerUserIdOrderByCreatedAtDescIdDesc(ownerUserId, pageable);
    }

    public PosterGenerationEntity getGeneration(Long ownerUserId, Long id) {
        return posterGenerationRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new BusinessException("海报生成记录不存在"));
    }

    private PosterGenerationEntity newGeneration(
        Long ownerUserId,
        Long productId,
        String prompt,
        String referenceAssetIdsText,
        int iteration,
        Long parentGenerationId
    ) {
        PosterGenerationEntity entity = new PosterGenerationEntity();
        entity.setId(idGenerator.nextId());
        entity.setOwnerUserId(ownerUserId);
        entity.setProductId(productId);
        entity.setPromptText(prompt);
        entity.setReferenceImageAssetIds(referenceAssetIdsText);
        entity.setStatus(STATUS_PENDING);
        entity.setCreditsCost(BigDecimal.ZERO);
        entity.setIteration(iteration);
        entity.setParentGenerationId(parentGenerationId);
        entity.setCreatedAt(System.currentTimeMillis());
        return entity;
    }

    private void markFailed(PosterGenerationEntity generation, BigDecimal creditsCost) {
        generation.setStatus(STATUS_FAILED);
        generation.setCreditsCost(creditsCost);
        posterGenerationRepository.save(generation);
    }

    /**
     * 迭代生成的双参考图策略：合并原参考图与上一轮生成结果，作为新一轮的参考输入。
     */
    private String buildIterateReferences(PosterGenerationEntity parent) {
        String originalRefs = parent.getReferenceImageAssetIds();
        String parentResult = parent.getResultImageUrl();
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(originalRefs)) {
            sb.append(originalRefs);
        }
        if (StringUtils.hasText(parentResult)) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append("parent:").append(parentResult);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String joinAssetIds(List<String> referenceAssetIds) {
        if (referenceAssetIds == null || referenceAssetIds.isEmpty()) {
            return null;
        }
        return String.join(",", referenceAssetIds.stream().filter(StringUtils::hasText).toList());
    }

    /**
     * 外部图像生成 API 占位实现。后续接入真实 API 时替换此方法。
     *
     * @param generationId 生成记录 ID
     * @param prompt       生成提示词
     * @param referenceAssetIds 参考图资产标识
     * @return 占位图片 URL
     */
    private String callExternalImageApi(Long generationId, String prompt, String referenceAssetIds) {
        return "https://placeholder.poster/generated-" + generationId + ".jpg";
    }
}

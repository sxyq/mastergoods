package com.zhihuiji.backend.api.controller.v2;

import com.zhihuiji.backend.api.common.ApiResponse;
import com.zhihuiji.backend.api.common.PaginationUtils;
import com.zhihuiji.backend.api.dto.v2.poster.V2PosterDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.CreditService;
import com.zhihuiji.backend.application.service.v2.PosterService;
import com.zhihuiji.backend.domain.entity.CreditTransactionEntity;
import com.zhihuiji.backend.domain.entity.PosterGenerationEntity;
import com.zhihuiji.backend.infrastructure.security.RequireStorePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 海报生成与积分 V2 控制器。
 *
 * <p>两类资源共用 /v2 前缀：{@code /v2/credits/*}（积分余额与流水）、{@code /v2/posters/*}（海报生成）。
 * 权限按方法级 {@link RequireStorePermission} 控制（方法注解优先于类注解）。
 */
@RestController
@RequestMapping("/v2")
public class V2PosterController {

    private final CreditService creditService;
    private final PosterService posterService;
    private final CurrentOwnerService currentOwnerService;

    public V2PosterController(
        CreditService creditService,
        PosterService posterService,
        CurrentOwnerService currentOwnerService
    ) {
        this.creditService = creditService;
        this.posterService = posterService;
        this.currentOwnerService = currentOwnerService;
    }

    @GetMapping("/credits/balance")
    @RequireStorePermission("credits:view")
    public ApiResponse<V2PosterDtos.CreditBalanceResponse> balance() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return ApiResponse.success(creditService.getBalanceSnapshot(ownerUserId));
    }

    @GetMapping("/credits/transactions")
    @RequireStorePermission("credits:view")
    public ApiResponse<List<V2PosterDtos.CreditTransactionResponse>> transactions(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<CreditTransactionEntity> rows = creditService.listTransactions(
            ownerUserId,
            PaginationUtils.pageable(page, size)
        );
        List<V2PosterDtos.CreditTransactionResponse> payload = rows.stream()
            .map(this::toTransactionResponse)
            .toList();
        return ApiResponse.success(payload);
    }

    @PostMapping("/posters/generate")
    @RequireStorePermission("posters:write")
    public ApiResponse<V2PosterDtos.PosterGenerationResponse> generate(
        @Valid @RequestBody V2PosterDtos.PosterGenerateRequest request
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        PosterGenerationEntity created = posterService.generate(
            ownerUserId,
            request.productId(),
            request.prompt(),
            request.referenceAssetIds()
        );
        return ApiResponse.success(toGenerationResponse(created));
    }

    @GetMapping("/posters/generations")
    @RequireStorePermission("posters:view")
    public ApiResponse<List<V2PosterDtos.PosterGenerationResponse>> generations(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<PosterGenerationEntity> rows = posterService.listGenerations(
            ownerUserId,
            PaginationUtils.pageable(page, size)
        );
        List<V2PosterDtos.PosterGenerationResponse> payload = rows.stream()
            .map(this::toGenerationResponse)
            .toList();
        return ApiResponse.success(payload);
    }

    @GetMapping("/posters/generations/{id}")
    @RequireStorePermission("posters:view")
    public ApiResponse<V2PosterDtos.PosterGenerationResponse> generation(@PathVariable Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return ApiResponse.success(toGenerationResponse(posterService.getGeneration(ownerUserId, id)));
    }

    @PostMapping("/posters/generations/{id}/iterate")
    @RequireStorePermission("posters:write")
    public ApiResponse<V2PosterDtos.PosterGenerationResponse> iterate(@PathVariable Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return ApiResponse.success(toGenerationResponse(posterService.iterate(ownerUserId, id)));
    }

    private V2PosterDtos.PosterGenerationResponse toGenerationResponse(PosterGenerationEntity entity) {
        return new V2PosterDtos.PosterGenerationResponse(
            entity.getId(),
            entity.getProductId(),
            entity.getPromptText(),
            entity.getResultImageUrl(),
            entity.getStatus(),
            entity.getCreditsCost(),
            entity.getIteration(),
            entity.getCreatedAt()
        );
    }

    private V2PosterDtos.CreditTransactionResponse toTransactionResponse(CreditTransactionEntity entity) {
        return new V2PosterDtos.CreditTransactionResponse(
            entity.getId(),
            entity.getAmount(),
            entity.getType(),
            entity.getRefType(),
            entity.getRefId(),
            entity.getNote(),
            entity.getCreatedAt()
        );
    }
}

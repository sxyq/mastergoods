// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
package com.zhihuiji.backend.application.service.v2.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhihuiji.backend.api.common.BusinessException;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.v2.AgentImageService;
import com.zhihuiji.backend.application.service.v2.agent.component.RunAuditService;
import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 草稿确认服务。
 *
 * <p>负责读取 agent_drafts 表中的 active 草稿，按 draftType 路由到对应执行器，
 * 确认成功后更新草稿 status=confirmed；取消则置为 cancelled。
 *
 * <p>旧业务领域（销售/采购/收付款/商品/客户等）的草稿写入路由已随业务域删除，
 * 当前仅保留 image_generate / media_upload 两类基础设施草稿；新的领域草稿
 * 类型等待新领域实现后在此接入。
 *
 * <p>错误处理：草稿不存在、状态非 active 抛 BusinessException；执行或反序列化失败时
 * 通过 try/catch 捕获并抛 BusinessException 携带错误信息，草稿保持 active 供用户重试或取消。
 */
@Service
public class AgentDraftConfirmService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_CONFIRMING = "confirming";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final int PENDING_PAGE_SIZE = 100;

    private final AgentDraftRepository agentDraftRepository;
    private final CurrentOwnerService currentOwnerService;
    private final ObjectMapper objectMapper;

    private final AgentImageService agentImageService;
    private final AgentDraftConfirmationStateService confirmationStateService;
    private final RunAuditService runAuditService;

    public AgentDraftConfirmService(
        AgentDraftRepository agentDraftRepository,
        CurrentOwnerService currentOwnerService,
        ObjectMapper objectMapper,
        AgentImageService agentImageService,
        AgentDraftConfirmationStateService confirmationStateService,
        RunAuditService runAuditService
    ) {
        this.agentDraftRepository = agentDraftRepository;
        this.currentOwnerService = currentOwnerService;
        this.objectMapper = objectMapper;
        this.agentImageService = agentImageService;
        this.confirmationStateService = confirmationStateService;
        this.runAuditService = runAuditService;
    }

    /**
     * 列出当前 owner 下所有 status=active 的待确认草稿。
     *
     * @return 草稿响应列表
     */
    @Transactional(readOnly = true)
    public List<V2AgentDtos.AgentDraftResponse> listPendingDrafts() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return agentDraftRepository
            .findAllByOwnerUserIdAndStatusIgnoreCaseOrderByUpdatedAtDescIdDesc(
                ownerUserId, STATUS_ACTIVE, Pageable.ofSize(PENDING_PAGE_SIZE))
            .stream()
            .map(this::toDraftResponse)
            .toList();
    }

    /**
     * 确认草稿：读取草稿 → 按 draftType 路由执行 → 更新 status=confirmed。
     *
     * <p>执行失败时抛 BusinessException，草稿保持 active 状态供重试。
     *
     * @param draftId 草稿 ID
     * @return 确认后的草稿响应
     */
    @Transactional
    public V2AgentDtos.AgentDraftResponse confirmDraft(Long draftId) {
        Long actorUserId = currentOwnerService.requireCurrentUserId();
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(draftId, ownerUserId)
            .orElseThrow(() -> new BusinessException("草稿不存在"));
        if (STATUS_CONFIRMED.equalsIgnoreCase(entity.getStatus())) {
            recordDraftAction(entity, ownerUserId, actorUserId, "confirmed", entity.getBusinessReference(), null);
            return toDraftResponse(entity);
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("草稿状态不是 active，无法确认：" + entity.getStatus());
        }
        if (agentDraftRepository.updateStatusIfCurrent(
            draftId,
            ownerUserId,
            STATUS_ACTIVE,
            STATUS_CONFIRMING,
            System.currentTimeMillis()
        ) != 1) {
            throw new BusinessException("草稿已被其他请求确认或状态已变化");
        }
        Object created;
        try {
            created = dispatchCreate(entity);
        } catch (BusinessException ex) {
            recordFailure(entity, ownerUserId, actorUserId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            String message = "草稿确认失败（" + entity.getDraftType() + "）：" + cause.getMessage();
            recordFailure(entity, ownerUserId, actorUserId, message);
            throw new BusinessException(message);
        }
        V2AgentDtos.AgentImageGenerateResponse imageResult = created instanceof V2AgentDtos.AgentImageGenerateResponse response
            ? response : null;
        if (imageResult != null) {
            entity.setContentJson(AgentDraftImageResultCodec.withImageResult(
                objectMapper, entity.getContentJson(), imageResult
            ));
        }
        entity.setConfirmedBy(actorUserId);
        entity.setConfirmedAt(System.currentTimeMillis());
        entity.setBusinessReference(businessReference(entity.getDraftType(), created));
        entity.setFailureReason(null);
        entity.setStatus(STATUS_CONFIRMED);
        entity.setUpdatedAt(System.currentTimeMillis());
        AgentDraftEntity saved = agentDraftRepository.save(entity);
        recordDraftAction(saved, ownerUserId, actorUserId, "confirmed", saved.getBusinessReference(), null);
        return toDraftResponse(saved);
    }

    /**
     * 取消草稿：将 status 置为 cancelled。
     *
     * @param draftId 草稿 ID
     * @return 取消后的草稿响应
     */
    @Transactional
    public V2AgentDtos.AgentDraftResponse cancelDraft(Long draftId) {
        Long actorUserId = currentOwnerService.requireCurrentUserId();
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        AgentDraftEntity entity = agentDraftRepository.findByIdAndOwnerUserId(draftId, ownerUserId)
            .orElseThrow(() -> new BusinessException("草稿不存在"));
        if (!STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("草稿状态不是 active，无法取消：" + entity.getStatus());
        }
        if (agentDraftRepository.updateStatusIfCurrent(
            draftId,
            ownerUserId,
            STATUS_ACTIVE,
            STATUS_CANCELLED,
            System.currentTimeMillis()
        ) != 1) {
            throw new BusinessException("草稿已被其他请求确认或状态已变化");
        }
        entity.setStatus(STATUS_CANCELLED);
        entity.setUpdatedAt(System.currentTimeMillis());
        AgentDraftEntity saved = agentDraftRepository.save(entity);
        recordDraftAction(saved, ownerUserId, actorUserId, "cancelled", null, null);
        return toDraftResponse(saved);
    }

    /**
     * 按 draftType 路由执行草稿确认动作。
     *
     * @param entity 草稿实体
     * @return 执行结果（媒体/图片草稿返回对应结果，其余为 null）
     */
    private Object dispatchCreate(AgentDraftEntity entity) throws Exception {
        String contentJson = entity.getContentJson();
        String draftType = entity.getDraftType();
        return switch (draftType) {
            case "media_upload" -> null;
            case "image_generate" -> agentImageService.generate(AgentDraftImageResultCodec.readRequest(objectMapper, contentJson));
            default -> throw new BusinessException("不支持的草稿类型：" + draftType);
        };
    }

    private void recordFailure(AgentDraftEntity entity, Long ownerUserId, Long actorUserId, String reason) {
        String safeReason = safeFailure(reason);
        entity.setStatus(STATUS_ACTIVE);
        entity.setFailureReason(safeReason);
        entity.setUpdatedAt(System.currentTimeMillis());
        if (confirmationStateService != null) {
            try {
                confirmationStateService.recordFailure(entity.getId(), ownerUserId, safeReason);
            } catch (RuntimeException ignored) {
                // Preserve the original confirmation failure when the evidence sink is unavailable.
            }
        }
        recordDraftAction(entity, ownerUserId, actorUserId, "confirmation_failed", null, safeReason);
    }

    private void recordDraftAction(
        AgentDraftEntity entity,
        Long ownerUserId,
        Long actorUserId,
        String action,
        String businessReference,
        String reason
    ) {
        if (runAuditService == null) {
            return;
        }
        try {
            runAuditService.recordDraftAction(
                ownerUserId,
                entity.getRunId(),
                entity.getId(),
                entity.getDraftType(),
                action,
                actorUserId,
                businessReference,
                reason,
                System.currentTimeMillis()
            );
        } catch (RuntimeException ignored) {
            // Keep the business result when the independent audit sink is unavailable.
        }
    }

    private String businessReference(String draftType, Object created) {
        if (created == null) return null;
        try {
            JsonNode id = objectMapper.valueToTree(created).path("id");
            if (id.isMissingNode() || id.isNull()) return null;
            String value = id.isTextual() ? id.asText() : id.toString();
            if (value.isBlank()) return null;
            return draftType + ":" + (value.length() <= 120 ? value : value.substring(0, 120));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String safeFailure(String reason) {
        if (reason == null || reason.isBlank()) return "草稿确认失败";
        String normalized = reason.replaceAll("[\\r\\n\\t]+", " ").trim();
        normalized = normalized.replaceAll(
            "(?i)(api[_-]?key|token|secret|password|authorization|bearer)(\\s*[:=]\\s*)[^\\s,;]+",
            "$1$2***"
        );
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private V2AgentDtos.AgentDraftResponse toDraftResponse(AgentDraftEntity entity) {
        return new V2AgentDtos.AgentDraftResponse(
            entity.getId(),
            entity.getConversationId(),
            entity.getDraftType(),
            entity.getTitle(),
            entity.getContentJson(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            AgentDraftImageResultCodec.readPersistedResult(objectMapper, entity)
        );
    }
}

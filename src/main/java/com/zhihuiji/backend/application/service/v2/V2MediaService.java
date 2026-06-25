package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.media.V2MediaDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.domain.entity.MediaBindingEntity;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.repository.MediaBindingRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class V2MediaService {
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaBindingRepository mediaBindingRepository;
    private final CurrentOwnerService currentOwnerService;
    private final MediaStorageService mediaStorageService;

    public V2MediaService(
        MediaAssetRepository mediaAssetRepository,
        MediaBindingRepository mediaBindingRepository,
        CurrentOwnerService currentOwnerService,
        MediaStorageService mediaStorageService
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaBindingRepository = mediaBindingRepository;
        this.currentOwnerService = currentOwnerService;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional(readOnly = true)
    public List<V2MediaDtos.MediaAssetResponse> listAssets() {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<MediaAssetEntity> assets = mediaAssetRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        return assets.stream().map(this::toAssetResponse).toList();
    }

    @Transactional(readOnly = true)
    public V2MediaDtos.MediaAssetResponse getAsset(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        return toAssetResponse(getOwnedAsset(id, ownerUserId));
    }

    @Transactional
    public V2MediaDtos.MediaAssetResponse createAsset(V2MediaDtos.MediaAssetCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String objectKey = normalizeRequired(request.objectKey(), "objectKey 不能为空");
        if (mediaAssetRepository.existsByOwnerUserIdAndObjectKey(ownerUserId, objectKey)) {
            throw new IllegalArgumentException("objectKey 已存在");
        }
        long now = System.currentTimeMillis();
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setAssetType(normalizeRequired(request.assetType(), "assetType 不能为空"));
        entity.setStorageProvider(normalizeRequired(request.storageProvider(), "storageProvider 不能为空"));
        entity.setBucketName(normalizeOptional(request.bucketName()));
        entity.setObjectKey(objectKey);
        entity.setOriginalFileName(normalizeRequired(request.originalFileName(), "originalFileName 不能为空"));
        entity.setMimeType(normalizeRequired(request.mimeType(), "mimeType 不能为空"));
        entity.setSizeBytes(request.sizeBytes());
        entity.setChecksum(normalizeOptional(request.checksum()));
        entity.setWidth(request.width());
        entity.setHeight(request.height());
        entity.setMetadataJson(normalizeOptional(request.metadataJson()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toAssetResponse(mediaAssetRepository.save(entity));
    }

    @Transactional
    public V2MediaDtos.MediaAssetResponse uploadFile(MultipartFile file, String assetType) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String normalizedAssetType = normalizeRequired(assetType, "assetType 不能为空");
        String objectKey = mediaStorageService.store(file);
        V2MediaDtos.MediaAssetCreateRequest createRequest = new V2MediaDtos.MediaAssetCreateRequest(
            normalizedAssetType,
            "local",
            "local-disk",
            objectKey,
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            null,
            null,
            null,
            null
        );
        return createAsset(createRequest);
    }

    @Transactional
    public void deleteAsset(Long id) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        MediaAssetEntity entity = getOwnedAsset(id, ownerUserId);
        mediaBindingRepository.deleteAllByOwnerUserIdAndAssetId(ownerUserId, id);
        mediaAssetRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<V2MediaDtos.MediaBindingResponse> listBindings(String targetType, Long targetId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<MediaBindingEntity> bindings = mediaBindingRepository.findAllByOwnerUserIdAndTargetTypeAndTargetIdOrderBySortOrderAscIdAsc(
            ownerUserId,
            normalizeRequired(targetType, "targetType 不能为空"),
            targetId
        );
        return bindings.stream().map(this::toBindingResponse).toList();
    }

    @Transactional
    public V2MediaDtos.MediaBindingResponse createBinding(V2MediaDtos.MediaBindingCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        MediaAssetEntity asset = getOwnedAsset(request.assetId(), ownerUserId);
        String targetType = normalizeRequired(request.targetType(), "targetType 不能为空");
        if (mediaBindingRepository.existsByOwnerUserIdAndAssetIdAndTargetTypeAndTargetId(ownerUserId, asset.getId(), targetType, request.targetId())) {
            throw new IllegalArgumentException("媒体绑定已存在");
        }
        MediaBindingEntity entity = new MediaBindingEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setAssetId(asset.getId());
        entity.setTargetType(targetType);
        entity.setTargetId(request.targetId());
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        entity.setCreatedAt(System.currentTimeMillis());
        return toBindingResponse(mediaBindingRepository.save(entity));
    }

    @Transactional
    public void deleteBinding(Long id) {
        MediaBindingEntity entity = mediaBindingRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("媒体绑定不存在"));
        mediaBindingRepository.delete(entity);
    }

    private MediaAssetEntity getOwnedAsset(Long id, Long ownerUserId) {
        return mediaAssetRepository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new IllegalArgumentException("媒体资源不存在"));
    }

    private V2MediaDtos.MediaAssetResponse toAssetResponse(MediaAssetEntity entity) {
        return new V2MediaDtos.MediaAssetResponse(
            entity.getId(),
            entity.getAssetType(),
            entity.getStorageProvider(),
            entity.getBucketName(),
            entity.getObjectKey(),
            entity.getOriginalFileName(),
            entity.getMimeType(),
            entity.getSizeBytes(),
            entity.getChecksum(),
            entity.getWidth(),
            entity.getHeight(),
            entity.getMetadataJson(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private V2MediaDtos.MediaBindingResponse toBindingResponse(MediaBindingEntity entity) {
        return new V2MediaDtos.MediaBindingResponse(
            entity.getId(),
            entity.getAssetId(),
            entity.getTargetType(),
            entity.getTargetId(),
            entity.getSortOrder(),
            entity.getCreatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}

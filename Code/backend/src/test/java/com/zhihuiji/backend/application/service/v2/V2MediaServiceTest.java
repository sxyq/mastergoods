package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.media.V2MediaDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.MediaAssetEntity;
import com.zhihuiji.backend.domain.entity.MediaBindingEntity;
import com.zhihuiji.backend.infrastructure.repository.MediaAssetRepository;
import com.zhihuiji.backend.infrastructure.repository.MediaBindingRepository;
import com.zhihuiji.backend.infrastructure.storage.MediaStorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

class V2MediaServiceTest {
    @Mock
    private MediaAssetRepository mediaAssetRepository;
    @Mock
    private MediaBindingRepository mediaBindingRepository;
    @Mock
    private CurrentOwnerService currentOwnerService;
    @Mock
    private MediaStorageService mediaStorageService;

    private V2MediaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2MediaService(mediaAssetRepository, mediaBindingRepository, currentOwnerService, mediaStorageService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(mediaAssetRepository.save(any(MediaAssetEntity.class))).thenAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 101L);
            }
            return entity;
        });
        when(mediaBindingRepository.save(any(MediaBindingEntity.class))).thenAnswer(invocation -> {
            MediaBindingEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                setId(entity, 201L);
            }
            return entity;
        });
    }

    @Test
    void listAssetsReturnsOwnerScopedList() {
        MediaAssetEntity asset = asset(1L);
        when(mediaAssetRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(asset));

        List<V2MediaDtos.MediaAssetResponse> result = service.listAssets();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void getAssetReturnsDetail() {
        MediaAssetEntity asset = asset(1L);
        when(mediaAssetRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.of(asset));

        V2MediaDtos.MediaAssetResponse response = service.getAsset(1L);
        assertEquals(1L, response.id());
        assertEquals("media/1.png", response.objectKey());
    }

    @Test
    void getAssetRejectsForeignOwner() {
        when(mediaAssetRepository.findByIdAndOwnerUserId(1L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.getAsset(1L)
        );
        assertEquals("媒体资源不存在", error.getMessage());
    }

    @Test
    void createAssetRejectsDuplicateObjectKey() {
        when(mediaAssetRepository.existsByOwnerUserIdAndObjectKey(1L, "media/product-1.png")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createAsset(new V2MediaDtos.MediaAssetCreateRequest(
                "image",
                "s3",
                "goods",
                "media/product-1.png",
                "product-1.png",
                "image/png",
                1024L,
                null,
                640,
                480,
                "{\"scene\":\"product\"}"
            ))
        );

        assertEquals("objectKey 已存在", error.getMessage());
    }

    @Test
    void createAssetSavesWithOwnerUserId() {
        V2MediaDtos.MediaAssetResponse response = service.createAsset(
            new V2MediaDtos.MediaAssetCreateRequest(
                "image", "s3", "goods", "media/new.png", "new.png", "image/png", 2048L, null, 800, 600, null
            )
        );
        assertEquals(101L, response.id());

        ArgumentCaptor<MediaAssetEntity> captor = ArgumentCaptor.forClass(MediaAssetEntity.class);
        verify(mediaAssetRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals("media/new.png", captor.getValue().getObjectKey());
    }

    @Test
    void uploadFileStoresAndRegistersMetadata() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(mediaStorageService.store(file)).thenReturn("abc123def456.png");
        when(mediaAssetRepository.existsByOwnerUserIdAndObjectKey(1L, "abc123def456.png")).thenReturn(false);

        V2MediaDtos.MediaAssetResponse response = service.uploadFile(file, "product_image");

        assertEquals("abc123def456.png", response.objectKey());
        assertEquals("local", response.storageProvider());
        assertEquals("local-disk", response.bucketName());
        assertEquals("product_image", response.assetType());
        assertEquals("photo.png", response.originalFileName());
        assertEquals("image/png", response.mimeType());
        assertEquals(1024L, response.sizeBytes());

        verify(mediaStorageService).store(file);
        ArgumentCaptor<MediaAssetEntity> captor = ArgumentCaptor.forClass(MediaAssetEntity.class);
        verify(mediaAssetRepository).save(captor.capture());
        MediaAssetEntity saved = captor.getValue();
        assertEquals(1L, saved.getOwnerUserId());
        assertEquals("abc123def456.png", saved.getObjectKey());
        assertEquals("local", saved.getStorageProvider());
        assertEquals("local-disk", saved.getBucketName());
        assertEquals("product_image", saved.getAssetType());
        assertEquals("photo.png", saved.getOriginalFileName());
        assertEquals("image/png", saved.getMimeType());
        assertEquals(1024L, saved.getSizeBytes());
    }

    @Test
    void uploadFileRejectsEmptyFile() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.uploadFile(file, "product_image")
        );
        assertEquals("文件不能为空", error.getMessage());
    }

    @Test
    void uploadFileRejectsBlankAssetTypeBeforeStoring() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.uploadFile(file, "   ")
        );

        assertEquals("assetType 不能为空", error.getMessage());
        verify(mediaStorageService, never()).store(any());
    }

    @Test
    void uploadFileRejectsUnsupportedMimeAndOversizedFileBeforeStoring() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("payload.svg");
        when(file.getContentType()).thenReturn("image/svg+xml");
        when(file.getSize()).thenReturn(1024L);

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "product_image"));
        verify(mediaStorageService, never()).store(any());

        when(file.getOriginalFilename()).thenReturn("payload.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(10L * 1024L * 1024L + 1L);

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "product_image"));
        verify(mediaStorageService, never()).store(any());
    }

    @Test
    void uploadFileRemovesStoredObjectWhenMetadataRegistrationFails() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(mediaStorageService.store(file)).thenReturn("orphan.png");
        when(mediaAssetRepository.existsByOwnerUserIdAndObjectKey(1L, "orphan.png")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "product_image"));

        verify(mediaStorageService).store(file);
        verify(mediaStorageService).delete("orphan.png");
    }

    @Test
    void deleteAssetDeletesBindingsBeforeAsset() {
        MediaAssetEntity asset = asset(8L);
        when(mediaAssetRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(asset));

        service.deleteAsset(8L);

        verify(mediaBindingRepository).deleteAllByOwnerUserIdAndAssetId(1L, 8L);
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void listBindingsReturnsOwnerScopedRows() {
        MediaBindingEntity first = binding(11L, 7L, "product", 9L, 0);
        MediaBindingEntity second = binding(12L, 7L, "product", 9L, 1);
        when(mediaBindingRepository.findAllByOwnerUserIdAndTargetTypeAndTargetIdOrderBySortOrderAscIdAsc(1L, "product", 9L))
            .thenReturn(List.of(first, second));

        List<V2MediaDtos.MediaBindingResponse> result = service.listBindings("product", 9L);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).id());
        assertEquals(1, result.get(1).sortOrder());
    }

    @Test
    void createBindingDefaultsSortOrderAndUsesOwnedAsset() {
        MediaAssetEntity asset = asset(7L);
        when(mediaAssetRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(asset));
        when(mediaBindingRepository.existsByOwnerUserIdAndAssetIdAndTargetTypeAndTargetId(1L, 7L, "product", 9L))
            .thenReturn(false);

        V2MediaDtos.MediaBindingResponse response = service.createBinding(
            new V2MediaDtos.MediaBindingCreateRequest(7L, "product", 9L, null)
        );

        assertEquals(201L, response.id());
        assertEquals(0, response.sortOrder());

        ArgumentCaptor<MediaBindingEntity> captor = ArgumentCaptor.forClass(MediaBindingEntity.class);
        verify(mediaBindingRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals(7L, captor.getValue().getAssetId());
        assertEquals("product", captor.getValue().getTargetType());
        assertEquals(9L, captor.getValue().getTargetId());
        assertEquals(0, captor.getValue().getSortOrder());
    }

    @Test
    void deleteBindingRejectsForeignOwner() {
        when(mediaBindingRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.deleteBinding(5L)
        );
        assertEquals("媒体绑定不存在", error.getMessage());
    }

    @Test
    void createBindingRejectsDuplicateOwnerScopedBinding() {
        MediaAssetEntity asset = asset(7L);
        when(mediaAssetRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(asset));
        when(mediaBindingRepository.existsByOwnerUserIdAndAssetIdAndTargetTypeAndTargetId(1L, 7L, "product", 9L))
            .thenReturn(true);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.createBinding(new V2MediaDtos.MediaBindingCreateRequest(7L, "product", 9L, 0))
        );

        assertEquals("媒体绑定已存在", error.getMessage());
    }

    @Test
    void deleteBindingDeletesOwnedRow() {
        MediaBindingEntity entity = binding(5L, 7L, "product", 9L, 0);
        when(mediaBindingRepository.findByIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(entity));

        service.deleteBinding(5L);

        verify(mediaBindingRepository).delete(entity);
    }

    private MediaAssetEntity asset(Long id) {
        MediaAssetEntity entity = new MediaAssetEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setAssetType("image");
        entity.setStorageProvider("s3");
        entity.setBucketName("goods");
        entity.setObjectKey("media/" + id + ".png");
        entity.setOriginalFileName("asset-" + id + ".png");
        entity.setMimeType("image/png");
        entity.setSizeBytes(2048L);
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(10L);
        return entity;
    }

    private void setId(MediaAssetEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = MediaAssetEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setId(MediaBindingEntity entity, Long id) {
        try {
            java.lang.reflect.Field field = MediaBindingEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private MediaBindingEntity binding(Long id, Long assetId, String targetType, Long targetId, Integer sortOrder) {
        MediaBindingEntity entity = new MediaBindingEntity();
        setId(entity, id);
        entity.setOwnerUserId(1L);
        entity.setAssetId(assetId);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setSortOrder(sortOrder);
        entity.setCreatedAt(10L + id);
        return entity;
    }
}

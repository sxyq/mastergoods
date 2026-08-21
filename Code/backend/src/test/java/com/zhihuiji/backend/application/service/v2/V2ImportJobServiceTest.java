package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.api.dto.v2.sync.V2ImportJobDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.ImportJobRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ImportJobServiceTest {
    @Mock private ImportJobRepository importJobRepository;
    @Mock private CurrentOwnerService currentOwnerService;

    private V2ImportJobService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ImportJobService(importJobRepository, currentOwnerService);
        when(currentOwnerService.requireCurrentOwnerUserId()).thenReturn(1L);
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void retryRejectsSucceededJob() {
        when(importJobRepository.findByIdAndOwnerUserId(7L, 1L)).thenReturn(Optional.of(job(7L, V2ImportJobService.STATUS_SUCCEEDED)));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.retry(7L, new V2ImportJobDtos.ImportJobRetryRequest("123|product|9"))
        );

        assertEquals("只有失败或已取消的导入任务才能重试", error.getMessage());
    }

    @Test
    void retryResetsFailedJobToPendingReplayReady() {
        ImportJobEntity entity = job(8L, V2ImportJobService.STATUS_FAILED);
        entity.setRetryCount(2);
        entity.setFailureCode("IMPORT_FAILED");
        entity.setFailureMessage("boom");
        entity.setStartedAt(100L);
        entity.setFinishedAt(200L);
        entity.setLastHeartbeatAt(150L);
        when(importJobRepository.findByIdAndOwnerUserId(8L, 1L)).thenReturn(Optional.of(entity));

        V2ImportJobDtos.ImportJobResponse response = service.retry(
            8L,
            new V2ImportJobDtos.ImportJobRetryRequest("300|product|7")
        );

        assertEquals(V2ImportJobService.STATUS_PENDING, response.status());
        assertEquals(V2ImportJobService.STAGE_REPLAY_READY, response.stage());
        assertEquals(3, response.retryCount());
        assertEquals("300|product|7", response.replayCursor());
        assertEquals(null, response.failureCode());
        assertEquals(null, response.failureMessage());
        assertEquals(null, response.startedAt());
        assertEquals(null, response.finishedAt());
        assertEquals(null, response.lastHeartbeatAt());
    }

    @Test
    void retryAllowsCancelledJob() {
        ImportJobEntity entity = job(9L, V2ImportJobService.STATUS_CANCELLED);
        entity.setRetryCount(1);
        entity.setFailureCode("CANCELLED");
        entity.setFailureMessage("user cancelled");
        entity.setFinishedAt(220L);
        when(importJobRepository.findByIdAndOwnerUserId(9L, 1L)).thenReturn(Optional.of(entity));

        V2ImportJobDtos.ImportJobResponse response = service.retry(
            9L,
            new V2ImportJobDtos.ImportJobRetryRequest("456|product|3")
        );

        assertEquals(V2ImportJobService.STATUS_PENDING, response.status());
        assertEquals(V2ImportJobService.STAGE_REPLAY_READY, response.stage());
        assertEquals(2, response.retryCount());
        assertEquals("456|product|3", response.replayCursor());
        assertEquals(null, response.failureCode());
        assertEquals(null, response.failureMessage());
        assertEquals(null, response.finishedAt());
    }

    @Test
    void retryRejectsRunningPendingAndSucceededJobs() {
        assertRetryRejected(10L, V2ImportJobService.STATUS_RUNNING);
        assertRetryRejected(11L, V2ImportJobService.STATUS_PENDING);
        assertRetryRejected(12L, V2ImportJobService.STATUS_SUCCEEDED);
    }

    @Test
    void cancelAllowsPendingJob() {
        ImportJobEntity entity = job(13L, V2ImportJobService.STATUS_PENDING);
        when(importJobRepository.findByIdAndOwnerUserId(13L, 1L)).thenReturn(Optional.of(entity));

        V2ImportJobDtos.ImportJobResponse response = service.cancel(13L);

        assertEquals(V2ImportJobService.STATUS_CANCELLED, response.status());
        assertEquals(V2ImportJobService.STAGE_CANCELLED, response.stage());
        assertEquals(response.updatedAt(), response.finishedAt());
    }

    @Test
    void cancelAllowsRunningJob() {
        ImportJobEntity entity = job(14L, V2ImportJobService.STATUS_RUNNING);
        entity.setStartedAt(150L);
        entity.setLastHeartbeatAt(180L);
        when(importJobRepository.findByIdAndOwnerUserId(14L, 1L)).thenReturn(Optional.of(entity));

        V2ImportJobDtos.ImportJobResponse response = service.cancel(14L);

        assertEquals(V2ImportJobService.STATUS_CANCELLED, response.status());
        assertEquals(V2ImportJobService.STAGE_CANCELLED, response.stage());
        assertEquals(response.updatedAt(), response.finishedAt());
    }

    @Test
    void cancelRejectsFailedSucceededAndCancelledJobs() {
        assertCancelRejected(15L, V2ImportJobService.STATUS_FAILED);
        assertCancelRejected(16L, V2ImportJobService.STATUS_SUCCEEDED);
        assertCancelRejected(17L, V2ImportJobService.STATUS_CANCELLED);
    }

    private void assertRetryRejected(Long id, String status) {
        when(importJobRepository.findByIdAndOwnerUserId(id, 1L)).thenReturn(Optional.of(job(id, status)));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.retry(id, new V2ImportJobDtos.ImportJobRetryRequest("123|product|9"))
        );

        assertEquals("只有失败或已取消的导入任务才能重试", error.getMessage());
    }

    private void assertCancelRejected(Long id, String status) {
        when(importJobRepository.findByIdAndOwnerUserId(id, 1L)).thenReturn(Optional.of(job(id, status)));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.cancel(id)
        );

        assertEquals("只有未完成的导入任务才能取消", error.getMessage());
    }

    private ImportJobEntity job(Long id, String status) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(id);
        entity.setOwnerUserId(1L);
        entity.setRequestedByUserId(1L);
        entity.setClientId("device-a");
        entity.setSourceType("kingdee_android_sqlite");
        entity.setStatus(status);
        entity.setStage(V2ImportJobService.STAGE_ACCEPTED);
        entity.setRetryCount(0);
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(10L);
        return entity;
    }
}

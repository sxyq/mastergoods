package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.ImportJobRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class V2ImportJobWorkerServiceTest {
    @Mock
    private ImportJobRepository importJobRepository;

    private V2ImportJobWorkerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ImportJobWorkerService(importJobRepository);
    }

    @Test
    void claimNextPendingJobMarksRunning() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(7L);
        entity.setStatus(V2ImportJobService.STATUS_PENDING);
        entity.setStage(V2ImportJobService.STAGE_ACCEPTED);
        entity.setCreatedAt(100L);
        entity.setUpdatedAt(100L);
        when(importJobRepository.findTop5ByStatusOrderByUpdatedAtAscCreatedAtAscIdAsc(V2ImportJobService.STATUS_PENDING))
            .thenReturn(List.of(entity));

        Long jobId = service.claimNextPendingJobId();

        assertEquals(7L, jobId);
        assertEquals(V2ImportJobService.STATUS_RUNNING, entity.getStatus());
        assertEquals("importing", entity.getStage());
        verify(importJobRepository).save(entity);
    }

    @Test
    void executeClaimedJobMarksLegacySourceAsFailedUntilImporterReimplemented() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(9L);
        entity.setOwnerUserId(1L);
        entity.setStatus(V2ImportJobService.STATUS_RUNNING);
        entity.setSourceType("legacy_sqlite");
        entity.setSourceUri("/tmp/legacy.db");
        when(importJobRepository.findById(9L)).thenReturn(Optional.of(entity));

        service.executeClaimedJob(9L);

        assertEquals(V2ImportJobService.STATUS_FAILED, entity.getStatus());
        assertEquals("failed", entity.getStage());
        assertEquals("invalid_request", entity.getFailureCode());
        assertEquals(true, entity.getFailureMessage() != null
            && entity.getFailureMessage().contains("暂不支持的导入来源"));
    }

    @Test
    void executeClaimedJobMarksUnsupportedSourceAsFailed() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(12L);
        entity.setOwnerUserId(1L);
        entity.setStatus(V2ImportJobService.STATUS_RUNNING);
        entity.setSourceType("csv_file");
        when(importJobRepository.findById(12L)).thenReturn(Optional.of(entity));

        service.executeClaimedJob(12L);

        assertEquals(V2ImportJobService.STATUS_FAILED, entity.getStatus());
        assertEquals("failed", entity.getStage());
        assertEquals("invalid_request", entity.getFailureCode());
    }

    @Test
    void recoverStaleJobsMarksRunningJobsAsFailedWhenHeartbeatExceeded() {
        ImportJobEntity staleJob = new ImportJobEntity();
        staleJob.setId(20L);
        staleJob.setStatus(V2ImportJobService.STATUS_RUNNING);
        staleJob.setLastHeartbeatAt(System.currentTimeMillis() - 20 * 60 * 1000L);
        when(importJobRepository.findByStatusAndLastHeartbeatAtBefore(
            eq(V2ImportJobService.STATUS_RUNNING),
            any(Long.class)
        )).thenReturn(List.of(staleJob));

        service.recoverStaleJobs();

        assertEquals(V2ImportJobService.STATUS_FAILED, staleJob.getStatus());
        assertEquals("failed", staleJob.getStage());
        assertEquals("heartbeat_timeout", staleJob.getFailureCode());
        verify(importJobRepository).save(staleJob);
    }

    @Test
    void recoverStaleJobsDoesNothingWhenNoStaleJobs() {
        when(importJobRepository.findByStatusAndLastHeartbeatAtBefore(
            eq(V2ImportJobService.STATUS_RUNNING),
            any(Long.class)
        )).thenReturn(List.of());

        service.recoverStaleJobs();

        verify(importJobRepository, never()).save(any());
    }
}

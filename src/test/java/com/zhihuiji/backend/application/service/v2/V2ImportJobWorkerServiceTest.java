package com.zhihuiji.backend.application.service.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.LegacySQLiteImportService;
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
    @Mock
    private LegacySQLiteImportService legacySQLiteImportService;

    private V2ImportJobWorkerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new V2ImportJobWorkerService(importJobRepository, legacySQLiteImportService, new ObjectMapper());
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
    void executeClaimedJobImportsLegacySqliteAndStoresSummary() throws Exception {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(9L);
        entity.setOwnerUserId(1L);
        entity.setStatus(V2ImportJobService.STATUS_RUNNING);
        entity.setSourceType("legacy_sqlite");
        entity.setSourceUri("/tmp/legacy.db");
        entity.setOptionsJson("{\"resetOwnedData\":true}");
        when(importJobRepository.findById(9L)).thenReturn(Optional.of(entity));
        when(legacySQLiteImportService.importIntoExistingOwner(any(), any())).thenReturn(
            new LegacySQLiteImportService.ImportResult(1L, "13800000000", "店主", "/tmp/legacy.db", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        );

        service.executeClaimedJob(9L);

        assertEquals(V2ImportJobService.STATUS_SUCCEEDED, entity.getStatus());
        assertEquals("completed", entity.getStage());
        assertNull(entity.getFailureCode());
        assertNull(entity.getFailureMessage());
        verify(importJobRepository).save(entity);
        verify(legacySQLiteImportService).importIntoExistingOwner(
            1L,
            new LegacySQLiteImportService.ExistingOwnerImportRequest("/tmp/legacy.db", true)
        );
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
}

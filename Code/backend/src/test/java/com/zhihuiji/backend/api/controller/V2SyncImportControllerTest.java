package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2ImportJobController;
import com.zhihuiji.backend.api.controller.v2.V2SyncController;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.application.service.LegacySQLiteImportService;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2ImportJobService;
import com.zhihuiji.backend.application.service.v2.V2SyncService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2SyncController.class,
    V2ImportJobController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2SyncImportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2SyncService v2SyncService;
    @MockBean
    private V2ImportJobService v2ImportJobService;
    @MockBean
    private SessionAccessService sessionAccessService;
    @MockBean
    private LegacySQLiteImportService legacySQLiteImportService;
    @MockBean
    private CurrentOwnerService currentOwnerService;

    @Test
    void syncHealthReturnsOwnerScopedContract() throws Exception {
        when(v2SyncService.health()).thenReturn(new V2SyncService.HealthResult(
            "ok",
            "owner scoped sync ready",
            true,
            1000L,
            List.of("product", "sale_order"),
            List.of("product")
        ));

        mockMvc.perform(get("/v2/sync/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.owner_scoped").value(true))
            .andExpect(jsonPath("$.data.supported_entity_types[0]").value("product"));
    }

    @Test
    void syncPullReturnsSnakeCaseFields() throws Exception {
        when(v2SyncService.pull(anyString(), any(), any())).thenReturn(new V2SyncService.PullResult(
            List.of(new V2SyncService.SyncChange("product", "7", "upsert", "{\"id\":7}", 2000L)),
            "1000",
            "2000",
            false
        ));

        mockMvc.perform(post("/v2/sync/pull")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "client_id": "device-a",
                      "since_cursor": "1000",
                      "limit": 50
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.effective_cursor").value("1000"))
            .andExpect(jsonPath("$.data.next_cursor").value("2000"))
            .andExpect(jsonPath("$.data.changes[0].entity_type").value("product"));
    }

    @Test
    void syncUploadReturnsOperationFailureDetails() throws Exception {
        when(v2SyncService.upload(anyString(), any(), any())).thenReturn(new V2SyncService.UploadResult(
            0,
            1,
            "partially_applied",
            "100|product|7",
            List.of(),
            List.of("op-conflict-1"),
            List.of(new V2SyncService.SyncOperationFailure(
                "op-conflict-1",
                "version_conflict",
                "sync version conflict: expected 1, current 2"
            )),
            List.of(new V2SyncService.SyncOperationResult(
                "op-conflict-1",
                "conflict",
                "version_conflict",
                "sync version conflict: expected 1, current 2",
                2L,
                List.of("name"),
                "{\"id\":7,\"name\":\"服务器商品\"}"
            ))
        ));

        mockMvc.perform(post("/v2/sync/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "client_id": "device-a",
                      "changes": [{
                        "operation_id": "op-conflict-1",
                        "entity_type": "product",
                        "entity_id": "7",
                        "operation": "update",
                        "base_version": 1
                      }]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.failed_operation_ids[0]").value("op-conflict-1"))
            .andExpect(jsonPath("$.data.failures[0].code").value("version_conflict"))
            .andExpect(jsonPath("$.data.failures[0].operation_id").value("op-conflict-1"))
            .andExpect(jsonPath("$.data.operation_results[0].server_version").value(2))
            .andExpect(jsonPath("$.data.operation_results[0].conflict_fields[0]").value("name"))
            .andExpect(jsonPath("$.data.operation_results[0].server_payload").value("{\"id\":7,\"name\":\"服务器商品\"}"));
    }

    @Test
    void importJobCreateReturnsSnakeCaseFields() throws Exception {
        when(v2ImportJobService.create(any())).thenReturn(new com.zhihuiji.backend.api.dto.v2.sync.V2ImportJobDtos.ImportJobResponse(
            3L,
            "device-a",
            "kingdee_android_sqlite",
            "/sdcard/legacy.db",
            "sha256-demo",
            "idem-1",
            "pending",
            "accepted",
            0,
            "0",
            null,
            "{\"mode\":\"dry-run\"}",
            null,
            null,
            1000L,
            1000L,
            null,
            null,
            null
        ));

        mockMvc.perform(post("/v2/import-jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "client_id": "device-a",
                      "source_type": "kingdee_android_sqlite",
                      "source_uri": "/sdcard/legacy.db",
                      "idempotency_key": "idem-1",
                      "options_json": "{\\\"mode\\\":\\\"dry-run\\\"}"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.source_type").value("kingdee_android_sqlite"))
            .andExpect(jsonPath("$.data.idempotency_key").value("idem-1"))
            .andExpect(jsonPath("$.data.retry_count").value(0));
    }
}

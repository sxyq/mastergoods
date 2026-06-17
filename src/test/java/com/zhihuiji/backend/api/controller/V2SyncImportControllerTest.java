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

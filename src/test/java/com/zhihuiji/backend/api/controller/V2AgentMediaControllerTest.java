package com.zhihuiji.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhihuiji.backend.api.controller.v2.V2AgentController;
import com.zhihuiji.backend.api.controller.v2.V2MediaController;
import com.zhihuiji.backend.api.dto.v2.agent.V2AgentDtos;
import com.zhihuiji.backend.api.dto.v2.media.V2MediaDtos;
import com.zhihuiji.backend.application.service.SessionAccessService;
import com.zhihuiji.backend.application.service.v2.V2AgentAiService;
import com.zhihuiji.backend.application.service.v2.V2AgentConversationService;
import com.zhihuiji.backend.application.service.v2.V2MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    V2AgentController.class,
    V2MediaController.class
})
@AutoConfigureMockMvc(addFilters = false)
class V2AgentMediaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private V2AgentConversationService v2AgentConversationService;
    @MockBean
    private V2AgentAiService v2AgentAiService;
    @MockBean
    private V2MediaService v2MediaService;
    @MockBean
    private SessionAccessService sessionAccessService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Agent Conversation tests ---

    @Test
    void listConversationsReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.listConversations(null, null)).thenReturn(List.of(
            new V2AgentDtos.AgentConversationResponse(1L, "采购讨论", "active", "最近讨论了补货计划", 1L, 2L, 2L)
        ));

        mockMvc.perform(get("/v2/agent/conversations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].latest_summary").value("最近讨论了补货计划"))
            .andExpect(jsonPath("$.data[0].last_message_at").value(2L));

        verify(v2AgentConversationService).listConversations(null, null);
    }

    @Test
    void listConversationsBindsPageAndLimitQuery() throws Exception {
        when(v2AgentConversationService.listConversations(1, 20)).thenReturn(List.of(
            new V2AgentDtos.AgentConversationResponse(1L, "采购讨论", "active", "摘要", 1L, 2L, 2L)
        ));

        mockMvc.perform(get("/v2/agent/conversations").param("page", "1").param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(1));

        verify(v2AgentConversationService).listConversations(1, 20);
    }

    @Test
    void getConversationReturnsDetail() throws Exception {
        when(v2AgentConversationService.getConversation(1L)).thenReturn(
            new V2AgentDtos.AgentConversationResponse(1L, "采购讨论", "active", "摘要", 1L, 2L, 2L)
        );

        mockMvc.perform(get("/v2/agent/conversations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("采购讨论"));
    }

    @Test
    void createConversationReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.createConversation(any())).thenReturn(
            new V2AgentDtos.AgentConversationResponse(2L, "新会话", "active", null, 10L, 10L, null)
        );

        mockMvc.perform(post("/v2/agent/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "新会话"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(2))
            .andExpect(jsonPath("$.data.title").value("新会话"));
    }

    @Test
    void updateConversationReturnsUpdatedFields() throws Exception {
        when(v2AgentConversationService.updateConversation(eq(1L), any())).thenReturn(
            new V2AgentDtos.AgentConversationResponse(1L, "更新标题", "closed", "摘要", 1L, 20L, 2L)
        );

        mockMvc.perform(put("/v2/agent/conversations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "更新标题",
                      "status": "closed"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("更新标题"))
            .andExpect(jsonPath("$.data.status").value("closed"));
    }

    @Test
    void deleteConversationReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/v2/agent/conversations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createMessageReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.createMessage(eq(7L), any())).thenReturn(
            new V2AgentDtos.AgentMessageResponse(3L, 7L, "user", "text", "帮我整理应付账款", null, 10L)
        );

        mockMvc.perform(post("/v2/agent/conversations/7/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "role": "user",
                      "message_type": "text",
                      "content": "帮我整理应付账款"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.conversation_id").value(7))
            .andExpect(jsonPath("$.data.message_type").value("text"));
    }

    @Test
    void listMessagesReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.listMessages(7L, null, null)).thenReturn(List.of(
            new V2AgentDtos.AgentMessageResponse(3L, 7L, "assistant", "summary", "这里是摘要", "{\"kind\":\"summary\"}", 10L)
        ));

        mockMvc.perform(get("/v2/agent/conversations/7/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].conversation_id").value(7))
            .andExpect(jsonPath("$.data[0].message_type").value("summary"))
            .andExpect(jsonPath("$.data[0].structured_data_json").value("{\"kind\":\"summary\"}"));

        verify(v2AgentConversationService).listMessages(7L, null, null);
    }

    @Test
    void listMessagesBindsPageAndLimitQuery() throws Exception {
        when(v2AgentConversationService.listMessages(7L, 2, 25)).thenReturn(List.of(
            new V2AgentDtos.AgentMessageResponse(4L, 7L, "user", "text", "正文", null, 11L)
        ));

        mockMvc.perform(get("/v2/agent/conversations/7/messages").param("page", "2").param("limit", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(4));

        verify(v2AgentConversationService).listMessages(7L, 2, 25);
    }

    // --- Agent Draft tests ---

    @Test
    void listDraftsReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.listDrafts(7L, null, null)).thenReturn(List.of(
            new V2AgentDtos.AgentDraftResponse(8L, 7L, "operation", "采购建议", "{\"sku\":1}", "active", 10L, 11L)
        ));

        mockMvc.perform(get("/v2/agent/drafts").param("conversation_id", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].conversation_id").value(7))
            .andExpect(jsonPath("$.data[0].draft_type").value("operation"))
            .andExpect(jsonPath("$.data[0].content_json").value("{\"sku\":1}"));

        verify(v2AgentConversationService).listDrafts(7L, null, null);
    }

    @Test
    void listDraftsBindsConversationPageAndLimitQuery() throws Exception {
        when(v2AgentConversationService.listDrafts(7L, 1, 20)).thenReturn(List.of(
            new V2AgentDtos.AgentDraftResponse(8L, 7L, "operation", "采购建议", "{\"sku\":1}", "active", 10L, 11L)
        ));

        mockMvc.perform(get("/v2/agent/drafts")
                .param("conversation_id", "7")
                .param("page", "1")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(8));

        verify(v2AgentConversationService).listDrafts(7L, 1, 20);
    }

    @Test
    void createDraftReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.createDraft(any())).thenReturn(
            new V2AgentDtos.AgentDraftResponse(9L, 7L, "operation", "新草稿", "{\"action\":\"purchase\"}", "active", 12L, 12L)
        );

        mockMvc.perform(post("/v2/agent/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "conversation_id": 7,
                      "draft_type": "operation",
                      "title": "新草稿",
                      "content_json": "{\\\"action\\\":\\\"purchase\\\"}",
                      "status": "active"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.conversation_id").value(7))
            .andExpect(jsonPath("$.data.draft_type").value("operation"))
            .andExpect(jsonPath("$.data.content_json").value("{\"action\":\"purchase\"}"));
    }

    @Test
    void updateDraftReturnsSnakeCaseFields() throws Exception {
        when(v2AgentConversationService.updateDraft(eq(9L), any())).thenReturn(
            new V2AgentDtos.AgentDraftResponse(9L, 7L, "operation", "更新草稿", "{\"action\":\"sync\"}", "archived", 12L, 18L)
        );

        mockMvc.perform(put("/v2/agent/drafts/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "conversation_id": 7,
                      "draft_type": "operation",
                      "title": "更新草稿",
                      "content_json": "{\\\"action\\\":\\\"sync\\\"}",
                      "status": "archived"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("更新草稿"))
            .andExpect(jsonPath("$.data.status").value("archived"));
    }

    @Test
    void deleteDraftReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/v2/agent/drafts/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getRunAuditReturnsSnakeCaseFieldsAndEvents() throws Exception {
        when(v2AgentAiService.getRunAudit("run-1")).thenReturn(
            new V2AgentDtos.AgentRunAuditResponse(
                "run-1",
                1L,
                7L,
                "completed",
                "tool_query_llm_streamed",
                "streaming",
                "keyword",
                1,
                2,
                1,
                0,
                true,
                3,
                List.of("audit_events_dropped:1"),
                "run-1:audit",
                "run-1:trace",
                null,
                null,
                100L,
                180L,
                180L,
                List.of(
                    new V2AgentDtos.AgentRunAuditEventResponse(
                        "run-1:1",
                        1,
                        "run_started",
                        objectMapper.readTree("{\"run_id\":\"run-1\",\"event_type\":\"run_started\"}"),
                        101L
                    )
                )
            )
        );

        mockMvc.perform(get("/v2/agent/runs/run-1/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.run_id").value("run-1"))
            .andExpect(jsonPath("$.data.conversation_id").value(7))
            .andExpect(jsonPath("$.data.llm_status").value("streaming"))
            .andExpect(jsonPath("$.data.event_count").value(2))
            .andExpect(jsonPath("$.data.audit_write_dropped_count").value(1))
            .andExpect(jsonPath("$.data.audit_write_failed_count").value(0))
            .andExpect(jsonPath("$.data.audit_lossy").value(true))
            .andExpect(jsonPath("$.data.emitted_event_count").value(3))
            .andExpect(jsonPath("$.data.warnings[0]").value("audit_events_dropped:1"))
            .andExpect(jsonPath("$.data.events[0].event_id").value("run-1:1"))
            .andExpect(jsonPath("$.data.events[0].event_type").value("run_started"))
            .andExpect(jsonPath("$.data.events[0].payload.run_id").value("run-1"));
    }

    // --- Media Asset tests ---

    @Test
    void listAssetsReturnsSnakeCaseFields() throws Exception {
        when(v2MediaService.listAssets()).thenReturn(List.of(
            new V2MediaDtos.MediaAssetResponse(1L, "image", "s3", "goods", "media/1.png", "1.png", "image/png", 1024L, null, 640, 480, null, 10L, 10L)
        ));

        mockMvc.perform(get("/v2/media/assets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].asset_type").value("image"))
            .andExpect(jsonPath("$.data[0].size_bytes").value(1024));
    }

    @Test
    void getAssetReturnsDetail() throws Exception {
        when(v2MediaService.getAsset(1L)).thenReturn(
            new V2MediaDtos.MediaAssetResponse(1L, "image", "s3", "goods", "media/1.png", "1.png", "image/png", 1024L, null, 640, 480, null, 10L, 10L)
        );

        mockMvc.perform(get("/v2/media/assets/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.object_key").value("media/1.png"));
    }

    @Test
    void deleteAssetReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/v2/media/assets/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createAssetReturnsSnakeCaseFields() throws Exception {
        when(v2MediaService.createAsset(any())).thenReturn(
            new V2MediaDtos.MediaAssetResponse(2L, "image", "s3", "goods", "media/2.png", "2.png", "image/png", 2048L, null, 800, 600, null, 12L, 12L)
        );

        mockMvc.perform(post("/v2/media/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "asset_type": "image",
                      "storage_provider": "s3",
                      "bucket_name": "goods",
                      "object_key": "media/2.png",
                      "original_file_name": "2.png",
                      "mime_type": "image/png",
                      "size_bytes": 2048,
                      "width": 800,
                      "height": 600
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.asset_type").value("image"))
            .andExpect(jsonPath("$.data.object_key").value("media/2.png"));
    }

    @Test
    void createAssetRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/v2/media/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "asset_type": "",
                      "storage_provider": "",
                      "object_key": "",
                      "original_file_name": "",
                      "mime_type": "",
                      "size_bytes": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    // --- Media Binding tests ---

    @Test
    void listBindingsReturnsSnakeCaseFields() throws Exception {
        when(v2MediaService.listBindings("product", 9L)).thenReturn(List.of(
            new V2MediaDtos.MediaBindingResponse(5L, 3L, "product", 9L, 0, 8L)
        ));

        mockMvc.perform(get("/v2/media/bindings")
                .param("target_type", "product")
                .param("target_id", "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].asset_id").value(3))
            .andExpect(jsonPath("$.data[0].sort_order").value(0));
    }

    @Test
    void deleteBindingReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/v2/media/bindings/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createBindingReturnsSnakeCaseFields() throws Exception {
        when(v2MediaService.createBinding(any())).thenReturn(
            new V2MediaDtos.MediaBindingResponse(6L, 3L, "product", 9L, 1, 12L)
        );

        mockMvc.perform(post("/v2/media/bindings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "asset_id": 3,
                      "target_type": "product",
                      "target_id": 9,
                      "sort_order": 1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.asset_id").value(3))
            .andExpect(jsonPath("$.data.target_type").value("product"))
            .andExpect(jsonPath("$.data.sort_order").value(1));
    }
}

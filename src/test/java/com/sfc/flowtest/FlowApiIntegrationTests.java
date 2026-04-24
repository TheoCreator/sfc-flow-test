package com.sfc.flowtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfc.flowtest.publish.hook.MockThirdPartyPublishHook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FlowApiIntegrationTests {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private MockThirdPartyPublishHook mockThirdPartyPublishHook;

    @Test
    void completeHappyPathFlow() throws Exception {
        long id = create("主流程测试稿件", "正文A");

        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("PENDING_1ST"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_2ND"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_3RD"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(mockThirdPartyPublishHook, atLeastOnce()).publish(ArgumentMatchers.any());
    }

    @Test
    void rejectAndResubmitFlowWorks() throws Exception {
        long id = create("退回重提测试稿件", "正文B");

        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_1ST"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_2ND"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"opinion":"二审意见：请补充出处"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReviewLevel").value(2));

        mockMvc.perform(put("/api/v1/manuscripts/{id}/draft", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"退回重提测试稿件-已修改","body":"正文B-已修订"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/resubmit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_1ST"));

        mockMvc.perform(get("/api/v1/manuscripts/{id}/records", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void rejectRequiresOpinion() throws Exception {
        long id = create("退回必填意见稿件", "正文C");

        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", id))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/manuscripts/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"opinion":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("40004"));
    }

    @Test
    void listSupportsKeywordPaginationAndMultiStatuses() throws Exception {
        long draftId = create("搜索关键词-草稿", "AlphaBody");
        long pendingId = create("普通稿件", "包含关键字：BetaBody");
        long completedId = create("已完成目标稿件", "正文Gamma");

        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", pendingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_1ST"));

        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", completedId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", completedId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", completedId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/manuscripts/{id}/approve", completedId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manuscripts")
                        .param("keyword", "关键词")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].id").value(draftId));

        mockMvc.perform(get("/api/v1/manuscripts")
                        .param("statuses", "PENDING_1ST,COMPLETED")
                        .param("pageNo", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/manuscripts")
                        .param("statuses", "COMPLETED")
                        .param("keyword", "已完成目标稿件")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].id").value(completedId));

        mockMvc.perform(get("/api/v1/manuscripts")
                        .param("pageNo", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void countEndpointMatchesListFilters() throws Exception {
        String unique = "COUNT-UNIQUE-" + UUID.randomUUID();
        long id = create(unique, "正文仅用于统计接口测试");

        mockMvc.perform(get("/api/v1/manuscripts/count").param("keyword", unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/v1/manuscripts")
                        .param("keyword", unique)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id));
    }

    @Test
    void uploadAssetWorksInDraftAndServesBinary() throws Exception {
        long id = create("素材上传稿件", "<p>正文含图</p>");
        MockMultipartFile file = new MockMultipartFile("file", "pixel.png", "image/png", ONE_PIXEL_PNG);
        MvcResult upload = mockMvc.perform(multipart("/api/v1/manuscripts/{id}/assets", id).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.url").isString())
                .andExpect(jsonPath("$.data.location").isString())
                .andReturn();
        long assetId = objectMapper.readTree(upload.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/manuscripts/assets/{assetId}", assetId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    void uploadAssetRejectedWhenNotEditable() throws Exception {
        long id = create("不可上传素材稿件", "<p>正文</p>");
        mockMvc.perform(post("/api/v1/manuscripts/{id}/submit", id))
                .andExpect(status().isOk());
        MockMultipartFile file = new MockMultipartFile("file", "pixel.png", "image/png", ONE_PIXEL_PNG);
        mockMvc.perform(multipart("/api/v1/manuscripts/{id}/assets", id).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("40003"));
    }

    @Test
    void uploadAssetRejectsUnsupportedMime() throws Exception {
        long id = create("非法类型素材", "<p>正文</p>");
        MockMultipartFile file = new MockMultipartFile("file", "x.bin", "application/octet-stream", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/manuscripts/{id}/assets", id).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("40007"));
    }

    private long create(String title, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/manuscripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","body":"%s"}
                                """.formatted(title, body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}

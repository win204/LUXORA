package com.luxora.commerce.aws.s3.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxora.commerce.aws.s3.S3StorageService;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlRequest;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.test.context.ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class MediaApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private S3StorageService storageService;

    @Test
    void presignedUploadUrlReturns200WithUrl() throws Exception {
        PresignedUploadUrlResponse mockResponse = new PresignedUploadUrlResponse(
                "https://s3.amazonaws.com/test-bucket/products/abc.png?sig=xyz",
                "products/abc.png",
                "https://test-bucket.s3.amazonaws.com/products/abc.png",
                Instant.now().plusSeconds(900)
        );

        when(storageService.generatePresignedUploadUrl(any(PresignedUploadUrlRequest.class)))
                .thenReturn(mockResponse);

        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("laptop.png", "image/png", "products");

        mockMvc.perform(post("/api/v1/media/presigned-upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(mockResponse.uploadUrl()))
                .andExpect(jsonPath("$.fileKey").value("products/abc.png"))
                .andExpect(jsonPath("$.publicUrl").value(mockResponse.publicUrl()));
    }
}

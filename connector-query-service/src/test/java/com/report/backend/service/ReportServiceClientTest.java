package com.report.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceClientTest {

    private ReportServiceClient client;

    @Mock
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        client = new ReportServiceClient("http://localhost:8084");
        ReflectionTestUtils.setField(client, "restClient", restClient);
    }

    @Test
    void isQueryMappedToTemplate_Mapped_ShouldReturnTrue() {
        mockResponse(true);
        assertTrue(client.isQueryMappedToTemplate("q1"));
    }

    @Test
    void isQueryMappedToTemplate_NotMapped_ShouldReturnFalse() {
        mockResponse(false);
        assertFalse(client.isQueryMappedToTemplate("q1"));
    }

    @Test
    void isQueryMappedToTemplate_Error_ShouldReturnTrue() {
        // Any exception should return true (fail closed)
        when(restClient.get()).thenThrow(new RuntimeException("API Error"));
        assertTrue(client.isQueryMappedToTemplate("q1"));
    }

    private void mockResponse(Boolean responseBody) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        // Correctly mock the multi-argument uri method
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Boolean.class)).thenReturn(responseBody);
    }
}

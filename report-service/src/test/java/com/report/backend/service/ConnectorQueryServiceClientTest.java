package com.report.backend.service;

import com.report.backend.dto.PlaceholderMetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectorQueryServiceClientTest {

    @Mock
    private RestClient restClient;

    private ConnectorQueryServiceClient client;

    @BeforeEach
    void setUp() {
        client = new ConnectorQueryServiceClient("http://test-url");
        // Manually inject the mocked restClient to override the one created in constructor
        ReflectionTestUtils.setField(client, "restClient", restClient);
    }

    @Test
    void executeQuery_ShouldReturnResults() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        List<Map<String, Object>> mockResponse = Collections.singletonList(Collections.singletonMap("key", "value"));
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

        List<Map<String, Object>> result = client.executeQuery("q1", Collections.emptyMap());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("value", result.get(0).get("key"));
    }

    @Test
    void getPlaceholders_ShouldReturnList() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        List<PlaceholderMetadataDto> mockResponse = Collections.emptyList();
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(mockResponse);

        List<PlaceholderMetadataDto> result = client.getPlaceholders("q1");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

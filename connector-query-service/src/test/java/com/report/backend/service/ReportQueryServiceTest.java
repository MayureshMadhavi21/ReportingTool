package com.report.backend.service;

import com.report.backend.dto.PlaceholderMetadataDto;
import com.report.backend.dto.ReportQueryDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportQueryRepository queryRepository;

    @Mock
    private ReportConnectorRepository connectorRepository;

    @Mock
    private DatabaseExecutionService executionService;

    @Mock
    private VaultService vaultService;

    @Mock
    private ReportServiceClient reportServiceClient;

    @InjectMocks
    private ReportQueryService queryService;

    @Test
    void getAllQueries_ShouldReturnDtoList() {
        ReportQuery entity = TestDataFactory.createQueryEntity();
        when(queryRepository.findAll()).thenReturn(Collections.singletonList(entity));

        List<ReportQueryDto> results = queryService.getAllQueries();

        assertEquals(1, results.size());
        assertEquals(entity.getName(), results.get(0).getName());
    }

    @Test
    void getQueriesByConnector_ShouldReturnList() {
        ReportQuery entity = TestDataFactory.createQueryEntity();
        when(queryRepository.findByConnectorId("conn-1")).thenReturn(Collections.singletonList(entity));

        List<ReportQueryDto> results = queryService.getQueriesByConnector("conn-1");

        assertFalse(results.isEmpty());
        verify(queryRepository).findByConnectorId("conn-1");
    }

    @Test
    void getQueryById_Found_ShouldReturnDto() {
        ReportQuery entity = TestDataFactory.createQueryEntity();
        when(queryRepository.findById("query-123")).thenReturn(Optional.of(entity));

        ReportQueryDto result = queryService.getQueryById("query-123");

        assertNotNull(result);
        assertEquals(entity.getId(), result.getId());
    }

    @Test
    void getQueryById_NotFound_ThrowsException() {
        when(queryRepository.findById("invalid")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> queryService.getQueryById("invalid"));
    }

    @Test
    void createQuery_NameExists_ThrowsException() {
        ReportQueryDto dto = new ReportQueryDto();
        dto.setName("Duplicate");
        when(queryRepository.findByName("Duplicate")).thenReturn(Optional.of(new ReportQuery()));

        assertThrows(RuntimeException.class, () -> queryService.createQuery(dto));
    }

    @Test
    void createQuery_ValidInput_ShouldSaveAndReturnDto() {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        ReportQuery entity = TestDataFactory.createQueryEntity();
        dto.setPlaceholderMetadata(Collections.singletonList(new PlaceholderMetadataDto("p1", "STRING", "desc")));
        
        when(connectorRepository.findById(dto.getConnectorId())).thenReturn(Optional.of(TestDataFactory.createConnectorEntity()));
        when(queryRepository.save(any(ReportQuery.class))).thenReturn(entity);

        ReportQueryDto result = queryService.createQuery(dto);

        assertNotNull(result);
        verify(queryRepository).save(any(ReportQuery.class));
    }

    @Test
    void updateQuery_ConnectorChange_ShouldUpdateConnector() {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        dto.setConnectorId("new-conn");
        ReportQuery existing = TestDataFactory.createQueryEntity();
        ReportConnector newConnector = TestDataFactory.createConnectorEntity();
        newConnector.setId("new-conn");

        when(queryRepository.findById("q1")).thenReturn(Optional.of(existing));
        when(connectorRepository.findById("new-conn")).thenReturn(Optional.of(newConnector));
        when(queryRepository.save(any(ReportQuery.class))).thenReturn(existing);

        queryService.updateQuery("q1", dto);

        verify(connectorRepository).findById("new-conn");
    }

    @Test
    void deleteQuery_Mapped_ThrowsException() {
        when(reportServiceClient.isQueryMappedToTemplate("q1")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> queryService.deleteQuery("q1"));
    }

    @Test
    void executeQuery_ShouldCallExecutionService() {
        ReportQuery entity = TestDataFactory.createQueryEntity();
        when(queryRepository.findById("q1")).thenReturn(Optional.of(entity));
        when(vaultService.getPassword(anyString())).thenReturn("pass");

        queryService.executeQuery("q1", new HashMap<>());

        verify(executionService).executeQuery(anyString(), anyString(), anyString(), anyString(), eq("pass"), anyString(), anyMap(), anyMap());
    }

    @Test
    void getPlaceholders_ShouldExtractFromText() {
        ReportQuery entity = TestDataFactory.createQueryEntity();
        entity.setQueryText("SELECT * FROM table WHERE id = :id AND name = :name");
        when(queryRepository.findById("q1")).thenReturn(Optional.of(entity));

        List<PlaceholderMetadataDto> results = queryService.getPlaceholders("q1");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(p -> p.getName().equals("id")));
        assertTrue(results.stream().anyMatch(p -> p.getName().equals("name")));
    }
}

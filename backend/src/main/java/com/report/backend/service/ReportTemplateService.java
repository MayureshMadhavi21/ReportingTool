package com.report.backend.service;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import com.report.backend.entity.ReportQuery;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.TemplateQueryMapping;
import com.report.backend.repository.ReportQueryRepository;
import com.report.backend.repository.ReportTemplateRepository;
import com.report.backend.repository.TemplateQueryMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private final ReportTemplateRepository templateRepository;
    private final ReportQueryRepository queryRepository;
    private final TemplateQueryMappingRepository mappingRepository;

    public List<ReportTemplateDto> getAllTemplates() {
        return templateRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ReportTemplateDto getTemplateById(Long id) {
        return templateRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }
    
    public ReportTemplate getTemplateEntityById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @Transactional
    public ReportTemplateDto uploadTemplate(String name, String description, MultipartFile file) throws IOException {
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setDescription(description);
        
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".xlsx")) {
            template.setFileType("XLSX");
        } else {
            template.setFileType("DOCX");
        }
        
        template.setFileData(file.getBytes());
        return mapToDto(templateRepository.save(template));
    }

    @Transactional
    public TemplateQueryMappingDto addMapping(Long templateId, TemplateQueryMappingDto dto) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        ReportQuery query = queryRepository.findById(dto.getQueryId())
                .orElseThrow(() -> new RuntimeException("Query not found"));

        TemplateQueryMapping mapping = new TemplateQueryMapping();
        mapping.setTemplate(template);
        mapping.setQuery(query);
        mapping.setJsonNodeName(dto.getJsonNodeName());

        return mapMappingToDto(mappingRepository.save(mapping));
    }

    @Transactional
    public void deleteMapping(Long mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    private ReportTemplateDto mapToDto(ReportTemplate entity) {
        ReportTemplateDto dto = new ReportTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setFileType(entity.getFileType());
        dto.setMappings(entity.getMappings().stream()
                .map(this::mapMappingToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private TemplateQueryMappingDto mapMappingToDto(TemplateQueryMapping mapping) {
        TemplateQueryMappingDto dto = new TemplateQueryMappingDto();
        dto.setId(mapping.getId());
        dto.setTemplateId(mapping.getTemplate().getId());
        dto.setQueryId(mapping.getQuery().getId());
        dto.setQueryName(mapping.getQuery().getName());
        dto.setJsonNodeName(mapping.getJsonNodeName());
        return dto;
    }
}

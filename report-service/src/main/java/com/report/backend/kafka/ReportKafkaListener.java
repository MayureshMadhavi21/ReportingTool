package com.report.backend.kafka;

import com.report.backend.dto.ReportGenerationRequestDto;
import com.report.backend.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportKafkaListener {

    private final ReportGenerationService reportGenerationService;

    @KafkaListener(topics = "report-requests", groupId = "report-service-group")
    public void consumeReportRequest(ReportGenerationRequestDto request) {
        log.info("Received Kafka request to generate report for template ID: {}", request.getTemplateId());
        try {
            byte[] reportData = reportGenerationService.generateReport(
                    request.getTemplateId(),
                    request.getVersionNumber(),
                    request.getFormat(),
                    request.getParameters()
            );

            // Since it is async, we'll save it to a local folder or external storage.
            // For now, we save it in a local output directory.
            File outDir = new File("kafka-reports-output");
            if (!outDir.exists()) outDir.mkdirs();
            
            String filename = "Kafka_Report_" + request.getTemplateId() + "_" + System.currentTimeMillis() + 
                 ("PDF".equalsIgnoreCase(request.getFormat()) ? ".pdf" : (".xlsx".equalsIgnoreCase(request.getFormat()) ? ".xlsx" : ".docx"));
                 
            File outFile = new File(outDir, filename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(reportData);
            }
            log.info("Successfully generated report via Kafka: {}", outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate report via Kafka", e);
        }
    }
}

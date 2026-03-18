package com.report.backend.service;

import com.aspose.words.Document;
import com.aspose.words.JsonDataLoadOptions;
import com.aspose.words.JsonDataSource;
import com.aspose.words.ReportingEngine;
import com.aspose.words.SaveFormat;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class AsposeProcessingService {

    public byte[] processTemplate(byte[] templateData, String jsonData, String outputFormat) throws Exception {
        // 1. Load the template into Aspose Document
        ByteArrayInputStream templateStream = new ByteArrayInputStream(templateData);
        Document doc = new Document(templateStream);

        // 2. Setup JSON Data Source
        JsonDataLoadOptions options = new JsonDataLoadOptions();
        options.setAlwaysGenerateRootObject(true);
        options.setSimpleValueParseMode(com.aspose.words.JsonSimpleValueParseMode.STRICT);
        
        ByteArrayInputStream jsonStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
        JsonDataSource dataSource = new JsonDataSource(jsonStream, options);

        // 3. Apply Reporting Engine mapping
        ReportingEngine engine = new ReportingEngine();
        // Allow missing members to avoid exceptions if data is partial
        engine.setOptions(com.aspose.words.ReportBuildOptions.ALLOW_MISSING_MEMBERS);
        
        // Pass the json root object. The data will be accessible based on the json nodes.
        engine.buildReport(doc, dataSource, "root");

        // 4. Save to output format
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int format = SaveFormat.DOCX;
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            format = SaveFormat.PDF;
        }
        
        doc.save(outStream, format);
        return outStream.toByteArray();
    }
}

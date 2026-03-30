package com.report.backend.service;

import com.aspose.words.Document;
import com.aspose.words.JsonDataSource;
import com.aspose.words.ReportingEngine;
import com.aspose.words.SaveFormat;
import com.aspose.cells.Workbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class AsposeProcessingService {

    public byte[] processTemplate(byte[] templateData, String jsonData, String templateType, String outputFormat)
            throws Exception {
        if ("XLSX".equalsIgnoreCase(templateType)) {
            return processExcelTemplate(templateData, jsonData, outputFormat);
        } else {
            return processWordTemplate(templateData, jsonData, outputFormat);
        }
    }

    private byte[] processWordTemplate(byte[] templateData, String jsonData, String outputFormat) throws Exception {
        ByteArrayInputStream templateStream = new ByteArrayInputStream(templateData);
        Document doc = new Document(templateStream);

        ByteArrayInputStream jsonStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
        JsonDataSource dataSource = new JsonDataSource(jsonStream); // Automatic root object handling is default or not needed for object-based JSON

        ReportingEngine engine = new ReportingEngine();
        engine.setOptions(com.aspose.words.ReportBuildOptions.ALLOW_MISSING_MEMBERS);
        engine.buildReport(doc, dataSource);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int format = SaveFormat.DOCX;
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            format = SaveFormat.PDF;
        }

        doc.save(outStream, format);
        return outStream.toByteArray();
    }

    private byte[] processExcelTemplate(byte[] templateData, String jsonData, String outputFormat) throws Exception {
        ByteArrayInputStream templateStream = new ByteArrayInputStream(templateData);
        Workbook workbook = new Workbook(templateStream);

        com.aspose.cells.WorkbookDesigner designer = new com.aspose.cells.WorkbookDesigner(workbook);
        designer.setUpdateReference(true);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> dataMap = mapper.readValue(jsonData, java.util.Map.class);

        for (String key : dataMap.keySet()) {
            Object nodeData = dataMap.get(key);
            // Use custom ICellsDataTable wrapper to support Map-based dynamic data with all marker types
            if (nodeData instanceof java.util.List) {
                designer.setDataSource(key, new JsonDataTable((java.util.List<java.util.Map<String, Object>>) nodeData));
            } else {
                designer.setDataSource(key, nodeData);
            }
        }

        designer.process();
        workbook.calculateFormula();

        for (int i = 0; i < workbook.getWorksheets().getCount(); i++) {
            com.aspose.cells.Worksheet worksheet = workbook.getWorksheets().get(i);
            for (int j = 0; j < worksheet.getCharts().getCount(); j++) {
                worksheet.getCharts().get(j).calculate();
            }
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int format = com.aspose.cells.SaveFormat.XLSX;
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            format = com.aspose.cells.SaveFormat.PDF;
        }
        workbook.save(outStream, format);
        return outStream.toByteArray();
    }

    /**
     * Custom implementation of ICellsDataTable to wrap a List of Maps.
     * This enables Aspose.Cells to treat dynamic JSON data as a proper table source.
     */
    private static class JsonDataTable implements com.aspose.cells.ICellsDataTable {
        private final java.util.List<java.util.Map<String, Object>> data;
        private final String[] columns;
        private int index = -1;

        public JsonDataTable(java.util.List<java.util.Map<String, Object>> data) {
            this.data = data;
            if (data != null && !data.isEmpty()) {
                this.columns = data.get(0).keySet().toArray(new String[0]);
            } else {
                this.columns = new String[0];
            }
        }

        @Override
        public String[] getColumns() { return columns; }

        @Override
        public int getCount() { return data != null ? data.size() : 0; }

        @Override
        public void beforeFirst() { index = -1; }

        @Override
        public boolean next() {
            index++;
            return index < getCount();
        }

        @Override
        public Object get(int columnIndex) {
            if (index >= 0 && index < getCount() && columnIndex >= 0 && columnIndex < columns.length) {
                return data.get(index).get(columns[columnIndex]);
            }
            return null;
        }

        @Override
        public Object get(String columnName) {
            if (index >= 0 && index < getCount()) {
                return data.get(index).get(columnName);
            }
            return null;
        }
    }
}

package com.report.backend.service;

public interface TemplateStorageStrategy {
    String saveTemplate(String templateId, byte[] fileData, String filename);
    byte[] loadTemplate(String storagePath);
    void deleteTemplate(String storagePath);
}

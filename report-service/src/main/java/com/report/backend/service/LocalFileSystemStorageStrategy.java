package com.report.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@ConditionalOnProperty(name = "template.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemStorageStrategy implements TemplateStorageStrategy {

    private final String baseDir;

    public LocalFileSystemStorageStrategy(@Value("${template.storage.local.dir:data/templates}") String baseDir) {
        this.baseDir = baseDir;
        new File(baseDir).mkdirs();
    }

    @Override
    public String saveTemplate(String templateId, byte[] fileData, String filename) {
        try {
            String pathStr = baseDir + File.separator + templateId + "_" + filename;
            Path path = Paths.get(pathStr);
            Files.write(path, fileData);
            log.info("Saved template to local file system: {}", pathStr);
            return pathStr;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save template to local file system", e);
        }
    }

    @Override
    public byte[] loadTemplate(String storagePath) {
        try {
            return Files.readAllBytes(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template from local file system: " + storagePath, e);
        }
    }

    @Override
    public void deleteTemplate(String storagePath) {
        if (storagePath != null && !storagePath.isEmpty()) {
            File file = new File(storagePath);
            if (file.exists()) {
                if (!file.delete()) {
                    log.warn("Failed to delete local template file: {}", storagePath);
                } else {
                    log.info("Deleted local template file: {}", storagePath);
                }
            }
        }
    }
}

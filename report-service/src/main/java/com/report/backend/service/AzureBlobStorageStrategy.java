package com.report.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Note: To fully implement this, you would include `azure-storage-blob` dependency in pom.xml.
 * For the scope of this refactor, we are using a simulated abstraction of Azure Blob Storage.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "template.storage.type", havingValue = "azure")
public class AzureBlobStorageStrategy implements TemplateStorageStrategy {

    // private final BlobServiceClient blobServiceClient;
    // Inject and configure blobServiceClient in a configuration class

    @Override
    public String saveTemplate(String templateId, byte[] fileData, String filename) {
        String blobName = "templates/" + templateId + "/" + filename;
        log.info("Simulating upload to Azure Blob Storage: {}", blobName);
        // BlobClient blobClient = containerClient.getBlobClient(blobName);
        // blobClient.upload(new ByteArrayInputStream(fileData), fileData.length, true);
        return blobName;
    }

    @Override
    public byte[] loadTemplate(String storagePath) {
        log.info("Simulating loading from Azure Blob Storage: {}", storagePath);
        // BlobClient blobClient = containerClient.getBlobClient(storagePath);
        // return blobClient.downloadContent().toBytes();
        return new byte[0]; // Stub
    }

    @Override
    public void deleteTemplate(String storagePath) {
        log.info("Simulating deletion from Azure Blob Storage: {}", storagePath);
        // BlobClient blobClient = containerClient.getBlobClient(storagePath);
        // blobClient.delete();
    }
}

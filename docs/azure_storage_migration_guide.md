# Azure Storage Migration Guide

This guide provides the complete set of steps to migrate your template storage from the local file system to **Azure Blob Storage**.

> [!IMPORTANT]
> Follow these steps in order. All code snippets are ready for manual copy-paste into your project.

---

## 1. Azure Portal Setup

1.  **Create a Storage Account**:
    *   Navigate to the [Azure Portal](https://portal.azure.com).
    *   Create a new **Storage Account** (e.g., `reporttoolstoragedev`).
2.  **Get Connection String**:
    *   Go to **Security + networking** > **Access keys**.
    *   Copy the **Connection string** for `key1`.
3.  **Create a Container**:
    *   Go to **Data storage** > **Containers**.
    *   Create a new container named `templates`.
    *   Set the Public access level to **Private** (recommended).

---

## 2. Dependency Management

Add the Azure Storage Blob SDK to your `report-service/pom.xml`.

```xml
<!-- Add this inside the <dependencies> section -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.25.1</version>
</dependency>
```

---

## 3. Configuration Changes

Update your `report-service/src/main/resources/application.yml` with the Azure credentials and switch the storage type.

```yaml
azure:
  storage:
    connection-string: "DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net"
    container-name: "templates"

template:
  storage:
    type: "azure" # Changed from "local" to "azure"
```

---

## 4. Code Implementation

### A. Create `AzureConfig.java`
Create this file to configure the Azure Client Bean.

**Path**: `src/main/java/com/report/backend/config/AzureConfig.java`

```java
package com.report.backend.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}
```

### B. Implement `AzureBlobStorageStrategy.java`
Replace the existing skeleton implementation with this full version.

**Path**: `src/main/java/com/report/backend/service/AzureBlobStorageStrategy.java`

```java
package com.report.backend.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@ConditionalOnProperty(name = "template.storage.type", havingValue = "azure")
public class AzureBlobStorageStrategy implements TemplateStorageStrategy {

    private final BlobContainerClient containerClient;

    public AzureBlobStorageStrategy(
            BlobServiceClient blobServiceClient,
            @Value("${azure.storage.container-name}") String containerName) {
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        // Ensure container exists
        if (!this.containerClient.exists()) {
            this.containerClient.create();
            log.info("Created Azure Blob container: {}", containerName);
        }
    }

    @Override
    public String saveTemplate(String templateId, byte[] fileData, String filename) {
        String blobName = "templates/" + templateId + "/" + filename;
        log.info("Uploading to Azure Blob Storage: {}", blobName);
        
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(new ByteArrayInputStream(fileData), fileData.length, true);
        
        return blobName;
    }

    @Override
    public byte[] loadTemplate(String storagePath) {
        log.info("Loading from Azure Blob Storage: {}", storagePath);
        
        BlobClient blobClient = containerClient.getBlobClient(storagePath);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        
        return outputStream.toByteArray();
    }

    @Override
    public void deleteTemplate(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return;
        
        log.info("Deleting from Azure Blob Storage: {}", storagePath);
        BlobClient blobClient = containerClient.getBlobClient(storagePath);
        blobClient.deleteIfExists();
    }
}
```

---

## 5. Verification Steps

1.  **Build the Project**: Run `mvn clean install` to ensure the new dependency is resolved and the code compiles.
2.  **Start the Application**: Ensure the application starts without `BeanCreationException` (which would indicate a missing connection string or injection error).
3.  **Upload a Template**: Use the UI or API to upload a new template.
4.  **Verify in Azure Portal**:
    *   Navigate to your Storage Account > Containers > `templates`.
    *   Check if a new folder/blob was created under `templates/`.
5.  **Generate a Report**: Ensure that reports can still be generated (this verifies `loadTemplate` is working).

---

> [!TIP]
> If you are using Managed Identity in Azure Dev Instance instead of Connection Strings, you can replace the `BlobServiceClientBuilder` logic in `AzureConfig.java` with `.credential(new DefaultAzureCredentialBuilder().build())`.

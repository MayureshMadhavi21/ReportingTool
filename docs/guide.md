# Vault Integration Guide

## Overview
As per your request to secure connector passwords without storing them in the SQL database, we have implemented an application-level AES encryption strategy using a mock File-Based Vault. This ensures passwords stay encrypted at rest and separate from the primary database logs.

## Vault Architecture
- **Service Owner:** `connector-query-service`
- **Responsibilities:** 
  1. Accepts plaintext passwords when a user creates/updates a Connector.
  2. Encrypts the password using a symmetric `AES` encryption algorithm.
  3. Stores the ciphertext in an isolated JSON store (`data/vault.json`), mapped to the connector's unique name.
  4. At query execution time, `ReportQueryService` securely retrieves and decrypts the password in-memory.
  5. The primary database (`Report_Connector` table) only stores the Connector Name, DB Type, JDBC URL, and Username—**no password data is written to the database**. This is achieved by annotating the password field as `@Transient` in Hibernate.

## Security Mechanics (AES)
We are using standard **AES/CBC/PKCS5Padding** with a predefined master key configured under the environment variable/application property `vault.secret.key`.

- **Encryption Engine:** `javax.crypto.Cipher`
- **Key Generation:** A predictable base key via SHA-1 hashing of `vault.secret.key`. (In a real HashiCorp Vault implementation this would be derived dynamically).
- **JSON Storage format:** 
  ```json
  {
    "Sales_DB": "rR53Z4b/K1T7pW...==",
    "HR_DB": "aBc2Q9f/M0V2nX...=="
  }
  ```

## Verification & Testing APIs
To verify that encryption and decryption are working, an admin-only testing endpoint has been exposed on `connector-query-service` (Port 8085):

- **Encrypt API:** `POST http://localhost:8085/api/vault/encrypt`
  - Body: Plaintext string
  - Returns: Encrypted Ciphertext
- **Decrypt API:** `POST http://localhost:8085/api/vault/decrypt`
  - Body: Encrypted string
  - Returns: Plaintext password (for admin manual verification)

*Note: In production environments, these test endpoints should be disabled or protected by strict RBAC.*

## Future Production Upgrade Path
Because we decoupled the vault logic into the `VaultService.java` class, migrating to **HashiCorp Vault** or **AWS KMS** in the future requires modifying only the `storePassword()`, `getPassword()`, and `deletePassword()` methods in this single class. The rest of the application ecosystem will remain entirely unaware of the transition.

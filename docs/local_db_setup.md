# Local Database Setup for Report Tool Testing

This guide provides instructions on how to spin up Microsoft SQL Server and Oracle Database locally using Docker Compose, along with test DDL, DML, and a Parameterized Select Query for both databases.

## 1. Directory Setup
Create a `docker-compose.yml` file in your root or a `db-setup` directory with the following content:

```yaml
version: '3.8'

services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    container_name: report_sqlserver
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=YourPassword123!
    ports:
      - "1433:1433"
    healthcheck:
      test: ["CMD", "/opt/mssql-tools/bin/sqlcmd", "-U", "sa", "-P", "YourPassword123!", "-Q", "SELECT 1"]
      interval: 10s
      timeout: 3s
      retries: 10

  oracle:
    image: gvenzl/oracle-free:latest
    container_name: report_oracle
    environment:
      - ORACLE_PASSWORD=YourPassword123!
    ports:
      - "1522:1521"
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 10s
      timeout: 3s
      retries: 10
```

Start the containers by running:
```bash
docker-compose up -d
```

## 2. SQL Server Configuration

### Connection Details
- **Host**: `localhost`
- **Port**: `1433`
- **Database**: `master` (or create a new one)
- **Username**: `sa`
- **Password**: `YourPassword123!`

### DDL & DML
Connect using any SQL Client (e.g., Azure Data Studio, DBeaver, SSMS) and run:

```sql
-- Create Test Table
CREATE TABLE Employees (
    Id INT PRIMARY KEY,
    FirstName VARCHAR(50),
    LastName VARCHAR(50),
    Department VARCHAR(50),
    HireDate DATE
);

-- Insert Sample Data
INSERT INTO Employees (Id, FirstName, LastName, Department, HireDate) VALUES 
(1, 'Alice', 'Smith', 'Engineering', '2023-01-15'),
(2, 'Bob', 'Johnson', 'Marketing', '2022-06-20'),
(3, 'Charlie', 'Brown', 'Sales', '2021-11-05');
```

### Parameterized Select Query
Use this in the Report Tool UI for the SQL Server connector:
```sql
SELECT Id, FirstName, LastName, Department, HireDate 
FROM Employees 
WHERE Department = :deptName AND HireDate >= :minHireDate
```
(Placeholders detected: `deptName` (STRING), `minHireDate` (DATE))

---

## 3. Oracle Configuration

### Connection Details
- **Host**: `localhost`
- **Port**: `1522`
- **SID/Service**: `FREEPDB1` (Check Oracle Mode as 'SERVICE')
- **Username**: `SYSTEM`
- **Password**: `YourPassword123!`

### DDL & DML
Connect using DBeaver or SQL Developer and run:

```sql
-- Create Test Table
CREATE TABLE EMPLOYEES (
    ID NUMBER PRIMARY KEY,
    FIRST_NAME VARCHAR2(50),
    LAST_NAME VARCHAR2(50),
    DEPARTMENT VARCHAR2(50),
    HIRE_DATE DATE
);

-- Insert Sample Data
INSERT INTO EMPLOYEES (ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE) VALUES (1, 'Alice', 'Smith', 'Engineering', TO_DATE('2023-01-15', 'YYYY-MM-DD'));
INSERT INTO EMPLOYEES (ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE) VALUES (2, 'Bob', 'Johnson', 'Marketing', TO_DATE('2022-06-20', 'YYYY-MM-DD'));
INSERT INTO EMPLOYEES (ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE) VALUES (3, 'Charlie', 'Brown', 'Sales', TO_DATE('2021-11-05', 'YYYY-MM-DD'));
COMMIT;
```

### Parameterized Select Query
Use this in the Report Tool UI for the Oracle connector:
```sql
SELECT ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE 
FROM EMPLOYEES 
WHERE DEPARTMENT = :deptName AND HIRE_DATE >= :minHireDate
```
(Placeholders detected: `deptName` (STRING), `minHireDate` (DATE))

# Reporting Tool Demo Guide

This guide provides the necessary configurations for connectors, queries, and templates to set up a full demo of the reporting tool.

---

## 1. Database Connectors

### H2 (Local Development)
- **Name:** `LocalH2`
- **Database Type:** `H2`
- **JDBC URL:** `jdbc:h2:file:./data/datadb`
- **Username:** `sa`
- **Password:** `password`

### Oracle
- **Host**: `localhost`
- **Port**: `1522`
- **SID/Service**: `FREEPDB1` (Check Oracle Mode as 'SERVICE')
- **Username**: `SYSTEM`
- **Password**: `YourPassword123!`

### SQL Server
- **Host**: `localhost`
- **Port**: `1433`
- **Database**: `master` (or create a new one)
- **Username**: `sa`
- **Password**: `YourPassword123!`

---

## 2. Queries

### Query 1: Single Value Data (Company Info)
- **Name:** `Company Info`
- **Connector:** Select `LocalH2`
- **SQL Query:** 
  ```sql
  SELECT 'Acme Corp' AS company_name, 'Q1 2026' AS quarter;
  ```

### Query 2: Key Highlights (Bullet Points)
- **Name:** `Key Highlights`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Reached $280K in total revenue' AS highlight UNION ALL
  SELECT 'Opened 3 new office branches' AS highlight UNION ALL
  SELECT 'Launched new hardware line' AS highlight;
  ```
### Query 3: Tabular & Chart Data (Sales Data)
- **Name:** `Sales Data`
- **Connector:** Select `LocalH2`
- **SQL Query:** 
  ```sql
  SELECT 'Laptops' AS category, 150000 AS revenue UNION ALL 
  SELECT 'Desktops' AS category, 85000 AS revenue UNION ALL
  SELECT 'Accessories' AS category, 45000 AS revenue;

### Query 4: Product Distribution (Pie Chart)
- **Name:** `Product Distribution`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Software' AS product, 60 AS percentage UNION ALL
  SELECT 'Hardware' AS product, 25 AS percentage UNION ALL
  SELECT 'Services' AS product, 15 AS percentage;
  ```

### Query 5: Monthly Revenue (Line Graph)
- **Name:** `Monthly Revenue`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Jan' AS month_name, 12000 AS revenue UNION ALL
  SELECT 'Feb' AS month_name, 15000 AS revenue UNION ALL
  SELECT 'Mar' AS month_name, 14000 AS revenue UNION ALL
  SELECT 'Apr' AS month_name, 18000 AS revenue;
  ```

### Parameterized Queries (Handling Date Formats)

Different database engines handle date strings (like `YYYY-MM-DD`) differently. Use the following examples for the `Employee Filter` query:

#### Oracle (Requires TO_DATE)
Oracle requires explicit conversion to avoid the `ORA-01861: literal does not match format string` error.
- **SQL Query:**
  ```sql
  SELECT ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE 
  FROM EMPLOYEES 
  WHERE DEPARTMENT = :deptName AND HIRE_DATE >= TO_DATE(:minHireDate, 'YYYY-MM-DD')
  ```

#### SQL Server & H2 (Standard Syntax)
These databases handle the `YYYY-MM-DD` string format automatically in most configurations.
- **SQL Query:**
  ```sql
  SELECT ID, FIRST_NAME, LAST_NAME, DEPARTMENT, HIRE_DATE 
  FROM EMPLOYEES 
  WHERE DEPARTMENT = :deptName AND HIRE_DATE >= :minHireDate
  ```
emporacle
empsql
---

## 3. Template Configuration (Word)

To create a Word-based report, follow these steps to configure your `.docx` template using the **Aspose LINQ Reporting Engine** syntax.

### Step A: Prepare the Word Document
1. Open Microsoft Word and create a new document.
2. Design your report layout (Header, Footer, Tables, etc.).
3. Use the following syntax for placeholders: `<<[property_name]>>`.

### Step B: Map Queries to JSON Nodes
When creating a **Template Version** in the tool, you must map your queries to **JSON Node Names**. These names will be used in your Word template.

| Query | JSON Node Name (Recommended) |
|---|---|
| `Company Info` | `company_info` |
| `Key Highlights` | `highlights` |
| `Product Distribution` | `distribution` |
| `Monthly Revenue` | `revenue_data` |

### Step C: Add Placeholder Tags in Word

#### 1. Single Field (using the first row of a result)
To display the company name from the `Company Info` query:
- Syntax: `<<[company_info[0].company_name]>>`
- Syntax: `<<[company_info[0].quarter]>>`

#### 2. Repeating Data (Lists/Tables)
To display the list of highlights:
- Syntax:
  ```text
  <<foreach [item in highlights]>>
  - <<[item.highlight]>>
  <</foreach>>
  ```

#### 3. Data for Charts (Excel-based charts in Word)
If your Word document contains an embedded Excel chart, the tool will automatically populate the data if the query columns match the chart series names (e.g., `month_name` and `revenue` for a line graph).

---

## 4. Generating the Report

1. Go to **Template Details** for your template.
2. Click **Create New Version**.
3. Upload your `.docx` file.
4. Add the Query Mappings as defined in **Step B**.
5. Save the version.
6. Click **Generate Report**, provide any required **Placeholder Values** (if using parameterized queries), and select **DOCX** or **PDF** as the output format.

### Placeholder Values Example:
If using the `Employee Filter` query:
- `deptName`: `Engineering`
- `minHireDate`: `2023-01-01`

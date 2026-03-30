# Master Excel Testing Guide (Smart Markers)

This guide provides steps to test four distinct reporting scenarios in a single Excel template using **Smart Marker** syntax.

---

## 1. Unified SQL Queries (Queries Tab)
Create these 4 queries in the **Queries** tab to provide test data:

### **Query 1: Report Meta (Single Value)**
*   **JSON Node Name**: `meta`
*   **SQL**: `SELECT 'Executive Performance Report' as title, 'FY 2026' as period`

### **Query 2: Recent Orders (Basic Table)**
*   **JSON Node Name**: `orders`
*   **SQL**: 
    ```sql
    SELECT 'ORD-101' as id, 'Laptop Pro' as item, 2 as qty, 3000 as price
    UNION ALL
    SELECT 'ORD-102', 'Desktop Ultra', 1, 1500
    UNION ALL
    SELECT 'ORD-103', 'Accessory Kit', 5, 250
    ``` 

### **Query 3: Regional Sales (Grouping & Subtotals)**  
*   **JSON Node Name**: `regional_sales`
*   **SQL**:
    ```sql
    SELECT 'North' as region, 'Alice' as rep, 45000 as amount
    UNION ALL
    SELECT 'North', 'Bob', 32000
    UNION ALL
    SELECT 'South', 'Charlie', 55000
    UNION ALL
    SELECT 'South', 'David', 41000
    UNION ALL
    SELECT 'East', 'Eve', 62000
    ```

### **Query 4: Category Distribution (Chart Data)**
*   **JSON Node Name**: `chart_stats`
*   **SQL**:
    ```sql
    SELECT 'Services' as category, 45 as contribution
    UNION ALL
    SELECT 'Hardware', 30
    UNION ALL
    SELECT 'Software', 25
    ```

---

## 2. Excel Template Configuration (.xlsx)
Apply these **Smart Markers** to your Excel file. Ensure markers are the only content in the cell if they represent the start of a table.

### **Scenario 1: Single Values (Metadata)**
| Cell | Marker |
| :--- | :--- |
| **A1** | `&=$meta(title)` |
| **A2** | `Period: &=$meta(period)` |

### **Scenario 2: Basic Table (Orders)**
| Cell | Marker | Description |
| :--- | :--- | :--- |
| **A5** | `&=$orders(id)` | Start of orders table |
| **B5** | `&=$orders(item)` | Item Name |
| **C5** | `&=$orders(qty)` | Quantity |

### **Scenario 3: Advanced Table (Grouping/Subtotals)**
| Cell | Marker | Description |
| :--- | :--- | :--- |
| **A9** | `&=$regional_sales(region)` | Groups identical regions (first column in table) |
| **B9** | `&=$regional_sales(rep)` | Rep Name |
| **C9** | `&=$regional_sales(amount)` | Individual amounts |
| **C10**| `&=$regional_sales(Subtotal,amount)` | Subtotal (SUM) per Region group |
| **C11**| `&=$regional_sales(GrandTotal,amount)` | Grand Total for the entire data set |

### **Scenario 4: Independent Chart (Hidden Sheet)**
1.  **Create a source sheet**: 
    - Create a new Sheet in your Excel file called `Data_Source`.
2.  **Add Markers**:
    - In `Data_Source` Sheet, add these markers:
        - **A1**: `&=$chart_stats(category)`
        - **B1**: `&=$chart_stats(contribution)`
3.  **Insert Chart**:
    - Go back to your main report Sheet.
    - Insert a **Pie Chart**.
4.  **Link Data**:
    - Right-click the Chart -> **Select Data**.
    - Set the Chart Data Range to `Data_Source!$A$1:$B$3`.
5.  **Hide the Sheet**:
    - Right-click the `Data_Source` tab at the bottom and select **Hide**.
6.  **Result**: The chart will appear on your main report, pulling live data from the hidden sheet which is populated during report generation!

---

## 3. How to Test
1.  Upload the `.xlsx` file as a **New Template**.
2.  Map each SQL query to the corresponding JSON Node Name (`meta`, `orders`, `regional_sales`, `chart_stats`).
3.  Navigate to **Generate**, select your template, and choose **XLSX** as the format.
4.  Verify the generated file:
    *   **Check A9**: Are identical regions grouped?
    *   **Check C10**: Is the grand total calculation correct?
    *   **Check Chart**: Does it reflect the 45/30/25 distribution?
    *   **Check Styles**: Did your bold/colored borders translate correctly from the template?

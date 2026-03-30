# Testing Guide: Dynamic Query Placeholders

This guide provides a step-by-step walkthrough for testing the dynamic query placeholder functionality in the Report Generation Tool using four distinct business scenarios.

---

## Prerequisites
1.  **Backend Services**: Ensure `connector-query-service` and `report-service` are running.
2.  **Database**: Access your H2 Console (usually at `http://localhost:8085/h2-console`) and run the setup scripts below.

---

## Step 1: Sample Data Setup
Run these scripts in your database to prepare for testing.

````carousel
```sql
-- Scenario 1: HR System
CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(100), dept VARCHAR(50), status VARCHAR(20), salary INT);
INSERT INTO employees VALUES (1, 'John', 'IT', 'Active', 75000), (2, 'Jane', 'IT', 'Active', 82000), (3, 'Bob', 'HR', 'Active', 60000);
```
<!-- slide -->
```sql
-- Scenario 2: Sales
CREATE TABLE orders (id INT PRIMARY KEY, amount DECIMAL(10,2), order_date DATE, status VARCHAR(20));
INSERT INTO orders VALUES (101, 1250.00, '2024-03-01', 'Shipped'), (102, 450.75, '2024-03-05', 'Processing');
```
<!-- slide -->
```sql
-- Scenario 3: Projects
CREATE TABLE tasks (id INT PRIMARY KEY, user_name VARCHAR(50), complexity INT, due_date DATE);
INSERT INTO tasks VALUES (1, 'Mike', 5, '2024-04-01'), (2, 'Sarah', 8, '2024-04-15');
```
<!-- slide -->
```sql
-- Scenario 4: Inventory
CREATE TABLE inventory (id INT PRIMARY KEY, product VARCHAR(100), cat VARCHAR(50), quantity INT);
INSERT INTO inventory VALUES (1, 'Laptop', 'Electronics', 45), (2, 'Chair', 'Furniture', 85);
```
````

---

## Step 2: Create & Map Parameterized Queries
For each scenario you wish to test, create a query in the **Queries** tab:

| Scenario | Query Name | SQL Query Text |
| :--- | :--- | :--- |
| **HR** | `Filter_Employees` | `SELECT * FROM employees WHERE dept = :dept AND status = :empStatus` |
| **Sales** | `High_Value_Orders` | `SELECT * FROM orders WHERE amount > :minAmount AND status = :status` |
| **Projects** | `User_Tasks` | `SELECT * FROM tasks WHERE user_name = :user AND complexity >= :minComp` |
| **Inventory**| `Low_Stock_Check` | `SELECT * FROM inventory WHERE cat = :category AND quantity < :threshold` |

---

## Step 3: Test the Generation UI
1.  Navigate to the **Generate** page and select your template.
2.  **EXPECTED BEHAVIOR**: The **QUERY PARAMETERS** section should automatically appear with fields matching your placeholders (e.g., `Dept`, `EmpStatus`, `MinAmount`).
3.  Enter sample data:
    - **HR**: `IT`, `Active`
    - **Sales**: `500`, `Shipped`
    - **Inventory**: `Electronics`, `50`
4.  Click **Generate & Download**.

---

## Step 4: Verification
1.  **Download Progress**: Ensure the notification shows "Download complete!".
2.  **Report Content**: Open the file and verify that the data is filtered correctly based on your inputs.
3.  **Logs**: Check `report-service` logs for the parameter bridge:
    `Starting generation for template: ... with params: {dept=IT, empStatus=Active}`

---

## Troubleshooting
- **No placeholders appearing?** Use `:paramName` syntax. Avoid spaces after the colon.
- **Empty Report?** Ensure input values match database records (H2 is case-sensitive for strings).
- **Download Fails?** Check that both microservices are healthy and the template file exists.

# End-to-End User Guide: Report Generation Tool

This complete guide explains how to fully configure the application from scratch to generate a comprehensive Word document report featuring single values, tables, bullet points, and dynamic charts.

---

## Step 1: Create a Database Connector
1. Open your web browser and navigate to the frontend application: `http://localhost:8084` (Wait for the start script to finish booting).
2. Use the left sidebar to navigate to **Connectors**.
3. Click the **Register Connector** button.
4. We will connect to the internal, in-memory test database (H2) for this demo. Fill in:
   - **Name:** `LocalH2`
   - **Database Type:** `H2`
   - **JDBC URL:** `jdbc:h2:file:./data/reportingdb`
   - **Username:** `sa`
   - **Password:** `password`
5. Click **Save**.

---

## Step 2: Register Queries
Navigate to **Queries** in the sidebar. We will create three mock queries that return static sets of data to immediately test the engine. 

*(Note: Because this is an in-memory database without physical tables yet, we can use `SELECT ... AS ...` chains to mock tabular return structures).*

### Query 1: Single Value Data (Company Info)
- **Name:** `Company Info`
- **Connector:** Select `LocalH2`
- **SQL Query:** 
  ```sql
  SELECT 'Acme Corp' AS company_name, 'Q1 2026' AS quarter;
  ```
- Click **Save**.

### Query 2: Tabular & Chart Data (Sales Data)
- **Name:** `Sales Data`
- **Connector:** Select `LocalH2`
- **SQL Query:** 
  ```sql
  SELECT 'Laptops' AS category, 150000 AS revenue UNION ALL 
  SELECT 'Desktops' AS category, 85000 AS revenue UNION ALL
  SELECT 'Accessories' AS category, 45000 AS revenue;
  ```
- Click **Save**.

### Query 3: Bullet points
- **Name:** `Key Highlights`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Reached $280K in total revenue' AS highlight UNION ALL
  SELECT 'Opened 3 new office branches' AS highlight UNION ALL
  SELECT 'Launched new hardware line' AS highlight;
  ```
- Click **Save**.

### Query 4: Multi-Row Data (User Details)
- **Name:** `User Details`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 1 AS id, 'Mayur' AS name, 'Address - jwdmncwncwencwenk' AS address UNION ALL
  SELECT 2 AS id, 'Rohit' AS name, 'Address - nwenwejcnwecn' AS address;
  ```
- Click **Save**.

### Query 5: Product Distribution (Pie Chart)
- **Name:** `Product Distribution`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Software' AS product, 60 AS percentage UNION ALL
  SELECT 'Hardware' AS product, 25 AS percentage UNION ALL
  SELECT 'Services' AS product, 15 AS percentage;
  ```
- Click **Save**.

### Query 6: Monthly Revenue (Line Graph)
- **Name:** `Monthly Revenue`
- **Connector:** Select `LocalH2`
- **SQL Query:**
  ```sql
  SELECT 'Jan' AS month_name, 12000 AS revenue UNION ALL
  SELECT 'Feb' AS month_name, 15000 AS revenue UNION ALL
  SELECT 'Mar' AS month_name, 14000 AS revenue UNION ALL
  SELECT 'Apr' AS month_name, 18000 AS revenue;
  ```
- Click **Save**.

---

## Step 3: Create the Word Template
Open Microsoft Word and create a blank document. We will use the **Aspose LINQ Reporting Engine syntax**. 

### 1. Static Values & Single Value Placeholder
Since all SQL queries return a "list of rows", we can extract single values from the dataset by calling `.first()` on the mapping node. Type this into your Word doc:

```text
Quarterly Report for <<[company.first().company_name]>>
Reporting Quarter: <<[company.first().quarter]>>

```

### 2. Dynamic Bullet Points (Numbered List)
To correctly duplicate a **Numbered List** item for every row of data, the `foreach` tags must be placed on their own lines *surrounding* the list item.

1. On a new, normal line (ensure there is **NO list formatting** applied to this line), type:
   `<<foreach [h in highlights]>>`
2. Press Enter to go to the next line. Click the **Numbered List** button (1, 2, 3) so this line becomes a numbered bullet. Type:
   `<<[h.highlight]>>`
3. Press Enter. Turn **OFF** the numbered list formatting for this new line (so it is just normal text again), and type:
   `<</foreach>>`

*Concept: Because the `foreach` and `/foreach` tags span across paragraphs, Aspose detects that you want to duplicate the entire block in between them. It will duplicate the numbered paragraph for each record, and Microsoft Word will automatically handle incrementing the numbers (1, 2, 3)!*

### 3. Dynamic Table
Insert a standard Word Table with 2 columns and 2 rows.
- **Top Row (Header):** Type `Category` and `Revenue`.
- **Bottom Row (Data):** 
  - In the left cell, type: `<<foreach [s in sales]>><<[s.category]>>`
  - In the right cell, type: `<<[s.revenue]>><</foreach>>`

*Concept: The `foreach` loop starts in the first cell and ends in the last cell. Aspose will copy the entire table row for every record in the `sales` mapping.*

### 4. Dynamic Table (Multi-Row Block Spanning)
If you have data that is too wide (like a long address) and you want a single record to print across multiple rows, you can do this by wrapping a 2-row block!

1. Insert a standard Word Table with 2 columns and 3 rows.
2. **Top Row (Header):** Type `ID` in the left cell and `Name` in the right cell.
3. **Middle Row (Data Start):** 
   - In the left cell, type: `<<foreach [u in users]>><<[u.id]>>`
   - In the right cell, type: `<<[u.name]>>`
4. **Bottom Row (Data End):**
   - Do **NOT** merge the cells in this row! Keep it as 2 separate cells to avoid crashing the Aspose Engine.
   - In the left cell, type: `Address - <<[u.address]>>`
   - In the right cell, type exactly: `<</foreach>>`

*Concept: Because the `foreach` tag starts in the middle row and the `/foreach` tag ends in the bottom row, Aspose detects that both rows belong to the same record. It repeats BOTH rows together for every person in the `users` query!*

### 5. Dynamic Chart
1. In Word, go to **Insert -> Chart** and pick a **Column Chart**.
2. Left-click to select the **Chart Title** on the page. Replace the title text with: 
   `<<foreach [s in sales]>><<x [s.category]>>Sales Overview`
3. Now, right click the Chart and click **Edit Data** so the small Excel sheet pops up.
4. Resize the blue data boundary box exactly to cells `A1:B2` (1 column of categories, 1 column of series).
5. Do not worry about "deleting" the top row (Row 1). You can simply overwrite it. In cell `A1` type `Category`.
6. In cell `B1` type EXACTLY: `<<y [s.revenue]>>Revenue` (The tag must be in the series name).
7. In the data cells beneath (`A2`, `B2`), you can type any mock values you want (e.g. `Laptop` and `1000`). The Aspose engine will overwrite the data cells dynamically. 
8. Delete the contents of all other cells so only `A1:B2` have data in them. Close the Excel window.

*Concept: The `foreach` loop and the `x` tag defined in the Chart Title tell Aspose this entire chart iterates over `sales` using the category for the X-axis. The `y` tag in the Series Name tells it to use the revenue for the Y-axis. It then stamps a new row for every x and y coordinate paired from that object.*

### 6. Dynamic Pie Chart
1. In Word, go to **Insert -> Chart** and pick a **Pie Chart**.
2. Left-click to select the **Chart Title** on the page. Replace the title text with: 
   `<<foreach [p in products]>><<x [p.product]>>Product Distribution`
3. Right click the Chart and click **Edit Data** so the Excel sheet pops up.
4. Resize the blue data boundary box exactly to cells `A1:B2`.
5. In cell `A1` type `Product`.
6. In cell `B1` type EXACTLY: `<<y [p.percentage]>>Share`
7. Type dummy data in `A2` and `B2` and close the Excel window.

### 7. Dynamic Line Graph
1. In Word, go to **Insert -> Chart** and pick a **Line Graph** (Line with Markers).
2. Left-click to select the **Chart Title** on the page. Replace the title text with: 
   `<<foreach [m in monthly_revenue]>><<x [m.month_name]>>Revenue Trend`
3. Right click the Chart and click **Edit Data** so the Excel sheet pops up.
4. Resize the blue data boundary box exactly to cells `A1:B2`.
5. In cell `A1` type `Month`.
6. In cell `B1` type EXACTLY: `<<y [m.revenue]>>Revenue`
7. Type dummy data in `A2` and `B2` and close the Excel window.

**Save this document** anywhere on your PC as `Quarterly_Template.docx`.

---

## Step 4: Upload and Map the Template
1. Go back to the web application and navigate to **Templates**.
2. Click **Upload Template**.
3. Name it `Q1 Report` and pick the `Quarterly_Template.docx` file you just saved.
4. Once it appears in the table, click the small **+ Add** button in the *Mappings* column six separate times to link your JSON Node Names to the queries:
   - Select Query: `Company Info` | JSON Node Name: `company` *(Matches `<<[company...]>>`)* 
   - Select Query: `Sales Data` | JSON Node Name: `sales` *(Matches `<<[sales...]>>`)*
   - Select Query: `Key Highlights` | JSON Node Name: `highlights` *(Matches `<<[highlights...]>>`)*
   - Select Query: `User Details` | JSON Node Name: `users` *(Matches `<<[u...]>>`)*
   - Select Query: `Product Distribution` | JSON Node Name: `products` *(Matches `<<[p...]>>`)*
   - Select Query: `Monthly Revenue` | JSON Node Name: `monthly_revenue` *(Matches `<<[m...]>>`)*

---

## Step 5: Generate the Final Report!
1. Go to the **Generate Report** page in the sidebar.
2. Select your `Q1 Report` template from the dropdown.
3. Leave the output format as `DOCX` (or switch to `PDF`!).
4. Click **Generate & Download**.

Open your freshly generated report! You will see your single parameters cleanly injected, a fully expanded numbered list, an aggregated data table, and a visually accurate column chart—all powered dynamically by the SQL queries!

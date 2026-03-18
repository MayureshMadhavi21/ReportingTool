# Fix Chart Documentation in User_Guide.md

The problem stems from incorrect tag placement in the Word template when configuring charts for the Aspose LINQ Reporting Engine. The [User_Guide.md](file:///d:/publishingV2/report-tool/docs/User_Guide.md) currently instructs users to place `<<x [s.category]>>` and `<<y [s.revenue]>>` into the Excel data cells (`A2` and `B2`). This is incorrect and causes the engine to fail to map the chart.

## Proposed Changes

### [d:\publishingV2\report-tool\docs\User_Guide.md](file:///d:/publishingV2/report-tool/docs/User_Guide.md)
I will modify the **4. Dynamic Chart** section to properly aligned with Aspose documentation:

1. **Chart Title Placement:** The `<<foreach>>` loop and the `<<x>>` tag must go into the **Chart Title** itself.
   - Change: `<<foreach [s in sales]>><<x [s.category]>>Sales Overview`
2. **Series Name Placement:** The `<<y>>` tag must go into the **Series Name** (which is cell `B1` in the "Edit Data" Excel spreadsheet).
   - Change: Cell `B1` should be exactly `<<y [s.revenue]>>` (or `<<y [s.revenue]>>Revenue`).
3. **Data Cells:** The actual data in the cells beneath (like `A2`, `B2`) are just mock placeholders and don't require the tags. 

## Verification Plan

### Manual Verification
1. I will ask you to open your Word template (`Quarterly_Template.docx`), delete the broken chart, and recreate it using these new instructions. 
2. Test generating the report via the UI again to verify that Aspose successfully injects the data into the chart without complaining or rendering it empty.

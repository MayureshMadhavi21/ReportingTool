# Load Testing Guide (Local H2 Environment)

This guide provides step-by-step instructions for using Apache JMeter to performance-test the `report-service` and `connector-query-service` locally using your existing H2 database. 

## 1. Prerequisites
1. Ensure both your microservices are running on your machine (Ports 8084 and 8085).
2. Use the React UI or Postman to create a complex **Report Template**. Make sure you map **at least 2 or 3 distinct SQL queries** to this template so the `ReportGenerationService` uses multi-threading via Java `CompletableFuture`.
3. Note the **Template ID** internally assigned to your template (e.g., `1`).
4. Ensure you have Apache JMeter installed (`jmeter.bat` / `jmeter.sh` ready to run).

## 2. API Target
- **Method:** `GET`
- **URL:** `http://localhost:8084/api/generate/{templateId}?format=PDF`
- *(Example: `http://localhost:8084/api/generate/1?format=PDF`)*

Because this endpoint returns a binary file stream (the final PDF/DOCX), JMeter needs to simply record the time taken for the first byte to arrive and the overall file download speed.

---

## 3. Creating the JMeter Test Plan

1. **Launch JMeter**
   - Run `jmeter.bat` from your JMeter `bin/` folder.

2. **Add a Thread Group**
   - Right-click `Test Plan` ➜ `Add` ➜ `Threads (Users)` ➜ `Thread Group`
   - Name it "Report Generation Load Test".
   - *This is where you will define the "users" testing the system.*

3. **Add HTTP Request Defaults (Optional but Recommended)**
   - Right-click `Thread Group` ➜ `Add` ➜ `Config Element` ➜ `HTTP Request Defaults`
   - Server Name/IP: `localhost`
   - Port Number: `8084`

4. **Add the HTTP Request Sampler**
   - Right-click `Thread Group` ➜ `Add` ➜ `Sampler` ➜ `HTTP Request`
   - Name it "Call Generate Report API"
   - Method: `GET`
   - Path: `/api/generate/1?format=PDF` *(Replace `1` with your actual Template ID)*

5. **Ensure Memory Efficiency (Discard Binary Downloads)** 
   *(Highly Recommended for reaching 1000 requests without JMeter crashing)*
   - When generating 1000 PDFs at once, downloading 1000 physical gigabytes into JMeter's memory will freeze your machine.
   - Right-click the `HTTP Request` ➜ `Add` ➜ `Listener` ➜ `Save Responses to a file`.
   - In the settings, uncheck "Save response as MD5 hash", or if possible, use a **JSR223 PostProcessor** to discard the payload to avoid out-of-memory errors on your JMeter client.
   - *Alternatively, just let JMeter receive the bytes but DO NOT use a "View Results Tree" listener permanently, as it will max out JMeter's RAM.*

6. **Add Summary Report Listener**
   - Right-click `Thread Group` ➜ `Add` ➜ `Listener` ➜ `Summary Report`.
   - *This will show you request counts, average times (ms), minimum times, maximum times, and throughput (`Requests Per Second`).*

---

## 4. Execution Plan (Milestone Runs)

To safely gauge how your CPU, connection pools, and H2 database respond to scale, run these sequentially. For each run:
1. Go to your **Thread Group**.
2. Modify the **Number of Threads (users)**.
3. Set **Ramp-up period** to `1` second (forces simultaneous execution).
4. Set **Loop Count** to `1`.
5. Click the green **Start** button at the top.
6. Check your `Summary Report`.
7. Once finished, click **Clear All** (Geared Broom icon) before starting the next test.

### Test 1: Baseline (1 Request)
- **Threads:** 1 / **Ramp-up:** 1
- **Goal:** Establishes parsing time, validates that the Aspose license initializes without error, verifies DB mapped execution works. Average time gives you a strict `1 user` baseline (e.g., 200ms).

### Test 2: (5 Requests)
- **Threads:** 5 / **Ramp-up:** 1
- **Goal:** Confirms that `CompletableFuture` spawns minimum background threads successfully and Spring HikariCP handles 5 concurrent H2 DB connections without locking natively.

### Test 3: (10 Requests)
- **Threads:** 10 / **Ramp-up:** 1
- **Goal:** Early sign of Aspose rendering optimization on small thread pools.

### Test 4: (25 Requests)
- **Threads:** 25 / **Ramp-up:** 1
- **Goal:** Hits 50% of the default HikariCP database pool limit (`maximum-pool-size: 50`). Wait times might slightly increase.

### Test 5: (50 Requests)
- **Threads:** 50 / **Ramp-up:** 1
- **Goal:** Maxes out the default database connection pool exactly. The Hikari connection manager should flawlessly queue and release connections without dropping any data.

### Test 6: (100 Requests) / *Threshold Warning*
- **Threads:** 100 / **Ramp-up:** 5 seconds
- **Goal:** Because H2 natively struggles with huge concurrent `SELECT` locks (unlike an enterprise Postgres/SQL Server), we add a 5-second ramp-up to give H2 breathing room over standard IO. Noticeably slower but shouldn't error.

### Test 7: (500 Requests)
- **Threads:** 500 / **Ramp-up:** 15 seconds
- **Goal:** Severe system load. Keep an eye on the `connector-query-service` terminal memory allocation and Aspose thread pooling capacity inside `report-service`.

### Test 8: (1000 Requests target limit)
- **Threads:** 1000 / **Ramp-up:** 30 seconds
- **Goal:** Stress test. Simulates massive corporate end-of-month reporting bursts. Success here indicates flawless architectural microservice decoupling, network strength, and memory handling.

*(Note: While running 500 & 1000, if you see high `Error %` in JMeter, it will most likely be due to H2 Database locking up, NOT the Java Spring backend. In production, Azure SQL/Postgres will natively handle this volume gracefully.)*

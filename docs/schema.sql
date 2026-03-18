-- 1. Connectors (DB Connections)
CREATE TABLE Report_Connector (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    db_type VARCHAR(50) NOT NULL, -- e.g., SQL_SERVER, MYSQL, ORACLE, POSTGRESQL
    jdbc_url VARCHAR(500) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_encrypted VARCHAR(500) NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- 2. Queries (SQL queries against Connectors)
CREATE TABLE Report_Query (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    connector_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    query_text NVARCHAR(MAX) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_Query_Connector FOREIGN KEY (connector_id) REFERENCES Report_Connector(id)
);

-- 3. Templates (Uploaded Word/Excel files)
CREATE TABLE Report_Template (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(500),
    file_type VARCHAR(20) NOT NULL, -- e.g., DOCX, XLSX
    file_data VARBINARY(MAX) NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- 4. Template-Query Mapping (Associates queries to templates and defines JSON node names)
CREATE TABLE Template_Query_Mapping (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    template_id BIGINT NOT NULL,
    query_id BIGINT NOT NULL,
    json_node_name VARCHAR(100) NOT NULL, -- e.g., 'salesData' -> query results will be mapped to JSON under "salesData"
    created_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_Mapping_Template FOREIGN KEY (template_id) REFERENCES Report_Template(id) ON DELETE CASCADE,
    CONSTRAINT FK_Mapping_Query FOREIGN KEY (query_id) REFERENCES Report_Query(id) ON DELETE CASCADE,
    CONSTRAINT UQ_Template_Node UNIQUE (template_id, json_node_name) -- A specific JSON node name should only appear once per template
);

-- Indexes for performance
CREATE INDEX IDX_Report_Query_Connector ON Report_Query(connector_id);
CREATE INDEX IDX_Mapping_Template ON Template_Query_Mapping(template_id);
CREATE INDEX IDX_Mapping_Query ON Template_Query_Mapping(query_id);

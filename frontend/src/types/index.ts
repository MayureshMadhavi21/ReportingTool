export interface Connector {
  id: string;
  name: string;
  dbType: string;
  jdbcUrl: string;
  host?: string;
  port?: number;
  dbName?: string;
  useRawUrl?: boolean;
  username: string;
  password?: string;
}

export interface PlaceholderMetadata {
  name: string;
  type: string;
  description: string;
}

export interface QueryDef {
  id: string;
  connectorId: string;
  name: string;
  queryText: string;
  description: string;
  placeholderMetadata: PlaceholderMetadata[];
}

export interface TemplateMapping {
  id: string;
  templateId: string;
  queryId: string;
  queryName?: string;
  connectorId?: string;
  connectorName?: string;
  connectorDbType?: string;
  jsonNodeName: string;
}

export interface TemplateVersion {
  id: string;
  versionNumber: number;
  storagePath: string;
  fileType: string;
  createdBy: string;
  isActive: number;
  createdAt: string;
  mappings: TemplateMapping[];
}

export interface Template {
  id: string;
  name: string;
  description: string;
  latestVersionNumber?: number;
  versions: TemplateVersion[];
}

// Migration Types
export interface ExportedConnector {
  name: string;
  type: string;
  url: string;
  username: string;
}

export interface ExportedQuery {
  name: string;
  queryText: string;
  connectorName: string;
  placeholderMetadata: PlaceholderMetadata[];
}

export interface ExportedMapping {
  queryName: string;
  connectorName: string;
  jsonNodeName: string;
}

export interface ExportedVersion {
  versionNumber: number;
  originalPath: string; // The full storage path from source
  fileName: string;     // Basename for debugging
  isActive: number;
  fileContentBase64: string;
  mappings: ExportedMapping[];
}

export interface TemplateExport {
  templateName: string;
  description: string;
  connectors: ExportedConnector[];
  queries: ExportedQuery[];
  versions: ExportedVersion[];
}

export interface ConnectorImportConfig {
  originalName: string;
  targetName: string;
  password?: string;
  strategy: 'SKIP' | 'OVERRIDE' | 'CREATE_NEW';
}

export interface QueryImportConfig {
  originalName: string;
  connectorName: string;
  targetName: string;
  strategy: 'SKIP' | 'OVERRIDE' | 'CREATE_NEW';
}

export interface TemplateImportConfig {
  originalName: string;
  targetName: string;
  strategy: 'SKIP' | 'OVERRIDE' | 'CREATE_NEW';
}

export interface ImportRequest {
  exportData: TemplateExport;
  connectors: ConnectorImportConfig[];
  queries: QueryImportConfig[];
  template: TemplateImportConfig;
}

// ---- Migration Analysis Types (from /analyze endpoint) ----

export interface ConnectorAnalysis {
  exists: boolean;
  existingId?: string;
}

export interface QueryAnalysis {
  exists: boolean;
  existingId?: string;
  currentQueryText?: string; // For SQL diff view
}

export interface MigrationAnalysis {
  connectors: Record<string, ConnectorAnalysis>;
  queries: Record<string, QueryAnalysis>;
  templateExists: boolean;
  connectorImpactMap: Record<string, string[]>; // connector name -> affected query names
  queryImpactMap: Record<string, string[]>;     // query name -> affected template version strings
}


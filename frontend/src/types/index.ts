export interface Connector {
  id: string;
  name: string;
  dbType: string;
  jdbcUrl: string;
  username: string;
  password?: string;
}

export interface QueryDef {
  id: string;
  connectorId: string;
  name: string;
  queryText: string;
  description: string;
}

export interface TemplateMapping {
  id: string;
  templateId: string;
  queryId: string;
  queryName?: string;
  connectorId?: string;
  connectorName?: string;
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

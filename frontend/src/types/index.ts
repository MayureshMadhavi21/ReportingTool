export interface Connector {
  id: number;
  name: string;
  dbType: string;
  jdbcUrl: string;
  username: string;
  password?: string; // Only used when creating
}

export interface QueryDef {
  id: number;
  connectorId: number;
  name: string;
  queryText: string;
  description: string;
}

export interface TemplateMapping {
  id: number;
  templateId: number;
  queryId: number;
  jsonNodeName: string;
  queryName?: string;
}

export interface Template {
  id: number;
  name: string;
  description: string;
  fileType: string;
  mappings: TemplateMapping[];
}

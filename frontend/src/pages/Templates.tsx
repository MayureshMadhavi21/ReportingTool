import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Button, Table, Group, Text, ActionIcon, Modal,
  TextInput, Stack, FileInput, Textarea, Card, ThemeIcon, Tooltip, Center,
  Stepper, Box, Divider, Alert, Badge, PasswordInput, Select
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  IconTrash, IconPencil, IconPlus, IconFileDescription,
  IconHistory, IconEye, IconUpload, IconDatabase, IconCheck,
  IconAlertTriangle, IconCircleCheck, IconArrowRight, IconShieldCheck
} from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { reportApi } from '../api';
import type {
  Template, TemplateExport, ConnectorImportConfig,
  QueryImportConfig, TemplateImportConfig, ImportRequest, MigrationAnalysis
} from '../types';

export default function Templates() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const navigate = useNavigate();

  const [uploadOpened, { open: openUpload, close: closeUpload }] = useDisclosure(false);
  const [file, setFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  // Import Wizard State
  const [importOpened, { open: openImport, close: closeImport }] = useDisclosure(false);
  const [activeStep, setActiveStep] = useState(0);
  const [importJson, setImportJson] = useState<TemplateExport | null>(null);
  const [analysis, setAnalysis] = useState<MigrationAnalysis | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [connectorConfigs, setConnectorConfigs] = useState<ConnectorImportConfig[]>([]);
  const [queryConfigs, setQueryConfigs] = useState<QueryImportConfig[]>([]);
  const [templateConfig, setTemplateConfig] = useState<TemplateImportConfig | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importedTemplateId, setImportedTemplateId] = useState<string | null>(null);

  // Compute which steps are needed (Story 10: auto-skip empty steps)
  const hasConnectors = (importJson?.connectors?.length ?? 0) > 0;
  const hasQueries = (importJson?.queries?.length ?? 0) > 0;
  const steps = ['upload', ...(hasConnectors ? ['connectors'] : []), ...(hasQueries ? ['queries'] : []), 'template'];
  const totalSteps = steps.length;

  const fetchData = async () => {
    try {
      const { data } = await reportApi.get('/templates');
      setTemplates(data);
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to load templates', color: 'red' });
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleUpload = async () => {
    if (!file || !name) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', name);
    formData.append('description', description);
    try {
      await reportApi.post('/templates', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      notifications.show({ title: 'Success', message: 'Template created', color: 'green' });
      closeUpload(); setFile(null); setName(''); setDescription('');
      fetchData();
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to create template', color: 'red' });
    }
  };

  const handleDeleteTemplate = async (id: string) => {
    if (!confirm('Are you certain? This will delete the template and all its snapshots.')) return;
    try {
      await reportApi.delete(`/templates/${id}`);
      notifications.show({ title: 'Success', message: 'Template removed', color: 'green' });
      fetchData();
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to delete template', color: 'red' });
    }
  };

  const resetWizard = () => {
    setImportJson(null); setAnalysis(null); setActiveStep(0);
    setConnectorConfigs([]); setQueryConfigs([]); setTemplateConfig(null);
    setImportedTemplateId(null);
  };

  // Story 2: Upload JSON + auto-analyze
  const handleJsonUpload = async (f: File | null) => {
    if (!f) return;
    const text = await f.text();
    let data: TemplateExport;
    try {
      data = JSON.parse(text);
      if (!data.templateName) throw new Error('Invalid');
    } catch {
      notifications.show({ title: 'Invalid File', message: 'The selected JSON is not a valid migration package.', color: 'red' });
      return;
    }

    setImportJson(data);
    setIsAnalyzing(true);
    try {
      // Story 3,4,5: Call analyze endpoint for smart conflict detection
      const { data: analysisResult } = await reportApi.post('/migration/analyze', data);
      setAnalysis(analysisResult);

      // Always CREATE_NEW — auto-suggest unique name if conflict exists
      setConnectorConfigs(data.connectors.map(c => {
        const ca = analysisResult.connectors?.[c.name];
        return {
          originalName: c.name,
          targetName: ca?.exists ? `${c.name} (Imported)` : c.name,
          strategy: 'CREATE_NEW' as const,
          password: ''
        };
      }));
      setQueryConfigs(data.queries.map(q => {
        const qa = analysisResult.queries?.[q.name];
        return {
          originalName: q.name,
          connectorName: q.connectorName,
          targetName: qa?.exists ? `${q.name} (Imported)` : q.name,
          strategy: 'CREATE_NEW' as const
        };
      }));
      setTemplateConfig({
        originalName: data.templateName,
        targetName: analysisResult.templateExists ? `${data.templateName} (Imported)` : data.templateName,
        strategy: 'CREATE_NEW'
      });

      setActiveStep(1);
    } catch {
      notifications.show({ title: 'Analysis Failed', message: 'Could not check target environment. Ensure the backend is running.', color: 'red' });
    } finally {
      setIsAnalyzing(false);
    }
  };

  // Password is always required for CREATE_NEW (no SKIP option)
  const connectorsPasswordValid = connectorConfigs.every(c =>
    c.password && c.password.trim() !== ''
  );

  const updateConnector = (idx: number, patch: Partial<ConnectorImportConfig>) =>
    setConnectorConfigs(prev => prev.map((c, i) => i === idx ? { ...c, ...patch } : c));
  const updateQuery = (idx: number, patch: Partial<QueryImportConfig>) =>
    setQueryConfigs(prev => prev.map((q, i) => i === idx ? { ...q, ...patch } : q));

  // Story 7: Execute with error handling (wizard stays open on failure)
  const handleExecuteImport = async () => {
    if (!importJson || !templateConfig) return;
    setIsImporting(true);
    try {
      const payload: ImportRequest = { exportData: importJson, connectors: connectorConfigs, queries: queryConfigs, template: templateConfig };
      const { data: result } = await reportApi.post('/migration/import', payload);
      setImportedTemplateId(result.id);
      fetchData();
      setActiveStep(totalSteps); // advance to success screen
    } catch (e: any) {
      notifications.show({
        title: 'Import Failed',
        message: e.response?.data?.message || e.message || 'Unexpected error. Check server logs.',
        color: 'red', autoClose: 8000
      });
      // Wizard stays open; user can fix and retry
    } finally {
      setIsImporting(false);
    }
  };

  // --- Render Helpers ---

  // Story 3,4,5: Visual badges
  const renderExistsBadge = (exists: boolean) =>
    exists
      ? <Badge color="orange" variant="light" size="sm" leftSection={<IconAlertTriangle size={11} />}>Already Exists</Badge>
      : <Badge color="teal" variant="light" size="sm" leftSection={<IconPlus size={11} />}>New</Badge>;

  // Connector Step — simplified: always CREATE_NEW, no strategy dropdown
  const renderConnectorStep = () => (
    <Stack py="md">
      <Alert icon={<IconShieldCheck size={16} />} title="Password Required" color="blue" variant="light">
        Passwords are <strong>never exported</strong>. Please enter the database password for each connector below.
      </Alert>
      {connectorConfigs.map((c, idx) => {
        const ca = analysis?.connectors?.[c.originalName];
        return (
          <Card key={idx} withBorder p="md" radius="md"
            style={{ borderColor: ca?.exists ? '#fd7e14' : '#12b886' }}>
            <Stack gap="sm">
              <Group justify="space-between" wrap="nowrap">
                <div>
                  <Group gap="xs" mb={2}>
                    <Text fw={700} size="sm">{c.originalName}</Text>
                    {renderExistsBadge(!!ca?.exists)}
                  </Group>
                  <Text size="xs" c="dimmed">{importJson?.connectors.find(conn => conn.name === c.originalName)?.url}</Text>
                </div>
                <Badge color="green" variant="light" size="sm">Import As New</Badge>
              </Group>

              {ca?.exists && (
                <Alert icon={<IconAlertTriangle size={14} />} color="orange" variant="light" p="xs">
                  A connector named <strong>"{c.originalName}"</strong> already exists.
                  This will be imported under the name below.
                </Alert>
              )}

              <TextInput
                size="xs"
                label="Target Name"
                description={ca?.exists ? 'Auto-renamed to avoid conflict — you can customise this' : 'Name that will be used in this environment'}
                value={c.targetName}
                onChange={e => updateConnector(idx, { targetName: e.target.value })}
                error={!c.targetName?.trim() ? 'Name is required' : undefined}
                rightSection={c.targetName?.trim() ? <IconCheck size={14} color="#2f9e44" /> : null}
              />

              <PasswordInput
                size="xs"
                placeholder={`Database password for "${c.originalName}"`}
                value={c.password || ''}
                onChange={e => updateConnector(idx, { password: e.target.value })}
                error={!c.password?.trim() ? 'Password is required' : undefined}
              />
            </Stack>
          </Card>
        );
      })}
      <Group justify="flex-end">
        <Button variant="default" onClick={() => setActiveStep(s => s - 1)}>Back</Button>
        <Tooltip label="Enter name and password for all connectors to continue" disabled={connectorsPasswordValid && connectorConfigs.every(c => c.targetName?.trim())}>
          <Button
            onClick={() => setActiveStep(s => s + 1)}
            disabled={!connectorsPasswordValid || connectorConfigs.some(c => !c.targetName?.trim())}>
            Next: Queries
          </Button>
        </Tooltip>
      </Group>
    </Stack>
  );

  // Query Step — simplified: always CREATE_NEW, no strategy dropdown
  const renderQueryStep = () => (
    <Stack py="md">
      {queryConfigs.map((q, idx) => {
        const qa = analysis?.queries?.[q.originalName];
        return (
          <Card key={idx} withBorder p="md" radius="md"
            style={{ borderColor: qa?.exists ? '#fd7e14' : '#12b886' }}>
            <Stack gap="sm">
              <Group justify="space-between" wrap="nowrap" align="flex-start">
                <div>
                  <Group gap="xs" mb={2}>
                    <Text fw={700} size="sm">{q.originalName}</Text>
                    {renderExistsBadge(!!qa?.exists)}
                  </Group>
                  <Text size="xs" c="dimmed">On connector: {q.connectorName}</Text>
                </div>
                {(!qa?.exists || q.strategy === 'CREATE_NEW') ? (
                  <Badge color="green" variant="light" size="sm">Import As New</Badge>
                ) : (
                  <Badge color="orange" variant="light" size="sm">Override Existing</Badge>
                )}
              </Group>

              {qa?.exists && (
                <Select
                  size="xs"
                  label="Import Strategy"
                  data={[
                    { value: 'CREATE_NEW', label: 'Import As New (Rename)' },
                    { value: 'OVERRIDE', label: 'Override Existing Query' }
                  ]}
                  value={q.strategy}
                  onChange={(v: any) => updateQuery(idx, { strategy: v as 'CREATE_NEW' | 'OVERRIDE' })}
                />
              )}

              {(!qa?.exists || q.strategy === 'CREATE_NEW') && (
                <TextInput
                  size="xs"
                  label="Target Name"
                  description={qa?.exists ? 'Auto-renamed to avoid conflict' : 'Name that will be used in this environment'}
                  value={q.targetName}
                  onChange={e => updateQuery(idx, { targetName: e.target.value })}
                  error={!q.targetName?.trim() ? 'Name is required' : undefined}
                  rightSection={q.targetName?.trim() ? <IconCheck size={14} color="#2f9e44" /> : null}
                />
              )}

              {qa?.exists && q.strategy === 'OVERRIDE' && (
                <Stack gap="xs" mt="sm">
                  <Alert icon={<IconAlertTriangle size={14} />} color="orange" variant="light" p="xs">
                    Overriding will update the SQL for the existing query <strong>"{q.originalName}"</strong>.
                  </Alert>

                  <Text size="xs" fw={700}>SQL Changes:</Text>
                  <Group grow align="flex-start" gap="xs">
                    <Textarea
                      size="xs"
                      label="Current (Live)"
                      readOnly
                      value={qa.currentQueryText || ''}
                      minRows={3}
                      maxRows={6}
                      styles={{ input: { backgroundColor: '#fff5f5', color: '#c92a2a', fontFamily: 'monospace' } }}
                    />
                    <Textarea
                      size="xs"
                      label="Imported (New)"
                      readOnly
                      value={importJson?.queries.find((iq: any) => iq.name === q.originalName)?.queryText || ''}
                      minRows={3}
                      maxRows={6}
                      styles={{ input: { backgroundColor: '#ebfbee', color: '#2b8a3e', fontFamily: 'monospace' } }}
                    />
                  </Group>

                  {(analysis?.queryImpactMap?.[q.originalName]?.length ?? 0) > 0 && (
                    <Box mt={4}>
                      <Text size="xs" fw={700} mb={4}>Impacted Templates (will use new SQL logic):</Text>
                      <Group gap={4}>
                        {analysis?.queryImpactMap?.[q.originalName].map((tName, tIdx) => (
                          <Badge key={tIdx} color="grape" variant="light" size="xs">{tName}</Badge>
                        ))}
                      </Group>
                    </Box>
                  )}
                </Stack>
              )}
            </Stack>
          </Card>
        );
      })}
      <Group justify="flex-end">
        <Button variant="default" onClick={() => setActiveStep(s => s - 1)}>Back</Button>
        <Button
          onClick={() => setActiveStep(s => s + 1)}
          disabled={queryConfigs.some(q => (!q.targetName?.trim() && q.strategy !== 'OVERRIDE'))}>
          Next: Template
        </Button>
      </Group>
    </Stack>
  );

  // Template Step — simplified: always CREATE_NEW, no strategy dropdown
  const renderTemplateStep = () => {

    const templateExists = analysis?.templateExists ?? false;
    const currentTargetName = templateConfig?.targetName ?? '';
    const nameIsEmpty = currentTargetName.trim() === '';

    const allActions = [
      ...connectorConfigs.map(c => ({ name: c.originalName, type: 'Connector', targetName: c.targetName })),
      ...queryConfigs.map(q => ({ name: q.originalName, type: 'Query', targetName: q.strategy === 'OVERRIDE' ? q.originalName : q.targetName })),
      { name: importJson?.templateName ?? '', type: 'Template', targetName: currentTargetName }
    ];

    return (
      <Stack py="md">
        {/* Template Configuration */}
        <Card withBorder p="md" radius="md" style={{ borderColor: templateExists ? '#fd7e14' : '#12b886' }}>
          <Stack gap="sm">
            <Group gap="xs">
              <Text fw={700}>{importJson?.templateName}</Text>
              {renderExistsBadge(templateExists)}
            </Group>
            <Divider />

            {templateExists && (
              <Alert icon={<IconAlertTriangle size={14} />} color="orange" variant="light">
                A template named <strong>"{importJson?.templateName}"</strong> already exists.
                This will be imported under the name below.
              </Alert>
            )}

            <TextInput
              label="Target Template Name"
              description={templateExists ? 'Auto-renamed to avoid conflict — you can customise this' : 'Name that will be used in this environment'}
              value={currentTargetName}
              onChange={e => setTemplateConfig(prev => prev ? { ...prev, targetName: e.target.value } : null)}
              error={nameIsEmpty ? 'Template name is required' : undefined}
              rightSection={currentTargetName.trim() ? <IconCheck size={16} color="#2f9e44" /> : null}
            />
            <Badge color="green" variant="light" w="fit-content">Import As New</Badge>
          </Stack>
        </Card>

        {/* Migration Plan Summary */}
        <Card withBorder p="md" radius="md" bg="gray.0">
          <Text fw={700} mb="sm">Migration Plan Summary</Text>
          <Table verticalSpacing="xs" withColumnBorders>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Source Name</Table.Th>
                <Table.Th>Type</Table.Th>
                <Table.Th>Target Name</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {allActions.map((a, i) => (
                <Table.Tr key={i}>
                  <Table.Td><Text size="sm" fw={500}>{a.name}</Text></Table.Td>
                  <Table.Td>
                    <Badge size="xs" variant="light" color={a.type === 'Connector' ? 'blue' : a.type === 'Query' ? 'violet' : 'teal'}>{a.type}</Badge>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c={a.name !== a.targetName ? 'orange.7' : 'green.7'} fw={500}>
                      {a.targetName || <Text size="sm" c="red" component="span">⚠ Name required</Text>}
                      {a.name !== a.targetName && <Text size="xs" c="dimmed" component="span"> (renamed)</Text>}
                    </Text>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>

        <Group justify="flex-end" mt="md">
          <Button variant="default" onClick={() => setActiveStep(s => s - 1)} disabled={isImporting}>Back</Button>
          <Tooltip
            label={nameIsEmpty ? 'Template name is required' : ''}
            disabled={!nameIsEmpty}
          >
            <Button
              color="green" loading={isImporting}
              disabled={nameIsEmpty || isImporting || queryConfigs.some(q => (!q.targetName?.trim() && q.strategy !== 'OVERRIDE')) || connectorConfigs.some(c => !c.targetName?.trim())}
              leftSection={<IconCheck size={16} />}
              onClick={handleExecuteImport}>
              Execute Import
            </Button>
          </Tooltip>
        </Group>
      </Stack>
    );
  };

  // Story 8: Post-import Success Screen
  const renderSuccessScreen = () => (
    <Stack align="center" py="xl" gap="lg">
      <ThemeIcon size={80} radius="xl" color="green" variant="light">
        <IconCircleCheck size={48} />
      </ThemeIcon>
      <Text size="xl" fw={800} ta="center">Migration Successful! 🎉</Text>
      <Text size="sm" c="dimmed" ta="center">
        <strong>{importJson?.templateName}</strong> and all its dependencies have been imported into this environment.
      </Text>
      <Card withBorder p="md" radius="md" w="100%">
        <Text fw={600} mb="sm">Quick Links</Text>
        <Stack gap="xs">
          {importedTemplateId && (
            <Button variant="light" color="blue" rightSection={<IconArrowRight size={14} />}
              onClick={() => { closeImport(); navigate(`/templates/${importedTemplateId}/view`); }}>
              View Imported Template →
            </Button>
          )}
          <Button variant="light" color="violet" rightSection={<IconArrowRight size={14} />}
            onClick={() => { closeImport(); navigate('/connectors'); }}>
            View Connectors →
          </Button>
          <Button variant="light" color="indigo" rightSection={<IconArrowRight size={14} />}
            onClick={() => { closeImport(); navigate('/queries'); }}>
            View Queries →
          </Button>
        </Stack>
      </Card>
      <Button variant="subtle" onClick={() => { resetWizard(); closeImport(); }}>Close</Button>
    </Stack>
  );

  const stepLabels: Record<string, { label: string; desc: string }> = {
    upload: { label: 'Upload', desc: 'Select JSON' },
    connectors: { label: 'Connectors', desc: 'Data Sources' },
    queries: { label: 'Queries', desc: 'SQL Logic' },
    template: { label: 'Template', desc: 'Confirm & Import' }
  };

  const isSuccessScreen = activeStep === totalSteps;

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Text size="xl" fw={800}>Report Inventory</Text>
          <Text size="sm" c="dimmed">Track your master templates and their historical revision snapshots.</Text>
        </div>
        <Group>
          <Button variant="light" leftSection={<IconUpload size={18} />} onClick={() => { resetWizard(); openImport(); }}>
            Import Package
          </Button>
          <Button leftSection={<IconPlus size={18} />} onClick={openUpload}>Create Template</Button>
        </Group>
      </Group>

      <Card withBorder shadow="sm" radius="md" p={0}>
        <Table verticalSpacing="md" horizontalSpacing="lg" highlightOnHover>
          <Table.Thead bg="gray.0">
            <Table.Tr>
              <Table.Th>Template Name</Table.Th>
              <Table.Th>Description</Table.Th>
              <Table.Th>Production Version</Table.Th>
              <Table.Th style={{ textAlign: 'center' }}>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {templates.map((t) => (
              <Table.Tr key={t.id}>
                <Table.Td>
                  <Group gap="sm">
                    <ThemeIcon color="blue" variant="light" size="md"><IconFileDescription size={18} /></ThemeIcon>
                    <Text fw={600} size="sm">{t.name}</Text>
                  </Group>
                </Table.Td>
                <Table.Td><Text size="xs" c="dimmed" lineClamp={1}>{t.description || 'No description provided'}</Text></Table.Td>
                <Table.Td><Text size="xs" fw={700}>v{t.latestVersionNumber}</Text></Table.Td>
                <Table.Td>
                  <Center>
                    <Group gap="md">
                      <Tooltip label="View Template Details">
                        <ActionIcon variant="light" color="blue" onClick={() => navigate(`/templates/${t.id}/view`)}><IconEye size={18} /></ActionIcon>
                      </Tooltip>
                      <Tooltip label="Edit Workshop">
                        <ActionIcon variant="light" color="orange" onClick={() => navigate(`/templates/${t.id}/edit`)}><IconPencil size={18} /></ActionIcon>
                      </Tooltip>
                      <Tooltip label="Version Snapshots">
                        <ActionIcon variant="light" color="indigo" onClick={() => navigate(`/templates/${t.id}/versions`)}><IconHistory size={18} /></ActionIcon>
                      </Tooltip>
                      <Tooltip label="Delete Template">
                        <ActionIcon variant="light" color="red" onClick={() => handleDeleteTemplate(t.id)}><IconTrash size={18} /></ActionIcon>
                      </Tooltip>
                    </Group>
                  </Center>
                </Table.Td>
              </Table.Tr>
            ))}
            {templates.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={4} style={{ textAlign: 'center', paddingTop: 32, paddingBottom: 32 }}>
                  <Text c="dimmed">No templates found.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      </Card>

      {/* Create Template Modal */}
      <Modal opened={uploadOpened} onClose={closeUpload} title="New Template Record" size="lg">
        <Stack>
          <TextInput label="Template Name" placeholder="e.g. Compliance Report" required value={name} onChange={(e) => setName(e.target.value)} />
          <Textarea label="Description" placeholder="Context for other authors" value={description} onChange={(e) => setDescription(e.target.value)} />

          {/* Drag & Drop File Upload */}
          <div>
            <Text size="sm" fw={500} mb={6}>
              Initial Document (v1) <span style={{ color: 'red' }}>*</span>
            </Text>
            <div
              onDragOver={e => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={e => {
                e.preventDefault();
                setDragOver(false);
                const dropped = e.dataTransfer.files[0];
                if (dropped && (dropped.name.endsWith('.docx') || dropped.name.endsWith('.xlsx'))) {
                  setFile(dropped);
                } else if (dropped) {
                  notifications.show({ title: 'Invalid File', message: 'Only .docx or .xlsx files are accepted.', color: 'red' });
                }
              }}
              style={{
                border: `2px dashed ${dragOver ? '#228be6' : file ? '#12b886' : '#dee2e6'}`,
                borderRadius: 8,
                background: dragOver ? '#e7f5ff' : file ? '#f0fff4' : '#fafafa',
                padding: '24px 16px',
                textAlign: 'center',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              {file ? (
                <Stack align="center" gap="xs">
                  <ThemeIcon size={40} radius="xl" color="green" variant="light">
                    <IconFileDescription size={22} />
                  </ThemeIcon>
                  <Text size="sm" fw={600} c="green.7">{file.name}</Text>
                  <Text size="xs" c="dimmed">{(file.size / 1024).toFixed(1)} KB</Text>
                  <Button
                    size="xs" variant="subtle" color="red" mt={4}
                    onClick={() => setFile(null)}
                  >
                    Remove file
                  </Button>
                </Stack>
              ) : (
                <Stack align="center" gap="xs">
                  <ThemeIcon size={40} radius="xl" color="blue" variant="light">
                    <IconUpload size={22} />
                  </ThemeIcon>
                  <Text size="sm" fw={500} c={dragOver ? 'blue' : 'dimmed'}>
                    {dragOver ? 'Drop file here' : 'Drag & drop your file here'}
                  </Text>
                  <Text size="xs" c="dimmed">or</Text>
                  <Button
                    size="xs" variant="light"
                    onClick={() => document.getElementById('template-file-input')?.click()}
                  >
                    Choose File
                  </Button>
                  <Text size="xs" c="dimmed" mt={2}>Supports .docx and .xlsx</Text>
                </Stack>
              )}
            </div>
            <input
              id="template-file-input"
              type="file"
              accept=".docx,.xlsx"
              style={{ display: 'none' }}
              onChange={e => {
                const f = e.target.files?.[0];
                if (f) setFile(f);
                e.target.value = '';
              }}
            />
          </div>

          <Group justify="flex-end" mt="md">
            <Button variant="subtle" onClick={closeUpload}>Cancel</Button>
            <Button onClick={handleUpload} disabled={!file || !name.trim()}>Create Inventory Record</Button>
          </Group>
        </Stack>
      </Modal>

      {/* Import Wizard Modal */}
      <Modal
        opened={importOpened}
        onClose={() => { resetWizard(); closeImport(); }}
        title={isSuccessScreen ? 'Migration Complete' : `Migration Wizard — ${importJson?.templateName ?? 'Select a package'}`}
        size="xl"
      >
        {isSuccessScreen ? renderSuccessScreen() : (
          <Stepper active={activeStep} allowNextStepsSelect={false} size="sm">
            {steps.map((stepKey) => {
              const { label, desc } = stepLabels[stepKey];
              return (
                <Stepper.Step key={stepKey} label={label} description={desc}>
                  {/* Story 2: Upload step with analyzing state */}
                  {stepKey === 'upload' && (
                    <Box py="xl">
                      <FileInput
                        label="Template Migration Package (.json)"
                        description="The exported JSON file from the source environment"
                        placeholder="Click to select or drag and drop"
                        accept=".json"
                        leftSection={<IconUpload size={16} />}
                        disabled={isAnalyzing}
                        onChange={handleJsonUpload}
                      />
                      {isAnalyzing && (
                        <Alert mt="md" icon={<IconDatabase size={16} />} color="blue" title="Analyzing migration package...">
                          Checking which connectors, queries, and templates already exist in this environment. Please wait.
                        </Alert>
                      )}
                    </Box>
                  )}
                  {stepKey === 'connectors' && renderConnectorStep()}
                  {stepKey === 'queries' && renderQueryStep()}
                  {stepKey === 'template' && renderTemplateStep()}
                </Stepper.Step>
              );
            })}
          </Stepper>
        )}
      </Modal>
    </Stack>
  );
}

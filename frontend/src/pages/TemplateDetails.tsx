import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select,
  Stack, Badge, Card, SimpleGrid, ThemeIcon, 
  FileInput, Tooltip, Center, Alert
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { 
  IconTrash, IconDownload, IconFileExport,
  IconArrowLeft, IconDatabase, IconPlus, IconPencil, IconEye, IconLock
} from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { reportApi, connectorApi } from '../api';
import type { Template, TemplateVersion, QueryDef } from '../types';

interface TemplateDetailsProps {
  editMode?: boolean;
  versionsMode?: boolean;
  versionViewMode?: boolean;
  versionEditMode?: boolean;
}

const DB_TYPES: Record<string, { label: string, color: string }> = {
  'SQL_SERVER': { label: 'MS SQL', color: 'blue' },
  'MYSQL': { label: 'MySQL', color: 'orange' },
  'POSTGRESQL': { label: 'PostgreSQL', color: 'grape' },
  'ORACLE': { label: 'Oracle', color: 'red' },
  'H2': { label: 'H2', color: 'gray' }
};

export default function TemplateDetails({ 
  editMode, 
  versionsMode,
  versionViewMode,
  versionEditMode
}: TemplateDetailsProps) {
  const { id, versionId } = useParams<{ id: string; versionId?: string }>();
  const navigate = useNavigate();
  const [template, setTemplate] = useState<Template | null>(null);
  const [queries, setQueries] = useState<QueryDef[]>([]);
  const [placeholders, setPlaceholders] = useState<import('../types').PlaceholderMetadata[]>([]);
  const [loading, setLoading] = useState(true);

  const getDbTypeLabel = (type?: string) => {
    if (!type) return { label: 'Unknown', color: 'gray' };
    return DB_TYPES[type.toUpperCase()] || { label: type, color: 'gray' };
  };

  // Mapping Workshop State
  const [mappingOpened, { open: openMapping, close: closeMapping }] = useDisclosure(false);
  const [targetVersion, setTargetVersion] = useState<TemplateVersion | null>(null);
  const [editingMappingId, setEditingMappingId] = useState<string | null>(null);
  const [mappingQueryId, setMappingQueryId] = useState<string>('');
  const [mappingNodeName, setMappingNodeName] = useState('');



  const fetchData = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [tRes, qRes] = await Promise.all([
        reportApi.get(`/templates/${id}`),
        connectorApi.get('/queries')
      ]);
      const data = tRes.data;
      setTemplate(data);
      setQueries(qRes.data);

      if (versionId) {
        const v = data.versions?.find((v: TemplateVersion) => v.id === versionId);
        setTargetVersion(v || null);
      } else {
        const latest = data.versions?.find((v: TemplateVersion) => v.isActive === 1) || data.versions?.[0];
        setTargetVersion(latest || null);
      }

      // Fetch placeholders for the active/target version
      const vId = versionId || data.versions?.find((v: any) => v.isActive === 1)?.id || data.versions?.[0]?.id;
      if (vId) {
        const { data: pData } = await reportApi.get(`/templates/versions/${vId}/placeholders`);
        setPlaceholders(pData);
      }
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load details', color: 'red' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id, versionId]);

  if (loading) return <Center py="xl"><Text>Loading...</Text></Center>;
  if (!template) return <Center py="xl"><Text color="red">Template not found</Text></Center>;

  const handleDownload = async (v: TemplateVersion) => {
    try {
      notifications.show({ id: 'downloading', title: 'Download', message: `Preparing snapshot v${v.versionNumber}...`, color: 'blue', loading: true, autoClose: false });
      const response = await reportApi.get(`/templates/versions/${v.id}/file`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = v.fileType === 'XLSX' ? 'xlsx' : 'docx';
      link.setAttribute('download', `${template.name}_v${v.versionNumber}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      notifications.update({ id: 'downloading', title: 'Success', message: 'Download complete!', color: 'green', loading: false, autoClose: 2000 });
    } catch (err) {
      notifications.update({ id: 'downloading', title: 'Error', message: 'Failed to download file', color: 'red', loading: false, autoClose: 3000 });
    }
  };

  const handleExport = async () => {
    if (!template) return;
    try {
      notifications.show({ id: 'exporting', title: 'Export', message: 'Generating migration package...', color: 'blue', loading: true, autoClose: false });
      const { data } = await reportApi.get(`/migration/export/${id}`);
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `template-export-${template.name.toLowerCase().replace(/\s+/g, '-')}.json`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      notifications.update({ id: 'exporting', title: 'Success', message: 'Migration package downloaded!', color: 'green', loading: false, autoClose: 2000 });
    } catch (err) {
      notifications.update({ id: 'exporting', title: 'Error', message: 'Export failed!', color: 'red', loading: false, autoClose: 3000 });
    }
  };

  const handleDeleteVersion = async (vId: string) => {
    if (!window.confirm("Are you sure you want to delete this version?")) return;
    try {
      await reportApi.delete(`/templates/versions/${vId}`);
      notifications.show({ title: 'Success', message: 'Version deleted', color: 'green' });
      fetchData();
    } catch (err: any) {
      notifications.show({ title: 'Error', message: err.response?.data?.message || 'Delete failed', color: 'red' });
    }
  };

  const handleSaveMapping = async () => {
    if (!targetVersion || !mappingQueryId || !mappingNodeName) return;
    try {
      const payload = { templateId: template.id, versionId: targetVersion.id, queryId: mappingQueryId, jsonNodeName: mappingNodeName };
      if (editingMappingId) {
        await reportApi.put(`/templates/mappings/${editingMappingId}`, payload);
      } else {
        await reportApi.post(`/templates/versions/${targetVersion.id}/mappings`, payload);
      }
      notifications.show({ title: 'Success', message: 'Mapping updated', color: 'green' });
      closeMapping(); setMappingNodeName(''); setMappingQueryId(''); setEditingMappingId(null);
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to save mapping', color: 'red' });
    }
  };



  const handleDeleteMapping = async (mappingId: string) => {
    try {
      await reportApi.delete(`/templates/mappings/${mappingId}`);
      notifications.show({ title: 'Success', message: 'Mapping removed', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to remove mapping', color: 'red' });
    }
  };

  const handleUpdateFile = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    try {
      await reportApi.put(`/templates/${id}/file`, formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      notifications.show({ title: 'Success', message: 'Template replaced (New version created)', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Update failed', color: 'red' });
    }
  };

  const renderHeader = () => {
    let title = template.name;
    let badgeText = `v${template.latestVersionNumber}`;
    let badgeColor = "blue";
    if (versionsMode) { title = `Version History: ${template.name}`; badgeText = "All Records"; badgeColor="indigo"; }
    else if (editMode) title = `Edit Template: ${template.name}`;
    else if (versionViewMode) title = `Snapshot View (v${targetVersion?.versionNumber})`;
    else if (versionEditMode) title = `Snapshot Edit (v${targetVersion?.versionNumber})`;

    return (
      <Group justify="space-between" mb="xl">
        <Group>
          <ActionIcon variant="light" color="gray" onClick={() => navigate(versionsMode || editMode || versionViewMode || versionEditMode ? `/templates/${id}/view` : '/templates')}>
            <IconArrowLeft size={18} />
          </ActionIcon>
          <div>
             <Group gap="xs">
               <Text size="xl" fw={800}>{title}</Text>
               <Badge variant="light" color={badgeColor}>{badgeText}</Badge>
             </Group>
             <Text size="sm" fw={500}>{template.description}</Text>
          </div>
        </Group>
        <Group>
          {!editMode && !versionsMode && !versionViewMode && !versionEditMode && (
             <Button variant="light" color="indigo" leftSection={<IconFileExport size={16}/>} onClick={handleExport}>
                Export Configuration
             </Button>
          )}
        </Group>
      </Group>
    );
  };



  let content = null;

  if (versionsMode) {
    content = (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md">
          <Table verticalSpacing="md" highlightOnHover>
            <Table.Thead bg="gray.0"><Table.Tr><Table.Th>Version</Table.Th><Table.Th>Date</Table.Th><Table.Th>Author</Table.Th><Table.Th>Status</Table.Th><Table.Th style={{ textAlign: 'center' }}>Actions</Table.Th></Table.Tr></Table.Thead>
            <Table.Tbody>
              {template.versions?.map(v => (
                <Table.Tr key={v.id}>
                   <Table.Td><Text fw={700}>v{v.versionNumber}</Text></Table.Td>
                   <Table.Td><Text size="sm">{new Date(v.createdAt).toLocaleDateString()}</Text></Table.Td>
                   <Table.Td><Text size="sm">{v.createdBy || 'System'}</Text></Table.Td>
                   <Table.Td>{v.isActive === 1 ? <Badge color="green">Active</Badge> : <Badge color="gray" variant="filled">Archived</Badge>}</Table.Td>
                   <Table.Td><Center><Group gap="lg">
                    <Tooltip label="View Details"><ActionIcon variant="subtle" color="blue" onClick={() => navigate(`/templates/${id}/versions/${v.id}/view`)}><IconEye size={18} /></ActionIcon></Tooltip>
                    {v.isActive === 1 && <Tooltip label="Edit"><ActionIcon variant="subtle" color="orange" onClick={() => navigate(`/templates/${id}/versions/${v.id}/edit`)}><IconPencil size={18} /></ActionIcon></Tooltip>}
                    <Tooltip label="Delete"><ActionIcon variant="subtle" color="red" disabled={v.isActive === 1} onClick={() => handleDeleteVersion(v.id)}><IconTrash size={18} /></ActionIcon></Tooltip>
                   </Group></Center></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
      </Stack>
    );
  } else if (editMode) {
    content = (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder p="xl" radius="md">
          <Stack>
            <Text fw={700}>Update Template</Text>
            <Text size="xs" c="dimmed">Create a new version by uploading a file.</Text>
            <FileInput placeholder="Replace file" accept=".docx,.xlsx" onChange={(f: any) => f && handleUpdateFile(f)} leftSection={<IconPlus size={16}/>} />
          </Stack>
        </Card>
        <Card withBorder p="xl" radius="md">
          <Group justify="space-between" mb="lg">
            <Text fw={700}>Mapping Workshop (v{targetVersion?.versionNumber})</Text>
            <Button size="xs" leftSection={<IconPlus size={14} />} onClick={() => { setEditingMappingId(null); setMappingNodeName(''); setMappingQueryId(''); openMapping(); }}>New Mapping</Button>
          </Group>
          <Table verticalSpacing="sm">
            <Table.Thead bg="gray.0"><Table.Tr><Table.Th>JSON Node</Table.Th><Table.Th>Target Query</Table.Th><Table.Th align="right" style={{ textAlign: 'right' }}>Actions</Table.Th></Table.Tr></Table.Thead>
            <Table.Tbody>
              {targetVersion?.mappings?.map(m => (
                <Table.Tr key={m.id}>
                  <Table.Td><Text fw={600} size="sm">{m.jsonNodeName}</Text></Table.Td>
                  <Table.Td><Text size="xs" c="dimmed">{m.queryName}</Text></Table.Td>
                  <Table.Td><Group gap="xs" justify="flex-end">
                    <ActionIcon variant="subtle" onClick={() => { setEditingMappingId(m.id); setMappingNodeName(m.jsonNodeName); setMappingQueryId(m.queryId); openMapping(); }}><IconPencil size={14} /></ActionIcon>
                    <ActionIcon variant="subtle" color="red" onClick={() => handleDeleteMapping(m.id)}><IconTrash size={14} /></ActionIcon>
                  </Group></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
        <Modal opened={mappingOpened} onClose={closeMapping} title="Mapping Configuration">
          <Stack>
            <Select label="Query" data={queries.map(q => ({ value: q.id, label: q.name }))} value={mappingQueryId} onChange={(v) => setMappingQueryId(v || '')} searchable required />
            <TextInput label="Node Name" value={mappingNodeName} onChange={(e: any) => setMappingNodeName(e.target.value)} required />
            <Button onClick={handleSaveMapping}>Apply</Button>
          </Stack>
        </Modal>
      </Stack>
    );
  } else if (versionViewMode) {
    content = (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md" p="md">
          <Group justify="space-between">
            <Group gap="xl">
              <div><Text size="xs" fw={700} c="dimmed">SNAPSHOT VERSION</Text><Badge variant="filled" color="indigo" size="lg">v{targetVersion?.versionNumber}</Badge></div>
              <div><Text size="xs" fw={700} c="dimmed">FORMAT</Text><Badge variant="light" size="lg">{targetVersion?.fileType}</Badge></div>
            </Group>
            <Group gap="sm">

              <Button variant="light" leftSection={<IconDownload size={16}/>} onClick={() => targetVersion && handleDownload(targetVersion)}>Download File</Button>
            </Group>
          </Group>
        </Card>
        <div>
           <Text fw={700} mb="md" size="lg">Mappings</Text>
           <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {targetVersion?.mappings?.map(m => (
              <Card key={m.id} withBorder p="md" radius="md" bg="gray.0">
                <Group justify="space-between">
                  <div><Text fw={700} size="sm">{m.jsonNodeName}</Text><Text size="xs" c="dimmed">{m.queryName}</Text></div>
                  <ThemeIcon color="gray" variant="light" radius="xl"><IconDatabase size={14}/></ThemeIcon>
                </Group>
              </Card>
            ))}
          </SimpleGrid>
        </div>
      </Stack>
    );
  } else if (versionEditMode) {
    const isArchived = targetVersion?.isActive !== 1;
    content = (
      <Stack gap="xl">
        {renderHeader()}
        {isArchived && <Alert color="gray" variant="light" title="Read-Only Snapshot" icon={<IconLock size={18} />}>Historical integrity enforced. Edit disabled.</Alert>}
        <Card withBorder p="xl" radius="md">
          <Group justify="space-between" mb="lg">
            <div><Text fw={700}>Remapping Session</Text></div>
            {!isArchived && <Button size="xs" leftSection={<IconPlus size={14} />} onClick={() => { setEditingMappingId(null); setMappingNodeName(''); setMappingQueryId(''); openMapping(); }}>Add Mapping</Button>}
          </Group>
          <Table verticalSpacing="sm">
            <Table.Thead bg="gray.0"><Table.Tr><Table.Th>JSON Node</Table.Th><Table.Th>Target Query</Table.Th>{!isArchived && <Table.Th align="right" style={{ textAlign: 'right' }}>Actions</Table.Th>}</Table.Tr></Table.Thead>
            <Table.Tbody>
              {targetVersion?.mappings?.map(m => (
                <Table.Tr key={m.id}>
                  <Table.Td><Text fw={600} size="sm">{m.jsonNodeName}</Text></Table.Td>
                  <Table.Td><Text size="xs" c="dimmed">{m.queryName}</Text></Table.Td>
                  {!isArchived && <Table.Td><Group gap="xs" justify="flex-end">
                    <ActionIcon variant="subtle" onClick={() => { setEditingMappingId(m.id); setMappingNodeName(m.jsonNodeName); setMappingQueryId(m.queryId); openMapping(); }}><IconPencil size={14} /></ActionIcon>
                    <ActionIcon variant="subtle" color="red" onClick={() => handleDeleteMapping(m.id)}><IconTrash size={14} /></ActionIcon>
                  </Group></Table.Td>}
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
        <Modal opened={mappingOpened} onClose={closeMapping} title="Edit Mapping">
          <Stack>
            <Select label="Query" data={queries.map(q => ({ value: q.id, label: q.name }))} value={mappingQueryId} onChange={(v) => setMappingQueryId(v || '')} searchable required />
            <TextInput label="Node Name" value={mappingNodeName} onChange={(e: any) => setMappingNodeName(e.target.value)} required />
            <Button onClick={handleSaveMapping}>Save</Button>
          </Stack>
        </Modal>
      </Stack>
    );
  } else {
    // 1. VIEW MODE (Read-Only)
    content = (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md" p="md">
          <Group justify="space-between">
            <Group gap="xl">
              <div><Text size="xs" fw={700} c="dimmed">ACTIVE VERSION</Text><Badge variant="filled" color="green" size="lg">v{template.latestVersionNumber}</Badge></div>
              <div><Text size="xs" fw={700} c="dimmed">FORMAT</Text><Badge variant="light" size="lg">{targetVersion?.fileType}</Badge></div>
            </Group>
            <Group gap="sm">

              <Button leftSection={<IconDownload size={16}/>} onClick={() => targetVersion && handleDownload(targetVersion)}>Download Template</Button>
            </Group>
          </Group>
        </Card>
        <div>
          <Text fw={700} mb="md" size="lg">Complete Live Mappings</Text>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {targetVersion?.mappings?.map(m => {
              const dbInfo = getDbTypeLabel(m.connectorDbType);
              return (
                <Card key={m.id} withBorder p="md" radius="md">
                  <Group justify="space-between" align="flex-start">
                    <div>
                      <Text fw={700} size="sm">{m.jsonNodeName}</Text>
                      <Text size="xs" c="dimmed" mb={4}>Source: {m.queryName}</Text>
                      <Badge size="xs" variant="light" color={dbInfo.color}>{dbInfo.label}</Badge>
                    </div>
                    <ThemeIcon color={dbInfo.color} variant="light" radius="xl"><IconDatabase size={14}/></ThemeIcon>
                  </Group>
                </Card>
              );
            })}
          </SimpleGrid>
        </div>

        <div>
           <Text fw={700} mb="md" size="lg">Required Parameters</Text>
           <Text size="sm" c="dimmed" mb="md">These parameters must be provided to generate a report from this snapshot.</Text>
           <Card withBorder radius="md" p={0}>
            <Table verticalSpacing="sm" horizontalSpacing="md">
              <Table.Thead bg="gray.0">
                <Table.Tr>
                  <Table.Th>Name</Table.Th>
                  <Table.Th>Type</Table.Th>
                  <Table.Th>Description</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {placeholders.map(p => (
                  <Table.Tr key={p.name}>
                    <Table.Td><Text fw={600} size="sm">{p.name}</Text></Table.Td>
                    <Table.Td>
                      <Badge size="xs" variant="light" color={p.type === 'STRING' ? 'blue' : p.type === 'NUMBER' ? 'orange' : 'teal'}>
                        {p.type}
                      </Badge>
                    </Table.Td>
                    <Table.Td><Text size="xs">{p.description || '--'}</Text></Table.Td>
                  </Table.Tr>
                ))}
                {placeholders.length === 0 && (
                  <Table.Tr>
                    <Table.Td colSpan={3} align="center"><Text size="xs" c="dimmed" fs="italic">No placeholders detected in mapped queries.</Text></Table.Td>
                  </Table.Tr>
                )}
              </Table.Tbody>
            </Table>
           </Card>
        </div>
      </Stack>
    );
  }

  return (
    <>

      {content}
    </>
  );
}

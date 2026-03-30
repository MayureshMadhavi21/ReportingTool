import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select,
  Stack, Badge, Card, SimpleGrid, ThemeIcon, 
  FileInput, Tooltip, Center
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { 
  IconTrash, IconDownload, 
  IconArrowLeft, IconDatabase, IconPlus, IconPencil, IconEye
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
  const [loading, setLoading] = useState(true);

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

      // Determine the target version for the workshop/view
      if (versionId) {
        const v = data.versions?.find((v: TemplateVersion) => v.id === versionId);
        setTargetVersion(v || null);
      } else {
        const latest = data.versions?.find((v: TemplateVersion) => v.isActive === 1) || data.versions?.[0];
        setTargetVersion(latest || null);
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
      
      const response = await reportApi.get(`/templates/versions/${v.id}/file`, {
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const extension = v.fileType === 'XLSX' ? 'xlsx' : 'docx';
      const filename = `${template.name}_v${v.versionNumber}.${extension}`;
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      notifications.update({ id: 'downloading', title: 'Success', message: 'Download complete!', color: 'green', loading: false, autoClose: 2000 });
    } catch (err) {
      notifications.update({ id: 'downloading', title: 'Error', message: 'Failed to download file', color: 'red', loading: false, autoClose: 3000 });
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
      const payload = {
        templateId: template.id,
        versionId: targetVersion.id,
        queryId: mappingQueryId,
        jsonNodeName: mappingNodeName
      };

      if (editingMappingId) {
        await reportApi.put(`/templates/mappings/${editingMappingId}`, payload);
      } else {
        await reportApi.post(`/templates/versions/${targetVersion.id}/mappings`, payload);
      }
      notifications.show({ title: 'Success', message: 'Mapping updated', color: 'green' });
      closeMapping();
      setMappingNodeName('');
      setMappingQueryId('');
      setEditingMappingId(null);
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
    if (editMode) title = `Edit Template: ${template.name}`;
    if (versionViewMode) title = `Version Snapshot Details (v${targetVersion?.versionNumber})`;
    if (versionEditMode) title = `Re-mapping Snapshot (v${targetVersion?.versionNumber})`;

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
             <Text size="sm" c="dimmed">{template.description}</Text>
          </div>
        </Group>
      </Group>
    );
  };

  // 1. VIEW MODE (Read-Only)
  if (!editMode && !versionsMode && !versionViewMode && !versionEditMode) {
    return (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md" p="md">
          <Group justify="space-between">
            <Group gap="xl">
              <div>
                <Text size="xs" fw={700} c="dimmed">ACTIVE VERSION</Text>
                <Badge variant="filled" color="green" size="lg">v{template.latestVersionNumber}</Badge>
              </div>
              <div>
                <Text size="xs" fw={700} c="dimmed">FORMAT</Text>
                <Badge variant="light" size="lg">{targetVersion?.fileType}</Badge>
              </div>
            </Group>
            <Button leftSection={<IconDownload size={16}/>} onClick={() => targetVersion && handleDownload(targetVersion)}>
              Download Template
            </Button>
          </Group>
        </Card>

        <div>
          <Text fw={700} mb="md" size="lg">Complete Live Mappings</Text>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {targetVersion?.mappings?.map(m => (
              <Card key={m.id} withBorder p="md" radius="md">
                <Group justify="space-between">
                  <div>
                    <Text fw={700} size="sm">{m.jsonNodeName}</Text>
                    <Text size="xs" c="dimmed">Source: {m.queryName}</Text>
                  </div>
                  <ThemeIcon color="green" variant="light" radius="xl"><IconDatabase size={14}/></ThemeIcon>
                </Group>
              </Card>
            ))}
          </SimpleGrid>
        </div>
      </Stack>
    );
  }

  // 2. EDIT MODE (Global Edit / Latest Version)
  if (editMode) {
    return (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder p="xl" radius="md">
          <Stack>
            <Text fw={700}>Update Template</Text>
            <Text size="xs" c="dimmed">Replace the existing template file. This will create a new version and preserve mapping config.</Text>
            <FileInput 
              placeholder="Click to upload/replace file" 
              accept=".docx,.xlsx" 
              onChange={(f: any) => f && handleUpdateFile(f)}
              leftSection={<IconPlus size={16}/>}
            />
          </Stack>
        </Card>

        <Card withBorder p="xl" radius="md">
            <Group justify="space-between" mb="lg">
              <Text fw={700}>Mapping Workshop (v{targetVersion?.versionNumber})</Text>
              <Button size="xs" leftSection={<IconPlus size={14} />} onClick={() => { setEditingMappingId(null); setMappingNodeName(''); setMappingQueryId(''); openMapping(); }}>
                New Mapping
              </Button>
            </Group>

            <Table verticalSpacing="sm">
              <Table.Thead bg="gray.0">
                <Table.Tr>
                  <Table.Th>JSON Node</Table.Th>
                  <Table.Th>Target Query</Table.Th>
                  <Table.Th align="right" style={{ textAlign: 'right' }}>Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {targetVersion?.mappings?.map(m => (
                  <Table.Tr key={m.id}>
                    <Table.Td><Text fw={600} size="sm">{m.jsonNodeName}</Text></Table.Td>
                    <Table.Td><Text size="xs" c="dimmed">{m.queryName}</Text></Table.Td>
                    <Table.Td>
                      <Group gap="xs" justify="flex-end">
                        <ActionIcon variant="subtle" onClick={() => { setEditingMappingId(m.id); setMappingNodeName(m.jsonNodeName); setMappingQueryId(m.queryId); openMapping(); }}>
                          <IconPencil size={14} />
                        </ActionIcon>
                        <ActionIcon variant="subtle" color="red" onClick={() => handleDeleteMapping(m.id)}>
                          <IconTrash size={14} />
                        </ActionIcon>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Card>
          {/* Mapping Modal Container */}
          <Modal opened={mappingOpened} onClose={closeMapping} title="Mapping Configuration">
            <Stack>
              <Select label="Query" data={queries.map(q => ({ value: q.id, label: q.name }))} value={mappingQueryId} onChange={(v) => setMappingQueryId(v || '')} searchable required />
              <TextInput label="Node Name" value={mappingNodeName} onChange={(e: any) => setMappingNodeName(e.target.value)} required />
              <Button onClick={handleSaveMapping}>Apply</Button>
            </Stack>
          </Modal>
      </Stack>
    );
  }

  // 3. VERSIONS MODE (History List)
  if (versionsMode) {
    return (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md">
          <Table verticalSpacing="md" highlightOnHover>
            <Table.Thead bg="gray.0">
               <Table.Tr>
                 <Table.Th>Version</Table.Th>
                 <Table.Th>Date</Table.Th>
                 <Table.Th>Author</Table.Th>
                 <Table.Th>Status</Table.Th>
                 <Table.Th style={{ textAlign: 'center' }}>Actions</Table.Th>
               </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {template.versions?.map(v => (
                <Table.Tr key={v.id}>
                   <Table.Td><Text fw={700}>v{v.versionNumber}</Text></Table.Td>
                   <Table.Td><Text size="sm">{new Date(v.createdAt).toLocaleDateString()}</Text></Table.Td>
                   <Table.Td><Text size="sm">{v.createdBy || 'System'}</Text></Table.Td>
                   <Table.Td>
                     {v.isActive === 1 ? <Badge color="green">Active</Badge> : <Badge color="gray" variant="light">Archived</Badge>}
                   </Table.Td>
                   <Table.Td>
                     <Center>
                        <Group gap="lg">
                          <Tooltip label="View Version">
                            <ActionIcon variant="subtle" color="blue" onClick={() => navigate(`/templates/${id}/versions/${v.id}/view`)}>
                              <IconEye size={18} />
                            </ActionIcon>
                          </Tooltip>
                          <Tooltip label="Edit Version">
                            <ActionIcon variant="subtle" color="orange" onClick={() => navigate(`/templates/${id}/versions/${v.id}/edit`)}>
                              <IconPencil size={18} />
                            </ActionIcon>
                          </Tooltip>
                          <Tooltip label="Delete Version">
                            <ActionIcon variant="subtle" color="red" disabled={v.isActive === 1} onClick={() => handleDeleteVersion(v.id)}>
                              <IconTrash size={18} />
                            </ActionIcon>
                          </Tooltip>
                        </Group>
                     </Center>
                   </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
      </Stack>
    );
  }

  // 4. VERSION VIEW (Read-Only specific snapshot)
  if (versionViewMode) {
    return (
      <Stack gap="xl">
        {renderHeader()}
        <Card withBorder radius="md" p="md">
          <Group justify="space-between">
            <Group gap="xl">
              <div>
                <Text size="xs" fw={700} c="dimmed">SNAPSHOT VERSION</Text>
                <Badge variant="filled" color="indigo" size="lg">v{targetVersion?.versionNumber}</Badge>
              </div>
              <div>
                <Text size="xs" fw={700} c="dimmed">FORMAT</Text>
                <Badge variant="light" size="lg">{targetVersion?.fileType}</Badge>
              </div>
            </Group>
            <Button variant="light" leftSection={<IconDownload size={16}/>} onClick={() => targetVersion && handleDownload(targetVersion)}>
              Download Snapshot File
            </Button>
          </Group>
        </Card>
        <div>
           <Text fw={700} mb="md" size="lg">Snapshot Mapping Details</Text>
           <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {targetVersion?.mappings?.length ? targetVersion.mappings.map(m => (
              <Card key={m.id} withBorder p="md" radius="md" bg="gray.0">
                <Group justify="space-between">
                  <div>
                    <Text fw={700} size="sm">{m.jsonNodeName}</Text>
                    <Text size="xs" c="dimmed">{m.queryName}</Text>
                  </div>
                  <ThemeIcon color="gray" variant="light" radius="xl"><IconDatabase size={14}/></ThemeIcon>
                </Group>
              </Card>
            )) : <Text size="sm" fs="italic" c="dimmed">No mappings for this version.</Text>}
          </SimpleGrid>
        </div>
      </Stack>
    );
  }

  // 5. VERSION EDIT (Mapping adjust specific snapshot)
  if (versionEditMode) {
    return (
       <Stack gap="xl">
         {renderHeader()}
         <Card withBorder p="xl" radius="md">
            <Group justify="space-between" mb="lg">
              <div>
                <Text fw={700}>Remapping Session</Text>
                <Text size="xs" c="dimmed">Modify query connections for this specific snapshot. This will not affect other versions.</Text>
              </div>
              <Button size="xs" leftSection={<IconPlus size={14} />} onClick={() => { setEditingMappingId(null); setMappingNodeName(''); setMappingQueryId(''); openMapping(); }}>
                Add Mapping
              </Button>
            </Group>

            <Table verticalSpacing="sm">
              <Table.Thead bg="gray.0">
                <Table.Tr>
                  <Table.Th>JSON Node</Table.Th>
                  <Table.Th>Target Query</Table.Th>
                  <Table.Th align="right" style={{ textAlign: 'right' }}>Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {targetVersion?.mappings?.map(m => (
                  <Table.Tr key={m.id}>
                    <Table.Td><Text fw={600} size="sm">{m.jsonNodeName}</Text></Table.Td>
                    <Table.Td><Text size="xs" c="dimmed">{m.queryName}</Text></Table.Td>
                    <Table.Td>
                      <Group gap="xs" justify="flex-end">
                        <ActionIcon variant="subtle" onClick={() => { setEditingMappingId(m.id); setMappingNodeName(m.jsonNodeName); setMappingQueryId(m.queryId); openMapping(); }}>
                          <IconPencil size={14} />
                        </ActionIcon>
                        <ActionIcon variant="subtle" color="red" onClick={() => handleDeleteMapping(m.id)}>
                          <IconTrash size={14} />
                        </ActionIcon>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Card>
          <Modal opened={mappingOpened} onClose={closeMapping} title="Edit Mapping">
            <Stack>
              <Select label="Query" data={queries.map(q => ({ value: q.id, label: q.name }))} value={mappingQueryId} onChange={(v) => setMappingQueryId(v || '')} searchable required />
              <TextInput label="Node Name" value={mappingNodeName} onChange={(e: any) => setMappingNodeName(e.target.value)} required />
              <Button onClick={handleSaveMapping}>Save Snapshot Change</Button>
            </Stack>
          </Modal>
       </Stack>
    );
  }

  return null;
}

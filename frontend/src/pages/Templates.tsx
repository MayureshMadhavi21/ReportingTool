import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, Stack, FileInput, Badge } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash, IconDeviceFloppy } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import api from '../api';
import type { Template, QueryDef } from '../types';

export default function Templates() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [queries, setQueries] = useState<QueryDef[]>([]);
  
  const [uploadOpened, { open: openUpload, close: closeUpload }] = useDisclosure(false);
  const [mappingOpened, { open: openMapping, close: closeMapping }] = useDisclosure(false);
  
  const [selectedTemplate, setSelectedTemplate] = useState<Template | null>(null);
  
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  
  const [mappingQueryId, setMappingQueryId] = useState<string>('');
  const [mappingNodeName, setMappingNodeName] = useState('');

  const fetchData = async () => {
    try {
      const [tRes, qRes] = await Promise.all([api.get('/templates'), api.get('/queries')]);
      setTemplates(tRes.data);
      setQueries(qRes.data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load data', color: 'red' });
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleUpload = async () => {
    if (!file || !name) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', name);
    formData.append('description', description);

    try {
      await api.post('/templates', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      notifications.show({ title: 'Success', message: 'Template uploaded', color: 'green' });
      closeUpload();
      setFile(null);
      setName('');
      setDescription('');
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to upload template', color: 'red' });
    }
  };

  const handleAddMapping = async () => {
    if (!selectedTemplate || !mappingQueryId || !mappingNodeName) return;
    try {
      await api.post(`/templates/${selectedTemplate.id}/mappings`, {
        templateId: selectedTemplate.id,
        queryId: Number(mappingQueryId),
        jsonNodeName: mappingNodeName
      });
      notifications.show({ title: 'Success', message: 'Mapping added', color: 'green' });
      closeMapping();
      setMappingNodeName('');
      setMappingQueryId('');
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to add mapping', color: 'red' });
    }
  };

  const handleDeleteMapping = async (mappingId: number) => {
    try {
      await api.delete(`/templates/mappings/${mappingId}`);
      notifications.show({ title: 'Success', message: 'Mapping removed', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to remove mapping', color: 'red' });
    }
  };

  return (
    <div>
      <Group justify="space-between" mb="md">
        <Text size="xl" fw={700}>Report Templates</Text>
        <Button onClick={openUpload}>Upload Template</Button>
      </Group>

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>Mappings</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {templates.map((t) => (
            <Table.Tr key={t.id}>
              <Table.Td>{t.name}</Table.Td>
              <Table.Td><Badge color={t.fileType === 'XLSX' ? 'green' : 'blue'}>{t.fileType}</Badge></Table.Td>
              <Table.Td>
                {t.mappings?.map(m => (
                  <Badge key={m.id} variant="outline" mr="xs" rightSection={
                    <ActionIcon size="xs" color="red" variant="transparent" onClick={() => handleDeleteMapping(m.id)}>
                      <IconTrash size={10} />
                    </ActionIcon>
                  }>
                    {m.jsonNodeName} ➜ {m.queryName}
                  </Badge>
                ))}
                <Button size="compact-xs" variant="light" onClick={() => { setSelectedTemplate(t); openMapping(); }}>
                  + Add
                </Button>
              </Table.Td>
              <Table.Td>
                {/* Future: Delete Template Action */}
              </Table.Td>
            </Table.Tr>
          ))}
          {templates.length === 0 && (
            <Table.Tr>
              <Table.Td colSpan={4} align="center">No templates found</Table.Td>
            </Table.Tr>
          )}
        </Table.Tbody>
      </Table>

      <Modal opened={uploadOpened} onClose={closeUpload} title="Upload New Template">
        <Stack>
          <TextInput label="Template Name" required value={name} onChange={(e) => setName(e.target.value)} />
          <TextInput label="Description" value={description} onChange={(e) => setDescription(e.target.value)} />
          <FileInput label="Document File" required placeholder="Select Word/Excel file" accept=".docx,.xlsx" value={file} onChange={setFile} />
          <Button onClick={handleUpload} mt="md" leftSection={<IconDeviceFloppy size={16}/>}>Upload</Button>
        </Stack>
      </Modal>

      <Modal opened={mappingOpened} onClose={closeMapping} title={`Map Query to ${selectedTemplate?.name}`}>
        <Stack>
          <Select 
            label="Query" 
            required 
            data={queries.map(q => ({ value: q.id.toString(), label: q.name }))} 
            value={mappingQueryId} 
            onChange={(v) => setMappingQueryId(v || '')} 
          />
          <TextInput 
            label="JSON Node Name (e.g. 'sales')" 
            description="The placeholder object name used in the Aspose template (e.g. <<[sales.amount]>>)"
            required 
            value={mappingNodeName} 
            onChange={(e) => setMappingNodeName(e.target.value)} 
          />
          <Button onClick={handleAddMapping} mt="md">Save Mapping</Button>
        </Stack>
      </Modal>
    </div>
  );
}

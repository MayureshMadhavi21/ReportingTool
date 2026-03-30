import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Button, Table, Group, Text, ActionIcon, Modal, 
  TextInput, Stack, FileInput, Textarea, Card, ThemeIcon, Tooltip, Center
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash, IconPencil, IconPlus, IconFileDescription, IconHistory, IconEye } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { reportApi } from '../api';
import type { Template } from '../types';

export default function Templates() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const navigate = useNavigate();
  
  const [uploadOpened, { open: openUpload, close: closeUpload }] = useDisclosure(false);
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const fetchData = async () => {
    try {
      const { data } = await reportApi.get('/templates');
      setTemplates(data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load templates', color: 'red' });
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
      await reportApi.post('/templates', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      notifications.show({ title: 'Success', message: 'Template created', color: 'green' });
      closeUpload();
      setFile(null);
      setName('');
      setDescription('');
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to create template', color: 'red' });
    }
  };

  const handleDeleteTemplate = async (id: string) => {
    if (!confirm('Are you certain? This will delete the template and all its snapshots.')) return;
    try {
      await reportApi.delete(`/templates/${id}`);
      notifications.show({ title: 'Success', message: 'Template removed', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to delete template', color: 'red' });
    }
  };

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Text size="xl" fw={800}>Report Inventory</Text>
          <Text size="sm" c="dimmed">Track your master templates and their historical revision snapshots.</Text>
        </div>
        <Button leftSection={<IconPlus size={18}/>} onClick={openUpload}>Create Template</Button>
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
                    <ThemeIcon color="blue" variant="light" size="md">
                      <IconFileDescription size={18} />
                    </ThemeIcon>
                    <Text fw={600} size="sm">{t.name}</Text>
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Text size="xs" c="dimmed" lineClamp={1}>{t.description || 'No description provided'}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="xs" fw={700}>v{t.latestVersionNumber}</Text>
                </Table.Td>
                <Table.Td>
                  <Center>
                    <Group gap="md">
                      <Tooltip label="View Template Details">
                        <ActionIcon variant="light" color="blue" onClick={() => navigate(`/templates/${t.id}/view`)}>
                          <IconEye size={18} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Edit Workshop">
                        <ActionIcon variant="light" color="orange" onClick={() => navigate(`/templates/${t.id}/edit`)}>
                          <IconPencil size={18} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Version Snapshots">
                        <ActionIcon variant="light" color="indigo" onClick={() => navigate(`/templates/${t.id}/versions`)}>
                          <IconHistory size={18} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Delete Template">
                        <ActionIcon variant="light" color="red" onClick={() => handleDeleteTemplate(t.id)}>
                          <IconTrash size={18} />
                        </ActionIcon>
                      </Tooltip>
                    </Group>
                  </Center>
                </Table.Td>
              </Table.Tr>
            ))}
            {templates.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={4} align="center" py="xl">
                  <Text c="dimmed">No templates found.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      </Card>

      <Modal opened={uploadOpened} onClose={closeUpload} title="New Template Record" size="lg">
        <Stack>
          <TextInput label="Template Name" placeholder="e.g. Compliance Report" required value={name} onChange={(e) => setName(e.target.value)} />
          <Textarea label="Description" placeholder="Context for other authors" value={description} onChange={(e) => setDescription(e.target.value)} />
          <FileInput label="Initial Document (v1)" required placeholder="Select .docx or .xlsx" accept=".docx,.xlsx" value={file} onChange={setFile} />
          <Group justify="flex-end" mt="md">
            <Button variant="subtle" onClick={closeUpload}>Cancel</Button>
            <Button onClick={handleUpload}>Create Inventory Record</Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}

import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, Stack, Textarea, Divider } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash, IconSettings } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { connectorApi } from '../api';
import type { QueryDef, Connector, PlaceholderMetadata } from '../types';

export default function Queries() {
  const [queries, setQueries] = useState<QueryDef[]>([]);
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [opened, { open, close }] = useDisclosure(false);
  const [formData, setFormData] = useState<Partial<QueryDef>>({});
  const [editingId, setEditingId] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      const [qRes, cRes] = await Promise.all([connectorApi.get('/queries'), connectorApi.get('/connectors')]);
      setQueries(qRes.data);
      setConnectors(cRes.data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load data', color: 'red' });
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // auto-detect placeholders
  useEffect(() => {
    if (!formData.queryText) {
      if (formData.placeholderMetadata && formData.placeholderMetadata.length > 0) {
        setFormData(prev => ({ ...prev, placeholderMetadata: [] }));
      }
      return;
    }

    const regex = /:(\w+)/g;
    const foundNames = new Set<string>();
    let match;
    while ((match = regex.exec(formData.queryText)) !== null) {
      foundNames.add(match[1]);
    }

    const currentMetadata = formData.placeholderMetadata || [];
    const currentNames = currentMetadata.map(m => m.name);

    const hasChanged = 
      foundNames.size !== currentNames.length || 
      Array.from(foundNames).some(name => !currentNames.includes(name));

    if (hasChanged) {
      const newMetadata: PlaceholderMetadata[] = Array.from(foundNames).map(name => {
        const existing = currentMetadata.find(m => m.name === name);
        return existing || { name, type: 'STRING', description: '' };
      });
      setFormData(prev => ({ ...prev, placeholderMetadata: newMetadata }));
    }
  }, [formData.queryText]);

  const handleSave = async () => {
    try {
      if (editingId) {
        await connectorApi.put(`/queries/${editingId}`, formData);
        notifications.show({ title: 'Success', message: 'Query updated successfully', color: 'green' });
      } else {
        await connectorApi.post('/queries', formData);
        notifications.show({ title: 'Success', message: 'Query created successfully', color: 'green' });
      }
      close();
      setFormData({});
      setEditingId(null);
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to save query', color: 'red' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      await connectorApi.delete(`/queries/${id}`);
      notifications.show({ title: 'Success', message: 'Query deleted', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to delete query', color: 'red' });
    }
  };

  const updatePlaceholder = (name: string, field: keyof PlaceholderMetadata, value: string) => {
    const newMetadata = (formData.placeholderMetadata || []).map(m => 
      m.name === name ? { ...m, [field]: value } : m
    );
    setFormData({ ...formData, placeholderMetadata: newMetadata });
  };

  return (
    <div>
      <Group justify="space-between" mb="md">
        <Text size="xl" fw={700}>Registered Queries</Text>
        <Button onClick={() => { setEditingId(null); setFormData({ placeholderMetadata: [] }); open(); }}>Add Query</Button>
      </Group>

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Connector ID</Table.Th>
            <Table.Th>Description</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {queries.map((q) => (
            <Table.Tr key={q.id}>
              <Table.Td>{q.name}</Table.Td>
              <Table.Td>{q.connectorId}</Table.Td>
              <Table.Td>{q.description}</Table.Td>
              <Table.Td>
                <Group gap="xs">
                  <Button size="compact-xs" variant="light" onClick={() => { 
                    setEditingId(q.id); 
                    setFormData({ 
                      name: q.name, 
                      connectorId: q.connectorId, 
                      description: q.description, 
                      queryText: q.queryText,
                      placeholderMetadata: q.placeholderMetadata || [] 
                    }); 
                    open(); 
                  }}>
                    Edit
                  </Button>
                  <ActionIcon color="red" onClick={() => handleDelete(q.id)}>
                    <IconTrash size={16} />
                  </ActionIcon>
                </Group>
              </Table.Td>
            </Table.Tr>
          ))}
          {queries.length === 0 && (
            <Table.Tr>
              <Table.Td colSpan={4} align="center">No queries found</Table.Td>
            </Table.Tr>
          )}
        </Table.Tbody>
      </Table>

      <Modal opened={opened} onClose={close} title={editingId ? "Edit Query" : "Add Query"} size="xl">
        <Stack>
          <Select 
            label="Connector" 
            required 
            data={connectors.map(c => ({ value: c.id.toString(), label: c.name }))} 
            value={formData.connectorId?.toString() || ''} 
            onChange={(v) => setFormData({...formData, connectorId: v || ''})} 
          />
          <TextInput label="Query Name" required value={formData.name || ''} onChange={(e) => setFormData({...formData, name: e.target.value})} />
          <TextInput label="Description" value={formData.description || ''} onChange={(e) => setFormData({...formData, description: e.target.value})} />
          <Textarea 
            label="SQL Query" 
            placeholder="SELECT * FROM table WHERE id = :id"
            required 
            minRows={4}
            value={formData.queryText || ''} 
            onChange={(e) => setFormData({...formData, queryText: e.target.value})} 
          />

          {formData.placeholderMetadata && formData.placeholderMetadata.length > 0 && (
            <>
              <Divider my="sm" label={<Group gap={2}><IconSettings size={14}/> <Text size="xs" fw={700}>PLACEHOLDER CONFIGURATION</Text></Group>} labelPosition="center" />
              <Table withTableBorder withColumnBorders>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Name</Table.Th>
                    <Table.Th style={{ width: 150 }}>Data Type</Table.Th>
                    <Table.Th>Description</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {formData.placeholderMetadata.map((meta) => (
                    <Table.Tr key={meta.name}>
                      <Table.Td><Text size="sm" fw={500}>{meta.name}</Text></Table.Td>
                      <Table.Td>
                        <Select 
                          size="xs"
                          data={['STRING', 'NUMBER', 'DATE', 'BOOLEAN']} 
                          value={meta.type} 
                          onChange={(v) => updatePlaceholder(meta.name, 'type', v || 'STRING')} 
                        />
                      </Table.Td>
                      <Table.Td>
                        <TextInput 
                          size="xs"
                          value={meta.description} 
                          onChange={(e) => updatePlaceholder(meta.name, 'description', e.target.value)} 
                          placeholder="What is this for?"
                        />
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </>
          )}

          <Button onClick={handleSave} mt="md">Save Query</Button>
        </Stack>
      </Modal>
    </div>
  );
}

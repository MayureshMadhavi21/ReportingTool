import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, Stack, Textarea } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import api from '../api';
import type { QueryDef, Connector } from '../types';

export default function Queries() {
  const [queries, setQueries] = useState<QueryDef[]>([]);
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [opened, { open, close }] = useDisclosure(false);
  const [formData, setFormData] = useState<Partial<QueryDef>>({});

  const fetchData = async () => {
    try {
      const [qRes, cRes] = await Promise.all([api.get('/queries'), api.get('/connectors')]);
      setQueries(qRes.data);
      setConnectors(cRes.data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load data', color: 'red' });
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSave = async () => {
    try {
      await api.post('/queries', formData);
      notifications.show({ title: 'Success', message: 'Query created successfully', color: 'green' });
      close();
      setFormData({});
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to save query', color: 'red' });
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure?')) return;
    try {
      await api.delete(`/queries/${id}`);
      notifications.show({ title: 'Success', message: 'Query deleted', color: 'green' });
      fetchData();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to delete query', color: 'red' });
    }
  };

  return (
    <div>
      <Group justify="space-between" mb="md">
        <Text size="xl" fw={700}>Registered Queries</Text>
        <Button onClick={open}>Add Query</Button>
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
                <ActionIcon color="red" onClick={() => handleDelete(q.id)}>
                  <IconTrash size={16} />
                </ActionIcon>
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

      <Modal opened={opened} onClose={close} title="Add Query" size="lg">
        <Stack>
          <Select 
            label="Connector" 
            required 
            data={connectors.map(c => ({ value: c.id.toString(), label: c.name }))} 
            value={formData.connectorId?.toString() || ''} 
            onChange={(v) => setFormData({...formData, connectorId: Number(v)})} 
          />
          <TextInput label="Query Name" required value={formData.name || ''} onChange={(e) => setFormData({...formData, name: e.target.value})} />
          <TextInput label="Description" value={formData.description || ''} onChange={(e) => setFormData({...formData, description: e.target.value})} />
          <Textarea 
            label="SQL Query" 
            required 
            minRows={4}
            value={formData.queryText || ''} 
            onChange={(e) => setFormData({...formData, queryText: e.target.value})} 
          />
          <Button onClick={handleSave} mt="md">Save Query</Button>
        </Stack>
      </Modal>
    </div>
  );
}

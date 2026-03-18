import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, PasswordInput, Stack } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import api from '../api';
import type { Connector } from '../types';

export default function Connectors() {
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [opened, { open, close }] = useDisclosure(false);
  const [formData, setFormData] = useState<Partial<Connector>>({});

  const fetchConnectors = async () => {
    try {
      const { data } = await api.get('/connectors');
      setConnectors(data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load connectors', color: 'red' });
    }
  };

  useEffect(() => {
    fetchConnectors();
  }, []);

  const handleSave = async () => {
    try {
      await api.post('/connectors', formData);
      notifications.show({ title: 'Success', message: 'Connector created successfully', color: 'green' });
      close();
      setFormData({});
      fetchConnectors();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to save connector', color: 'red' });
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure?')) return;
    try {
      await api.delete(`/connectors/${id}`);
      notifications.show({ title: 'Success', message: 'Connector deleted', color: 'green' });
      fetchConnectors();
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to delete connector', color: 'red' });
    }
  };

  return (
    <div>
      <Group justify="space-between" mb="md">
        <Text size="xl" fw={700}>Database Connectors</Text>
        <Button onClick={open}>Add Connector</Button>
      </Group>

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>JDBC URL</Table.Th>
            <Table.Th>Username</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {connectors.map((c) => (
            <Table.Tr key={c.id}>
              <Table.Td>{c.name}</Table.Td>
              <Table.Td>{c.dbType}</Table.Td>
              <Table.Td>{c.jdbcUrl}</Table.Td>
              <Table.Td>{c.username}</Table.Td>
              <Table.Td>
                <ActionIcon color="red" onClick={() => handleDelete(c.id)}>
                  <IconTrash size={16} />
                </ActionIcon>
              </Table.Td>
            </Table.Tr>
          ))}
          {connectors.length === 0 && (
            <Table.Tr>
              <Table.Td colSpan={5} align="center">No connectors found</Table.Td>
            </Table.Tr>
          )}
        </Table.Tbody>
      </Table>

      <Modal opened={opened} onClose={close} title="Add Connector">
        <Stack>
          <TextInput label="Name" required value={formData.name || ''} onChange={(e) => setFormData({...formData, name: e.target.value})} />
          <Select label="DB Type" required data={['SQL_SERVER', 'MYSQL', 'POSTGRESQL', 'ORACLE', 'H2']} value={formData.dbType || ''} onChange={(v) => setFormData({...formData, dbType: v || ''})} />
          <TextInput label="JDBC URL" required value={formData.jdbcUrl || ''} onChange={(e) => setFormData({...formData, jdbcUrl: e.target.value})} />
          <TextInput label="Username" required value={formData.username || ''} onChange={(e) => setFormData({...formData, username: e.target.value})} />
          <PasswordInput label="Password" required value={formData.password || ''} onChange={(e) => setFormData({...formData, password: e.target.value})} />
          <Button onClick={handleSave} mt="md">Save Connector</Button>
        </Stack>
      </Modal>
    </div>
  );
}

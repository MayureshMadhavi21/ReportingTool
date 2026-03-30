import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, PasswordInput, Stack } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconTrash, IconEdit, IconCheck } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { connectorApi } from '../api';
import type { Connector } from '../types';

export default function Connectors() {
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [opened, { open, close }] = useDisclosure(false);
  const [formData, setFormData] = useState<Partial<Connector>>({});
  const [editingId, setEditingId] = useState<string | null>(null);
  const [testing, setTesting] = useState(false);

  const fetchConnectors = async () => {
    try {
      const { data } = await connectorApi.get('/connectors');
      setConnectors(data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load connectors', color: 'red' });
    }
  };

  useEffect(() => {
    fetchConnectors();
  }, []);

  const handleOpenAdd = () => {
    setEditingId(null);
    setFormData({});
    open();
  };

  const handleOpenEdit = (connector: Connector) => {
    setEditingId(connector.id);
    setFormData({ ...connector, password: '' });
    open();
  };

  const handleTestConnection = async () => {
    setTesting(true);
    try {
      await connectorApi.post('/connectors/test', formData);
      notifications.show({ title: 'Success', message: 'Connection test passed!', color: 'green', icon: <IconCheck size={16} /> });
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Connection failed';
      notifications.show({ title: 'Error', message: msg, color: 'red' });
    } finally {
      setTesting(false);
    }
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await connectorApi.put(`/connectors/${editingId}`, formData);
        notifications.show({ title: 'Success', message: 'Connector updated', color: 'green' });
      } else {
        await connectorApi.post('/connectors', formData);
        notifications.show({ title: 'Success', message: 'Connector created', color: 'green' });
      }
      close();
      setFormData({});
      fetchConnectors();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save';
      notifications.show({ title: 'Error', message: msg, color: 'red' });
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      await connectorApi.delete(`/connectors/${id}`);
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
        <Button onClick={handleOpenAdd}>Add Connector</Button>
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
              <Table.Td fw={500}>{c.name}</Table.Td>
              <Table.Td>{c.dbType}</Table.Td>
              <Table.Td>{c.jdbcUrl}</Table.Td>
              <Table.Td>{c.username}</Table.Td>
              <Table.Td>
                <Group gap="xs">
                  <ActionIcon variant="light" color="blue" onClick={() => handleOpenEdit(c)}>
                    <IconEdit size={16} />
                  </ActionIcon>
                  <ActionIcon variant="light" color="red" onClick={() => handleDelete(c.id)}>
                    <IconTrash size={16} />
                  </ActionIcon>
                </Group>
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

      <Modal opened={opened} onClose={close} title={editingId ? "Edit Connector" : "Add Connector"}>
        <Stack>
          <TextInput label="Name" required value={formData.name || ''} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
          <Select label="DB Type" required data={['SQL_SERVER', 'MYSQL', 'POSTGRESQL', 'ORACLE', 'H2']} value={formData.dbType || ''} onChange={(v) => setFormData({ ...formData, dbType: v || '' })} />
          <TextInput label="JDBC URL" required value={formData.jdbcUrl || ''} onChange={(e) => setFormData({ ...formData, jdbcUrl: e.target.value })} />
          <TextInput label="Username" required value={formData.username || ''} onChange={(e) => setFormData({ ...formData, username: e.target.value })} />
          <PasswordInput label={editingId ? "New Password (Optional)" : "Password"} required={!editingId} value={formData.password || ''} onChange={(e) => setFormData({ ...formData, password: e.target.value })} />
          
          <Group grow mt="md">
            <Button variant="outline" onClick={handleTestConnection} loading={testing}>Test Connection</Button>
            <Button onClick={handleSave}>Save Connector</Button>
          </Group>
        </Stack>
      </Modal>
    </div>
  );
}

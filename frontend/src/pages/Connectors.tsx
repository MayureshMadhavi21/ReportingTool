import { useState, useEffect } from 'react';
import { 
  Button, Table, Group, Text, ActionIcon, Stack, Badge, Card, ThemeIcon, Title
} from '@mantine/core';
import { 
  IconTrash, IconEdit, IconPlus, IconServer, IconEye
} from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { useNavigate } from 'react-router-dom';
import { connectorApi } from '../api';
import type { Connector } from '../types';

const DB_TYPES = [
  { value: 'SQL_SERVER', label: 'MS SQL Server', color: 'blue' },
  { value: 'MYSQL', label: 'MySQL', color: 'orange' },
  { value: 'POSTGRESQL', label: 'PostgreSQL', color: 'grape' },
  { value: 'ORACLE', label: 'Oracle', color: 'red' },
  { value: 'H2', label: 'H2 Database', color: 'gray' },
];

export default function Connectors() {
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const navigate = useNavigate();

  const fetchConnectors = async () => {
    try {
      const { data } = await connectorApi.get('/connectors');
      setConnectors(data);
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to load connectors', color: 'red' });
    }
  };

  useEffect(() => { fetchConnectors(); }, []);

  const handleDelete = async (id: string | number) => {
    if (!confirm('Are you sure you want to delete this connector? This cannot be undone.')) return;
    try {
      await connectorApi.delete(`/connectors/${id}`);
      notifications.show({ title: 'Success', message: 'Connector deleted', color: 'green' });
      fetchConnectors();
    } catch (err: any) {
      notifications.show({ title: 'Error', message: err.response?.data?.message || 'Failed to delete connector', color: 'red' });
    }
  };

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <Stack gap={4}>
          <Title order={2}>Database Connectors</Title>
          <Text size="sm" c="dimmed">Manage data source connections for your report queries.</Text>
        </Stack>
        <Button 
            leftSection={<IconPlus size={18} />} 
            onClick={() => navigate('/connectors/add')}
        >
            Add Connector
        </Button>
      </Group>

      <Card withBorder shadow="sm" radius="md" p={0}>
        <Table verticalSpacing="md" horizontalSpacing="lg" highlightOnHover>
          <Table.Thead bg="gray.0">
            <Table.Tr>
              <Table.Th>Name</Table.Th>
              <Table.Th>Type</Table.Th>
              <Table.Th>Details</Table.Th>
              <Table.Th>Username</Table.Th>
              <Table.Th style={{ textAlign: 'center' }}>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {connectors.map((c) => {
              const typeInfo = DB_TYPES.find(t => t.value === c.dbType);
              return (
                <Table.Tr key={c.id}>
                  <Table.Td>
                    <Group gap="sm">
                      <ThemeIcon color={typeInfo?.color || 'gray'} variant="light" size="md">
                        <IconServer size={18} />
                      </ThemeIcon>
                      <Text fw={600} size="sm">{c.name}</Text>
                    </Group>
                  </Table.Td>
                  <Table.Td>
                    <Badge color={typeInfo?.color || 'gray'} variant="filled">
                      {typeInfo?.label || c.dbType}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
                    <Text size="xs" fw={500} c="dark" style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {c.jdbcUrl}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">{c.username === 'WINDOWS_AUTH' ? <Badge variant="dot" color="blue">Windows Auth</Badge> : c.username}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="center">
                      <ActionIcon variant="light" color="gray" onClick={() => navigate(`/connectors/${c.id}/view`)} title="View Details">
                        <IconEye size={18} />
                      </ActionIcon>
                      <ActionIcon variant="light" color="blue" onClick={() => navigate(`/connectors/${c.id}/edit`)} title="Edit Connector">
                        <IconEdit size={18} />
                      </ActionIcon>
                      <ActionIcon variant="light" color="red" onClick={() => handleDelete(c.id)} title="Delete Connector">
                        <IconTrash size={18} />
                      </ActionIcon>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              );
            })}
            {connectors.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={5}>
                  <Text ta="center" py="xl" c="dimmed">No database connectors configured yet.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      </Card>
    </Stack>
  );
}

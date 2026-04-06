import { useState, useEffect } from 'react';
import { Button, Table, Group, Text, ActionIcon, Stack, Paper, Title } from '@mantine/core';
import { IconTrash, IconEdit, IconPlus, IconDatabase, IconEye } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { useNavigate } from 'react-router-dom';
import { connectorApi } from '../api';
import type { QueryDef, Connector } from '../types';

export default function Queries() {
  const [queries, setQueries] = useState<QueryDef[]>([]);
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const navigate = useNavigate();

  const fetchData = async () => {
    try {
      const [qRes, cRes] = await Promise.all([
        connectorApi.get('/queries'), 
        connectorApi.get('/connectors')
      ]);
      setQueries(qRes.data);
      setConnectors(cRes.data);
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to load data', color: 'red' });
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this query? This action cannot be undone.')) return;
    try {
      await connectorApi.delete(`/queries/${id}`);
      notifications.show({ title: 'Success', message: 'Query deleted successfully', color: 'green' });
      fetchData();
    } catch (err: any) {
      notifications.show({ 
        title: 'Error', 
        message: err.response?.data?.message || 'Failed to delete query', 
        color: 'red' 
      });
    }
  };

  const getConnectorName = (id: string | number) => {
    return connectors.find(c => c.id.toString() === id.toString())?.name || id;
  };

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <Stack gap={4}>
          <Title order={2}>Query Management</Title>
          <Text size="sm" color="dimmed">Register and manage SQL queries for your database connectors.</Text>
        </Stack>
        <Button 
          onClick={() => navigate('/queries/add')} 
          leftSection={<IconPlus size={18}/>}
        >
          Add Query
        </Button>
      </Group>

      <Paper withBorder p="md" shadow="xs">
        <Table striped highlightOnHover verticalSpacing="md">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Query Name</Table.Th>
              <Table.Th><Group gap={4}><IconDatabase size={14}/> Connector</Group></Table.Th>
              <Table.Th>Description</Table.Th>
              <Table.Th style={{ width: 120 }}>Actions</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {queries.map((q) => (
              <Table.Tr key={q.id}>
                <Table.Td>
                  <Text fw={500} color="blue">{q.name}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{getConnectorName(q.connectorId)}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm" color="dimmed" lineClamp={1}>{q.description || 'No description'}</Text>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end" wrap="nowrap">
                    <ActionIcon 
                      variant="light" 
                      color="gray" 
                      onClick={() => navigate(`/queries/${q.id}/view`)} 
                      title="View Query"
                    >
                      <IconEye size={16} />
                    </ActionIcon>
                    <ActionIcon 
                      variant="light" 
                      color="blue" 
                      onClick={() => navigate(`/queries/${q.id}/edit`)} 
                      title="Edit Query"
                    >
                      <IconEdit size={16} />
                    </ActionIcon>
                    <ActionIcon 
                      variant="light" 
                      color="red" 
                      onClick={() => handleDelete(q.id)} 
                      title="Delete Query"
                    >
                      <IconTrash size={16} />
                    </ActionIcon>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
            {queries.length === 0 && (
              <Table.Tr>
                <Table.Td colSpan={4} align="center">
                  <Text py="xl" color="dimmed">No queries found. Click "Add Query" to get started.</Text>
                </Table.Td>
              </Table.Tr>
            )}
          </Table.Tbody>
        </Table>
      </Paper>
    </Stack>
  );
}

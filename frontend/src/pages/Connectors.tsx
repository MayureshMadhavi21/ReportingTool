import { useState, useEffect, useMemo } from 'react';
import { 
  Button, Table, Group, Text, ActionIcon, Modal, TextInput, Select, 
  PasswordInput, Stack, Badge, Switch, NumberInput, Divider, Card, ThemeIcon, Alert
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { 
  IconTrash, IconEdit, IconCheck, IconServer, 
  IconDatabase, IconShieldLock
} from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { connectorApi } from '../api';
import type { Connector } from '../types';

const DB_TYPES = [
  { value: 'SQL_SERVER', label: 'MS SQL Server', color: 'blue', port: 1433 },
  { value: 'MYSQL', label: 'MySQL', color: 'orange', port: 3306 },
  { value: 'POSTGRESQL', label: 'PostgreSQL', color: 'grape', port: 5432 },
  { value: 'ORACLE', label: 'Oracle', color: 'red', port: 1521 },
  { value: 'H2', label: 'H2 Database', color: 'gray', port: null },
];

export default function Connectors() {
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [opened, { open, close }] = useDisclosure(false);
  const [formData, setFormData] = useState<Partial<Connector>>({});
  const [editingId, setEditingId] = useState<string | null>(null);
  const [testing, setTesting] = useState(false);

  // Structured fields for URL Builder
  const [host, setHost] = useState('');
  const [port, setPort] = useState<number | string>(1433);
  const [dbName, setDbName] = useState('');
  const [useRawUrl, setUseRawUrl] = useState(false);
  const [oracleMode, setOracleMode] = useState<'SID' | 'SERVICE'>('SID');
  const [useWindowsAuth, setUseWindowsAuth] = useState(false);

  const fetchConnectors = async () => {
    try {
      const { data } = await connectorApi.get('/connectors');
      setConnectors(data);
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to load connectors', color: 'red' });
    }
  };

  useEffect(() => { fetchConnectors(); }, []);

  // US-1: Generate URL based on fields
  const generatedUrl = useMemo(() => {
    if (useRawUrl) return formData.jdbcUrl || '';
    
    const h = host || 'localhost';
    const p = port || '';
    const d = dbName || 'master';

    switch (formData.dbType) {
      case 'SQL_SERVER':
        let base = `jdbc:sqlserver://${h}${p ? ':' + p : ''};databaseName=${d};encrypt=true;trustServerCertificate=true`;
        if (useWindowsAuth) base += ';integratedSecurity=true';
        return base;
      case 'MYSQL':
        return `jdbc:mysql://${h}${p ? ':' + p : ''}/${d}?useSSL=false&serverTimezone=UTC`;
      case 'POSTGRESQL':
        return `jdbc:postgresql://${h}${p ? ':' + p : ''}/${d}`;
      case 'ORACLE':
        return oracleMode === 'SID' 
          ? `jdbc:oracle:thin:@${h}:${p}:${d}`
          : `jdbc:oracle:thin:@//${h}:${p}/${d}`;
      case 'H2':
        return `jdbc:h2:file:./data/${d}`;
      default:
        return '';
    }
  }, [formData.dbType, host, port, dbName, useRawUrl, oracleMode, useWindowsAuth, formData.jdbcUrl]);

  // Sync generated URL to formData
  useEffect(() => {
    if (!useRawUrl) {
      setFormData(prev => ({ ...prev, jdbcUrl: generatedUrl }));
    }
  }, [generatedUrl, useRawUrl]);

  const handleOpenAdd = () => {
    setEditingId(null);
    setFormData({ dbType: 'SQL_SERVER' });
    setHost('localhost');
    setPort(1433);
    setDbName('');
    setUseRawUrl(false);
    setUseWindowsAuth(false);
    setOracleMode('SID');
    open();
  };

  const handleOpenEdit = (connector: Connector) => {
    setEditingId(connector.id);
    setFormData({ ...connector, password: '' });
    
    // US-7: Populate structured state from record
    setHost(connector.host || '');
    setPort(connector.port || '');
    setDbName(connector.dbName || '');
    setUseRawUrl(connector.useRawUrl || false);
    
    // Detect mode from URL parts if possible, but the DB record is source of truth
    if (connector.dbType === 'ORACLE') {
      setOracleMode(connector.jdbcUrl.includes(':thin:@//') ? 'SERVICE' : 'SID');
    }
    setUseWindowsAuth(connector.jdbcUrl.includes('integratedSecurity=true'));
    
    open();
  };

  const handleDbTypeChange = (type: string | null) => {
    const info = DB_TYPES.find(t => t.value === type);
    setFormData({ ...formData, dbType: type || '' });
    if (info?.port) setPort(info.port);
    if (type === 'ORACLE') setOracleMode('SID');
    setUseWindowsAuth(false);
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
      const payload = { 
        ...formData,
        host,
        port,
        dbName,
        useRawUrl
      };
      if (useWindowsAuth && payload.dbType === 'SQL_SERVER') {
        payload.username = 'WINDOWS_AUTH';
        payload.password = 'WINDOWS_AUTH';
      }

      if (editingId) {
        await connectorApi.put(`/connectors/${editingId}`, payload);
        notifications.show({ title: 'Success', message: 'Connector updated', color: 'green' });
      } else {
        await connectorApi.post('/connectors', payload);
        notifications.show({ title: 'Success', message: 'Connector created', color: 'green' });
      }
      close(); setFormData({}); fetchConnectors();
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
    } catch {
      notifications.show({ title: 'Error', message: 'Failed to delete connector', color: 'red' });
    }
  };

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <div>
          <Text size="xl" fw={800}>Database Connectors</Text>
          <Text size="sm" c="dimmed">Manage data source connections for your report queries.</Text>
        </div>
        <Button leftSection={<IconDatabase size={18} />} onClick={handleOpenAdd}>Add Connector</Button>
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
                    <Badge color={typeInfo?.color || 'gray'} variant="light">
                      {typeInfo?.label || c.dbType}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
                    <Text size="xs" c="dimmed" style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {c.jdbcUrl}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">{c.username === 'WINDOWS_AUTH' ? <Badge variant="dot" color="blue">Windows Auth</Badge> : c.username}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="center">
                      <ActionIcon variant="subtle" color="blue" onClick={() => handleOpenEdit(c)}>
                        <IconEdit size={18} />
                      </ActionIcon>
                      <ActionIcon variant="subtle" color="red" onClick={() => handleDelete(c.id)}>
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

      <Modal opened={opened} onClose={close} title={editingId ? "Edit Connector" : "Add New Connector"} size="lg">
        <Stack gap="md">
          <Group grow>
            <TextInput label="Connector Name" placeholder="My Database" required value={formData.name || ''} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
            <Select 
              label="Database Type" 
              required 
              data={DB_TYPES} 
              value={formData.dbType || ''} 
              onChange={handleDbTypeChange} 
            />
          </Group>

          <Card withBorder p="md" bg="gray.0" radius="md">
            <Stack gap="xs">
              <Group justify="space-between">
                <Text size="xs" fw={700} c="dimmed" tt="uppercase">JDBC Connection Builder</Text>
                <Switch 
                  size="xs" 
                  label="Custom URL" 
                  checked={useRawUrl} 
                  onChange={(e) => setUseRawUrl(e.currentTarget.checked)} 
                />
              </Group>
              
              {!useRawUrl ? (
                <Stack gap="sm">
                  <Group grow>
                    <TextInput label="Host" placeholder="localhost" size="xs" value={host} onChange={(e) => setHost(e.target.value)} />
                    <NumberInput label="Port" placeholder="Port" size="xs" hideControls value={port} onChange={setPort} />
                  </Group>
                  <TextInput label={formData.dbType === 'H2' ? "File Path" : "Database Name"} placeholder="db_name" size="xs" value={dbName} onChange={(e) => setDbName(e.target.value)} />
                  
                  {formData.dbType === 'ORACLE' && (
                    <Select label="Oracle Mode" size="xs" data={['SID', 'SERVICE']} value={oracleMode} onChange={(v: any) => setOracleMode(v)} />
                  )}
                  {formData.dbType === 'SQL_SERVER' && (
                    <Checkbox label="Use Windows Authentication (Integrated Security)" size="xs" checked={useWindowsAuth} onChange={(e: any) => setUseWindowsAuth(e.currentTarget.checked)} />
                  )}
                </Stack>
              ) : (
                <TextInput label="JDBC Raw URL" placeholder="jdbc:..." size="xs" required value={formData.jdbcUrl || ''} onChange={(e) => setFormData({ ...formData, jdbcUrl: e.target.value })} />
              )}
              
              <Divider mt="xs" label="Preview" labelPosition="center" />
              <Card p="xs" withBorder>
                <Text size="xs" style={{ wordBreak: 'break-all' }} c="blue" fw={500}>{formData.jdbcUrl || '(Enter fields above)'}</Text>
              </Card>
            </Stack>
          </Card>

          {!useWindowsAuth && (
            <Group grow>
              <TextInput label="Username" placeholder="db_user" required value={formData.username || ''} onChange={(e) => setFormData({ ...formData, username: e.target.value })} />
              <PasswordInput label={editingId ? "New Password (Optional)" : "Password"} required={!editingId} value={formData.password || ''} onChange={(e) => setFormData({ ...formData, password: e.target.value })} />
            </Group>
          )}

          {useWindowsAuth && (
            <Alert icon={<IconShieldLock size={16} />} color="blue" variant="light">
              Integrated Security uses the credentials of the Windows account running the service. Password storage is disabled.
            </Alert>
          )}

          <Group justify="flex-end" mt="lg">
            <Button variant="default" onClick={close}>Cancel</Button>
            <Button variant="outline" onClick={handleTestConnection} loading={testing}>Test Connection</Button>
            <Button onClick={handleSave}>Save Connector</Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}

// Helper to avoid build error
function Checkbox(props: any) {
  return (
    <Group gap="xs">
      <Switch size="xs" {...props} />
    </Group>
  );
}

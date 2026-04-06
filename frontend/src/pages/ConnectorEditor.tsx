import { useState, useEffect, useMemo } from 'react';
import { 
  Button, Group, Text, TextInput, Select, PasswordInput, Stack, 
  Switch, NumberInput, Divider, Card, ThemeIcon, Alert, Paper, Title, 
  Breadcrumbs, Anchor
} from '@mantine/core';
import { 
  IconChevronLeft, IconServer, IconDatabase, 
  IconShieldLock, IconDeviceFloppy, IconTestPipe
} from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { connectorApi } from '../api';
import type { Connector } from '../types';

const DB_TYPES = [
  { value: 'SQL_SERVER', label: 'MS SQL Server', color: 'blue', port: 1433 },
  { value: 'MYSQL', label: 'MySQL', color: 'orange', port: 3306 },
  { value: 'POSTGRESQL', label: 'PostgreSQL', color: 'grape', port: 5432 },
  { value: 'ORACLE', label: 'Oracle', color: 'red', port: 1521 },
  { value: 'H2', label: 'H2 Database', color: 'gray', port: null },
];

export default function ConnectorEditor({ 
  editMode = false, 
  viewOnly = false 
}: { 
  editMode?: boolean; 
  viewOnly?: boolean;
}) {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const inputStyles = {
    input: {
      '&:disabled': {
        color: '#000000 !important',
        opacity: '1 !important',
        backgroundColor: 'var(--mantine-color-gray-0)',
        WebkitTextFillColor: '#000000 !important',
      }
    }
  };

  const [formData, setFormData] = useState<Partial<Connector>>({});
  const [testing, setTesting] = useState(false);

  // Structured fields for URL Builder
  const [host, setHost] = useState('localhost');
  const [port, setPort] = useState<number | string>(1433);
  const [dbName, setDbName] = useState('');
  const [useRawUrl, setUseRawUrl] = useState(false);
  const [oracleMode, setOracleMode] = useState<'SID' | 'SERVICE'>('SID');
  const [useWindowsAuth, setUseWindowsAuth] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      if ((editMode || viewOnly) && id) {
        try {
          const { data } = await connectorApi.get(`/connectors/${id}`);
          setFormData({ ...data, password: '' });
          
          setHost(data.host || '');
          setPort(data.port || '');
          setDbName(data.dbName || '');
          setUseRawUrl(data.useRawUrl || false);
          
          if (data.dbType === 'ORACLE') {
            setOracleMode(data.jdbcUrl.includes(':thin:@//') ? 'SERVICE' : 'SID');
          }
          setUseWindowsAuth(data.jdbcUrl.includes('integratedSecurity=true'));
        } catch {
          notifications.show({ title: 'Error', message: 'Failed to load connector details', color: 'red' });
          navigate('/connectors');
        } finally {
          // Data loaded
        }
      } else {
          setFormData({ dbType: 'SQL_SERVER' });
      }
    };
    fetchData();
  }, [id, editMode, viewOnly, navigate]);

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

  useEffect(() => {
    if (!useRawUrl) {
      setFormData(prev => ({ ...prev, jdbcUrl: generatedUrl }));
    }
  }, [generatedUrl, useRawUrl]);

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
      notifications.show({ title: 'Success', message: 'Connection test passed!', color: 'green' });
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

      if (editMode && id) {
        await connectorApi.put(`/connectors/${id}`, payload);
        notifications.show({ title: 'Success', message: 'Connector updated', color: 'green' });
      } else {
        await connectorApi.post('/connectors', payload);
        notifications.show({ title: 'Success', message: 'Connector created', color: 'green' });
      }
      navigate('/connectors');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save';
      notifications.show({ title: 'Error', message: msg, color: 'red' });
    }
  };

  const typeInfo = DB_TYPES.find(t => t.value === formData.dbType);

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <Stack gap={4}>
          <Breadcrumbs>
            <Anchor component={Link} to="/connectors">Connectors</Anchor>
            <Text size="sm" color="dimmed">{viewOnly ? 'View' : editMode ? 'Edit' : 'Add New'}</Text>
          </Breadcrumbs>
          <Title order={2}>
            {viewOnly ? 'Connector Details' : editMode ? 'Edit Connector' : 'Add New Connector'}
          </Title>
        </Stack>
        <Button variant="default" leftSection={<IconChevronLeft size={16}/>} onClick={() => navigate('/connectors')}>
          Back to List
        </Button>
      </Group>

      <Paper withBorder p="xl" shadow="sm" radius="md">
        <Stack gap="xl">
          <Group grow align="flex-start">
            <TextInput 
                label="Connector Name" 
                placeholder="My Database" 
                required 
                value={formData.name || ''} 
                onChange={(e) => setFormData({ ...formData, name: e.target.value })} 
                disabled={viewOnly}
                styles={inputStyles}
            />
            <Select 
              label="Database Type" 
              required 
              data={DB_TYPES} 
              value={formData.dbType || ''} 
              onChange={handleDbTypeChange} 
              disabled={viewOnly || editMode} 
              styles={inputStyles}
              leftSection={typeInfo?.color && (
                <ThemeIcon color={typeInfo.color} size="xs" variant="filled" radius="xl">
                    <IconServer size={10} />
                </ThemeIcon>
              )}
            />
          </Group>

          <Card withBorder p="md" bg="gray.0" radius="md">
            <Stack gap="xs">
              <Group justify="space-between">
                <Group gap="xs">
                    <IconDatabase size={16} />
                    <Text size="xs" fw={700} c="dark" tt="uppercase">JDBC Connection Builder</Text>
                </Group>
                <Switch 
                  size="xs" 
                  label="Custom URL" 
                  checked={useRawUrl} 
                  onChange={(e) => setUseRawUrl(e.currentTarget.checked)} 
                  disabled={viewOnly}
                />
              </Group>
              
              {!useRawUrl ? (
                <Stack gap="sm">
                  <Group grow>
                    <TextInput label="Host" placeholder="localhost" size="sm" value={host} onChange={(e) => setHost(e.target.value)} disabled={viewOnly} styles={inputStyles} />
                    <NumberInput label="Port" placeholder="Port" size="sm" hideControls value={port} onChange={setPort} disabled={viewOnly} styles={inputStyles} />
                  </Group>
                  <TextInput label={formData.dbType === 'H2' ? "File Path" : "Database Name"} placeholder="db_name" size="sm" value={dbName} onChange={(e) => setDbName(e.target.value)} disabled={viewOnly} styles={inputStyles} />
                  
                  {formData.dbType === 'ORACLE' && (
                    <Select label="Oracle Mode" size="sm" data={['SID', 'SERVICE']} value={oracleMode} onChange={(v: any) => setOracleMode(v)} disabled={viewOnly} />
                  )}
                  {formData.dbType === 'SQL_SERVER' && (
                    <Group mt="xs">
                        <Switch label="Use Windows Authentication (Integrated Security)" size="xs" checked={useWindowsAuth} onChange={(e: any) => setUseWindowsAuth(e.currentTarget.checked)} disabled={viewOnly} />
                    </Group>
                  )}
                </Stack>
              ) : (
                <TextInput label="JDBC Raw URL" placeholder="jdbc:..." size="sm" required value={formData.jdbcUrl || ''} onChange={(e) => setFormData({ ...formData, jdbcUrl: e.target.value })} disabled={viewOnly} styles={inputStyles} />
              )}
              
              <Divider mt="xs" label="Connection URL Preview" labelPosition="center" />
              <Card p="sm" withBorder bg="white">
                <Text size="xs" style={{ wordBreak: 'break-all' }} c="blue" fw={600}>{formData.jdbcUrl || '(Enter fields above)'}</Text>
              </Card>
            </Stack>
          </Card>

          {!useWindowsAuth && (
            <Group grow align="flex-end">
              <TextInput 
                label="Username" 
                placeholder="db_user" 
                required 
                value={formData.username || ''} 
                onChange={(e) => setFormData({ ...formData, username: e.target.value })} 
                disabled={viewOnly} 
                styles={inputStyles}
              />
              {!viewOnly && (
                <PasswordInput 
                    label={editMode ? "New Password (Optional)" : "Password"} 
                    placeholder="••••••••"
                    required={!editMode} 
                    value={formData.password || ''} 
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })} 
                />
              )}
            </Group>
          )}

          {useWindowsAuth && (
            <Alert icon={<IconShieldLock size={16} />} color="blue" variant="light">
              Integrated Security uses the credentials of the Windows account running the service. Password storage is disabled.
            </Alert>
          )}

          <Divider />

          <Group justify="flex-end">
            <Button variant="subtle" color="gray" onClick={() => navigate('/connectors')}>
              {viewOnly ? 'Close' : 'Cancel'}
            </Button>
            {!viewOnly && (
              <>
                <Button 
                    variant="outline" 
                    color="blue" 
                    onClick={handleTestConnection} 
                    loading={testing}
                    leftSection={<IconTestPipe size={18} />}
                >
                    Test Connection
                </Button>
                <Button 
                    onClick={handleSave} 
                    color="green"
                    leftSection={<IconDeviceFloppy size={18}/>}
                >
                    {editMode ? 'Update Connector' : 'Save Connector'}
                </Button>
              </>
            )}
          </Group>
        </Stack>
      </Paper>
    </Stack>
  );
}

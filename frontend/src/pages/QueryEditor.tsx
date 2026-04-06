import { useState, useEffect, useRef } from 'react';
import { Button, Group, Text, TextInput, Select, Stack, Divider, Table, Paper, Title, Breadcrumbs, Anchor } from '@mantine/core';
import { IconChevronLeft, IconCheck, IconAlertCircle, IconSettings, IconDeviceFloppy } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { connectorApi } from '../api';
import type { QueryDef, Connector, PlaceholderMetadata } from '../types';
import Prism from 'prismjs';
import 'prismjs/components/prism-sql';
import 'prismjs/themes/prism.css'; // We will override some of this

// Custom Code Editor Component
const CodeEditor = ({ 
  value, 
  onChange, 
  disabled = false, 
  placeholder = "Enter SQL query..." 
}: { 
  value: string; 
  onChange: (val: string) => void; 
  disabled?: boolean;
  placeholder?: string;
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const preRef = useRef<HTMLPreElement>(null);

  // Sync scrolling
  const handleScroll = (e: React.UIEvent<HTMLTextAreaElement>) => {
    if (preRef.current) {
      preRef.current.scrollTop = e.currentTarget.scrollTop;
      preRef.current.scrollLeft = e.currentTarget.scrollLeft;
    }
  };

  const highlighted = Prism.highlight(value || '', Prism.languages.sql, 'sql')
    .replace(/:(\w+)/g, '<span class="token placeholder">:$1</span>');

  const lineCount = (value || '').split('\n').length;
  const lineNumbers = Array.from({ length: Math.max(lineCount, 1) }, (_, i) => i + 1).join('\n');

  return (
    <div className="custom-code-editor-container" style={{ 
      display: 'flex', 
      border: '1px solid var(--mantine-color-gray-3)', 
      borderRadius: 'var(--mantine-radius-sm)',
      backgroundColor: disabled ? 'var(--mantine-color-gray-0)' : 'white',
      fontFamily: 'var(--mantine-font-family-monospace)',
      fontSize: '14px',
      minHeight: '200px',
      position: 'relative',
      overflow: 'hidden',
      opacity: disabled ? 0.95 : 1,
      pointerEvents: disabled ? 'none' : 'auto'
    }}>
      {/* Line Numbers */}
      <div className="line-numbers" style={{
        padding: '10px 8px',
        backgroundColor: 'var(--mantine-color-gray-1)',
        color: 'var(--mantine-color-gray-5)',
        textAlign: 'right',
        userSelect: 'none',
        whiteSpace: 'pre',
        borderRight: '1px solid var(--mantine-color-gray-3)',
        minWidth: '35px'
      }}>
        {lineNumbers}
      </div>

      {/* Editor Content Area */}
      <div style={{ position: 'relative', flex: 1, overflow: 'hidden' }}>
        <pre
          ref={preRef}
          aria-hidden="true"
          style={{
            margin: 0,
            padding: '10px',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
            overflow: 'hidden',
            pointerEvents: 'none',
            color: 'inherit',
            fontFamily: 'inherit',
            fontSize: 'inherit'
          }}
          dangerouslySetInnerHTML={{ __html: highlighted + (value?.endsWith('\n') ? ' ' : '') }}
        />
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onScroll={handleScroll}
          placeholder={placeholder}
          spellCheck={false}
          style={{
            margin: 0,
            padding: '10px',
            width: '100%',
            height: '100%',
            minHeight: '200px',
            background: 'transparent',
            outline: 'none',
            border: 'none',
            color: 'transparent',
            caretColor: 'var(--mantine-color-blue-6)',
            resize: 'none',
            fontFamily: 'inherit',
            fontSize: 'inherit',
            lineHeight: 'inherit',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
            display: 'block',
            position: 'relative',
            zIndex: 1
          }}
        />
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .token.placeholder { color: #e64980; font-weight: bold; } /* Pink/Red for placeholders */
        .token.keyword { color: #1c7ed6; font-weight: bold; }    /* Blue for keywords */
        .token.string { color: #40c057; }                      /* Green for strings */
        .token.number { color: #f08c00; }                      /* Orange for numbers */
        .token.function { color: #7048e8; }                    /* Violet for functions */
        .token.comment { color: #adb5bd; font-style: italic; }
      `}} />
    </div>
  );
};

export default function QueryEditor({ 
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

  const [formData, setFormData] = useState<Partial<QueryDef>>({
    placeholderMetadata: []
  });
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [validating, setValidating] = useState(false);
  const [isValidated, setIsValidated] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const cRes = await connectorApi.get('/connectors');
        setConnectors(cRes.data);

        if ((editMode || viewOnly) && id) {
          const qRes = await connectorApi.get(`/queries/${id}`);
          setFormData(qRes.data);
          setIsValidated(true); // Existing/Viewed queries start as validated
        }
      } catch (err) {
        notifications.show({ title: 'Error', message: 'Failed to load data', color: 'red' });
      }
    };
    fetchData();
  }, [id, editMode, viewOnly]);

  // auto-detect placeholders
  useEffect(() => {
    if (!formData.queryText) {
      if (formData.placeholderMetadata && formData.placeholderMetadata.length > 0) {
        setFormData(prev => ({ ...prev, placeholderMetadata: [] }));
      }
      setIsValidated(false);
      return;
    }

    // Reset validation if SQL changes
    setIsValidated(false);

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

  const handleValidate = async () => {
    if (!formData.connectorId || !formData.queryText) {
       notifications.show({ title: 'Input Required', message: 'Please select a connector and enter SQL.', color: 'yellow' });
       return;
    }
    setValidating(true);
    try {
      await connectorApi.post('/queries/validate', formData);
      notifications.show({ title: 'Success', message: 'Query syntax is valid', color: 'green', icon: <IconCheck size={16}/> });
      setIsValidated(true);
    } catch (err: any) {
      notifications.show({ 
        title: 'Validation Failed', 
        message: err.response?.data?.message || 'Invalid syntax. Check your SQL and connector type.', 
        color: 'red',
        icon: <IconAlertCircle size={16}/>
      });
      setIsValidated(false);
    } finally {
      setValidating(false);
    }
  };

  const handleSave = async () => {
    if (!isValidated) {
      notifications.show({ title: 'Validation Required', message: 'Please validate the query before saving.', color: 'yellow' });
      return;
    }

    try {
      if (editMode && id) {
        await connectorApi.put(`/queries/${id}`, formData);
        notifications.show({ title: 'Success', message: 'Query updated successfully', color: 'green' });
      } else {
        await connectorApi.post('/queries', formData);
        notifications.show({ title: 'Success', message: 'Query created successfully', color: 'green' });
      }
      navigate('/queries');
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Failed to save query', color: 'red' });
    }
  };

  const updatePlaceholder = (name: string, field: keyof PlaceholderMetadata, value: string) => {
    const newMetadata = (formData.placeholderMetadata || []).map(m => 
      m.name === name ? { ...m, [field]: value } : m
    );
    setFormData({ ...formData, placeholderMetadata: newMetadata });
  };

  const isSaveDisabled = !formData.connectorId || !formData.name || !formData.queryText || !isValidated;

  return (
    <Stack gap="lg">
      <Group justify="space-between">
        <Stack gap={4}>
          <Breadcrumbs>
            <Anchor component={Link} to="/queries">Queries</Anchor>
            <Text size="sm" color="dimmed">{viewOnly ? 'View' : editMode ? 'Edit' : 'Add New'}</Text>
          </Breadcrumbs>
          <Title order={2}>{viewOnly ? 'Query Details' : editMode ? 'Edit Query' : 'Add New Query'}</Title>
        </Stack>
        <Button variant="default" leftSection={<IconChevronLeft size={16}/>} onClick={() => navigate('/queries')}>
          Back to List
        </Button>
      </Group>

      <Paper withBorder p="xl" shadow="sm">
        <Stack gap="xl">
          <Group grow align="flex-start">
            <Select 
              label="1. Database Connector" 
              placeholder="Select a connector"
              required 
              data={connectors.map(c => ({ value: c.id.toString(), label: `${c.name} (${c.dbType})` }))} 
              value={formData.connectorId?.toString() || ''} 
              onChange={(v) => {
                setFormData({...formData, connectorId: v || ''});
                setIsValidated(false);
              }} 
              disabled={viewOnly}
              styles={inputStyles}
            />
            <TextInput 
              label="2. Query Name" 
              placeholder="e.g., GetEmployeeSales"
              required 
              value={formData.name || ''} 
              onChange={(e) => setFormData({...formData, name: e.target.value})} 
              disabled={viewOnly}
              styles={inputStyles}
            />
          </Group>

          <TextInput 
            label="Description" 
            placeholder="What does this query do?"
            value={formData.description || ''} 
            onChange={(e) => setFormData({...formData, description: e.target.value})} 
            disabled={viewOnly}
            styles={inputStyles}
          />

          <Stack gap={4}>
            <Group justify="space-between">
              <Text size="sm" fw={500}>3. SQL Query <Text span color="red">*</Text></Text>
              {!formData.connectorId && !viewOnly && (
                <Text size="xs" color="orange" fw={500}>Please select a connector first</Text>
              )}
            </Group>
            <CodeEditor 
              value={formData.queryText || ''} 
              onChange={(val) => setFormData({...formData, queryText: val})}
              disabled={viewOnly || !formData.connectorId}
              placeholder="e.g., SELECT * FROM users WHERE status = :status"
            />
            {isValidated && !viewOnly && (
              <Text size="xs" color="green" fw={500}>✓ SQL Syntax validated</Text>
            )}
          </Stack>

          {formData.placeholderMetadata && formData.placeholderMetadata.length > 0 && (
            <Stack gap="xs">
              <Divider my="sm" label={<Group gap={4}><IconSettings size={14}/> <Text size="xs" fw={700}>PLACEHOLDER CONFIGURATION</Text></Group>} labelPosition="center" />
              <Table withTableBorder withColumnBorders striped>
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
                      <Table.Td><Text size="sm" fw={600} color="blue">{meta.name}</Text></Table.Td>
                      <Table.Td>
                        <Select 
                          size="xs"
                          data={['STRING', 'NUMBER', 'DATE', 'BOOLEAN']} 
                          value={meta.type} 
                          onChange={(v) => updatePlaceholder(meta.name, 'type', v || 'STRING')} 
                          disabled={viewOnly}
                          styles={inputStyles}
                        />
                      </Table.Td>
                      <Table.Td>
                        <TextInput 
                          size="xs"
                          value={meta.description} 
                          onChange={(e) => updatePlaceholder(meta.name, 'description', e.target.value)} 
                          placeholder="Usage instructions..."
                          disabled={viewOnly}
                          styles={inputStyles}
                        />
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Stack>
          )}

          <Divider />

          <Group justify="flex-end">
            <Button variant="subtle" color="gray" onClick={() => navigate('/queries')}>
              {viewOnly ? 'Close' : 'Cancel'}
            </Button>
            {!viewOnly && (
              <>
                <Button 
                  variant="light" 
                  color="blue" 
                  onClick={handleValidate} 
                  loading={validating}
                  disabled={!formData.connectorId || !formData.queryText}
                >
                  Validate SQL Syntax
                </Button>
                <Button 
                  onClick={handleSave} 
                  disabled={isSaveDisabled}
                  leftSection={<IconDeviceFloppy size={18}/>}
                  color="green"
                >
                  {editMode ? 'Update Query' : 'Save Query'}
                </Button>
              </>
            )}
          </Group>
        </Stack>
      </Paper>
    </Stack>
  );
}

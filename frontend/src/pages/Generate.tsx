import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Button, Select, Stack, Group, Text, Card, ThemeIcon, TextInput, Divider, SimpleGrid, Badge } from '@mantine/core';
import { IconFileExport, IconDatabase } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import { reportApi } from '../api';
import type { Template } from '../types';

export default function Generate() {
  const location = useLocation();
  const [templates, setTemplates] = useState<Template[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<Template | null>(null);
  const [selectedVersionId, setSelectedVersionId] = useState<string>('');
  const [format, setFormat] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [placeholders, setPlaceholders] = useState<string[]>([]);
  const [params, setParams] = useState<Record<string, string>>({});

  // 1. Initial Load
  useEffect(() => {
    reportApi.get('/templates')
      .then(res => {
        setTemplates(res.data);
        // Handle pre-selected template from navigation state
        if (location.state?.templateId) {
          const t = res.data.find((item: Template) => item.id === location.state.templateId);
          if (t) {
            setSelectedTemplate(t);
            // Pre-select version if provided
            const version = location.state.versionNumber 
              ? t.versions.find((v: any) => v.versionNumber === location.state.versionNumber)
              : t.versions.find((v: any) => v.isActive === 1) || t.versions[0];
            
            if (version) {
              setSelectedVersionId(version.id);
            }
          }
        }
      })
      .catch(() => notifications.show({ title: 'Error', message: 'Failed to load templates', color: 'red' }));
  }, []);

  // 2. Handle Template Change
  const handleTemplateChange = (id: string | null) => {
    if (!id) {
      setSelectedTemplate(null);
      setSelectedVersionId('');
      return;
    }
    const t = templates.find(item => item.id === id);
    if (t) {
      setSelectedTemplate(t);
      const active = t.versions.find(v => v.isActive === 1) || t.versions[0];
      setSelectedVersionId(active?.id || '');
    }
  };

  // 3. Load Placeholders for Version
  useEffect(() => {
    if (selectedVersionId) {
      reportApi.get(`/templates/versions/${selectedVersionId}/placeholders`)
        .then(res => {
          setPlaceholders(res.data);
          const initialParams = res.data.reduce((acc: any, p: string) => ({ ...acc, [p]: '' }), {});
          setParams(initialParams);
          
          // Set default format based on version file type
          const version = selectedTemplate?.versions.find(v => v.id === selectedVersionId);
          setFormat(version?.fileType || 'DOCX');
        })
        .catch(() => notifications.show({ title: 'Error', message: 'Failed to load placeholders', color: 'red' }));
    } else {
      setPlaceholders([]);
      setParams({});
    }
  }, [selectedVersionId]);

  const handleParamChange = (p: string, value: string) => {
    setParams(prev => ({ ...prev, [p]: value }));
  };

  const handleGenerate = async () => {
    if (!selectedTemplate || !selectedVersionId) return;
    setLoading(true);

    const version = selectedTemplate.versions.find(v => v.id === selectedVersionId);

    try {
      const response = await reportApi.post('/generate', {
        templateId: selectedTemplate.id,
        versionNumber: version?.versionNumber,
        format,
        parameters: params
      }, {
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const filename = `Generated_${selectedTemplate.name}_v${version?.versionNumber}.${format.toLowerCase()}`;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();

      notifications.show({ title: 'Success', message: 'Report generated successfully', color: 'green' });
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Report generation failed', color: 'red' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '40px auto' }}>
      <Stack gap="xl">
        <Group>
          <ThemeIcon size="xl" radius="md" variant="gradient" gradient={{ from: 'indigo', to: 'blue' }}>
            <IconFileExport size={28} />
          </ThemeIcon>
          <div>
            <Text size="xl" fw={800}>Report Engine</Text>
            <Text size="sm" c="dimmed">Generate production-ready documents from historical snapshots.</Text>
          </div>
        </Group>

        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="xl">
          <Card shadow="sm" radius="md" withBorder p="xl">
            <Stack gap="lg">
              <Text fw={700}>System Configuration</Text>
              
              <Select
                label="Master Template"
                placeholder="Choose inventory record"
                data={templates.map(t => ({ value: t.id, label: t.name }))}
                value={selectedTemplate?.id || ''}
                onChange={handleTemplateChange}
                required
                searchable
              />

              <Select
                label="Snapshot Revision"
                placeholder="Select version"
                disabled={!selectedTemplate}
                data={selectedTemplate?.versions.map(v => ({ 
                  value: v.id, 
                  label: `Version ${v.versionNumber} ${v.isActive === 1 ? '(Live)' : '(Archive)'}` 
                }))}
                value={selectedVersionId}
                onChange={(v) => setSelectedVersionId(v || '')}
              />

              <Select
                label="Output Format"
                required
                data={['DOCX', 'PDF', 'XLSX']}
                value={format}
                onChange={(v) => setFormat(v || 'DOCX')}
              />

              <Button
                mt="lg"
                size="md"
                fullWidth
                loading={loading}
                onClick={handleGenerate}
                disabled={!selectedVersionId || (placeholders.some(p => !params[p]))}
                leftSection={<IconFileExport size={20} />}
              >
                Assemble Report
              </Button>
            </Stack>
          </Card>

          <Card shadow="sm" radius="md" withBorder p="xl" bg="gray.0">
            <Stack gap="md">
              <Group justify="space-between">
                <Text fw={700}>Unified Parameters</Text>
                <Badge variant="dot" color="blue">{placeholders.length} Target Keys</Badge>
              </Group>
              <Text size="xs" c="dimmed">Values provided here will be dynamically injected into all mapped queries for this snapshot.</Text>
              
              <Divider mb="sm" />

              {placeholders.length > 0 ? (
                <Stack gap="sm">
                  {placeholders.map(p => (
                    <TextInput
                      key={p}
                      label={p.charAt(0).toUpperCase() + p.slice(1)}
                      placeholder={`Enter ${p}...`}
                      size="sm"
                      value={params[p] || ''}
                      onChange={(e) => handleParamChange(p, e.currentTarget.value)}
                      required
                    />
                  ))}
                </Stack>
              ) : (
                <Stack align="center" py="xl" gap="xs">
                   <ThemeIcon color="gray" variant="light" size="xl"><IconDatabase size={24}/></ThemeIcon>
                   <Text size="sm" c="dimmed" fs="italic">No parameters required for this snapshot.</Text>
                </Stack>
              )}
            </Stack>
          </Card>
        </SimpleGrid>
      </Stack>
    </div>
  );
}

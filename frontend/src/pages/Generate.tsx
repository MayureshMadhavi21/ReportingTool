import { useState, useEffect } from 'react';
import { Button, Select, Stack, Group, Text, Card, ThemeIcon } from '@mantine/core';
import { IconFileExport } from '@tabler/icons-react';
import { notifications } from '@mantine/notifications';
import api from '../api';
import type { Template } from '../types';

export default function Generate() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('');
  const [format, setFormat] = useState<string>('DOCX');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/templates')
      .then(res => setTemplates(res.data))
      .catch(() => notifications.show({ title: 'Error', message: 'Failed to load templates', color: 'red' }));
  }, []);

  const handleGenerate = async () => {
    if (!selectedTemplateId) return;
    setLoading(true);
    
    try {
      // The backend returns a byte array (file). Axios needs responseType 'blob'
      const response = await api.get(`/generate/${selectedTemplateId}`, {
        params: { format },
        responseType: 'blob'
      });
      
      // Create object URL and trigger download
      const contentDisposition = response.headers['content-disposition'];
      let filename = `Generated_Report.${format.toLowerCase()}`;
      if (contentDisposition) {
        const matches = /filename="?([^"]+)"?/.exec(contentDisposition);
        if (matches != null && matches[1]) {
          filename = matches[1];
        }
      }

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      
      notifications.show({ title: 'Success', message: 'Report generated successfully', color: 'green' });
    } catch (err) {
      notifications.show({ title: 'Error', message: 'Report generation failed', color: 'red' });
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 600, margin: '0 auto', paddingTop: 40 }}>
      <Card shadow="sm" padding="lg" radius="md" withBorder>
        <Group mb="md">
          <ThemeIcon size="xl" radius="md" variant="light" color="blue">
            <IconFileExport size={28} />
          </ThemeIcon>
          <Text size="xl" fw={700}>Generate Report</Text>
        </Group>
        
        <Stack>
          <Select
            label="Select Template"
            placeholder="Choose a template to run"
            required
            data={templates.map(t => ({ value: t.id.toString(), label: t.name }))}
            value={selectedTemplateId}
            onChange={(v) => setSelectedTemplateId(v || '')}
          />
          
          <Select
            label="Output Format"
            required
            data={['DOCX', 'PDF', 'XLSX']}
            value={format}
            onChange={(v) => setFormat(v || 'DOCX')}
          />
          
          <Button 
            mt="xl" 
            size="md" 
            fullWidth 
            loading={loading}
            onClick={handleGenerate}
            disabled={!selectedTemplateId}
            leftSection={<IconFileExport size={20} />}
          >
            Generate & Download
          </Button>
        </Stack>
      </Card>
    </div>
  );
}

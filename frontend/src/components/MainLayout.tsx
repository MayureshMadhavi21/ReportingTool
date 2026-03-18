import { AppShell, Burger, Group, NavLink, Text, ThemeIcon } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconDatabase, IconFileReport, IconFiles, IconTemplate } from '@tabler/icons-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

export default function MainLayout() {
  const [opened, { toggle }] = useDisclosure();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { label: 'Dashboard', icon: IconFileReport, path: '/' },
    { label: 'Connectors', icon: IconDatabase, path: '/connectors' },
    { label: 'Queries', icon: IconFiles, path: '/queries' },
    { label: 'Templates', icon: IconTemplate, path: '/templates' },
    { label: 'Generate Report', icon: IconFileReport, path: '/generate' },
  ];

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{
        width: 250,
        breakpoint: 'sm',
        collapsed: { mobile: !opened },
      }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md">
          <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
          <ThemeIcon size="lg" variant="gradient" gradient={{ from: 'blue', to: 'cyan' }}>
            <IconFileReport size={20} />
          </ThemeIcon>
          <Text fw={700} size="lg">Report Generator</Text>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        {navItems.map((item) => (
          <NavLink
            key={item.label}
            label={item.label}
            leftSection={<item.icon size={16} stroke={1.5} />}
            active={location.pathname === item.path}
            onClick={() => navigate(item.path)}
            variant="filled"
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main bg="gray.0">
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}

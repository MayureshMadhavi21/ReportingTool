import { BrowserRouter, Route, Routes } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import Connectors from './pages/Connectors';
import Queries from './pages/Queries';
import Templates from './pages/Templates';
import Generate from './pages/Generate';

const Dashboard = () => <div style={{ padding: 20 }}>Welcome to Report Generator! Select an option from the sidebar to begin managing data sources or generating reports.</div>;

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="connectors" element={<Connectors />} />
          <Route path="queries" element={<Queries />} />
          <Route path="templates" element={<Templates />} />
          <Route path="generate" element={<Generate />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;

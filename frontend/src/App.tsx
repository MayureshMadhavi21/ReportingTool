import { BrowserRouter, Route, Routes } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import Connectors from './pages/Connectors';
import ConnectorEditor from './pages/ConnectorEditor';
import Queries from './pages/Queries';
import QueryEditor from './pages/QueryEditor';
import Templates from './pages/Templates';
import TemplateDetails from './pages/TemplateDetails';
import Generate from './pages/Generate';

const Dashboard = () => <div style={{ padding: 20 }}>Welcome to Report Generator! Select an option from the sidebar to begin managing data sources or generating reports.</div>;

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="connectors" element={<Connectors />} />
          <Route path="connectors/add" element={<ConnectorEditor />} />
          <Route path="connectors/:id/edit" element={<ConnectorEditor editMode={true} />} />
          <Route path="connectors/:id/view" element={<ConnectorEditor viewOnly={true} />} />
          
          <Route path="queries" element={<Queries />} />
          <Route path="queries/add" element={<QueryEditor />} />
          <Route path="queries/:id/edit" element={<QueryEditor editMode={true} />} />
          <Route path="queries/:id/view" element={<QueryEditor viewOnly={true} />} />
          <Route path="templates" element={<Templates />} />
          
          {/* Main Template Actions */}
          <Route path="templates/:id/view" element={<TemplateDetails />} />
          <Route path="templates/:id/edit" element={<TemplateDetails editMode={true} />} />
          <Route path="templates/:id/versions" element={<TemplateDetails versionsMode={true} />} />
          
          {/* Version Specific Actions */}
          <Route path="templates/:id/versions/:versionId/view" element={<TemplateDetails versionViewMode={true} />} />
          <Route path="templates/:id/versions/:versionId/edit" element={<TemplateDetails versionEditMode={true} />} />
          
          <Route path="generate" element={<Generate />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;

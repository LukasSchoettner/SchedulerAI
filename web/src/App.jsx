import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom';
import FrontPage from './pages/FrontPage.jsx';
import TaskCrudPage from './pages/TaskCrudPage';
import HomePage from './pages/HomePage';
import SchedulePage from './pages/SchedulePage.jsx';
import Layout from './components/Layout.jsx';
import SettingsPage from './pages/SettingsPage.jsx';
import ProfileSettingsPage from './pages/settings/ProfileSettingsPage.jsx';
import SchedulerPreferencesPage from './pages/settings/SchedulerPreferencesPage.jsx';
import ZoneSettingsPage from './pages/settings/ZoneSettingsPage.jsx';
import SavedLocationsSettingsPage from './pages/settings/SavedLocationsSettingsPage.jsx';
import NotificationSettingsPage from './pages/settings/NotificationSettingsPage.jsx';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<FrontPage />} />
                <Route element={<Layout />}>
                    <Route path="/home" element={<HomePage />} />
                    <Route path="/schedule" element={<SchedulePage />} />
                    <Route path="/tasks" element={<TaskCrudPage />} />
                    <Route path="/settings" element={<SettingsPage />} />
                    <Route path="/settings/profile" element={<ProfileSettingsPage />} />
                    <Route path="/settings/scheduler" element={<SchedulerPreferencesPage />} />
                    <Route path="/settings/zones" element={<ZoneSettingsPage />} />
                    <Route path="/settings/locations" element={<SavedLocationsSettingsPage />} />
                    <Route path="/settings/notifications" element={<NotificationSettingsPage />} />
                    <Route path="/customer" element={<Navigate to="/settings/profile" replace />} />
                    <Route path="/locations" element={<Navigate to="/settings/locations" replace />} />
                    <Route path="/onboarding/scheduling" element={<Navigate to="/settings/scheduler" replace />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default App;

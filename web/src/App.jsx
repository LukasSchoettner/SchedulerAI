import { BrowserRouter, Routes, Route } from 'react-router-dom';
import FrontPage from './pages/FrontPage.jsx';
import TaskCrudPage from './pages/TaskCrudPage';
import HomePage from './pages/HomePage';
import SchedulePage from './pages/SchedulePage.jsx';
import CustomerPage from './pages/CustomerPage';
import Layout from './components/Layout.jsx';
import LocationListPage from "./pages/LocationListPage.jsx";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<FrontPage />} />
                <Route element={<Layout />}>
                    <Route path="/home" element={<HomePage />} />
                    <Route path="/schedule" element={<SchedulePage />} />
                    <Route path="/tasks" element={<TaskCrudPage />} />
                    <Route path="/customer" element={<CustomerPage />} />
                    <Route path="/locations" element={<LocationListPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default App;

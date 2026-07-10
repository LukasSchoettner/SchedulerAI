import React from 'react';
import 'leaflet/dist/leaflet.css';
import './lib/leafletIconFix';  // just side-effects
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import { registerServiceWorker } from './pwa/registerServiceWorker';

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);

registerServiceWorker();

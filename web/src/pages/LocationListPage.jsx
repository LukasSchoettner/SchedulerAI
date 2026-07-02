// src/pages/LocationListPage.jsx
import { useEffect, useState } from 'react';
import api from '../lib/api';
import LocationPicker from '../components/LocationPicker';
import styles from './LocationListPage.module.css';

function LocationListPage() {
    const [locations, setLocations] = useState([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    // form / picker state for new location
    const [newLocation, setNewLocation] = useState({
        addressText: '',
        latitude: null,
        longitude: null,
    });
    const token = localStorage.getItem('token');
    const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

    const fetchLocations = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await api.get('/routing/addresses', { headers: authHeaders });
            setLocations(res.data || []);
        } catch (e) {
            console.error('Failed to load locations', e);
            setError('Could not load locations');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLocations();
    }, []);

    const handleCreate = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError(null);
        try {
            if (!newLocation.latitude || !newLocation.longitude) {
                alert('Please choose coordinates (click on the map or enter lat/lon).');
                return;
            }

            const payload = {
                addressLine: newLocation.addressText || 'Unnamed location',
                latitude: newLocation.latitude,
                longitude: newLocation.longitude,
            };

            await api.post('/routing/addresses', payload, { headers: authHeaders });
            setNewLocation({ addressText: '', latitude: null, longitude: null });
            fetchLocations();
        } catch (e) {
            console.error('Failed to save location', e);
            setError('Could not save location');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Delete this location?')) return;
        try {
            await api.delete(`/routing/addresses/${id}`, { headers: authHeaders });
            setLocations((prev) => prev.filter((loc) => loc.id !== id));
        } catch (e) {
            console.error('Failed to delete location', e);
            alert('Could not delete location');
        }
    };

    return (
        <div className={styles.container}>
            <h2>Saved Locations</h2>

            {error && <div className={styles.error}>{error}</div>}

            {/* Create location form */}
            <section className={styles.card}>
                <h3>Add new location</h3>
                <form onSubmit={handleCreate}>
                    <LocationPicker
                        value={newLocation}
                        onChange={(loc) => setNewLocation(loc)}
                    />
                    <button type="submit" disabled={saving}>
                        {saving ? 'Saving…' : 'Save Location'}
                    </button>
                </form>
            </section>

            {/* List of existing locations */}
            <section className={styles.card}>
                <h3>Existing locations</h3>
                {loading && <p>Loading…</p>}
                {!loading && locations.length === 0 && (
                    <p>No locations saved yet.</p>
                )}
                {!loading && locations.length > 0 && (
                    <table className={styles.table}>
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Address</th>
                            <th>Latitude</th>
                            <th>Longitude</th>
                            <th />
                        </tr>
                        </thead>
                        <tbody>
                        {locations.map((loc) => (
                            <tr key={loc.id}>
                                <td>{loc.id}</td>
                                <td>{loc.addressLine}</td>
                                <td>{loc.latitude}</td>
                                <td>{loc.longitude}</td>
                                <td>
                                    <button
                                        type="button"
                                        onClick={() => handleDelete(loc.id)}
                                    >
                                        Delete
                                    </button>
                                    {/* Later: edit button, “use in task”, etc. */}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </section>
        </div>
    );
}

export default LocationListPage;

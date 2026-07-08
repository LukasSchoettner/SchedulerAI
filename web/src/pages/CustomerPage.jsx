import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import styles from './CustomerPage.module.css';

function CustomerPage() {
    const navigate = useNavigate();
    const [mode, setMode] = useState('view');
    const [customer, setCustomer] = useState(null);
    const [formData, setFormData] = useState({
        customername: '',
        email: '',
        password: '',
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchCustomer().finally(() => setLoading(false));
    }, []);

    const fetchCustomer = async () => {
        setError('');
        try {
            const response = await api.get('/customers/me');
            setCustomer(response.data);
            setFormData({
                customername: response.data.customername || '',
                email: response.data.email || '',
                password: '',
            });
        } catch (err) {
            console.error('Error loading customer:', err);
            setError('Failed to load profile.');
        }
    };

    const handleProfileSubmit = async (event) => {
        event.preventDefault();
        try {
            await api.put(
                `/customers/${customer.id}`,
                Object.fromEntries(Object.entries(formData).filter(([, value]) => value !== ''))
            );
            setMode('view');
            await fetchCustomer();
        } catch (err) {
            console.error('Error updating profile:', err);
            setError('Failed to update profile.');
        }
    };

    if (loading) return <p className={styles.status}>Loading profile...</p>;
    if (error) return <p className={styles.error}>{error}</p>;

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <span className={styles.eyebrow}>Account</span>
                <h2>Profile</h2>
                <p>Manage your account details. Scheduler setup lives in its own settings areas.</p>
            </header>

            <section className={styles.panel}>
                <div className={styles.sectionHeader}>
                    <div>
                        <h3>{customer?.customername}</h3>
                        <p>{customer?.email} · {customer?.membershipLevel}</p>
                    </div>
                    <div className={styles.actions}>
                        <button type="button" onClick={() => setMode(mode === 'edit' ? 'view' : 'edit')}>
                            {mode === 'edit' ? 'Cancel' : 'Edit Profile'}
                        </button>
                        <button type="button" onClick={() => navigate('/settings/scheduler')}>
                            Scheduler Preferences
                        </button>
                        <button type="button" onClick={() => navigate('/settings/zones')}>
                            Scheduling Profiles
                        </button>
                    </div>
                </div>

                {mode === 'edit' && (
                    <form onSubmit={handleProfileSubmit} className={styles.profileForm}>
                        <label>
                            Name
                            <input
                                value={formData.customername}
                                onChange={event => setFormData(prev => ({ ...prev, customername: event.target.value }))}
                                required
                            />
                        </label>
                        <label>
                            Email
                            <input
                                type="email"
                                value={formData.email}
                                onChange={event => setFormData(prev => ({ ...prev, email: event.target.value }))}
                                required
                            />
                        </label>
                        <label>
                            New password
                            <input
                                type="password"
                                value={formData.password}
                                onChange={event => setFormData(prev => ({ ...prev, password: event.target.value }))}
                                placeholder="Optional"
                            />
                        </label>
                        <button type="submit">Save Profile</button>
                    </form>
                )}
            </section>
        </div>
    );
}

export default CustomerPage;

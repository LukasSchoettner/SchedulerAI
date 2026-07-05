// src/pages/HomePage.jsx
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../lib/api';
import styles from './HomePage.module.css';

export default function HomePage() {
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [taskCount, setTaskCount] = useState(null);
    const [locationCount, setLocationCount] = useState(null);

    useEffect(() => {
        api.get('/customers/me')
            .then(res => setProfile(res.data))
            .catch(() => setProfile({ customername: 'User' }));

        api.get('/tasks')
            .then(res => setTaskCount(res.data.length))
            .catch(() => setTaskCount('-'));

        api.get('/routing/addresses')
            .then(res => setLocationCount(res.data.length))
            .catch(() => setLocationCount('-'));
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/');
    };

    return (
        <div className={styles.container}>
            <h2>Welcome{profile ? `, ${profile.customername}` : ''}!</h2>
            <p className={styles.subtitle}>
                Plan tasks with fixed commitments, category priorities, scheduling zones, and a daily briefing before you start the day.
            </p>

            <div className={styles.cardList}>
                <div className={styles.card}>
                    <h4>Tasks</h4>
                    <span className={styles.meta}>Create fixed, flexible, recurring, and project tasks</span>
                    <p className={styles.count}>
                        {taskCount === null ? '...' : taskCount}
                    </p>
                    <Link to="/tasks">View</Link>
                </div>

                <div className={styles.card}>
                    <h4>Locations</h4>
                    <span className={styles.meta}>Saved places for travel-aware planning</span>
                    <p className={styles.count}>
                        {locationCount === null ? '...' : locationCount}
                    </p>
                    <Link to="/locations">View</Link>
                </div>

                <div className={styles.card}>
                    <h4>Schedule</h4>
                    <span className={styles.meta}>Week/day calendar with daily briefing</span>
                    <p className={styles.count}>Ready</p>
                    <Link to="/schedule">View</Link>
                </div>

                <div className={styles.card}>
                    <h4>Scheduler Setup</h4>
                    <span className={styles.meta}>Category priority, quiet hours, zones, and pauses</span>
                    <p className={styles.count}>MVP</p>
                    <Link to="/onboarding/scheduling">Configure</Link>
                </div>

                <div className={styles.card}>
                    <h4>Notifications</h4>
                    <span className={styles.meta}>Reminder fields exist; push notification UI is future work</span>
                    <p className={styles.count}>Planned</p>
                    <Link to="/tasks">Set reminders</Link>
                </div>
            </div>

            <button onClick={handleLogout}>Logout</button>
        </div>
    );
}

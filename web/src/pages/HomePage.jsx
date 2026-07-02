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

            <div className={styles.cardList}>
                <div className={styles.card}>
                    <h4>Tasks</h4>
                    <p className={styles.count}>
                        {taskCount === null ? '...' : taskCount}
                    </p>
                    <Link to="/tasks">View</Link>
                </div>

                <div className={styles.card}>
                    <h4>Locations</h4>
                    <p className={styles.count}>
                        {locationCount === null ? '...' : locationCount}
                    </p>
                    <Link to="/locations">View</Link>
                </div>

                <div className={styles.card}>
                    <h4>Schedule</h4>
                    <p className={styles.count}>Ready</p>
                    <Link to="/schedule">View</Link>
                </div>
            </div>

            <button onClick={handleLogout}>Logout</button>
        </div>
    );
}

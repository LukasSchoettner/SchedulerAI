import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import styles from './LoginPage.module.css';

function LoginPage() {
    const [customername, setCustomername] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await api.post('/auth/login', { customername, password });
            if (response.data?.token) {
                localStorage.setItem('token', response.data.token);
            }
            try {
                const status = await api.get('/customers/preferences/onboarding-status');
                navigate(status.data?.completed ? '/home' : '/onboarding/scheduling');
            } catch {
                navigate('/onboarding/scheduling');
            }
        } catch (err) {
            console.error('Login failed:', err);
            alert('Login failed');
        }
    };

    return (
        <div className={styles.container}>
            <h2>Login</h2>
            <form onSubmit={handleLogin}>
                <div>
                    <label>Customername: </label>
                    <input
                        value={customername}
                        onChange={e => setCustomername(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label>Password: </label>
                    <input
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        type="password"
                        required
                    />
                </div>
                <button type="submit">Login</button>
            </form>
        </div>
    );
}

export default LoginPage;

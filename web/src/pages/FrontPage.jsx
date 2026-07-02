import { useState } from 'react';
import api from '../lib/api';
import { useNavigate } from 'react-router-dom';
import styles from './FrontPage.module.css';


function FrontPage() {
    const [mode, setMode] = useState('login');
    // mode can be 'login' or 'register'

    // Login form state
    const [loginCustomername, setLoginCustomername] = useState('');
    const [loginPassword, setLoginPassword] = useState('');

    // Registration form state
    const [regCustomername, setRegCustomername] = useState('');
    const [regEmail, setRegEmail] = useState('');
    const [regPassword, setRegPassword] = useState('');

    const navigate = useNavigate();

    // Handle LOGIN
    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            // If your gateway routes /auth/** to the proper service, use /auth/login
            const response = await api.post(`/auth/login`, {
                customername: loginCustomername,
                password: loginPassword
            });
            // Expect { token: "..." } or similar
            console.log('Login success', response.data);

            if (response.data && response.data.token) {
                // store token in localStorage
                localStorage.setItem('token', response.data.token);
            }

            // On success, go to /home
            navigate('/home');
        } catch (err) {
            console.error('Login failed', err);
            alert('Login failed');
        }
    };

    // Handle REGISTER
    const handleRegister = async (e) => {
        e.preventDefault();
        try {
            // If your gateway routes /customers/** to the user-service,
            // then /customers is correct for creation:
            const response = await api.post(`/auth/register`, {
                customername: regCustomername,
                email: regEmail,
                password: regPassword
            });
            console.log('Register success', response.data);

            // If your back-end returns a token upon registration, store it:
            localStorage.setItem('token', response.data.token);

            // For now, just navigate to /home
            navigate('/home');
        } catch (err) {
            console.error('Register failed', err);
            alert('Register failed');
        }
    };

    return (
        <div className={styles.container}>
            <h2>Welcome to the Scheduler</h2>
            <div>
                <button onClick={() => setMode('login')}>Login</button>
                <button onClick={() => setMode('register')}>Register</button>
            </div>

            {mode === 'login' ? (
                <div>
                    <h3>Login</h3>
                    <form onSubmit={handleLogin}>
                        <div>
                            <label>Customername: </label>
                            <input
                                value={loginCustomername}
                                onChange={(e) => setLoginCustomername(e.target.value)}
                                required
                            />
                        </div>
                        <div>
                            <label>Password: </label>
                            <input
                                type="password"
                                value={loginPassword}
                                onChange={(e) => setLoginPassword(e.target.value)}
                                required
                            />
                        </div>
                        <button type="submit">Login</button>
                    </form>
                </div>
            ) : (
                <div>
                    <h3>Register</h3>
                    <form onSubmit={handleRegister}>
                        <div>
                            <label>Customername: </label>
                            <input
                                value={regCustomername}
                                onChange={(e) => setRegCustomername(e.target.value)}
                                required
                            />
                        </div>
                        <div>
                            <label>Email: </label>
                            <input
                                type="email"
                                value={regEmail}
                                onChange={(e) => setRegEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div>
                            <label>Password: </label>
                            <input
                                type="password"
                                value={regPassword}
                                onChange={(e) => setRegPassword(e.target.value)}
                                required
                            />
                        </div>
                        <button type="submit">Register</button>
                    </form>
                </div>
            )}
        </div>
    );
}

export default FrontPage;

import { Link, Outlet } from 'react-router-dom';
import styles from './Layout.module.css';

export default function Layout() {
  return (
    <div className={styles.layout}>
      <nav className={styles.nav}>
        <Link to="/home">Home</Link>
        <Link to="/tasks">Tasks</Link>
        <Link to="/schedule">Schedule</Link>
        <Link to="/locations">Locations</Link>
        <Link to="/customer">Settings</Link>
        <Link to="/onboarding/scheduling">Scheduler Setup</Link>
      </nav>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}

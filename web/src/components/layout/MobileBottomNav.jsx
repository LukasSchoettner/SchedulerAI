import { Link, useLocation } from 'react-router-dom';
import NotificationCenter from '../NotificationCenter';
import styles from './MobileBottomNav.module.css';

export default function MobileBottomNav({ onQuickAdd }) {
  const location = useLocation();

  return (
    <nav className={styles.mobileNav} aria-label="Mobile navigation">
      <MobileLink to="/home" active={location.pathname === '/home'} label="Today" />
      <MobileLink to="/schedule" active={location.pathname === '/schedule'} label="Schedule" />
      <button type="button" className={styles.quickAdd} onClick={onQuickAdd} aria-label="Quick Add Task">
        +
      </button>
      <MobileLink to="/tasks" active={location.pathname === '/tasks'} label="Tasks" />
      <NotificationCenter variant="mobileNav" />
    </nav>
  );
}

function MobileLink({ to, active, label }) {
  return (
    <Link className={`${styles.navItem} ${active ? styles.navItemActive : ''}`} to={to}>
      {label}
    </Link>
  );
}

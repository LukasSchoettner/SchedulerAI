import { useState } from 'react';
import { Link, Outlet } from 'react-router-dom';
import NotificationCenter from './NotificationCenter';
import { DayPlanActionsProvider, useDayPlanActions } from './layout/DayPlanActionsContext';
import MobileBottomNav from './layout/MobileBottomNav';
import ConnectionStatusBanner from './pwa/ConnectionStatusBanner';
import InstallAppPrompt from './pwa/InstallAppPrompt';
import QuickAddTaskSheet from './tasks/QuickAddTaskSheet';
import styles from './Layout.module.css';

export default function Layout() {
  return (
    <DayPlanActionsProvider>
      <LayoutContent />
    </DayPlanActionsProvider>
  );
}

function LayoutContent() {
  const [quickAddOpen, setQuickAddOpen] = useState(false);
  const { actions } = useDayPlanActions();

  return (
    <div className={styles.layout}>
      <ConnectionStatusBanner />
      <nav className={styles.nav}>
        <Link to="/home">Home</Link>
        <Link to="/tasks">Tasks</Link>
        <Link to="/schedule">Schedule</Link>
        <Link to="/settings">Settings</Link>
        <NotificationCenter />
      </nav>
      <main className={styles.main}>
        <Outlet />
      </main>
      <MobileBottomNav onQuickAdd={() => setQuickAddOpen(true)} />
      <QuickAddTaskSheet
        open={quickAddOpen}
        onClose={() => setQuickAddOpen(false)}
        regenerateToday={actions.regenerateToday}
      />
      <InstallAppPrompt />
    </div>
  );
}

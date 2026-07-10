import { useEffect, useState } from 'react';
import styles from './PwaPrompts.module.css';

export default function ConnectionStatusBanner() {
  const [online, setOnline] = useState(() => navigator.onLine);
  const [showReconnected, setShowReconnected] = useState(false);

  useEffect(() => {
    const handleOffline = () => {
      setOnline(false);
      setShowReconnected(false);
    };

    const handleOnline = () => {
      setOnline(true);
      setShowReconnected(true);
      window.setTimeout(() => setShowReconnected(false), 3500);
    };

    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
    };
  }, []);

  if (!online) {
    return (
      <div className={`${styles.connectionBanner} ${styles.offline}`} role="status">
        Offline. The app shell may still load, but tasks, day plans, and notifications need a live connection.
      </div>
    );
  }

  if (showReconnected) {
    return (
      <div className={`${styles.connectionBanner} ${styles.online}`} role="status">
        Back online. Live scheduler data can refresh again.
      </div>
    );
  }

  return null;
}

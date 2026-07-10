import { useEffect, useState } from 'react';
import styles from './PwaPrompts.module.css';

const DISMISS_KEY = 'scheduler.installPrompt.dismissed';

export default function InstallAppPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (localStorage.getItem(DISMISS_KEY) === 'true') return undefined;

    const handleBeforeInstallPrompt = (event) => {
      event.preventDefault();
      setDeferredPrompt(event);
      setVisible(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  }, []);

  if (!visible || !deferredPrompt) return null;

  const dismiss = () => {
    localStorage.setItem(DISMISS_KEY, 'true');
    setVisible(false);
    setDeferredPrompt(null);
  };

  const install = async () => {
    try {
      await deferredPrompt.prompt();
      await deferredPrompt.userChoice;
    } finally {
      dismiss();
    }
  };

  return (
    <section className={styles.installPrompt} aria-label="Install app prompt">
      <div>
        <strong>Install SchedulerAI</strong>
        <span>Add it to your Android home screen for a cleaner daily flow.</span>
      </div>
      <div className={styles.promptActions}>
        <button type="button" onClick={install}>Install</button>
        <button type="button" onClick={dismiss}>Not now</button>
      </div>
    </section>
  );
}

export { DISMISS_KEY };

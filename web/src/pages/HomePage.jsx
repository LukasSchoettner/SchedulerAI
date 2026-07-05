import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../lib/api';
import useDayPlan from '../hooks/useDayPlan';
import MorningBriefingPanel from '../components/day-plan/MorningBriefingPanel';
import NextTaskCard from '../components/day-plan/NextTaskCard';
import TodayPlanPreview from '../components/day-plan/TodayPlanPreview';
import { formatDayLabel } from '../components/day-plan/dayPlanUtils';
import styles from './HomePage.module.css';

export default function HomePage() {
    const navigate = useNavigate();
    const dayPlanState = useDayPlan();
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
            <header className={styles.hero}>
                <div>
                    <span className={styles.eyebrow}>Home</span>
                    <h2>Good day{profile ? `, ${profile.customername}` : ''}.</h2>
                    <p>{formatDayLabel(dayPlanState.dateKey)} - Review today's plan and see what comes next.</p>
                </div>
                <button type="button" className={styles.logoutBtn} onClick={handleLogout}>Logout</button>
            </header>

            <section className={styles.dashboardGrid}>
                <div className={styles.mainColumn}>
                    <MorningBriefingPanel
                        variant="home"
                        dayPlan={dayPlanState.dayPlan}
                        activeItems={dayPlanState.activeItems}
                        skippedCount={dayPlanState.skippedCount}
                        loading={dayPlanState.loading}
                        error={dayPlanState.error}
                        onConfirm={dayPlanState.confirmDayPlan}
                        onRegenerate={dayPlanState.regenerateDayPlan}
                    />
                    <TodayPlanPreview items={dayPlanState.activeItems} maxItems={5} />
                </div>

                <aside className={styles.sideColumn}>
                    <NextTaskCard
                        items={dayPlanState.activeItems}
                        onComplete={dayPlanState.completeItem}
                    />
                    <QuickLinks taskCount={taskCount} locationCount={locationCount} />
                </aside>
            </section>
        </div>
    );
}

function QuickLinks({ taskCount, locationCount }) {
    return (
        <section className={styles.quickLinks}>
            <div>
                <span className={styles.eyebrow}>Quick links</span>
                <h3>Where to go next</h3>
            </div>
            <div className={styles.linkGrid}>
                <QuickLink to="/tasks" title="Add task" meta={`${taskCount ?? '...'} tasks`} />
                <QuickLink to="/schedule" title="Open full schedule" meta="Adjust today's timeline" />
                <QuickLink to="/settings/scheduler" title="Scheduler preferences" meta="Priorities, pauses, zones" />
                <QuickLink to="/settings/locations" title="Saved locations" meta={`${locationCount ?? '...'} saved`} />
                <QuickLink to="/settings/zones" title="Zones" meta="Scheduling windows" />
            </div>
        </section>
    );
}

function QuickLink({ to, title, meta }) {
    return (
        <Link className={styles.quickLink} to={to}>
            <strong>{title}</strong>
            <span>{meta}</span>
        </Link>
    );
}

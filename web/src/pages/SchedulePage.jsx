import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import api from '../lib/api';
import { BUILT_IN_CATEGORIES, canonicalizeCategory, getCategoryMeta } from '../lib/categories';
import styles from './SchedulePage.module.css';

export default function SchedulePage() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(false);
    const [hasGenerated, setHasGenerated] = useState(false);
    const [activeCategories, setActiveCategories] = useState(() => new Set(BUILT_IN_CATEGORIES));
    const [calendarStartDate, setCalendarStartDate] = useState(null);
    const [confirmState, setConfirmState] = useState({
        open: false,
        taskId: null,
        title: '',
    });
    const calendarRef = useRef(null);

    const navigate = useNavigate();

    const token = localStorage.getItem('token');
    const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

    const runScheduler = async () => {
        setLoading(true);
        try {
            const res = await api.get('/scheduling', { headers: authHeaders });
            const tasks = res.data.scheduledTasks || [];

            console.log('Scheduler response:', tasks);

            const mapped = tasks.flatMap((st, slotIndex) => {
                const task = st.task;
                const slots = Array.isArray(st.assignedSlots) ? st.assignedSlots : [];

                if (!task) {
                    console.warn('ScheduledTask without task:', st);
                    return [];
                }

                return slots.map((slot, idx) => {
                    return {
                        // unique for FullCalendar
                        id: `${task.id}-${idx}`,
                        title: `${task.title} [${task.category}] (p${task.priority})`,
                        start: slot.start,
                        end: slot.end,
                        extendedProps: {
                            taskId: task.id, // real id for backend calls
                            type: task.type,
                            category: canonicalizeCategory(task.category),
                            description: task.description || 'No description',
                            dueDate: task.dueDate,
                            bufferTime: task.bufferTime,
                        },
                        backgroundColor: getColorForCategory(task.category),
                        borderColor: getColorForCategory(task.category),
                    };
                });
            });

            console.log('Mapped events:', mapped);
            setEvents(mapped);
            setHasGenerated(true);
            setActiveCategories(new Set([
                ...BUILT_IN_CATEGORIES,
                ...mapped.map(event => event.extendedProps.category).filter(Boolean),
            ]));

            const firstEvent = mapped
                .filter(event => event.start)
                .sort((a, b) => new Date(a.start) - new Date(b.start))[0];
            setCalendarStartDate(firstEvent?.start || null);
        } catch (err) {
            console.error('Failed to fetch schedule:', err);
            alert('Failed to generate schedule.');
        } finally {
            setLoading(false);
        }
    };

    const completeTask = async (taskId) => {
        console.log('Completing task with id:', taskId);

        try {
            const res = await api.patch(
                `/tasks/${taskId}/status`,
                { status: 'COMPLETED' },
                { headers: authHeaders }
            );
            console.log('Task completion response:', res.status, res.data);

            await runScheduler(); // refresh schedule so completed tasks disappear / reschedule
        } catch (err) {
            console.error('Error completing task:', err);
            throw err;
        }
    };

    const onEventClick = (clickInfo) => {
        const { taskId } = clickInfo.event.extendedProps;
        const title = clickInfo.event.title;

        console.log('Clicked event, extracted taskId:', taskId);

        if (!taskId) {
            alert('Could not determine task id for this event.');
            return;
        }

        // Open our custom confirm dialog
        setConfirmState({
            open: true,
            taskId,
            title,
        });
    };

    const handleConfirmYes = async () => {
        const { taskId } = confirmState;
        setConfirmState({ open: false, taskId: null, title: '' });

        try {
            await completeTask(taskId);
        } catch (e) {
            alert('Failed to mark task as done.');
        }
    };

    const handleConfirmNo = () => {
        setConfirmState({ open: false, taskId: null, title: '' });
    };

    const getColorForCategory = (category) => {
        return getCategoryMeta(category).color;
    };

    const showAllCategories = () => {
        setActiveCategories(new Set(legendCategories));
        goToFirstEvent(events);
    };

    const focusCategory = (category) => {
        setActiveCategories(prev => {
            if (prev.size === 1 && prev.has(category)) {
                return new Set(legendCategories);
            }
            return new Set([category]);
        });

        const matchingEvents = events.filter(event => event.extendedProps.category === category);
        goToFirstEvent(matchingEvents);
    };

    const goToFirstEvent = (eventList) => {
        const firstEvent = eventList
            .filter(event => event.start)
            .sort((a, b) => new Date(a.start) - new Date(b.start))[0];
        if (firstEvent && calendarRef.current) {
            calendarRef.current.getApi().gotoDate(firstEvent.start);
        }
    };

    const categoryCounts = events.reduce((counts, event) => {
        const category = event.extendedProps.category || 'Custom';
        counts[category] = (counts[category] || 0) + 1;
        return counts;
    }, {});
    const visibleEvents = events.filter(event => activeCategories.has(event.extendedProps.category));
    const legendCategories = [
        ...BUILT_IN_CATEGORIES,
        ...Object.keys(categoryCounts).filter(category => !BUILT_IN_CATEGORIES.includes(category)),
    ];

    useEffect(() => {
        if (calendarStartDate && calendarRef.current) {
            calendarRef.current.getApi().gotoDate(calendarStartDate);
        }
    }, [calendarStartDate, events.length]);

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h2 className={styles.title}>Your Schedule</h2>
                <div>
                    <button onClick={() => navigate('/home')} className={styles.backBtn}>
                        ← Back
                    </button>
                    <button onClick={runScheduler} disabled={loading}>
                        {loading ? 'Loading…' : 'Generate Schedule'}
                    </button>
                </div>
            </div>

            <div className={styles.content}>
                {!hasGenerated && !loading && (
                    <p className={styles.info}>
                        Click &quot;Generate Schedule&quot; to see your tasks.
                    </p>
                )}

                {hasGenerated && events.length === 0 && (
                    <p className={styles.noTasks}>No tasks could be scheduled.</p>
                )}

                {events.length > 0 && (
                    <>
                        <div className={styles.categoryToolbar} aria-label="Schedule categories">
                            <button
                                type="button"
                                className={`${styles.categoryChip} ${activeCategories.size === legendCategories.length ? styles.categoryChipActive : ''}`}
                                onClick={showAllCategories}
                            >
                                All
                                <span className={styles.categoryCount}>
                                    {events.length}
                                </span>
                            </button>
                            {legendCategories.map(category => {
                                const selected = activeCategories.has(category);
                                const meta = getCategoryMeta(category);
                                return (
                                    <button
                                        key={category}
                                        type="button"
                                        className={`${styles.categoryChip} ${selected ? styles.categoryChipActive : ''}`}
                                        onClick={() => focusCategory(category)}
                                        title={meta.description}
                                    >
                                        <span
                                            className={styles.categoryDot}
                                            style={{ backgroundColor: meta.color }}
                                            aria-hidden="true"
                                        />
                                        {category}
                                        <span className={styles.categoryCount}>
                                            {categoryCounts[category] || 0}
                                        </span>
                                    </button>
                                );
                            })}
                        </div>
                        {visibleEvents.length === 0 && (
                            <p className={styles.noTasks}>No events match the selected categories.</p>
                        )}
                        <FullCalendar
                            ref={calendarRef}
                            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                            initialView="timeGridWeek"
                            initialDate={calendarStartDate || undefined}
                            events={visibleEvents}
                            height="auto"
                            contentHeight="auto"
                            expandRows={true}
                            eventClick={onEventClick}
                        />
                    </>
                )}
            </div>

            {confirmState.open && (
                <div className={styles.confirmBackdrop}>
                    <div className={styles.confirmDialog}>
                        <p>Mark &quot;{confirmState.title}&quot; as done?</p>
                        <div className={styles.confirmButtons}>
                            <button onClick={handleConfirmYes}>Yes</button>
                            <button onClick={handleConfirmNo}>No</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

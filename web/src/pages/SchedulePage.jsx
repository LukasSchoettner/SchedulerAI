import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import api from '../lib/api';
import styles from './SchedulePage.module.css';

export default function SchedulePage() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(false);
    const [confirmState, setConfirmState] = useState({
        open: false,
        taskId: null,
        title: '',
    });

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
                            description: task.description || 'No description',
                            dueDate: task.dueDate,
                            bufferTime: task.bufferTime,
                        },
                        backgroundColor: getColorForCategory(task.category),
                    };
                });
            });

            console.log('Mapped events:', mapped);
            setEvents(mapped);
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
        const map = {
            Work: '#4caf50',
            Duty: '#2196f3',
            Sport: '#ff9800',
            Leisure: '#9c27b0',
        };
        return map[category] || '#607d8b';
    };

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
                {!events.length && (
                    <p className={styles.info}>
                        Click &quot;Generate Schedule&quot; to see your tasks.
                    </p>
                )}

                {events.length === 0 && (
                    <p className={styles.noTasks}>No tasks could be scheduled.</p>
                )}

                {events.length > 0 && (
                    <FullCalendar
                        plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                        initialView="timeGridWeek"
                        events={events}
                        height="auto"
                        contentHeight="auto"
                        expandRows={true}
                        eventClick={onEventClick}
                    />
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

import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import api from '../lib/api';
import { BUILT_IN_CATEGORIES, canonicalizeCategory, getCategoryMeta } from '../lib/categories';
import useDayPlan from '../hooks/useDayPlan';
import MorningBriefingPanel from '../components/day-plan/MorningBriefingPanel';
import PlanStatusChip from '../components/day-plan/PlanStatusChip';
import TaskDetailsDrawer from '../components/day-plan/TaskDetailsDrawer';
import TodayPlanTimeline from '../components/day-plan/TodayPlanTimeline';
import { useDayPlanActions } from '../components/layout/DayPlanActionsContext';
import {
    categorySoftStyle,
    dayPlanItemToEvent,
    formatDateTime,
    formatDayLabel,
    formatTaskType,
    localDateKey,
} from '../components/day-plan/dayPlanUtils';
import styles from './SchedulePage.module.css';

const TODAY_VIEW = 'today';
const WEEK_VIEW = 'week';
const CALENDAR_WEEK_VIEW = 'timeGridWeek';

export default function SchedulePage() {
    const dayPlanState = useDayPlan();
    const { setActions, clearActions } = useDayPlanActions();
    const [weeklyEvents, setWeeklyEvents] = useState([]);
    const [activeCategories, setActiveCategories] = useState(() => new Set(BUILT_IN_CATEGORIES));
    const [pageView, setPageView] = useState(TODAY_VIEW);
    const [visibleDateRange, setVisibleDateRange] = useState(null);
    const [selectedItem, setSelectedItem] = useState(null);
    const [selectedTaskDetails, setSelectedTaskDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);
    const [detailsError, setDetailsError] = useState('');
    const calendarRef = useRef(null);
    const regenerateTodayRef = useRef(null);
    const navigate = useNavigate();

    const {
        dateKey,
        dayPlan,
        activeItems,
        skippedCount,
        loading,
        error,
        requiresLogin,
        loadDayPlan,
        confirmDayPlan,
        regenerateDayPlan,
        skipItem,
        keepTimeFree,
        completeItem,
    } = dayPlanState;

    const todayEvents = activeItems.map(dayPlanItemToEvent);
    const calendarEvents = mergeTodayIntoWeekly(weeklyEvents, todayEvents, dateKey);
    const categoryCounts = countByCategory(calendarEvents);
    const legendCategories = [
        ...BUILT_IN_CATEGORIES,
        ...Object.keys(categoryCounts).filter(category => !BUILT_IN_CATEGORIES.includes(category)),
    ];
    const visibleEvents = calendarEvents.filter(event => activeCategories.has(event.extendedProps.category));
    const { slotMinTime, slotMaxTime, firstTaskLabel, visibleRangeLabel } = computeVisibleTimeRange(calendarEvents, visibleDateRange);
    useEffect(() => {
        refreshWeeklySchedule();
    }, []);

    useEffect(() => {
        setActions({
            regenerateToday: () => regenerateTodayRef.current?.(),
        });
        return clearActions;
    }, [clearActions, setActions]);

    useEffect(() => {
        setActiveCategories(new Set([
            ...BUILT_IN_CATEGORIES,
            ...weeklyEvents.map(event => event.extendedProps.category).filter(Boolean),
            ...activeItems.map(item => canonicalizeCategory(item.categorySnapshot)).filter(Boolean),
        ]));
    }, [weeklyEvents, activeItems]);

    useEffect(() => {
        if (calendarRef.current) {
            const calendarApi = calendarRef.current.getApi();
            calendarApi.changeView(CALENDAR_WEEK_VIEW);
            calendarApi.setOption('slotMinTime', slotMinTime);
            calendarApi.setOption('slotMaxTime', slotMaxTime);
        }
    }, [slotMinTime, slotMaxTime, pageView]);

    const refreshWeeklySchedule = async () => {
        try {
            const res = await api.get('/scheduling');
            setWeeklyEvents(mapScheduledTasks(res.data.scheduledTasks || []));
        } catch (err) {
            console.warn('Weekly schedule could not be loaded:', err);
        }
    };

    const regenerateAndRefresh = async () => {
        await regenerateDayPlan();
        await refreshWeeklySchedule();
    };

    regenerateTodayRef.current = regenerateAndRefresh;

    const completeAndRefresh = async (item) => {
        await completeItem(item);
        await refreshWeeklySchedule();
    };

    const keepFreeAndRefresh = async (item) => {
        await keepTimeFree(item);
        await refreshWeeklySchedule();
    };

    const openTaskDetails = async (item) => {
        setSelectedItem(item);
        setSelectedTaskDetails(null);
        setDetailsError('');
        setDetailsLoading(false);
        if (!item?.taskId) return;
        setDetailsLoading(true);
        try {
            const res = await api.get(`/tasks/${item.taskId}`);
            setSelectedTaskDetails(res.data);
        } catch {
            setDetailsError('Full task notes could not be loaded.');
        } finally {
            setDetailsLoading(false);
        }
    };

    const closeTaskDetails = () => {
        setSelectedItem(null);
        setSelectedTaskDetails(null);
        setDetailsError('');
        setDetailsLoading(false);
    };

    const showAllCategories = () => {
        setActiveCategories(new Set(legendCategories));
    };

    const focusCategory = (category) => {
        setActiveCategories(prev => {
            if (prev.size === 1 && prev.has(category)) {
                return new Set(legendCategories);
            }
            return new Set([category]);
        });
    };

    const renderEventContent = (eventInfo) => {
        const { category, type, itemStatus } = eventInfo.event.extendedProps;
        return (
            <div className={styles.eventCard}>
                <strong>{eventInfo.event.title}</strong>
                <span>{category || 'Task'} - {formatTaskType(type)} - {itemStatus || 'Proposed'}</span>
            </div>
        );
    };

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <div className={styles.headerTitle}>
                    <span className={styles.eyebrow}>Schedule</span>
                    <h2>Planning and corrections</h2>
                    <p>{formatDayLabel(dateKey)}</p>
                </div>
                <div className={styles.headerControls}>
                    <PlanStatusChip dayPlan={dayPlan} loading={loading} />
                    <div className={styles.viewToggle} aria-label="Schedule view">
                        <button type="button" className={pageView === TODAY_VIEW ? styles.viewToggleActive : ''} onClick={() => setPageView(TODAY_VIEW)}>
                            Today
                        </button>
                        <button type="button" className={pageView === WEEK_VIEW ? styles.viewToggleActive : ''} onClick={() => setPageView(WEEK_VIEW)}>
                            Week
                        </button>
                    </div>
                </div>
            </header>

            {loading && (
                <EmptyState title="Building today's plan..." text="Loading persisted day-plan data and asking the scheduler only when needed." />
            )}

            {!loading && error && (
                <section className={styles.emptyState}>
                    <h3>{error}</h3>
                    {requiresLogin ? (
                        <button type="button" className={styles.primaryBtn} onClick={() => navigate('/')}>Log in again</button>
                    ) : (
                        <button type="button" className={styles.primaryBtn} onClick={loadDayPlan}>Try again</button>
                    )}
                </section>
            )}

            {!loading && !error && dayPlan && pageView === TODAY_VIEW && (
                <main className={styles.todayLayout}>
                    <MorningBriefingPanel
                        variant="schedule"
                        dayPlan={dayPlan}
                        activeItems={activeItems}
                        skippedCount={skippedCount}
                        onConfirm={confirmDayPlan}
                        onRegenerate={regenerateAndRefresh}
                    />
                    <section className={styles.panel}>
                        <div className={styles.sectionHeader}>
                            <div>
                                <span className={styles.eyebrow}>Today timeline</span>
                                <h3>Corrections workspace</h3>
                            </div>
                            <span className={styles.statusChip}>{activeItems.length} scheduled</span>
                        </div>
                        <TodayPlanTimeline
                            items={activeItems}
                            transitions={dayPlan.transitions}
                            empty="No tasks scheduled today. Add tasks or regenerate the plan."
                            onComplete={completeAndRefresh}
                            onOpenDetails={openTaskDetails}
                            onKeepFree={keepFreeAndRefresh}
                            onSkip={skipItem}
                        />
                    </section>
                </main>
            )}

            {!loading && !error && dayPlan && pageView === WEEK_VIEW && (
                <main className={styles.weekLayout}>
                    <section className={styles.panel}>
                        <div className={styles.sectionHeader}>
                            <div>
                                <span className={styles.eyebrow}>Week view</span>
                                <h3>Proposed future schedule</h3>
                            </div>
                            <span className={styles.statusChip}>{visibleEvents.length} visible</span>
                        </div>
                        <div className={styles.summaryGrid}>
                            <div><span>Scheduled</span><strong>{calendarEvents.length}</strong></div>
                            <div><span>First task</span><strong>{firstTaskLabel}</strong></div>
                            <div><span>Visible time</span><strong>{visibleRangeLabel}</strong></div>
                            <div><span>Filters</span><strong>{activeCategories.size === legendCategories.length ? 'All' : `${activeCategories.size} active`}</strong></div>
                        </div>
                        <CategoryFilters
                            categories={legendCategories}
                            counts={categoryCounts}
                            activeCategories={activeCategories}
                            total={calendarEvents.length}
                            onAll={showAllCategories}
                            onCategory={focusCategory}
                        />
                    </section>

                    <section className={styles.calendarShell}>
                        <FullCalendar
                            ref={calendarRef}
                            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                            initialView={CALENDAR_WEEK_VIEW}
                            initialDate={dateKey}
                            events={visibleEvents}
                            firstDay={1}
                            allDaySlot={false}
                            nowIndicator={true}
                            slotMinTime={slotMinTime}
                            slotMaxTime={slotMaxTime}
                            height="auto"
                            contentHeight="auto"
                            expandRows={true}
                            datesSet={({ start, end }) => {
                                setVisibleDateRange(prev => {
                                    const nextStart = start.toISOString();
                                    const nextEnd = end.toISOString();
                                    if (prev?.start === nextStart && prev?.end === nextEnd) return prev;
                                    return { start: nextStart, end: nextEnd };
                                });
                            }}
                            eventContent={renderEventContent}
                            dayHeaderFormat={{ weekday: 'short', day: '2-digit', month: '2-digit' }}
                            slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
                            eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
                        />
                    </section>
                </main>
            )}

            <TaskDetailsDrawer
                item={selectedItem}
                taskDetails={selectedTaskDetails}
                loading={detailsLoading}
                error={detailsError}
                onClose={closeTaskDetails}
                onEdit={() => navigate('/tasks')}
            />
        </div>
    );
}

function CategoryFilters({ categories, counts, activeCategories, total, onAll, onCategory }) {
    return (
        <div className={styles.categoryToolbar} aria-label="Schedule categories">
            <button
                type="button"
                className={`${styles.categoryChip} ${activeCategories.size === categories.length ? styles.categoryChipActive : ''}`}
                onClick={onAll}
            >
                All <span>{total}</span>
            </button>
            {categories.map(category => {
                const selected = activeCategories.has(category);
                const meta = getCategoryMeta(category);
                return (
                    <button
                        key={category}
                        type="button"
                        className={`${styles.categoryChip} ${selected ? styles.categoryChipActive : ''}`}
                        onClick={() => onCategory(category)}
                        title={meta.description}
                    >
                        <span className={styles.categoryDot} style={{ backgroundColor: categorySoftStyle(category).border }} />
                        {category}
                        <span>{counts[category] || 0}</span>
                    </button>
                );
            })}
        </div>
    );
}

function EmptyState({ title, text }) {
    return (
        <section className={styles.emptyState}>
            <h3>{title}</h3>
            {text && <p>{text}</p>}
        </section>
    );
}

function mapScheduledTasks(tasks) {
    return tasks.flatMap((scheduledTask) => {
        const task = scheduledTask.task;
        const slots = Array.isArray(scheduledTask.assignedSlots) ? scheduledTask.assignedSlots : [];
        if (!task) return [];
        return slots.map((slot, idx) => {
            const category = canonicalizeCategory(task.category);
            const color = categorySoftStyle(category);
            return {
                id: `weekly-${task.id}-${idx}`,
                title: task.title,
                start: slot.start,
                end: slot.end,
                backgroundColor: color.backgroundColor,
                borderColor: color.border,
                textColor: color.color,
                extendedProps: {
                    taskId: task.id,
                    type: task.type,
                    category,
                    priority: task.priority,
                    itemStatus: task.type === 'FIXED' ? 'Fixed' : 'Proposed',
                },
            };
        });
    });
}

function mergeTodayIntoWeekly(weeklyEvents, todayEvents, todayKey) {
    return [
        ...weeklyEvents.filter(event => localDateKey(new Date(event.start)) !== todayKey),
        ...todayEvents,
    ].sort((a, b) => new Date(a.start) - new Date(b.start));
}

function countByCategory(events) {
    return events.reduce((counts, event) => {
        const category = event.extendedProps.category || 'Custom';
        counts[category] = (counts[category] || 0) + 1;
        return counts;
    }, {});
}

function computeVisibleTimeRange(events, visibleDateRange) {
    const datedEvents = events
        .filter(event => event.start)
        .map(event => ({ start: new Date(event.start), end: event.end ? new Date(event.end) : new Date(event.start) }))
        .filter(event => !Number.isNaN(event.start.getTime()))
        .filter(event => isEventInVisibleRange(event, visibleDateRange));

    if (!datedEvents.length) {
        return { slotMinTime: '08:00:00', slotMaxTime: '20:00:00', firstTaskLabel: '-', visibleRangeLabel: '08:00-20:00' };
    }

    const earliest = datedEvents.reduce((min, event) => event.start < min ? event.start : min, datedEvents[0].start);
    const latest = datedEvents.reduce((max, event) => event.end > max ? event.end : max, datedEvents[0].end);
    const minHour = Math.max(0, earliest.getHours());
    const latestHour = latest.getHours() + (latest.getMinutes() > 0 || latest.getSeconds() > 0 ? 1 : 0);
    const maxHour = Math.min(24, Math.max(20, latestHour));

    return {
        slotMinTime: toCalendarTimeString(minHour),
        slotMaxTime: toCalendarTimeString(maxHour),
        firstTaskLabel: formatDateTime(earliest),
        visibleRangeLabel: `${formatHour(minHour)}-${formatHour(maxHour)}`,
    };
}

function isEventInVisibleRange(event, visibleDateRange) {
    if (!visibleDateRange?.start || !visibleDateRange?.end) return true;
    const rangeStart = new Date(visibleDateRange.start);
    const rangeEnd = new Date(visibleDateRange.end);
    if (Number.isNaN(rangeStart.getTime()) || Number.isNaN(rangeEnd.getTime())) return true;
    return event.start < rangeEnd && event.end >= rangeStart;
}

function toCalendarTimeString(hour) {
    return `${String(hour).padStart(2, '0')}:00:00`;
}

function formatHour(hour) {
    return `${String(hour).padStart(2, '0')}:00`;
}

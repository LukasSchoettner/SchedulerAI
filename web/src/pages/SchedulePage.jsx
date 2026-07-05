import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import api from '../lib/api';
import { BUILT_IN_CATEGORIES, canonicalizeCategory, getCategoryMeta } from '../lib/categories';
import styles from './SchedulePage.module.css';

const TODAY_VIEW = 'today';
const WEEK_VIEW = 'week';
const CALENDAR_WEEK_VIEW = 'timeGridWeek';

export default function SchedulePage() {
    const [dayPlan, setDayPlan] = useState(null);
    const [weeklyEvents, setWeeklyEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [activeCategories, setActiveCategories] = useState(() => new Set(BUILT_IN_CATEGORIES));
    const [pageView, setPageView] = useState(TODAY_VIEW);
    const [visibleDateRange, setVisibleDateRange] = useState(null);
    const calendarRef = useRef(null);
    const navigate = useNavigate();

    const todayKey = localDateKey(new Date());
    const activeItems = (dayPlan?.items || []).filter(item => item.status !== 'SKIPPED');
    const skippedCount = (dayPlan?.items || []).filter(item => item.status === 'SKIPPED').length;
    const todayEvents = activeItems.map(dayPlanItemToEvent);
    const calendarEvents = mergeTodayIntoWeekly(weeklyEvents, todayEvents, todayKey);
    const categoryCounts = countByCategory(calendarEvents);
    const legendCategories = [
        ...BUILT_IN_CATEGORIES,
        ...Object.keys(categoryCounts).filter(category => !BUILT_IN_CATEGORIES.includes(category)),
    ];
    const visibleEvents = calendarEvents.filter(event => activeCategories.has(event.extendedProps.category));
    const { slotMinTime, slotMaxTime, firstTaskLabel, visibleRangeLabel } = computeVisibleTimeRange(calendarEvents, visibleDateRange);
    const statusLabel = dayPlanStatusLabel(dayPlan);
    const planNeedsConfirmation = dayPlan && (dayPlan.status !== 'CONFIRMED' || dayPlan.changedFromConfirmed);
    const requiresLogin = error.includes('session expired') || error.includes('log in again');

    useEffect(() => {
        loadSchedulePage();
    }, []);

    useEffect(() => {
        if (calendarRef.current) {
            const calendarApi = calendarRef.current.getApi();
            calendarApi.changeView(CALENDAR_WEEK_VIEW);
            calendarApi.setOption('slotMinTime', slotMinTime);
            calendarApi.setOption('slotMaxTime', slotMaxTime);
        }
    }, [slotMinTime, slotMaxTime, pageView]);

    const loadSchedulePage = async () => {
        setLoading(true);
        setError('');
        try {
            const plan = await loadOrGenerateDayPlan(todayKey);
            let weekly = [];
            try {
                weekly = await loadWeeklySchedule();
            } catch (weeklyErr) {
                console.warn('Weekly schedule could not be loaded:', weeklyErr);
            }
            setDayPlan(plan);
            setWeeklyEvents(weekly);
            setActiveCategories(new Set([
                ...BUILT_IN_CATEGORIES,
                ...weekly.map(event => event.extendedProps.category).filter(Boolean),
                ...(plan.items || []).map(item => canonicalizeCategory(item.categorySnapshot)).filter(Boolean),
            ]));
        } catch (err) {
            console.error('Failed to load schedule page:', err);
            setError(`Could not generate today's plan.${formatErrorMessage(err)}`);
        } finally {
            setLoading(false);
        }
    };

    const loadOrGenerateDayPlan = async (dateKey) => {
        try {
            const existing = await api.get(`/day-plans/me/${dateKey}`);
            return existing.data;
        } catch (err) {
            if (err.response?.status !== 404) {
                throw err;
            }
            const generated = await api.post(`/day-plans/generate?date=${dateKey}`);
            return generated.data;
        }
    };

    const loadWeeklySchedule = async () => {
        const res = await api.get('/scheduling');
        return mapScheduledTasks(res.data.scheduledTasks || []);
    };

    const refreshWeeklySchedule = async () => {
        try {
            setWeeklyEvents(await loadWeeklySchedule());
        } catch (err) {
            console.warn('Weekly schedule could not be refreshed:', err);
        }
    };

    const confirmDayPlan = async () => {
        if (!dayPlan?.id) return;
        const res = await api.post(`/day-plans/${dayPlan.id}/confirm`);
        setDayPlan(res.data);
    };

    const regenerateDayPlan = async () => {
        if (!dayPlan?.id) {
            await loadSchedulePage();
            return;
        }
        setLoading(true);
        setError('');
        try {
            const res = await api.post(`/day-plans/${dayPlan.id}/regenerate`);
            setDayPlan(res.data);
            await refreshWeeklySchedule();
        } catch (err) {
            console.error('Failed to regenerate day plan:', err);
            setError(`Could not regenerate today's plan.${formatErrorMessage(err)}`);
        } finally {
            setLoading(false);
        }
    };

    const skipItem = async (item) => {
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/skip-today`);
        setDayPlan(res.data);
    };

    const keepTimeFree = async (item) => {
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/keep-free`);
        setDayPlan(res.data);
        await refreshWeeklySchedule();
    };

    const completeItem = async (item) => {
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/complete`);
        setDayPlan(res.data);
        await refreshWeeklySchedule();
    };

    const openTaskDetails = () => {
        navigate('/tasks');
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
                    <h2>Schedule</h2>
                    <p>{formatDayLabel(todayKey)}</p>
                </div>
                <div className={styles.headerControls}>
                    <span className={`${styles.statusChip} ${statusClass(dayPlan)}`}>{loading ? 'Generating' : statusLabel}</span>
                    <div className={styles.viewToggle} aria-label="Schedule view">
                        <button type="button" className={pageView === TODAY_VIEW ? styles.viewToggleActive : ''} onClick={() => setPageView(TODAY_VIEW)}>
                            Today
                        </button>
                        <button type="button" className={pageView === WEEK_VIEW ? styles.viewToggleActive : ''} onClick={() => setPageView(WEEK_VIEW)}>
                            Week
                        </button>
                    </div>
                    <button type="button" className={styles.secondaryBtn} onClick={regenerateDayPlan} disabled={loading}>
                        Regenerate
                    </button>
                    {planNeedsConfirmation && (
                        <button type="button" className={styles.primaryBtn} onClick={confirmDayPlan} disabled={loading}>
                            Confirm plan
                        </button>
                    )}
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
                        <button type="button" className={styles.primaryBtn} onClick={loadSchedulePage}>Try again</button>
                    )}
                </section>
            )}

            {!loading && !error && dayPlan && pageView === TODAY_VIEW && (
                <main className={styles.todayLayout}>
                    <MorningBriefingPanel
                        dayPlan={dayPlan}
                        activeItems={activeItems}
                        skippedCount={skippedCount}
                        onConfirm={confirmDayPlan}
                        onRegenerate={regenerateDayPlan}
                    />
                    <section className={styles.panel}>
                        <div className={styles.sectionHeader}>
                            <div>
                                <span className={styles.eyebrow}>Today's Plan</span>
                                <h3>Chronological timeline</h3>
                            </div>
                            <span className={styles.statusChip}>{activeItems.length} scheduled</span>
                        </div>
                        <DayPlanTimeline
                            items={activeItems}
                            empty="No tasks scheduled today. Add tasks or regenerate the plan."
                            onComplete={completeItem}
                            onOpenDetails={openTaskDetails}
                            onKeepFree={keepTimeFree}
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
                            initialDate={todayKey}
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
        </div>
    );
}

function MorningBriefingPanel({ dayPlan, activeItems, skippedCount, onConfirm, onRegenerate }) {
    const [showDetails, setShowDetails] = useState(false);
    const fixed = activeItems.filter(item => item.taskTypeSnapshot === 'FIXED');
    const flexible = activeItems.filter(item => item.taskTypeSnapshot !== 'FIXED');
    const completed = activeItems.filter(item => item.status === 'COMPLETED').length;
    const isConfirmed = dayPlan.status === 'CONFIRMED' && !dayPlan.changedFromConfirmed;

    if (isConfirmed) {
        return (
            <section className={`${styles.panel} ${styles.briefingCompact}`}>
                <div>
                    <span className={styles.eyebrow}>Morning briefing</span>
                    <h3>Today's plan confirmed.</h3>
                    <p>{activeItems.length} scheduled items - {formatMinutes(dayPlan.freeGapMinutes)} free time.</p>
                </div>
                <div className={styles.inlineActions}>
                    <button type="button" className={styles.secondaryBtn} onClick={() => setShowDetails(prev => !prev)}>
                        {showDetails ? 'Hide details' : 'Review again'}
                    </button>
                    <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>
                </div>
                {showDetails && <BriefingDetails dayPlan={dayPlan} fixed={fixed} flexible={flexible} skippedCount={skippedCount} completed={completed} />}
            </section>
        );
    }

    return (
        <section className={styles.panel}>
            <div className={styles.briefingTop}>
                <div>
                    <span className={styles.eyebrow}>Morning briefing</span>
                    <h3>{briefingHeadline(dayPlan, activeItems)}</h3>
                    <p>{briefingSubtitle(dayPlan)}</p>
                </div>
                <div className={styles.inlineActions}>
                    <button type="button" className={styles.primaryBtn} onClick={onConfirm}>Confirm today's plan</button>
                    <button type="button" className={styles.secondaryBtn} onClick={() => setShowDetails(prev => !prev)}>
                        {showDetails ? 'Hide details' : 'Review changes'}
                    </button>
                    <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>
                </div>
            </div>

            <div className={styles.briefingChips}>
                <MetricChip label="Fixed" value={fixed.length} />
                <MetricChip label="Flexible" value={flexible.length} />
                <MetricChip label="Free" value={formatMinutes(dayPlan.freeGapMinutes)} />
                <MetricChip label="Skipped" value={skippedCount} />
            </div>

            <div className={styles.importantRow}>
                <span>{dayPlan.tightSpotSummary || 'No tight transitions'}</span>
                {dayPlan.changedFromConfirmed && <strong>This regenerated plan differs from the confirmed plan.</strong>}
            </div>

            {showDetails && <BriefingDetails dayPlan={dayPlan} fixed={fixed} flexible={flexible} skippedCount={skippedCount} completed={completed} />}
        </section>
    );
}

function BriefingDetails({ dayPlan, fixed, flexible, skippedCount, completed }) {
    return (
        <div className={styles.detailsGrid}>
            <div>Fixed commitments today: <strong>{fixed.length}</strong></div>
            <div>Automatically planned flexible tasks: <strong>{flexible.length}</strong></div>
            <div>Remaining free time: <strong>{formatMinutes(dayPlan.freeGapMinutes)}</strong></div>
            <div>Skipped today: <strong>{skippedCount}</strong></div>
            <div>Completed: <strong>{completed}</strong></div>
            <div>{dayPlan.tightSpotSummary || 'No tight transitions'}</div>
        </div>
    );
}

function MetricChip({ label, value }) {
    return (
        <div className={styles.metricChip}>
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

function DayPlanTimeline({ items, empty, onComplete, onOpenDetails, onKeepFree, onSkip }) {
    if (!items.length) {
        return <p className={styles.emptyInline}>{empty}</p>;
    }

    return (
        <ol className={styles.timeline}>
            {items.map(item => {
                const category = canonicalizeCategory(item.categorySnapshot);
                const color = categorySoftStyle(category);
                const isCompleted = item.status === 'COMPLETED';
                const isFreeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
                return (
                    <li
                        key={item.id}
                        className={`${styles.timelineItem} ${isCompleted ? styles.timelineItemCompleted : ''} ${isFreeTime ? styles.timelineItemReserved : ''}`}
                        style={{ borderLeftColor: color.border }}
                    >
                        <div className={styles.timelineTime}>
                            <strong>{formatTimeOnly(item.startDateTime)}</strong>
                            <span>{formatTimeOnly(item.endDateTime)}</span>
                        </div>
                        <div className={styles.timelineMain}>
                            <div className={styles.timelineTitleRow}>
                                <strong>{item.titleSnapshot}</strong>
                                <span className={styles.categoryPill} style={color}>{isFreeTime ? 'Reserved' : category}</span>
                            </div>
                            <p>
                                {category} - {formatTaskType(item.taskTypeSnapshot)} - {formatItemStatus(item.status)}
                                {item.occurrenceKey ? ' - Recurring' : ''}
                            </p>
                        </div>
                        <TaskActions
                            item={item}
                            onComplete={onComplete}
                            onOpenDetails={onOpenDetails}
                            onKeepFree={item.taskTypeSnapshot === 'FIXED' ? null : onKeepFree}
                            onSkip={canSkipItem(item) ? onSkip : null}
                        />
                    </li>
                );
            })}
        </ol>
    );
}

function TaskActions({ item, onComplete, onOpenDetails, onKeepFree, onSkip }) {
    const [open, setOpen] = useState(false);
    const isCompleted = item.status === 'COMPLETED';
    const isFreeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
    const canComplete = !isCompleted && !isFreeTime;

    const choose = async (action) => {
        setOpen(false);
        if (action === 'details') onOpenDetails(item);
        if (action === 'complete') await onComplete(item);
        if (action === 'free' && onKeepFree) await onKeepFree(item);
        if (action === 'skip' && onSkip) await onSkip(item);
    };

    return (
        <div className={styles.taskActions}>
            {canComplete && (
                <button type="button" className={styles.doneBtn} onClick={() => choose('complete')}>
                    Done
                </button>
            )}
            <div className={styles.moreMenu}>
                <button type="button" className={styles.moreBtn} aria-label={`More actions for ${item.titleSnapshot}`} onClick={() => setOpen(prev => !prev)}>
                    ...
                </button>
                {open && (
                    <div className={styles.moreMenuPanel}>
                        <button type="button" onClick={() => setOpen(false)}>Keep</button>
                        {onSkip && <button type="button" onClick={() => choose('skip')}>{skipLabelForItem(item)}</button>}
                        {onKeepFree && <button type="button" onClick={() => choose('free')}>Keep this time free</button>}
                        <button type="button" onClick={() => choose('details')}>Open task details</button>
                        {item.taskTypeSnapshot !== 'FIXED' && <button type="button" disabled>Move to another day - future</button>}
                        {item.taskTypeSnapshot !== 'FIXED' && <button type="button" disabled>Shorten duration - future</button>}
                        {item.taskTypeSnapshot !== 'FIXED' && <button type="button" disabled>Replace task - future</button>}
                    </div>
                )}
            </div>
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

function dayPlanItemToEvent(item) {
    const category = canonicalizeCategory(item.categorySnapshot);
    const color = categorySoftStyle(category);
    const isFreeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
    return {
        id: `day-plan-${item.id}`,
        title: item.titleSnapshot,
        start: item.startDateTime,
        end: item.endDateTime,
        backgroundColor: isFreeTime ? '#f3f4f6' : color.backgroundColor,
        borderColor: isFreeTime ? '#6b7280' : color.border,
        textColor: isFreeTime ? '#374151' : color.color,
        extendedProps: {
            taskId: item.taskId,
            itemId: item.id,
            type: item.taskTypeSnapshot,
            category,
            priority: item.prioritySnapshot,
            itemStatus: isFreeTime ? 'Free time' : item.status === 'CONFIRMED' ? 'Confirmed today' : item.status,
        },
    };
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

function briefingHeadline(dayPlan, items) {
    if (dayPlan.changedFromConfirmed) return 'Plan changed after regeneration';
    if (dayPlan.status === 'CONFIRMED') return "Today's plan is confirmed";
    if (!items.length) return 'No tasks scheduled today';
    return items.length > 7 ? 'Today looks busy' : 'Today looks manageable';
}

function briefingSubtitle(dayPlan) {
    if (dayPlan.changedFromConfirmed) return 'This generated plan differs from the last confirmed version.';
    return 'Review the generated plan and confirm when it looks right.';
}

function dayPlanStatusLabel(dayPlan) {
    if (!dayPlan) return 'No plan';
    if (dayPlan.changedFromConfirmed) return 'Changed';
    if (dayPlan.status === 'CONFIRMED') return 'Confirmed';
    if (dayPlan.status === 'GENERATED') return 'Ready for review';
    return dayPlan.status;
}

function statusClass(dayPlan) {
    if (!dayPlan) return styles.statusNeutral;
    if (dayPlan.changedFromConfirmed) return styles.statusChanged;
    if (dayPlan.status === 'CONFIRMED') return styles.statusConfirmed;
    if (dayPlan.status === 'GENERATED') return styles.statusReview;
    return styles.statusNeutral;
}

function canSkipItem(item) {
    if (!item || item.status === 'COMPLETED' || item.status === 'FREE_TIME') return false;
    if (item.taskTypeSnapshot !== 'FIXED') return true;
    return Boolean(item.occurrenceKey);
}

function skipLabelForItem(item) {
    return item.occurrenceKey ? 'Skip this occurrence' : 'Skip today';
}

function categorySoftStyle(category) {
    const stylesByCategory = {
        Work: { backgroundColor: '#e8f5e9', border: '#43a047', color: '#1b5e20' },
        Duty: { backgroundColor: '#e3f2fd', border: '#1e88e5', color: '#0d47a1' },
        Health: { backgroundColor: '#e0f7f4', border: '#00897b', color: '#00564d' },
        Social: { backgroundColor: '#fce4ec', border: '#d81b60', color: '#880e4f' },
        Sport: { backgroundColor: '#fff3e0', border: '#fb8c00', color: '#8a4b00' },
        Leisure: { backgroundColor: '#f3e5f5', border: '#8e24aa', color: '#4a148c' },
        Education: { backgroundColor: '#ede7f6', border: '#5e35b1', color: '#311b92' },
    };
    return stylesByCategory[category] || { backgroundColor: '#f3f4f6', border: '#6b7280', color: '#374151' };
}

function formatErrorMessage(err) {
    if (err?.response?.status) {
        if (err.response.status === 401) return ' Your session expired. Please log in again.';
        return ` Backend returned ${err.response.status}.`;
    }
    if (err?.message) return ` ${err.message}`;
    return '';
}

function localDateKey(date) {
    return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
    ].join('-');
}

function formatDayLabel(dateKey) {
    const [year, month, day] = dateKey.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString(undefined, {
        weekday: 'long',
        month: 'long',
        day: '2-digit',
    });
}

function formatDateTime(date) {
    return date.toLocaleString(undefined, { weekday: 'short', hour: '2-digit', minute: '2-digit' });
}

function formatTimeOnly(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

function formatMinutes(minutes) {
    if (!minutes) return '0m';
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    if (!hours) return `${rest}m`;
    return rest ? `${hours}h ${rest}m` : `${hours}h`;
}

function formatTaskType(type) {
    if (!type) return 'Task';
    return type.charAt(0) + type.slice(1).toLowerCase();
}

function formatItemStatus(status) {
    if (!status) return 'Planned';
    return status.split('_').map(part => part.charAt(0) + part.slice(1).toLowerCase()).join(' ');
}

function toCalendarTimeString(hour) {
    return `${String(hour).padStart(2, '0')}:00:00`;
}

function formatHour(hour) {
    return `${String(hour).padStart(2, '0')}:00`;
}

import { Fragment, useState } from 'react';
import { canonicalizeCategory } from '../../lib/categories';
import TravelTransitionNotice from './TravelTransitionNotice';
import styles from './dayPlanStyles.module.css';
import {
    canSkipItem,
    categorySoftStyle,
    formatItemStatus,
    formatTaskType,
    formatTimeOnly,
    skipLabelForItem,
} from './dayPlanUtils';

export default function TodayPlanTimeline({ items = [], transitions = [], empty, onComplete, onOpenDetails, onKeepFree, onSkip }) {
    if (!items.length) {
        return <p className={styles.emptyInline}>{empty || 'No tasks scheduled today.'}</p>;
    }

    return (
        <ol className={styles.timeline}>
            {items.map((item, index) => {
                const category = canonicalizeCategory(item.categorySnapshot);
                const color = categorySoftStyle(category);
                const isCompleted = item.status === 'COMPLETED';
                const isFreeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
                const transition = findTransition(transitions, item, items[index + 1]);
                return (
                    <Fragment key={item.id || `${item.startDateTime}-${item.titleSnapshot}`}>
                        <li
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
                        {transition && <TravelTransitionNotice transition={transition} />}
                    </Fragment>
                );
            })}
        </ol>
    );
}

function findTransition(transitions, from, to) {
    if (!to || !Array.isArray(transitions)) return null;
    return transitions.find(transition => (
        transition.fromDayPlanItemId === from.id && transition.toDayPlanItemId === to.id
    )) || null;
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

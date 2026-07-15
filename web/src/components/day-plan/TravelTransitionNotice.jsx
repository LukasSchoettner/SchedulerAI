import styles from './dayPlanStyles.module.css';

const WARNING_TONES = new Set(['INSUFFICIENT_TRAVEL_TIME', 'MISSING_LOCATION', 'UNKNOWN_TRAVEL_TIME']);

export default function TravelTransitionNotice({ transition }) {
    if (!transition) return null;
    const warningCode = transition.warningCode || '';
    const isWarning = WARNING_TONES.has(warningCode);
    const message = transition.warningMessage || fallbackMessage(warningCode);
    return (
        <li className={`${styles.travelNotice} ${isWarning ? styles.travelNoticeWarning : styles.travelNoticeOk}`}>
            <span className={styles.travelMarker}>{isWarning ? '!' : 'i'}</span>
            <div>
                <strong>{message}</strong>
                <p>
                    {formatMinutes(transition.availableMinutes)} available
                    {transition.estimatedTravelMinutes != null && ` - about ${formatMinutes(transition.estimatedTravelMinutes)} travel`}
                </p>
            </div>
        </li>
    );
}

function formatMinutes(value) {
    if (value == null) return 'unknown time';
    return `${value} min`;
}

function fallbackMessage(warningCode) {
    if (warningCode === 'SAME_LOCATION') return 'Same location: no travel time needed.';
    if (warningCode === 'MISSING_LOCATION') return 'Location missing: travel feasibility could not be checked.';
    if (warningCode === 'UNKNOWN_TRAVEL_TIME') return 'Travel time unknown.';
    return 'Travel feasibility checked.';
}

import styles from './dayPlanStyles.module.css';

const WARNING_TONES = new Set(['INSUFFICIENT_TRAVEL_TIME', 'MISSING_LOCATION', 'UNKNOWN_TRAVEL_TIME']);

export default function TravelTransitionNotice({ transition }) {
    if (!transition) return null;
    const warningCode = transition.warningCode || '';
    const isWarning = WARNING_TONES.has(warningCode);
    return (
        <li className={`${styles.travelNotice} ${isWarning ? styles.travelNoticeWarning : styles.travelNoticeOk}`}>
            <span className={styles.travelMarker}>{isWarning ? '!' : 'i'}</span>
            <div>
                <strong>{transition.warningMessage || 'Travel feasibility checked.'}</strong>
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

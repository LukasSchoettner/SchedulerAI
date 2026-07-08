export const DAYS = [
    { label: 'Mon', full: 'Monday', bit: 1 },
    { label: 'Tue', full: 'Tuesday', bit: 2 },
    { label: 'Wed', full: 'Wednesday', bit: 4 },
    { label: 'Thu', full: 'Thursday', bit: 8 },
    { label: 'Fri', full: 'Friday', bit: 16 },
    { label: 'Sat', full: 'Saturday', bit: 32 },
    { label: 'Sun', full: 'Sunday', bit: 64 },
];

export function explainPlanningWindow(window) {
    const days = formatDays(window.dayMask);
    const main = window.primaryCategory || 'selected tasks';
    const secondary = window.secondaryCategories || [];
    const strict = window.behaviorMode === 'STRICT';
    const placement = window.targetPlacementMode || 'ALLOW_ELSEWHERE';
    const urgent = strict && Number(window.priorityOverrideThreshold) === 5;
    const secondaryText = secondary.length
        ? `${secondary.join(', ')} tasks may also be placed here. `
        : '';
    const modeText = strict
        ? `Because this Planning Window is Strict, other categories should not use this time${urgent ? ' unless urgent override applies' : ''}. `
        : 'Because this Planning Window is Preferred, other important flexible tasks may still use this time if your day is crowded. ';
    const placementText = placementTextFor(main, placement);

    return {
        shortExplanation: `The scheduler will try to place ${main} tasks here first. ${placementText}`,
        detailedExplanation: `This Planning Window tells the scheduler to prefer ${main} tasks between ${window.startTime} and ${window.endTime} on ${days}. ${secondaryText}${modeText}${placementText} Fixed appointments are unchanged.`,
        cardSummary: `${days} · ${window.startTime}-${window.endTime}`,
    };
}

export function formatDays(dayMask) {
    if (dayMask === 127) return 'Every day';
    if (dayMask === 31) return 'Monday to Friday';
    if (dayMask === 96) return 'Saturday and Sunday';
    const selected = DAYS.filter(day => (dayMask & day.bit) !== 0).map(day => day.label);
    return selected.length ? selected.join(', ') : 'No days selected';
}

export function placementLabel(mode, category = 'tasks') {
    if (mode === 'PREFER_INSIDE_WINDOW') return `Prefer ${category} inside this window`;
    if (mode === 'KEEP_INSIDE_WINDOW') return `Keep ${category} inside this window`;
    return `Allow ${category} elsewhere if needed`;
}

function placementTextFor(category, mode) {
    if (mode === 'PREFER_INSIDE_WINDOW') {
        return `The scheduler should try to keep ${category} tasks in this window, but may move them if important or urgent.`;
    }
    if (mode === 'KEEP_INSIDE_WINDOW') {
        return `The scheduler should keep ${category} tasks inside this window whenever possible.`;
    }
    return `${category} tasks may also be scheduled elsewhere if this block is full.`;
}

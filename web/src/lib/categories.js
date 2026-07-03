export const BUILT_IN_CATEGORIES = [
    'Work',
    'Duty',
    'Health',
    'Social',
    'Sport',
    'Leisure',
    'Education',
];

export const CATEGORY_META = {
    Work: { color: '#2e7d32', description: 'Job and fixed work blocks' },
    Duty: { color: '#1565c0', description: 'Errands, chores, and obligations' },
    Health: { color: '#00897b', description: 'Health appointments and recovery' },
    Social: { color: '#c2185b', description: 'Friends, family, and social plans' },
    Sport: { color: '#ef6c00', description: 'Training and fitness' },
    Leisure: { color: '#6a1b9a', description: 'Free time and hobbies' },
    Education: { color: '#795548', description: 'Studying, school, and learning' },
};

const STORAGE_KEY = 'scheduler.customCategories';
const EDUCATION_ALIASES = new Set(['studying', 'study', 'school']);

export const canonicalizeCategory = (value) => {
    const trimmed = String(value || '').trim().replace(/\s+/g, ' ');
    if (!trimmed) return '';

    const lower = trimmed.toLowerCase();
    if (EDUCATION_ALIASES.has(lower)) return 'Education';

    const builtIn = BUILT_IN_CATEGORIES.find(category => category.toLowerCase() === lower);
    if (builtIn) return builtIn;

    const custom = getCustomCategories().find(category => category.toLowerCase() === lower);
    return custom || titleCase(trimmed);
};

export const getCustomCategories = () => {
    try {
        const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
        return Array.isArray(parsed)
            ? parsed.map(canonicalizeStoredCategory).filter(Boolean)
            : [];
    } catch {
        return [];
    }
};

export const getAllCategories = () => uniqueCategories([
    ...BUILT_IN_CATEGORIES,
    ...getCustomCategories(),
]);

export const saveCustomCategory = (value) => {
    const category = canonicalizeCategory(value);
    if (!category || BUILT_IN_CATEGORIES.some(item => item.toLowerCase() === category.toLowerCase())) {
        return category;
    }

    const customCategories = uniqueCategories([...getCustomCategories(), category]);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(customCategories));
    return category;
};

export const normalizeCategoryList = (categories = []) =>
    uniqueCategories(categories.map(canonicalizeCategory).filter(Boolean));

export const getCategoryMeta = (category) => {
    const normalized = canonicalizeCategory(category);
    return CATEGORY_META[normalized] || {
        color: '#546e7a',
        description: 'Custom scheduling category',
    };
};

const uniqueCategories = (categories) => {
    const seen = new Set();
    return categories.filter(category => {
        const key = category.toLowerCase();
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });
};

const canonicalizeStoredCategory = (value) => {
    const trimmed = String(value || '').trim().replace(/\s+/g, ' ');
    if (!trimmed) return '';

    const lower = trimmed.toLowerCase();
    if (EDUCATION_ALIASES.has(lower)) return 'Education';
    return titleCase(trimmed);
};

const titleCase = (value) =>
    value
        .split(' ')
        .map(part => part ? part[0].toUpperCase() + part.slice(1).toLowerCase() : '')
        .join(' ');

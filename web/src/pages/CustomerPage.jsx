import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import {
    canonicalizeCategory,
    getAllCategories,
    normalizeCategoryList,
    saveCustomCategory
} from '../lib/categories';
import styles from './CustomerPage.module.css';


const DAYS = [
    { label: 'Mon', bit: 1 << 0 },
    { label: 'Tue', bit: 1 << 1 },
    { label: 'Wed', bit: 1 << 2 },
    { label: 'Thu', bit: 1 << 3 },
    { label: 'Fri', bit: 1 << 4 },
    { label: 'Sat', bit: 1 << 5 },
    { label: 'Sun', bit: 1 << 6 }
];


function CustomerPage() {
    const navigate = useNavigate();
    const [mode, setMode] = useState('view'); // 'view' or 'edit'
    const [customer, setCustomer] = useState(null);
    const [formData, setFormData] = useState({
        customername: '',
        email: '',
        password: '',
        membershipLevel: ''
    });
    const [allZones, setAllZones] = useState([]);
    const [selectedZoneId, setSelectedZoneId] = useState(null);
    const [definitions, setDefinitions] = useState([]);
    const [editingDefinitionId, setEditingDefinitionId] = useState(null);

    const [zoneName, setZoneName] = useState('');
    const [newStartTime, setNewStartTime] = useState('08:00');
    const [newEndTime, setNewEndTime] = useState('18:00');

    // Definition form state
    const [defTitle, setDefTitle] = useState('');
    const [defDayMask, setDefDayMask] = useState(127);
    const [defStartTime, setDefStartTime] = useState('08:00');
    const [defEndTime, setDefEndTime] = useState('17:00');
    const [defAllowed, setDefAllowed] = useState([]);
    const [defExcluded, setDefExcluded] = useState([]);
    const [defPrimary, setDefPrimary] = useState('Work');
    const [defSecondary, setDefSecondary] = useState([]);
    const [defBehavior, setDefBehavior] = useState('STRICT');
    const [defPriority, setDefPriority] = useState('');
    const [categoryOptions, setCategoryOptions] = useState(getAllCategories());
    const [customCategory, setCustomCategory] = useState('');

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Load customer and zones
    const fetchData = async () => {
        try {
            const [custResp, zonesResp] = await Promise.all([
                api.get('/customers/me'),
                api.get('/customers/zones'),
            ]);
            setCustomer(custResp.data);
            setAllZones(zonesResp.data || []);
            // preload form data
            setFormData({
                customername: custResp.data.customername || '',
                email: custResp.data.email || '',
                password: '',
            });
        } catch (err) {
            console.error('Error loading data:', err);
            setError('Failed to load customer or zones.');
        }
    };

    // Update profile
    const handleProfileSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.put(
                `/customers/${customer.id}`,
                Object.fromEntries(Object.entries(formData).filter(([, value]) => value !== ''))
            );
            setMode('view');
            await fetchData();
        } catch (err) {
            console.error('Error updating profile:', err);
            setError('Failed to update profile.');
        }
    };

    // Zone CRUD functions
    const createZone = async () => {
        if (!zoneName.trim()) return;
        try {
            const payload = { name: zoneName, active: false, startTime: newStartTime, endTime: newEndTime };
            await api.post('/customers/zones', payload);
            setZoneName('');
            await fetchData();
        } catch (err) {
            console.error('Failed to create zone:', err);
            setError('Zone creation failed.');
        }
    };

    const activateZone = async (id) => {
        try {
            await api.put(`/customers/zones/${id}/activate`);
            await fetchData();
        } catch (err) {
            console.error('Failed to activate zone:', err);
            setError('Could not activate zone.');
        }
    };

    const deleteZone = async (id) => {
        try {
            await api.delete(`/customers/zones/${id}`);
            await fetchData();
            if (selectedZoneId === id) {
                setSelectedZoneId(null);
                setDefinitions([]);
            }
        } catch (err) {
            console.error('Failed to delete zone:', err);
            setError('Could not delete zone.');
        }
    };

    // Definition CRUD
    const fetchDefinitions = async (zoneId) => {
        try {
            const resp = await api.get(
                `/customers/zones/${zoneId}/definitions`,
            );
            const loadedDefinitions = resp.data || [];
            setDefinitions(loadedDefinitions);
            setCategoryOptions(normalizeCategoryList([
                ...getAllCategories(),
                ...loadedDefinitions.flatMap(def => [
                    def.primaryCategory,
                    ...(def.secondaryCategories || []),
                    ...(def.allowedCategories || []),
                    ...(def.excludedCategories || []),
                ].filter(Boolean)),
            ]));
        } catch (err) {
            console.error('Error fetching definitions:', err);
            setError('Failed to load definitions');
        }
    };

    const createDefinition = async () => {
        if (!selectedZoneId || !defTitle.trim()) return;
        try {
            const payload = definitionPayload();
            const request = editingDefinitionId
                ? api.put(`/customers/zones/${selectedZoneId}/definitions/${editingDefinitionId}`, payload)
                : api.post(`/customers/zones/${selectedZoneId}/definitions`, payload);

            await request;
            resetDefinitionForm();
            fetchDefinitions(selectedZoneId);
        } catch (err) {
            console.error('Error saving definition:', err);
            setError('Could not save definition');
        }
    };

    const definitionPayload = () => {
        const primary = canonicalizeCategory(defPrimary || 'Work');
        const secondary = normalizeCategoryList(defSecondary).filter(category => category !== primary);
        return {
                title: defTitle,
                dayMask: defDayMask,
                startTime: defStartTime,
                endTime: defEndTime,
                primaryCategory: primary,
                secondaryCategories: secondary,
                behaviorMode: defBehavior,
                allowedCategories: normalizeCategoryList([primary, ...secondary]),
                excludedCategories: normalizeCategoryList(defExcluded),
                priorityOverrideThreshold: defPriority === '' ? null : Number(defPriority),
        };
    };

    const resetDefinitionForm = () => {
        setEditingDefinitionId(null);
        setDefTitle('');
        setDefDayMask(127);
        setDefStartTime('08:00');
        setDefEndTime('17:00');
        setDefAllowed([]);
        setDefExcluded([]);
        setDefPrimary('Work');
        setDefSecondary([]);
        setDefBehavior('STRICT');
        setDefPriority('');
        setCustomCategory('');
    };

    const editDefinition = (def) => {
        setEditingDefinitionId(def.id);
        setDefTitle(def.title || '');
        setDefDayMask(def.dayMask ?? 127);
        setDefStartTime(def.startTime || '08:00');
        setDefEndTime(def.endTime || '17:00');
        setDefAllowed(normalizeCategoryList(def.allowedCategories || []));
        setDefExcluded(normalizeCategoryList(def.excludedCategories || []));
        setDefPrimary(def.primaryCategory || def.allowedCategories?.[0] || 'Work');
        setDefSecondary(normalizeCategoryList(def.secondaryCategories?.length ? def.secondaryCategories : (def.allowedCategories || []).slice(1)));
        setDefBehavior(def.behaviorMode || 'STRICT');
        setDefPriority(def.priorityOverrideThreshold ?? '');
    };

    const deleteDefinition = async (defId) => {
        if (!selectedZoneId) return;
        try {
            await api.delete(
                `/customers/zones/${selectedZoneId}/definitions/${defId}`,
            );
            if (editingDefinitionId === defId) resetDefinitionForm();
            fetchDefinitions(selectedZoneId);
        } catch (err) {
            console.error('Error deleting definition:', err);
            setError('Could not delete definition');
        }
    };

    const toggleDay = (bit) => {
        setDefDayMask(mask => (mask ^ bit)); // toggle bit
    };

    const selectedOptions = (event) =>
        Array.from(event.target.selectedOptions).map(option => option.value);

    const addCustomCategory = () => {
        const savedCategory = saveCustomCategory(customCategory);
        if (!savedCategory) return;

        setCategoryOptions(getAllCategories());
        setCustomCategory('');
    };

    const displayCategories = (categories = []) =>
        normalizeCategoryList(categories).join(', ') || 'Any category';

    const displayZoneTargets = (def) => {
        const primary = def.primaryCategory || def.allowedCategories?.[0] || 'Any';
        const secondary = def.secondaryCategories?.length
            ? def.secondaryCategories
            : (def.allowedCategories || []).slice(1);
        return `primary: ${primary}; secondary: ${displayCategories(secondary)}`;
    };

    useEffect(() => {
        fetchData().finally(() => setLoading(false));
    }, []);

    const onSelectZone = (zoneId) => {
        setSelectedZoneId(zoneId);
        resetDefinitionForm();
        fetchDefinitions(zoneId);
    };

    if (loading) return <p>Loading...</p>;
    if (error) return <p className={styles.error}>{error}</p>;

    return (

        <div className={styles.container}>
            <h2>Customer: {customer?.customername}</h2>
            <section className={styles.panel}>
                <div className={styles.sectionHeader}>
                    <div>
                        <h3>Profile</h3>
                        <p>{customer?.email} - {customer?.membershipLevel}</p>
                    </div>
                    <button onClick={() => setMode(mode === 'edit' ? 'view' : 'edit')}>
                        {mode === 'edit' ? 'Cancel' : 'Edit Profile'}
                    </button>
                    <button onClick={() => navigate('/onboarding/scheduling')}>
                        Scheduling Preferences
                    </button>
                </div>

                {mode === 'edit' && (
                    <form onSubmit={handleProfileSubmit} className={styles.profileForm}>
                        <input
                            value={formData.customername}
                            onChange={e => setFormData(prev => ({ ...prev, customername: e.target.value }))}
                            placeholder="Customer name"
                            required
                        />
                        <input
                            type="email"
                            value={formData.email}
                            onChange={e => setFormData(prev => ({ ...prev, email: e.target.value }))}
                            placeholder="Email"
                            required
                        />
                        <input
                            type="password"
                            value={formData.password}
                            onChange={e => setFormData(prev => ({ ...prev, password: e.target.value }))}
                            placeholder="New password (optional)"
                        />
                        <button type="submit">Save Profile</button>
                    </form>
                )}
            </section>
 

            <h3>Zone Configurations</h3>
            {allZones.length === 0 && <p>No zones created.</p>}
            {allZones.map(zone => (
                <div key={zone.id} className={styles.zoneBlock}>
                    <div>
                        <strong>{zone.name}</strong> ({zone.startTime} - {zone.endTime})
                        {zone.active ? (
                            <span className={`${styles.active} ${styles.ml}`}>(Active)</span>
                        ) : (

                            <button onClick={() => activateZone(zone.id)} className={styles.ml}>
                                Activate
                            </button>
                        )}
                        <button onClick={() => deleteZone(zone.id)} className={styles.ml}>

                            Delete
                        </button>
                        <button onClick={() => onSelectZone(zone.id)} className={styles.ml}>
                            Manage Definitions
                        </button>
                    </div>
                </div>
            ))}

            <h4>Create New Zone</h4>
            <div className={styles.zoneForm}>
                <input
                    type="text"
                    placeholder="Zone name"
                    value={zoneName}
                    onChange={e => setZoneName(e.target.value)}
                    className={styles.grow}
                />
                <input
                    type="time"
                    value={newStartTime}
                    onChange={e => setNewStartTime(e.target.value)}
                />
                <input
                    type="time"
                    value={newEndTime}
                    onChange={e => setNewEndTime(e.target.value)}
                />
                <button onClick={createZone} disabled={!zoneName.trim()}>
                    Create Zone
                </button>
            </div>

            {selectedZoneId && (
                <div className={styles.definitions}>
                    <h3>Definitions for Zone {selectedZoneId}</h3>
                    {definitions.length === 0 && <p>No definitions.</p>}
                    <ul className={styles.definitionList}>
                        {definitions.map(def => (
                            <li key={def.id} className={styles.definitionItem}>
                                {def.title}: {def.startTime} - {def.endTime}, mask {def.dayMask}
                                <span className={styles.categorySummary}>
                                    {displayZoneTargets(def)}
                                </span>
                                <span className={styles.categorySummary}>
                                    behavior: {def.behaviorMode || 'STRICT'}
                                </span>
                                <button onClick={() => editDefinition(def)} className={styles.ml}>
                                    Edit
                                </button>
                                <button onClick={() => deleteDefinition(def.id)} className={styles.ml}>
                                    Delete
                                </button>
                            </li>
                        ))}

                        <h4>{editingDefinitionId ? 'Edit Definition' : 'Add Definition'}</h4>
                        <div className={styles.defForm}>
                            <input
                                type="text"
                                placeholder="Title"
                                value={defTitle}
                                onChange={e => setDefTitle(e.target.value)}
                            />

                            <input
                                type="number"
                                placeholder="Day Mask"
                                value={defDayMask}
                                onChange={e => setDefDayMask(+e.target.value)}
                            />
                            <div className={styles.dayToggles}>
                                {DAYS.map(day => (
                                    <label key={day.label}>
                                        <input
                                            type="checkbox"
                                            checked={(defDayMask & day.bit) !== 0}
                                            onChange={() => toggleDay(day.bit)}
                                        />
                                        {day.label}
                                    </label>
                                ))}
                            </div>
                            <input
                                type="time"
                                value={defStartTime}
                                onChange={e => setDefStartTime(e.target.value)}
                            />
                            <input
                                type="time"
                                value={defEndTime}
                                onChange={e => setDefEndTime(e.target.value)}
                            />
                            <label className={styles.categorySelect}>
                                Primary category
                                <select
                                    value={canonicalizeCategory(defPrimary)}
                                    onChange={e => {
                                        setDefPrimary(e.target.value);
                                        setDefSecondary(prev => prev.filter(category => category !== e.target.value));
                                    }}
                                >
                                    {categoryOptions.map(category => (
                                        <option key={category} value={category}>{category}</option>
                                    ))}
                                </select>
                            </label>
                            <label className={styles.categorySelect}>
                                Secondary categories
                                <select
                                    multiple
                                    value={defSecondary.map(canonicalizeCategory)}
                                    onChange={e => setDefSecondary(selectedOptions(e).filter(category => category !== defPrimary))}
                                >
                                    {categoryOptions
                                        .filter(category => category !== defPrimary)
                                        .map(category => (
                                            <option key={category} value={category}>{category}</option>
                                        ))}
                                </select>
                            </label>
                            <label className={styles.categorySelect}>
                                Behavior
                                <select value={defBehavior} onChange={e => setDefBehavior(e.target.value)}>
                                    <option value="STRICT">Strict</option>
                                    <option value="PREFERRED">Preferred</option>
                                </select>
                            </label>
                            <label className={styles.categorySelect}>
                                Excluded categories
                                <select
                                    multiple
                                    value={defExcluded.map(canonicalizeCategory)}
                                    onChange={e => setDefExcluded(selectedOptions(e))}
                                >
                                    {categoryOptions.map(category => (
                                        <option key={category} value={category}>{category}</option>
                                    ))}
                                </select>
                            </label>
                            <div className={styles.customCategoryRow}>
                                <input
                                    type="text"
                                    placeholder="Custom category"
                                    value={customCategory}
                                    onChange={e => setCustomCategory(e.target.value)}
                                />
                                <button type="button" onClick={addCustomCategory}>
                                    Add
                                </button>
                            </div>
                            <input
                                type="number"
                                placeholder="Priority Override"
                                value={defPriority}
                                onChange={e => setDefPriority(e.target.value)}
                            />
                            <button
                                type="button"
                                onClick={() => {
                                    setDefPrimary('Education');
                                    setDefSecondary([]);
                                }}
                                className={styles.quickCategoryButton}
                            >
                                Education only
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    setDefPrimary('Sport');
                                    setDefSecondary([]);
                                }}
                                className={styles.quickCategoryButton}
                            >
                                Sport only
                            </button>
                            <button
                                onClick={createDefinition}
                                disabled={!defTitle.trim()}
                                className={styles.addDefButton}
                            >
                                {editingDefinitionId ? 'Save Definition' : 'Add Definition'}
                            </button>
                            {editingDefinitionId && (
                                <button type="button" onClick={resetDefinitionForm} className={styles.addDefButton}>
                                    Cancel Edit
                                </button>
                            )}
                        </div>
                    </ul>
                </div>
            )}
        </div>
    );
}

export default CustomerPage;

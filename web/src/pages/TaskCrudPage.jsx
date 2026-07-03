import { useEffect, useState } from 'react';
import api from '../lib/api';
import {
    canonicalizeCategory,
    getCategoryMeta,
    getAllCategories,
    normalizeCategoryList,
    saveCustomCategory
} from '../lib/categories';
import styles from './TaskCrudPage.module.css';
import LocationPicker from '../components/LocationPicker';

function TaskCrudPage() {
    const defaultTask = {
        title: '',
        type: 'FLEXIBLE',
        priority: 3,
        dueDate: new Date(Date.now() + 24 * 3600e3).toISOString().slice(0, 16),
        reminderDate: new Date(Date.now() + 2 * 3600e3).toISOString().slice(0, 16),
        status: 'PENDING',
        description: '',
        category: 'Work',
        recurrencePattern: 'NONE',
        estimatedDuration: 60,
        bufferTime: 10,
        taskNature: 'FIXED_ESTIMATE',
        minimalBlockSize: 30,
        maximalBlockSize: 120,
        progressive: false,
        canBeSeparated: true,
        earliestStartDateTime: '',
        latestEndDateTime: '',
        startDateTime: '',
        endDateTime: '',
        // NEW: routing-related fields
        addressId: null,     // optional link to routing-service Address
        addressText: ''      // human-readable address
    };

    const [newTask, setNewTask] = useState(defaultTask);
    const [editingTaskId, setEditingTaskId] = useState(null);
    const [categoryOptions, setCategoryOptions] = useState(getAllCategories());
    const [customCategory, setCustomCategory] = useState('');

    // separate state for location (lat/lon)
    const [location, setLocation] = useState({
        addressText: '',
        latitude: null,
        longitude: null,
        addressId: null, // if user picks an existing saved location later
    });

    const [tasks, setTasks] = useState([]);

    const generateDummy = async () => {
        try {
            await api.post('/tasks/generate-dummy?count=20');
            fetchTasks();
        } catch (err) {
            console.error('Failed to generate dummy tasks', err);
            alert('Could not generate tasks');
        }
    };

    const fetchTasks = async () => {
        try {
            const response = await api.get(`/tasks`);
            const loadedTasks = response.data || [];
            setTasks(loadedTasks);
            setCategoryOptions(normalizeCategoryList([
                ...getAllCategories(),
                ...loadedTasks.map(task => task.category),
            ]));
        } catch (err) {
            console.error('Error fetching tasks:', err);
            alert('Failed to fetch tasks');
        }
    };

    const createOrReuseAddress = async () => {
        // if no coordinates, don't create an address
        if (location.latitude == null || location.longitude == null) {
            return null;
        }

        // very simple: always create a new address
        // later you could de-duplicate by (customerId, lat/lon) or name
        const payload = {
            addressLine: location.addressText || 'Unnamed location',
            latitude: location.latitude,
            longitude: location.longitude,
        };

        const res = await api.post('/routing/addresses', payload);
        return res.data; // expect { id, addressLine, latitude, longitude }
    };

    const saveTask = async (e) => {
        e.preventDefault();
        try {
            let addressId = newTask.addressId;

            // if user chose a location but we have no addressId yet -> create it
            if (!addressId && location.latitude && location.longitude) {
                const addr = await createOrReuseAddress();
                if (addr) {
                    addressId = addr.id;
                }
            }

            const taskPayload = {
                ...newTask,
                category: canonicalizeCategory(newTask.category),
                addressId,
                addressText: location.addressText || newTask.addressText || null,
            };

            const cleanPayload = Object.fromEntries(
                Object.entries(taskPayload).map(([k, v]) => [k, v === '' ? null : v])
            );

            if (editingTaskId) {
                await api.put(`/tasks/${editingTaskId}`, cleanPayload);
            } else {
                await api.post(`/tasks`, cleanPayload);
            }
            resetTaskForm();
            fetchTasks();
        } catch (err) {
            console.error('Error saving task:', err);
            alert('Failed to save task');
        }
    };


    const deleteTask = async (id) => {
        try {
            await api.delete(`/tasks/${id}`);
            fetchTasks();
        } catch (err) {
            console.error('Error deleting task:', err);
            alert('Failed to delete task');
        }
    };

    const completeTask = async (id) => {
        try {
            await api.patch(`/tasks/${id}/status`, { status: 'COMPLETED' });
            fetchTasks();
        } catch (err) {
            console.error('Error completing task:', err);
            alert('Failed to complete task');
        }
    };

    const startEdit = (task) => {
        setEditingTaskId(task.id);
        setNewTask({
            ...defaultTask,
            ...task,
            category: canonicalizeCategory(task.category || defaultTask.category),
            dueDate: toLocalInputValue(task.dueDate),
            reminderDate: toLocalInputValue(task.reminderDate),
            earliestStartDateTime: toLocalInputValue(task.earliestStartDateTime),
            latestEndDateTime: toLocalInputValue(task.latestEndDateTime),
            startDateTime: toLocalInputValue(task.startDateTime),
            endDateTime: toLocalInputValue(task.endDateTime),
        });
        setLocation({
            addressText: task.addressText || '',
            latitude: null,
            longitude: null,
            addressId: task.addressId || null,
        });
    };

    const resetTaskForm = () => {
        setEditingTaskId(null);
        setNewTask(defaultTask);
        setCustomCategory('');
        setLocation({ addressText: '', latitude: null, longitude: null, addressId: null });
    };

    const toLocalInputValue = (value) => {
        if (!value) return '';
        return String(value).slice(0, 16);
    };

    useEffect(() => {
        fetchTasks();
    }, []);

    const handleChange = (field, value) =>
        setNewTask((prev) => ({ ...prev, [field]: value }));

    const currentCategoryValue = categoryOptions.some(
        option => option.toLowerCase() === String(newTask.category || '').toLowerCase()
    )
        ? canonicalizeCategory(newTask.category)
        : '__custom__';

    const addCustomCategory = () => {
        const savedCategory = saveCustomCategory(customCategory);
        if (!savedCategory) return;

        setCategoryOptions(getAllCategories());
        setNewTask(prev => ({ ...prev, category: savedCategory }));
        setCustomCategory('');
    };

    return (
        <div className={styles.container}>
            <h2>Task Management</h2>
            <button onClick={generateDummy} className={styles.generateBtn}>
                Generate dummy-tasks
            </button>

            <form onSubmit={saveTask} className={styles.form}>
                <h3>{editingTaskId ? 'Edit Task' : 'Create Task'}</h3>
                <div>
                    <label>Title</label>
                    <input
                        placeholder="Title"
                        value={newTask.title}
                        onChange={e => handleChange('title', e.target.value)}
                        required
                    />
                </div>

                <div>
                    <label>Type</label>
                    <select
                        value={newTask.type}
                        onChange={e => handleChange('type', e.target.value)}
                    >
                        <option value="FIXED">FIXED</option>
                        <option value="FLEXIBLE">FLEXIBLE</option>
                        <option value="PROJECT">PROJECT</option>
                    </select>
                </div>

                <div>
                    <label>Priority</label>
                    <input
                        type="number"
                        placeholder="Priority"
                        value={newTask.priority}
                        onChange={e => handleChange('priority', +e.target.value)}
                    />
                </div>

                <div>
                    <label>Due Date</label>
                    <input
                        type="datetime-local"
                        value={newTask.dueDate || ''}
                        onChange={e => handleChange('dueDate', e.target.value)}
                    />
                </div>

                <div>
                    <label>Reminder Date</label>
                    <input
                        type="datetime-local"
                        value={newTask.reminderDate || ''}
                        onChange={e => handleChange('reminderDate', e.target.value)}
                    />
                </div>

                <div>
                    <label>Category</label>
                    <select
                        value={currentCategoryValue}
                        onChange={e => {
                            if (e.target.value === '__custom__') {
                                handleChange('category', '');
                            } else {
                                handleChange('category', e.target.value);
                                setCustomCategory('');
                            }
                        }}
                    >
                        {categoryOptions.map(category => (
                            <option key={category} value={category}>{category}</option>
                        ))}
                        <option value="__custom__">Add custom category</option>
                    </select>
                    {currentCategoryValue === '__custom__' && (
                        <div className={styles.inlineControl}>
                            <input
                                value={customCategory}
                                onChange={e => setCustomCategory(e.target.value)}
                                placeholder="Custom category"
                            />
                            <button type="button" onClick={addCustomCategory}>
                                Add
                            </button>
                        </div>
                    )}
                </div>

                {/* NEW: optional address input */}
                <div>
                    <label>Address (optional)</label>
                    <input
                        placeholder="e.g. Main Street 12, Regensburg"
                        value={newTask.addressText || ''}
                        onChange={e => handleChange('addressText', e.target.value)}
                    />
                </div>

                {newTask.type === 'FIXED' && (
                    <>
                        <div>
                            <label>Start DateTime</label>
                            <input
                                type="datetime-local"
                                value={newTask.startDateTime || ''}
                                onChange={e => handleChange('startDateTime', e.target.value)}
                            />
                        </div>
                        <div>
                            <label>End DateTime</label>
                            <input
                                type="datetime-local"
                                value={newTask.endDateTime || ''}
                                onChange={e => handleChange('endDateTime', e.target.value)}
                            />
                        </div>
                    </>
                )}

                {newTask.type === 'FLEXIBLE' && (
                    <>
                        <div>
                            <label>Estimated Duration (min)</label>
                            <input
                                type="number"
                                value={newTask.estimatedDuration || ''}
                                onChange={e => handleChange('estimatedDuration', +e.target.value)}
                            />
                        </div>
                        <div>
                            <label>Task Nature</label>
                            <select
                                value={newTask.taskNature || 'FIXED_ESTIMATE'}
                                onChange={e => handleChange('taskNature', e.target.value)}
                            >
                                <option value="FIXED_ESTIMATE">FIXED_ESTIMATE</option>
                                <option value="OPEN_ENDED">OPEN_ENDED</option>
                            </select>
                        </div>
                        <div>
                            <label>Minimal Block Size (min)</label>
                            <input
                                type="number"
                                value={newTask.minimalBlockSize || ''}
                                onChange={e => handleChange('minimalBlockSize', +e.target.value)}
                            />
                        </div>
                        <div>
                            <label>Maximal Block Size (min)</label>
                            <input
                                type="number"
                                value={newTask.maximalBlockSize || ''}
                                onChange={e => handleChange('maximalBlockSize', +e.target.value)}
                            />
                        </div>
                        <div>
                            <label>Buffer Time (min)</label>
                            <input
                                type="number"
                                value={newTask.bufferTime || ''}
                                onChange={e => handleChange('bufferTime', +e.target.value)}
                            />
                        </div>
                        <div>
                            <label>Earliest Start</label>
                            <input
                                type="datetime-local"
                                value={newTask.earliestStartDateTime || ''}
                                onChange={e => handleChange('earliestStartDateTime', e.target.value)}
                            />
                        </div>
                        <div>
                            <label>Latest End</label>
                            <input
                                type="datetime-local"
                                value={newTask.latestEndDateTime || ''}
                                onChange={e => handleChange('latestEndDateTime', e.target.value)}
                            />
                        </div>
                        <div>
                            <label>
                                <input
                                    type="checkbox"
                                    checked={newTask.canBeSeparated || false}
                                    onChange={e => handleChange('canBeSeparated', e.target.checked)}
                                />
                                Can Be Separated
                            </label>
                        </div>
                        <div>
                            <label>
                                <input
                                    type="checkbox"
                                    checked={newTask.progressive || false}
                                    onChange={e => handleChange('progressive', e.target.checked)}
                                />
                                Progressive
                            </label>
                        </div>
                    </>
                )}
                <h3>Optional Location</h3>
                <LocationPicker
                    value={location}
                    onChange={(loc) => {
                        setLocation(loc);
                        // keep addressText in newTask for display / DTO consistency
                        setNewTask((prev) => ({
                            ...prev,
                            addressText: loc.addressText || prev.addressText,
                        }));
                    }}
                />
                <button type="submit">{editingTaskId ? 'Save Task' : 'Create Task'}</button>
                {editingTaskId && (
                    <button type="button" onClick={resetTaskForm}>Cancel Edit</button>
                )}
            </form>

            <ul className={styles.taskList}>
                {tasks.map(t => (
                    <li key={t.id} className={styles.taskItem}>
                        <div>
                            <div className={styles.taskTitleRow}>
                                <strong>{t.title}</strong>
                                <span
                                    className={styles.categoryBadge}
                                    style={{ backgroundColor: getCategoryMeta(t.category).color }}
                                    title={getCategoryMeta(t.category).description}
                                >
                                    {canonicalizeCategory(t.category) || 'Uncategorized'}
                                </span>
                            </div>
                            <span className={styles.taskMeta}>
                                {t.type}, p{t.priority}, {t.status}
                            </span>
                            {t.addressText && (
                                <> @ <span>{t.addressText}</span></>
                            )}
                        </div>
                        <div className={styles.taskActions}>
                            <button onClick={() => startEdit(t)}>Edit</button>
                            {t.status !== 'COMPLETED' && (
                                <button onClick={() => completeTask(t.id)}>Complete</button>
                            )}
                            <button onClick={() => deleteTask(t.id)}>Delete</button>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default TaskCrudPage;

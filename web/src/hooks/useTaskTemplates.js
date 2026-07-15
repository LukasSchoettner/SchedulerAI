import { useCallback, useEffect, useState } from 'react';
import api from '../lib/api';

export default function useTaskTemplates({ enabled = true } = {}) {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const loadTemplates = useCallback(async () => {
    if (!enabled) return [];
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/tasks/templates');
      const data = response.data || [];
      setTemplates(data);
      return data;
    } catch (err) {
      setError('Task templates could not be loaded.');
      return [];
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    if (enabled) loadTemplates();
  }, [enabled, loadTemplates]);

  const createTemplate = async (payload) => {
    setSaving(true);
    setError('');
    try {
      const response = await api.post('/tasks/templates', payload);
      await loadTemplates();
      return response.data;
    } catch (err) {
      setError('Task template could not be saved.');
      throw err;
    } finally {
      setSaving(false);
    }
  };

  const updateTemplate = async (id, payload) => {
    setSaving(true);
    setError('');
    try {
      const response = await api.put(`/tasks/templates/${id}`, payload);
      await loadTemplates();
      return response.data;
    } catch (err) {
      setError('Task template could not be updated.');
      throw err;
    } finally {
      setSaving(false);
    }
  };

  const deleteTemplate = async (id) => {
    setSaving(true);
    setError('');
    try {
      await api.delete(`/tasks/templates/${id}`);
      await loadTemplates();
    } catch (err) {
      setError('Task template could not be removed.');
      throw err;
    } finally {
      setSaving(false);
    }
  };

  const instantiateTemplate = async (id, payload = {}) => {
    setSaving(true);
    setError('');
    try {
      const response = await api.post(`/tasks/templates/${id}/instantiate`, payload);
      await loadTemplates();
      return response.data;
    } catch (err) {
      setError('Task could not be created from the template.');
      throw err;
    } finally {
      setSaving(false);
    }
  };

  return {
    templates,
    loading,
    saving,
    error,
    setError,
    loadTemplates,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    instantiateTemplate,
  };
}

export const TEMPLATE_ICON_LABELS = {
  shopping_cart: 'Shopping cart',
  laundry: 'Laundry',
  medication: 'Medication',
  work: 'Work',
  study: 'Study',
  walk: 'Walk',
  home: 'Home',
  health: 'Health',
  admin: 'Admin',
  email: 'Email',
  phone: 'Phone',
};

export const TEMPLATE_ICON_SYMBOLS = {
  shopping_cart: 'Cart',
  laundry: 'Wash',
  medication: 'Meds',
  work: 'Work',
  study: 'Study',
  walk: 'Walk',
  home: 'Home',
  health: 'Health',
  admin: 'Admin',
  email: 'Email',
  phone: 'Call',
};

export function templateIcon(template) {
  if (template?.icon && TEMPLATE_ICON_SYMBOLS[template.icon]) return template.icon;
  const category = String(template?.category || '').toLowerCase();
  if (category === 'duty') return 'admin';
  if (category === 'health') return 'health';
  if (category === 'sport') return 'walk';
  if (category === 'education') return 'study';
  if (category === 'work') return 'work';
  return 'home';
}

export function templateIconText(template) {
  const icon = templateIcon(template);
  return TEMPLATE_ICON_SYMBOLS[icon] || TEMPLATE_ICON_SYMBOLS.home;
}

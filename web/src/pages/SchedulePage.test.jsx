import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('SchedulePage source', () => {
  test('does not render duplicate header regenerate or confirm buttons', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/SchedulePage.jsx'), 'utf8');

    expect(source).not.toContain('className={styles.secondaryBtn} onClick={regenerateAndRefresh} disabled={loading}');
    expect(source).not.toContain('className={styles.primaryBtn} onClick={confirmDayPlan} disabled={loading}');
    expect(source).toContain('onRegenerate={regenerateAndRefresh}');
    expect(source).toContain('onConfirm={confirmDayPlan}');
  });

  test('opens timeline task details locally before offering edit navigation', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/SchedulePage.jsx'), 'utf8');

    expect(source).toContain('const openTaskDetails = async (item) =>');
    expect(source).toContain('api.get(`/tasks/${item.taskId}`)');
    expect(source).toContain('onOpenDetails={openTaskDetails}');
    expect(source).toContain('<TaskDetailsDrawer');
    expect(source).toContain("onEdit={() => navigate('/tasks')}");
    expect(source).not.toContain("onOpenDetails={() => navigate('/tasks')}");
  });
});

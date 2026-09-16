import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('HomePage source', () => {
  test('registered Quick Add regenerate action uses the visible replan flow', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/HomePage.jsx'), 'utf8');

    expect(source).toContain('regenerateTodayRef.current = () => replanFromNow();');
    expect(source).toContain('setEffectivePlanStart(start);');
  });

  test('follow-up reschedule sends selected remaining minutes for partly done tasks', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/HomePage.jsx'), 'utf8');

    expect(source).toContain(
      "remainingMinutes: reason === 'STARTED_NOT_FINISHED' ? Number(remainingMinutes) : undefined"
    );
    expect(source).toContain('await dayPlanState.rescheduleItem(followUpItem, payload);');
  });

  test('failed follow-up reschedule keeps the modal open with an error', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/HomePage.jsx'), 'utf8');

    expect(source).toContain('catch (err)');
    expect(source).toContain("setFollowUpError('Could not reschedule this task. Please try again.');");
    expect(source).toContain('{error && <p className={styles.followUpError}>{error}</p>}');
    expect(source).not.toContain('window.location.reload');
  });
});

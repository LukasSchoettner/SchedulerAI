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
});

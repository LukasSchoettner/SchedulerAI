import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('HomePage source', () => {
  test('registered Quick Add regenerate action uses the visible replan flow', () => {
    const source = readFileSync(join(process.cwd(), 'src/pages/HomePage.jsx'), 'utf8');

    expect(source).toContain('regenerateTodayRef.current = () => replanFromNow();');
    expect(source).toContain('setEffectivePlanStart(start);');
  });
});

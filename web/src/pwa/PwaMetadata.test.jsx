import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, test } from 'vitest';

describe('PWA metadata', () => {
  test('index contains generic manifest and theme-color metadata', () => {
    const html = fs.readFileSync(path.join(process.cwd(), 'index.html'), 'utf8');

    expect(html).toContain('<link rel="manifest" href="/manifest.webmanifest" />');
    expect(html).toContain('<meta name="theme-color" content="#1565c0" />');
    expect(html).not.toContain('apple-mobile-web-app');
  });

  test('manifest targets Android installability', () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(process.cwd(), 'public', 'manifest.webmanifest'), 'utf8')
    );

    expect(manifest.name).toBe('SchedulerAI');
    expect(manifest.start_url).toBe('/home');
    expect(manifest.display).toBe('standalone');
    expect(manifest.icons).toEqual(expect.arrayContaining([
      expect.objectContaining({ src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' }),
      expect.objectContaining({ src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' }),
      expect.objectContaining({ src: '/icons/maskable-icon-512.png', purpose: 'maskable' }),
    ]));
  });
});

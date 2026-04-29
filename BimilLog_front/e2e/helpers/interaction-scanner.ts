import type { Locator, Page } from '@playwright/test';

export interface InteractionTrigger {
  selector: string;
  category:
    | 'modal' | 'dropdown' | 'menu' | 'share-kakao' | 'toast-trigger'
    | 'inline-edit' | 'expand' | 'tab' | 'oauth' | 'unknown';
  label: string;
  pageUrl: string;
}

const SELECTORS = [
  '[data-testid*="modal"]',
  '[data-testid*="dialog"]',
  '[data-testid*="dropdown"]',
  '[data-testid*="menu"]',
  '[aria-haspopup="true"]',
  '[aria-haspopup="menu"]',
  '[aria-haspopup="dialog"]',
  '[data-testid*="share"]',
  'button[aria-label*="알림"]',
  'button[aria-label*="공유"]',
];

export async function scanInteractionTriggers(page: Page): Promise<InteractionTrigger[]> {
  const triggers: InteractionTrigger[] = [];
  const seen = new Set<string>();

  for (const sel of SELECTORS) {
    const all = page.locator(sel);
    const count = await all.count();
    for (let i = 0; i < count; i++) {
      const el = all.nth(i);
      const visible = await el.isVisible().catch(() => false);
      if (!visible) continue;

      const label = await getLabel(el);
      const category = classify(sel, label);
      const key = `${category}::${label}`;
      if (seen.has(key)) continue;
      seen.add(key);

      triggers.push({
        selector: sel,
        category,
        label,
        pageUrl: page.url(),
      });

      if (triggers.length >= 12) return triggers;
    }
  }
  return triggers;
}

async function getLabel(el: Locator): Promise<string> {
  return (
    (await el.getAttribute('aria-label')) ||
    (await el.getAttribute('data-testid')) ||
    ((await el.textContent()) ?? '').trim() ||
    'unknown'
  );
}

function classify(sel: string, label: string): InteractionTrigger['category'] {
  if (sel.includes('modal') || sel.includes('dialog') || sel.includes('aria-haspopup="dialog"')) return 'modal';
  if (sel.includes('dropdown') || sel.includes('aria-haspopup="menu"')) return 'dropdown';
  if (sel.includes('menu')) return 'menu';
  if (sel.includes('share') || /카카오/.test(label)) return 'share-kakao';
  if (/알림/.test(label)) return 'dropdown';
  if (sel.includes('aria-haspopup="true"')) return 'menu';
  return 'unknown';
}

import type { APIRequestContext } from '@playwright/test';

const BACKEND = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface SeedHealth {
  ok: boolean;
  details: Record<string, unknown>;
}

/**
 * baseline 시드가 충분한지 백엔드에 가볍게 질의해 확인.
 * 부족한 경우 e2e-agent 가 seed-request.md 작성 → 메인 → backend-agent 시드 모드.
 */
export async function checkBaselineSeed(request: APIRequestContext): Promise<SeedHealth> {
  const res = await request.get(`${BACKEND}/api/post?page=0&size=10`);
  if (!res.ok()) {
    return { ok: false, details: { status: res.status(), reason: 'backend down or 4xx/5xx' } };
  }
  const json = await res.json();
  const total = json?.data?.totalElements ?? 0;
  return {
    ok: total >= 10,
    details: { totalPosts: total },
  };
}

/**
 * 시드 부족을 발견했을 때 e2e-result.md 옆에 seed-request.md 생성용 페이로드.
 */
export function seedRequestFor(reason: string, missing: string[]): string {
  return [
    '# Seed Request',
    '',
    `**Reason:** ${reason}`,
    '',
    '## Missing data',
    ...missing.map((m) => `- ${m}`),
    '',
    '## Resolution',
    '메인은 이 파일을 backend-agent 에 전달 (시드 모드).',
  ].join('\n');
}

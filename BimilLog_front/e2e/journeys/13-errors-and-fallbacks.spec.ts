import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-13-errors-and-fallbacks';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 background polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

/**
 * NotFoundView 마커 — RETURN 도장 / 봉투 / 액션
 * 다중 마커 OR 평가 (라운드 13 메타포 표준 검증).
 */
async function detectNotFoundMarkers(page: Page) {
  const headingVisible = await page
    .getByRole('heading', { level: 1, name: /주소가 잘못된 편지|롤링페이퍼를 찾을 수 없|편지는 주인만 열어볼 수 있/ })
    .first()
    .isVisible({ timeout: 3000 })
    .catch(() => false);

  // 봉투 SVG (currentColor) — fill-paper-50 클래스로 식별
  const envelopeSvgVisible = await page
    .locator('svg rect.fill-paper-50, svg rect.dark\\:fill-paper-card')
    .first()
    .isVisible({ timeout: 1500 })
    .catch(() => false);

  // RETURN 또는 PRIVATE 도장 텍스트 (text 요소 안)
  const stampLabelVisible = await page
    .locator('text=/반송\\s*RETURN|PRIVATE|수신인 없음|배달 실패\\s*FAILED/')
    .first()
    .isVisible({ timeout: 1500 })
    .catch(() => false);

  // 홈으로 돌아가기 액션
  const homeActionVisible = await page
    .getByRole('link', { name: /홈으로 돌아가기/ })
    .first()
    .isVisible({ timeout: 1500 })
    .catch(() => false);

  // h1 sr-only 또는 시각 h1
  const h1Count = await page.locator('h1').count().catch(() => 0);

  return { headingVisible, envelopeSvgVisible, stampLabelVisible, homeActionVisible, h1Count };
}

/**
 * round-13-errors-and-fallbacks iter-1
 *  - 비인증 컨텍스트 (시드 토큰 없음).
 *  - 라운드 13 핵심: NotFoundView / ErrorView / EmptyView 3종 신설 + 18 functional bug 회귀.
 *  - 메타포 통일 (RETURN 도장, paper-aged + stamp-red ring) 검증.
 *  - fail-soft: 모든 step 매트릭스 캡처 확보.
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-not-found-global              /존재안하는경로 → 글로벌 NotFoundView (RETURN 도장)
 *  step-02-not-found-domain-paper        /rolling-paper/존재안함 → 도메인 inline → NotFoundView (수신인 없음 도장)
 *  step-03-admin-redirect-toast          /admin (비인증) → 토스트 + redirect 회귀
 *  step-04-mypage-protected-redirect     /mypage (비인증) → /login redirect (또는 빈 데이터 화면)
 *  step-05-board-post-not-found          /board/post/9999 → 도메인 not-found 또는 NotFoundView
 *  step-06-empty-popular-papers          / 홈 인기 페이퍼 빈/에러 EmptyView (시드 부족 fallback)
 *  step-07-mobile-not-found-grid         모바일 RETURN 도장 액션 grid + truncate (F-13-BUG-20)
 *  step-08-dark-tokens                   다크 토큰 (paper-aged + stamp-red ring + 봉투 currentColor)
 */
test.describe('round-13-errors-and-fallbacks', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /존재안하는경로 → 글로벌 NotFoundView (RETURN 도장)
  //  - app/not-found.tsx → <NotFoundView /> 렌더 회귀
  //  - 봉투 SVG (currentColor) + RETURN 도장 + 4개 액션
  //  - <h1> 1개 보장 (WCAG 1.3.1) — F-13-020
  // ============================================================
  test('step-01-not-found-global', async ({ page }, testInfo) => {
    await page.goto('/this-route-does-not-exist-13');
    await waitSettled(page, 1500);

    const markers = await detectNotFoundMarkers(page);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-not-found-global',
    );

    test.info().annotations.push({
      type: 'not-found-markers',
      description: JSON.stringify(markers, null, 2),
    });

    // sanity — 페이지 최소 마운트
    expect(page.url()).toContain('this-route-does-not-exist-13');
  });

  // ============================================================
  // step-02: 도메인 not-found — /rolling-paper/존재안함
  //  - RollingPaperContainer.isError → NotFoundView
  //  - stampCode="404" stampLabel="수신인 없음"
  //  - 글로벌과 동일 메타포 회귀 (F-13-BUG-6)
  // ============================================================
  test('step-02-not-found-domain-paper', async ({ page }, testInfo) => {
    // 시드 데이터 부족 환경 — 어떤 닉네임이든 isError 분기로 빠질 가능성 높음.
    // 동시에 backend 가 정상이면 isLoading 후 isError 까지 retry 도 발생할 수 있어
    // 충분히 wait 한다 (TanStack Query 기본 retry 2회 + 백오프 — F-13-BUG-11).
    await page.goto('/rolling-paper/e2e-unknown-user-13');
    await waitSettled(page, 3500);

    const markers = await detectNotFoundMarkers(page);
    const targetNickRendered = await page
      .locator('text=/e2e-unknown-user-13/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // "수신인 없음" 도메인 라벨 검증 (글로벌 RETURN 과 차별)
    const domainLabelVisible = await page
      .locator('text=/수신인 없음/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 친구 찾기 / 내 페이지 추가 액션 검증
    const findFriendVisible = await page
      .getByRole('link', { name: /친구 찾기/ })
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-not-found-domain-paper',
    );

    test.info().annotations.push({
      type: 'domain-not-found',
      description: JSON.stringify(
        { ...markers, targetNickRendered, domainLabelVisible, findFriendVisible },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-03: /admin 비인증 → 토스트 + redirect (F-13-BUG-1)
  //  - AdminClient: !isAdmin → showWarning 토스트 + router.push("/")
  //  - redirectedRef 가드로 토스트 1회만
  //  - 시드 부족 시 isAuthLoading=false → !isAdmin → 즉시 redirect 분기
  // ============================================================
  test('step-03-admin-redirect-toast', async ({ page }, testInfo) => {
    await page.goto('/admin');
    // 토스트가 떠있는 짧은 순간을 캡처 — redirect 후 토스트는 timeout 으로 사라짐
    await page.waitForTimeout(800);

    const isOnHome = page.url().endsWith('/') || page.url().match(/\/[\?#]?$/) !== null;
    const adminToastVisible = await page
      .locator('text=/관리자 전용 페이지|관리자만 접근할 수 있어요/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 토스트 직후 캡처
    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-admin-redirect-toast',
    );

    // 토스트가 사라진 후 redirect 결과 화면 추가 캡처는 step-03 단일 step 에서 통합 — 토스트 분기만 annotation
    test.info().annotations.push({
      type: 'admin-redirect',
      description: JSON.stringify(
        { url: page.url(), isOnHome, adminToastVisible },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-04: /mypage 비인증 → /login redirect (또는 빈 데이터)
  //  - F-13-BUG-2 보류 상태 (라운드 13 범위 외) — client-side useAuth redirect 만 작동
  //  - 현재 환경에서 /login 으로 가는지, /mypage 빈 데이터 화면인지 분기 캡처
  // ============================================================
  test('step-04-mypage-protected-redirect', async ({ page }, testInfo) => {
    await page.goto('/mypage');
    await waitSettled(page, 1800);

    const url = page.url();
    const onLogin = url.includes('/login');
    const onMypage = url.includes('/mypage');

    // 로그인 페이지 헤더 또는 마이페이지 텍스트
    const loginHeading = await page
      .getByRole('heading', { name: /로그인|간편 로그인|환영합니다/ })
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    const mypageBody = await page
      .locator('text=/마이페이지|내 정보|내가 받은 편지/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-mypage-protected-redirect',
    );

    test.info().annotations.push({
      type: 'protected-redirect',
      description: JSON.stringify(
        { url, onLogin, onMypage, loginHeading, mypageBody },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: /board/post/9999 → 404 또는 NotFoundView
  //  - 존재하지 않는 게시글 ID 진입
  //  - Next.js notFound() 또는 도메인 inline 에러 분기
  // ============================================================
  test('step-05-board-post-not-found', async ({ page }, testInfo) => {
    await page.goto('/board/post/9999999');
    await waitSettled(page, 3500);

    const markers = await detectNotFoundMarkers(page);

    // 도메인 인라인 분기 또는 글로벌 not-found 마커
    const anyNotFoundCue = await page
      .locator(
        'text=/주소가 잘못된 편지|찾을 수 없|존재하지 않|편지가 도착하지 못했어요|삭제되었거나/',
      )
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-board-post-not-found',
    );

    test.info().annotations.push({
      type: 'post-not-found',
      description: JSON.stringify({ ...markers, anyNotFoundCue, url: page.url() }, null, 2),
    });
  });

  // ============================================================
  // step-06: 홈 인기 페이퍼 EmptyView 메타포 (F-13-BUG-16)
  //  - PopularPapersSection: 빈/에러 분기 EmptyView 회귀
  //  - paper-aged + dashed stamp-red 보더 + MailOpen
  //  - 시드 부족 → 빈 상태 또는 에러 상태
  // ============================================================
  test('step-06-empty-popular-papers', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 2500);

    // 인기 페이퍼 섹션 스크롤
    const section = page.locator('[data-testid="popular-papers-section"]').first();
    const sectionVisible = await section
      .isVisible({ timeout: 3000 })
      .catch(() => false);
    if (sectionVisible) {
      await section.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(400);
    }

    // 빈/에러 상태 마커
    const emptyTextVisible = await page
      .locator(
        'text=/아직 인기 편지함이 없어요|인기 편지함을 불러오지 못했어요|롤링페이퍼 둘러보기/',
      )
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    const retryWrapperVisible = await page
      .locator('[data-testid="popular-papers-retry"]')
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-empty-popular-papers',
    );

    test.info().annotations.push({
      type: 'popular-papers-empty',
      description: JSON.stringify(
        { sectionVisible, emptyTextVisible, retryWrapperVisible },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-07: 모바일 NotFoundView 액션 grid + truncate (F-13-BUG-20)
  //  - 모바일(375px) 4개 액션 그리드 텍스트 잘림 회귀
  //  - 데스크톱은 동일 페이지 캡처 (라이트/다크 양쪽 비교)
  // ============================================================
  test('step-07-mobile-not-found-grid', async ({ page }, testInfo) => {
    await page.goto('/this-mobile-route-not-exist-13');
    await waitSettled(page, 1500);

    const vp = viewportFromProjectName(testInfo.project.name);

    // 액션 버튼들 텍스트 잘림 검증 — getBoundingClientRect 로 확인
    const homeBtn = page.getByRole('link', { name: /홈으로 돌아가기/ }).first();
    const homeVisible = await homeBtn.isVisible({ timeout: 1500 }).catch(() => false);

    let homeBtnHeight = 0;
    if (homeVisible) {
      homeBtnHeight = await homeBtn
        .evaluate((el) => el.getBoundingClientRect().height)
        .catch(() => 0);
    }

    // 게시판 / 롤링페이퍼 보조 액션
    const boardLink = page.getByRole('link', { name: /^게시판$/ }).first();
    const paperLink = page.getByRole('link', { name: /^롤링페이퍼$/ }).first();
    const boardVisible = await boardLink.isVisible({ timeout: 800 }).catch(() => false);
    const paperVisible = await paperLink.isVisible({ timeout: 800 }).catch(() => false);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-not-found-grid',
    );

    test.info().annotations.push({
      type: 'mobile-grid',
      description: JSON.stringify(
        { vp, homeVisible, homeBtnHeight, boardVisible, paperVisible },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-08: 다크 토큰 (paper-aged + stamp-red ring + 봉투 currentColor)
  //  - 라이트/다크 양쪽에서 동일 위치 캡처 → 토큰 일관성 비교
  //  - F-13-BUG-5 — SVG 색상이 currentColor / fill-paper-50 토큰화 검증
  //  - F-13-007 — ErrorFallback / NotFoundView paper 토큰화
  // ============================================================
  test('step-08-dark-tokens', async ({ page }, testInfo) => {
    await page.goto('/this-route-is-for-dark-token-13');
    await waitSettled(page, 1500);

    const theme = themeFromProjectName(testInfo.project.name);

    // 봉투 rect 의 fill 클래스 추출 — 토큰화 회귀
    const envelopeFillClass = await page
      .locator('svg rect[rx="3"]')
      .first()
      .getAttribute('class')
      .catch(() => null);

    // 도장 텍스트 (stamp-red fill) — RETURN 라벨 확인
    const stampTextVisible = await page
      .locator('text=/반송\\s*RETURN/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // h1 의 ink/foreground 토큰
    const h1ColorClass = await page
      .locator('h1')
      .first()
      .getAttribute('class')
      .catch(() => null);

    // 본문 paper background
    const bodyHasPaperBg = await page
      .locator('div.bg-paper, div.min-h-screen.bg-paper')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-dark-tokens',
    );

    test.info().annotations.push({
      type: 'dark-tokens',
      description: JSON.stringify(
        { theme, envelopeFillClass, stampTextVisible, h1ColorClass, bodyHasPaperBg },
        null,
        2,
      ),
    });
  });
});

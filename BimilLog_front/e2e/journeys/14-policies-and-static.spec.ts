import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-14-policies-and-static';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 이 background polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

/**
 * round-14-policies-and-static iter-1
 *  - 비인증 컨텍스트 (시드 토큰 없음).
 *  - 라운드 14 핵심: 정적 페이지 (/privacy, /terms, /install) 마이그레이션.
 *  - 22 functional bug 회귀 + 4종 공통 컴포넌트 신설:
 *      PolicyDocument / PolicyTOC / InstallGuideRouter / useBrowserDetection
 *  - paper-aged + ink + stamp-red 토큰 일관, 인쇄 친화 @media print, ToC.
 *  - fail-soft: 모든 step 매트릭스 캡처 확보.
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-privacy-base                 /privacy 진입 (PolicyDocument + ToC + 인장 헤더)
 *  step-02-terms-base                   /terms 진입 (intro 보강 + 시행일자)
 *  step-03-install-base                 /install 진입 (Hero/Benefits SSR + InstallGuideRouter desktop)
 *  step-04-toc-anchor-scroll            ToC 클릭 → 앵커 스크롤 (URL 해시 갱신)
 *  step-05-mobile-toc-details           모바일 ToC <details> 토글
 *  step-06-dark-tokens                  다크 paper-aged + stamp-red dark:text-stamp-red/90
 *  step-07-print-preview-sim            인쇄 emulate(print) — @media print + .no-print
 *  step-08-external-link-target-rel     외부 링크 target/rel 검증 (mailto/playstore)
 */
test.describe('round-14-policies-and-static', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /privacy 진입 (PolicyDocument + ToC)
  //  - LegalDocumentHeader (paper-aged + 공식 도장)
  //  - 14개 섹션 anchor (#section-1 ~ #section-14) + scroll-mt-24
  //  - ToC sticky sidebar (lg+) / details (lg-)
  //  - text-ink dark:text-foreground 본문 + break-keep
  //  - 시행일자 + 개정 이력 푸터 (F-14-BUG-8)
  //  - 책임자 박스 paper-aged 토큰화 (F-14-BUG-3)
  // ============================================================
  test('step-01-privacy-base', async ({ page }, testInfo) => {
    await page.goto('/privacy');
    await waitSettled(page, 1500);

    // h1 = "개인정보 처리방침" 가시성
    const titleVisible = await page
      .getByRole('heading', { level: 1, name: /개인정보 처리방침/ })
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // ToC 데스크톱 sidebar 또는 모바일 details
    const tocSidebarVisible = await page
      .locator('aside[aria-label="목차"]')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    const tocDetailsVisible = await page
      .locator('details:has(summary:has-text("목차 보기"))')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // section anchor 가 14개 존재하는지 (data-policy-section)
    const sectionCount = await page
      .locator('section[data-policy-section]')
      .count()
      .catch(() => 0);

    // 섹션 헤더 stamp-red 톤 — text-stamp-red 클래스 존재
    const firstH2Class = await page
      .locator('section[data-policy-section] h2')
      .first()
      .getAttribute('class')
      .catch(() => null);

    // 책임자 박스 검증 — paper-aged 토큰 (F-14-BUG-3)
    const officerBoxPaperAged = await page
      .locator('div.bg-paper-aged, div.dark\\:bg-paper-card')
      .filter({ hasText: /개인정보 보호 책임자|책임자/ })
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-privacy-base',
    );

    test.info().annotations.push({
      type: 'privacy-base',
      description: JSON.stringify(
        {
          titleVisible,
          tocSidebarVisible,
          tocDetailsVisible,
          sectionCount,
          firstH2Class,
          officerBoxPaperAged,
          url: page.url(),
        },
        null,
        2,
      ),
    });

    // sanity
    expect(page.url()).toContain('/privacy');
  });

  // ============================================================
  // step-02: /terms 진입 (intro 보강 + 시행일자 + ToC)
  //  - 8개 섹션 (article-1 ~ article-8)
  //  - intro: "비밀로그 사용 시 지켜야 할 규칙입니다…" (F-14-010)
  //  - 시행일자: 2025년 6월 26일 (F-14-BUG-8)
  //  - server component (use client 없음)
  // ============================================================
  test('step-02-terms-base', async ({ page }, testInfo) => {
    await page.goto('/terms');
    await waitSettled(page, 1500);

    const titleVisible = await page
      .getByRole('heading', { level: 1, name: /이용약관/ })
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // intro 문구
    const introVisible = await page
      .locator('text=/지켜야 할 규칙/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 시행일자 박스
    const effectiveDateVisible = await page
      .locator('text=/시행일자.*2025/')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 섹션 카운트
    const sectionCount = await page
      .locator('section[data-policy-section]')
      .count()
      .catch(() => 0);

    // 본문 폐기 토큰 잔존 검증 (F-14-BUG-2 회귀)
    const brandPrimaryCount = await page
      .locator('.text-brand-primary')
      .count()
      .catch(() => 0);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-terms-base',
    );

    test.info().annotations.push({
      type: 'terms-base',
      description: JSON.stringify(
        {
          titleVisible,
          introVisible,
          effectiveDateVisible,
          sectionCount,
          brandPrimaryCount,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-03: /install 진입 (InstallGuideRouter — 기본 desktop)
  //  - Hero: h1 "비밀로그를 앱으로 설치하세요" SSR 렌더 (F-14-BUG-18)
  //  - InstallGuideRouter — Playwright UA = 데스크톱 Chromium → desktop 분기
  //  - desktop: PWA 설치 + 플레이스토어 새 탭 (F-14-BUG-10/13)
  //  - 자체 <header> 제거 → AuthHeader 만 (F-14-BUG-1)
  //  - PWAInstallButton 페이지 직접 호출 0 (F-14-BUG-11)
  // ============================================================
  test('step-03-install-base', async ({ page }, testInfo) => {
    await page.goto('/install');
    await waitSettled(page, 2500);

    const heroH1Visible = await page
      .getByRole('heading', { level: 1, name: /앱으로 설치하세요/ })
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // SSR 렌더 — Benefits 섹션 4개 카드
    const benefitsHeading = await page
      .getByRole('heading', { name: /앱으로 설치하면 더 좋은 점/ })
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 디바이스 분기 카드 — desktop / android / ios / inapp 중 하나
    const desktopGuideVisible = await page
      .locator('#desktop-guide')
      .first()
      .isVisible({ timeout: 4000 })
      .catch(() => false);

    const androidGuideVisible = await page
      .locator('#android-guide')
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    const inappGuideVisible = await page
      .locator('#inapp-guide')
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    const iosSafariVisible = await page
      .locator('#ios-safari-guide')
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    const iosChromeVisible = await page
      .locator('#ios-chrome-guide')
      .first()
      .isVisible({ timeout: 800 })
      .catch(() => false);

    // 헤더 중복 검증 — install 페이지 자체 <header> 제거 (F-14-BUG-1)
    const headerCount = await page
      .locator('header')
      .count()
      .catch(() => 0);

    // PWAInstallButton 직접 호출 0 — install 페이지 trap 차단 (F-14-BUG-11)
    const directPwaButtonCount = await page
      .locator('button:has-text("앱으로 설치"), button:has-text("설치하기")')
      .count()
      .catch(() => 0);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-install-base',
    );

    test.info().annotations.push({
      type: 'install-base',
      description: JSON.stringify(
        {
          heroH1Visible,
          benefitsHeading,
          desktopGuideVisible,
          androidGuideVisible,
          inappGuideVisible,
          iosSafariVisible,
          iosChromeVisible,
          headerCount,
          directPwaButtonCount,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-04: ToC 클릭 → 앵커 스크롤 (URL 해시 갱신)
  //  - data-policy-section + scroll-mt-24 회귀 (F-14-BUG-7/F-14-012)
  //  - PolicyTOC handleClick → smooth scroll + history.replaceState
  //  - 데스크톱: aside sidebar 의 8번째 항목 클릭
  //  - 모바일: details 펼친 후 클릭
  // ============================================================
  test('step-04-toc-anchor-scroll', async ({ page }, testInfo) => {
    await page.goto('/privacy');
    await waitSettled(page, 1500);

    const vp = viewportFromProjectName(testInfo.project.name);

    // 모바일이면 details 먼저 펼치기
    if (vp === 'mobile-375') {
      const summary = page
        .locator('details summary:has-text("목차 보기")')
        .first();
      const summaryVisible = await summary
        .isVisible({ timeout: 1500 })
        .catch(() => false);
      if (summaryVisible) {
        await summary.click().catch(() => undefined);
        await page.waitForTimeout(400);
      }
    }

    // ToC 항목 클릭 — section-8 (회사 후반부)
    const tocLink = page.locator('a[href="#section-8"]').first();
    const tocLinkVisible = await tocLink
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    let urlHashAfter = '';
    let scrolledY = 0;
    let activeAfter = false;

    if (tocLinkVisible) {
      await tocLink.click().catch(() => undefined);
      await page.waitForTimeout(800); // smooth scroll
      urlHashAfter = await page.evaluate(() => window.location.hash);
      scrolledY = await page.evaluate(() => window.scrollY);

      // active highlight — text-stamp-red 클래스 적용
      const activeClass = await page
        .locator('a[href="#section-8"]')
        .first()
        .getAttribute('class')
        .catch(() => null);
      activeAfter = !!activeClass && activeClass.includes('stamp-red');
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-toc-anchor-scroll',
    );

    test.info().annotations.push({
      type: 'toc-anchor',
      description: JSON.stringify(
        { vp, tocLinkVisible, urlHashAfter, scrolledY, activeAfter },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: 모바일 ToC <details> 펼침/닫힘 (F-14-011)
  //  - details summary 클릭 → open 속성 토글
  //  - 데스크톱은 details 가 lg:hidden 으로 숨김 — 그래도 캡처
  // ============================================================
  test('step-05-mobile-toc-details', async ({ page }, testInfo) => {
    await page.goto('/privacy');
    await waitSettled(page, 1500);

    const vp = viewportFromProjectName(testInfo.project.name);

    const details = page
      .locator('details:has(summary:has-text("목차 보기"))')
      .first();
    const detailsExists = (await details.count()) > 0;

    let detailsOpenAfter = false;
    let summaryClicked = false;

    if (detailsExists) {
      const summary = details.locator('summary').first();
      const summaryVisible = await summary
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      if (summaryVisible) {
        await summary.click().catch(() => undefined);
        await page.waitForTimeout(400);
        summaryClicked = true;
        detailsOpenAfter = await details
          .evaluate((el) => (el as HTMLDetailsElement).open)
          .catch(() => false);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-mobile-toc-details',
    );

    test.info().annotations.push({
      type: 'mobile-toc',
      description: JSON.stringify(
        { vp, detailsExists, summaryClicked, detailsOpenAfter },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-06: 다크 토큰 (paper-aged + ink + stamp-red dark:text-stamp-red/90)
  //  - F-14-BUG-2/3/21 — 폐기 토큰 0 + paper-aged 박스 + 다크 인장 보존
  //  - h2 헤더 클래스에 stamp-red 명시 (라이트/다크 양쪽)
  //  - 본문 text-ink dark:text-foreground 표준
  // ============================================================
  test('step-06-dark-tokens', async ({ page }, testInfo) => {
    await page.goto('/privacy');
    await waitSettled(page, 1800);

    const theme = themeFromProjectName(testInfo.project.name);

    // 폐기 토큰 잔존 0 — F-14-BUG-2
    const brandPrimaryCount = await page.locator('.text-brand-primary').count().catch(() => 0);
    const brandMutedCount = await page.locator('.text-brand-muted').count().catch(() => 0);

    // h2 stamp-red 클래스 잔존
    const h2WithStampRedCount = await page
      .locator('section[data-policy-section] h2.text-stamp-red')
      .count()
      .catch(() => 0);

    // 본문 ink 토큰 잔존 — text-ink 클래스 카운트
    const inkClassCount = await page.locator('.text-ink').count().catch(() => 0);

    // paper-aged 박스 잔존
    const paperAgedCount = await page.locator('.bg-paper-aged').count().catch(() => 0);

    // 페이지 배경 — paper-card 카드 외곽
    const paperCardVisible = await page
      .locator('.bg-paper-card')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 첫 h2 의 색 (computed) 가 다크 모드에서도 빨간 톤인지 (rgb(204,51,51) 근처)
    const h2Color = await page
      .locator('section[data-policy-section] h2')
      .first()
      .evaluate((el) => window.getComputedStyle(el).color)
      .catch(() => '');

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-dark-tokens',
    );

    test.info().annotations.push({
      type: 'dark-tokens',
      description: JSON.stringify(
        {
          theme,
          brandPrimaryCount,
          brandMutedCount,
          h2WithStampRedCount,
          inkClassCount,
          paperAgedCount,
          paperCardVisible,
          h2Color,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-07: 인쇄 미리보기 시뮬 (@media print)
  //  - emulateMedia({ media: 'print' }) → globals.css @media print 트리거
  //  - .no-print 의 display:none / 외부 링크 footnote / paper 평탄화
  //  - F-14-BUG-4 회귀
  //  - 스킵 가능 — emulateMedia 미지원 환경 graceful fallback
  // ============================================================
  test('step-07-print-preview-sim', async ({ page }, testInfo) => {
    await page.goto('/privacy');
    await waitSettled(page, 1500);

    let emulated = false;
    try {
      await page.emulateMedia({ media: 'print' });
      emulated = true;
      await page.waitForTimeout(400);
    } catch (e) {
      emulated = false;
    }

    // .no-print 요소가 인쇄 시 hidden — display 검증
    const tocSidebar = page.locator('aside[aria-label="목차"]').first();
    const tocSidebarExists = (await tocSidebar.count()) > 0;

    let tocHidden = false;
    if (tocSidebarExists) {
      tocHidden = await tocSidebar
        .evaluate((el) => window.getComputedStyle(el).display === 'none')
        .catch(() => false);
    }

    // 헤더 .no-print 적용 — BaseLayout AuthHeader
    const authHeader = page.locator('header').first();
    const headerExists = (await authHeader.count()) > 0;

    let headerHidden = false;
    if (headerExists) {
      // 인쇄 시 .no-print 가 적용되어 display:none
      headerHidden = await authHeader
        .evaluate((el) => {
          const cs = window.getComputedStyle(el);
          // 인쇄 모드에서 hidden 처리되거나 부모가 hidden
          return cs.display === 'none' || cs.visibility === 'hidden';
        })
        .catch(() => false);
    }

    // 본문 카드 paper 토큰이 print 시 흰 평탄화 — bg-paper-card 의 background-color
    const paperCardBg = await page
      .locator('.bg-paper-card')
      .first()
      .evaluate((el) => window.getComputedStyle(el).backgroundColor)
      .catch(() => '');

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-print-preview-sim',
    );

    // 미디어 emulate 복원
    if (emulated) {
      await page.emulateMedia({ media: 'screen' }).catch(() => undefined);
    }

    test.info().annotations.push({
      type: 'print-preview',
      description: JSON.stringify(
        { emulated, tocSidebarExists, tocHidden, headerExists, headerHidden, paperCardBg },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-08: 외부 링크 target/rel 검증
  //  - /privacy: mailto: + grow-farm <Link href="/"> (F-14-BUG-5/6)
  //  - /install: 플레이스토어 target="_blank" rel="noopener noreferrer" (F-14-BUG-13)
  // ============================================================
  test('step-08-external-link-target-rel', async ({ page }, testInfo) => {
    // 1) /privacy — mailto + grow-farm 링크
    await page.goto('/privacy');
    await waitSettled(page, 1500);

    // 책임자 박스로 스크롤
    const officerBox = page
      .locator('div')
      .filter({ hasText: /개인정보 보호 책임자/ })
      .first();
    const officerExists = (await officerBox.count()) > 0;
    if (officerExists) {
      await officerBox.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(400);
    }

    const mailtoHref = await page
      .locator('a[href^="mailto:"]')
      .first()
      .getAttribute('href')
      .catch(() => null);

    // grow-farm 링크 존재 (F-14-BUG-5)
    const growFarmLinkCount = await page
      .locator('a[href="/"]:has-text("grow-farm.com"), a:has-text("grow-farm.com")')
      .count()
      .catch(() => 0);

    // 2) /install — 플레이스토어 새 탭 (F-14-BUG-13)
    await page.goto('/install');
    await waitSettled(page, 2500);

    // 디바이스 분기 카드 로드 대기 (data-testid 없으므로 #desktop-guide 또는 #android-guide)
    const guideLoaded = await page
      .locator('#desktop-guide, #android-guide, #inapp-guide, #ios-safari-guide, #ios-chrome-guide')
      .first()
      .isVisible({ timeout: 4000 })
      .catch(() => false);

    const playstoreLink = page
      .locator('a[href*="play.google.com"], a[href*="PLAY_STORE"]')
      .first();
    const playstoreVisible = await playstoreLink
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    let playstoreTarget: string | null = null;
    let playstoreRel: string | null = null;
    if (playstoreVisible) {
      playstoreTarget = await playstoreLink.getAttribute('target').catch(() => null);
      playstoreRel = await playstoreLink.getAttribute('rel').catch(() => null);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-external-link-target-rel',
    );

    test.info().annotations.push({
      type: 'external-link-rel',
      description: JSON.stringify(
        {
          mailtoHref,
          growFarmLinkCount,
          guideLoaded,
          playstoreVisible,
          playstoreTarget,
          playstoreRel,
        },
        null,
        2,
      ),
    });
  });
});

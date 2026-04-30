import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-15-blacklist-management';
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

type ProtectedBranch = 'blacklist' | 'login' | 'unknown';

/**
 * fail-soft 진입 헬퍼.
 *  /blacklist 는 (protected) 라우트 그룹. 비인증 시 미들웨어/RSC 가 /login 으로 리다이렉트.
 *   - blacklist 분기: BlacklistManager 의 EmptyView/리스트/페이지네이션/접근성/paper 토큰 검증 가능
 *   - login 분기: 로그인 화면 paper 토큰/카피 회귀 검증 가능
 *  분기 종류는 annotation 에 남겨 디자인 리뷰 시 참고.
 */
async function gotoBlacklist(page: Page): Promise<ProtectedBranch> {
  await page.goto('/blacklist');
  await waitSettled(page, 1500);

  const finalUrl = page.url();
  if (/\/blacklist/.test(finalUrl)) return 'blacklist';
  if (/\/login/.test(finalUrl)) return 'login';
  return 'unknown';
}

/**
 * round-15-blacklist-management iter-1
 *  - 비인증 컨텍스트 (시드 토큰 없음).
 *  - 라운드 15 핵심: 블랙리스트 관리 — 라운드 9 친구 패턴 (옵티미스틱 + 토스트 액션 +
 *    useConfirmModal + 44px) + 라운드 13 EmptyView 표준 + 라운드 5 undo 토스트 +
 *    라운드 1 paper 토큰을 일괄 차용.
 *  - 18 functional bug 회귀:
 *      F-15-BUG-1 DELETE URL 정리 + 응답 활용 (Page<BlacklistDTO> setQueryData)
 *      F-15-BUG-2 옵티미스틱 + previous 백업 + 실패 시 복원
 *      F-15-BUG-3 "다시 차단" undo 토스트 (라운드 5 패턴)
 *      F-15-BUG-4 window.confirm → useConfirmModal
 *      F-15-BUG-5/14 paper/postal-navy/stamp-red/ink/ink-soft 토큰 일괄
 *      F-15-BUG-6 빈 상태 EmptyView (MailX, "차단한 발신인이 없어요")
 *      F-15-BUG-7 에러 상태 EmptyView assertive (ShieldOff)
 *      F-15-BUG-8 페이지네이션 nav aria-label + 44px + aria-live
 *      F-15-BUG-9 헤더 "총 N명" 카운트
 *      F-15-BUG-10 formatRelativeDate + <time dateTime> 시맨틱
 *      F-15-BUG-11 옵티미스틱으로 카드 즉시 사라짐 → pending state 분리 불필요
 *      F-15-BUG-12 aria-hidden / aria-label / <ul aria-label> / <time>
 *      F-15-BUG-13 페이지 변경 후 listRef.current?.focus()
 *      F-15-BUG-15 useBlacklistCheck 100건 한계 + 409 친화 카피
 *      F-15-BUG-16 (protected)/blacklist/page.tsx 그라데이션 → bg-paper-50
 *      F-15-BUG-17 isLoading && !blacklistData 가드로 단순화
 *      F-15-BUG-18 Spinner color="failure"
 *  - fail-soft: /login 분기에서도 매트릭스 캡처 확보.
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-blacklist-entry              /blacklist 진입 base — branch (blacklist/login)
 *  step-02-empty-state-emptyview        EmptyView 빈 상태 — MailX + "차단한 발신인이 없어요"
 *  step-03-list-or-empty                차단 카드 리스트 (시드 부족 시 빈 상태) + Avatar 이니셜
 *  step-04-confirm-modal-metaphor       useConfirmModal 메타포 ("차단 해제" + UserCheck stroke-postal-navy)
 *  step-05-dark-tokens                  paper/postal-navy/stamp-red 다크 토큰 일관
 *  step-06-mobile-sticky-short-label    모바일 sticky + 단축 라벨 (sm:inline)
 *  step-07-pagination-44px-focus        페이지네이션 44px + nav aria-label + focus 이동
 *  step-08-aria-semantic                ARIA — role=list / time semantic / aria-label
 */
test.describe('round-15-blacklist-management', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 시드 토큰 없는 fail-soft 검증.
    // /blacklist 는 (protected) 라 /login 으로 리다이렉트 될 수 있음.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /blacklist 진입 base — branch (blacklist/login)
  // ============================================================
  test('step-01-blacklist-entry', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    // protected 가드를 통과한 경우 BlacklistManager 또는 EmptyView 가 마운트
    let managerVisible = false;
    let loginFormVisible = false;

    if (branch === 'blacklist') {
      managerVisible = await page
        .getByRole('heading', { level: 1, name: /받지 않는 발신인/ })
        .first()
        .isVisible({ timeout: 4000 })
        .catch(() => false);
    } else if (branch === 'login') {
      loginFormVisible = await page
        .locator('button:has-text("카카오"), button:has-text("로그인")')
        .first()
        .isVisible({ timeout: 4000 })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-blacklist-entry',
    );

    test.info().annotations.push({
      type: 'blacklist-entry',
      description: JSON.stringify(
        {
          branch,
          url: page.url(),
          managerVisible,
          loginFormVisible,
        },
        null,
        2,
      ),
    });

    // sanity — protected 라 login 으로 가도 OK
    expect(['blacklist', 'login']).toContain(branch);
  });

  // ============================================================
  // step-02: 빈 상태 EmptyView — F-15-BUG-6
  //  - 시드 부족 시 (비인증/0건) 빈 상태 카피 검증
  //  - "차단한 발신인이 없어요" + 보조 설명 + MailX 아이콘
  //  - role="status" 또는 role="alert" 자동 (EmptyView 내부)
  //  - login 분기 시: login 화면이 paper 토큰 적용됐는지 보조 캡처
  // ============================================================
  test('step-02-empty-state-emptyview', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    let emptyTitleVisible = false;
    let emptyDescriptionPresent = false;
    let mailXIconCount = 0;
    let shieldOffIconCount = 0;
    let emptyViewRole: string | null = null;

    if (branch === 'blacklist') {
      // EmptyView 의 title — "차단한 발신인이 없어요"
      emptyTitleVisible = await page
        .locator('text=/차단한 발신인이 없어요/')
        .first()
        .isVisible({ timeout: 3000 })
        .catch(() => false);

      // 보조 설명 — "롤링페이퍼에서 받기 싫은 편지가 오면..."
      emptyDescriptionPresent = await page
        .locator('text=/받기 싫은 편지|발신인을 차단/')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      // MailX 아이콘 (lucide-react SVG class) — w-9 h-9 strokeWidth=1.6
      mailXIconCount = await page
        .locator('svg.lucide-mail-x, svg[class*="lucide-mail"]')
        .count()
        .catch(() => 0);

      shieldOffIconCount = await page
        .locator('svg.lucide-shield-off, svg[class*="lucide-shield"]')
        .count()
        .catch(() => 0);

      // EmptyView wrapper role — role="status" (default) 또는 "alert" (assertive)
      emptyViewRole = await page
        .locator('[role="status"], [role="alert"]')
        .first()
        .getAttribute('role')
        .catch(() => null);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-empty-state-emptyview',
    );

    test.info().annotations.push({
      type: 'empty-state',
      description: JSON.stringify(
        {
          branch,
          emptyTitleVisible,
          emptyDescriptionPresent,
          mailXIconCount,
          shieldOffIconCount,
          emptyViewRole,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-03: 차단 카드 리스트 — F-15-BUG-9/10/12
  //  - <ul aria-label="블랙리스트 목록" tabIndex="-1">
  //  - 카드: Avatar 이니셜 + 닉네임 + <time dateTime> + "차단 해제" 버튼
  //  - 헤더 "총 N명" 카운트 (응답에 totalElements 가 있을 때)
  //  - 시드 부족 시 빈 상태 캡처 (이전 step-02 와 동일하지만 헤더/마크업 검증)
  // ============================================================
  test('step-03-list-or-empty', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    let h1Visible = false;
    let h1Class: string | null = null;
    let subTextVisible = false;
    let listExists = false;
    let listAriaLabel: string | null = null;
    let listTabIndex: string | null = null;
    let listItemCount = 0;
    let totalCountVisible = false;
    let avatarCount = 0;
    let timeTagCount = 0;

    if (branch === 'blacklist') {
      h1Visible = await page
        .getByRole('heading', { level: 1, name: /받지 않는 발신인/ })
        .first()
        .isVisible({ timeout: 4000 })
        .catch(() => false);

      h1Class = await page
        .getByRole('heading', { level: 1, name: /받지 않는 발신인/ })
        .first()
        .getAttribute('class')
        .catch(() => null);

      // 헤더 부제 (paper 메타포) — "차단한 사람은..."
      subTextVisible = await page
        .locator('text=/차단한 사람은 회원님의 롤링페이퍼/')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      // <ul aria-label="블랙리스트 목록"> 존재
      const ul = page.locator('ul[aria-label="블랙리스트 목록"]').first();
      listExists = (await ul.count()) > 0;

      if (listExists) {
        listAriaLabel = await ul.getAttribute('aria-label').catch(() => null);
        listTabIndex = await ul.getAttribute('tabindex').catch(() => null);
        listItemCount = await ul.locator('li').count().catch(() => 0);
        avatarCount = await ul.locator('img, [data-testid="flowbite-avatar"], div[class*="avatar"]').count().catch(() => 0);
        timeTagCount = await ul.locator('time[datetime]').count().catch(() => 0);
      }

      // "총 N명" 텍스트
      totalCountVisible = await page
        .locator('text=/총 \\d+명/')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-list-or-empty',
    );

    test.info().annotations.push({
      type: 'list-or-empty',
      description: JSON.stringify(
        {
          branch,
          h1Visible,
          h1Class,
          subTextVisible,
          listExists,
          listAriaLabel,
          listTabIndex,
          listItemCount,
          totalCountVisible,
          avatarCount,
          timeTagCount,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-04: useConfirmModal 메타포 — F-15-BUG-4/14
  //  - 카드의 "차단 해제" 클릭 → useConfirmModal 열림
  //  - 카피 "차단 해제 / {nickname}님 차단을 풀어드릴까요? 다시 편지를 받을 수 있어요."
  //  - 아이콘 <UserCheck stroke-postal-navy>
  //  - 시드 부족 시 모달 못 띄움 → 빈 상태 캡처로 fail-soft
  // ============================================================
  test('step-04-confirm-modal-metaphor', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    let removeBtnVisible = false;
    let modalVisible = false;
    let modalTitleVisible = false;
    let modalMessageVisible = false;
    let userCheckIconCount = 0;
    let cancelBtnText: string | null = null;
    let confirmBtnText: string | null = null;

    if (branch === 'blacklist') {
      // 첫 카드의 차단 해제 버튼 — aria-label="...님 차단 해제"
      const removeBtn = page
        .locator('button[aria-label*="차단 해제"]')
        .first();
      removeBtnVisible = await removeBtn
        .isVisible({ timeout: 2000 })
        .catch(() => false);

      if (removeBtnVisible) {
        await removeBtn.click().catch(() => undefined);
        await page.waitForTimeout(700);

        // ConfirmModal 가시성 — Flowbite Modal popup
        modalTitleVisible = await page
          .locator('text=/차단 해제/')
          .first()
          .isVisible({ timeout: 2000 })
          .catch(() => false);

        modalMessageVisible = await page
          .locator('text=/차단을 풀어드릴까요|다시 편지를 받을 수 있어요/')
          .first()
          .isVisible({ timeout: 1500 })
          .catch(() => false);

        // UserCheck 아이콘 — stroke-postal-navy 클래스
        userCheckIconCount = await page
          .locator('svg.lucide-user-check, svg[class*="lucide-user-check"]')
          .count()
          .catch(() => 0);

        cancelBtnText = await page
          .locator('button:has-text("돌아가기"), button:has-text("취소")')
          .first()
          .textContent()
          .catch(() => null);

        confirmBtnText = await page
          .locator('button:has-text("차단 해제"):not([aria-label])')
          .first()
          .textContent()
          .catch(() => null);

        modalVisible = modalTitleVisible || modalMessageVisible;
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-confirm-modal-metaphor',
    );

    // 모달이 열린 경우 닫기 (다음 step 영향 방지)
    if (modalVisible) {
      const cancelBtn = page
        .locator('button:has-text("돌아가기"), button:has-text("취소")')
        .first();
      if (await cancelBtn.isVisible({ timeout: 600 }).catch(() => false)) {
        await cancelBtn.click().catch(() => undefined);
        await page.waitForTimeout(300);
      }
    }

    test.info().annotations.push({
      type: 'confirm-modal',
      description: JSON.stringify(
        {
          branch,
          removeBtnVisible,
          modalVisible,
          modalTitleVisible,
          modalMessageVisible,
          userCheckIconCount,
          cancelBtnText,
          confirmBtnText,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: 다크 토큰 — F-15-BUG-5/16
  //  - paper/postal-navy/stamp-red/ink/ink-soft 토큰 일관
  //  - 폐기 토큰 잔존 0 (text-gray-900, bg-white, bg-gray-50, text-red-600 직접 색)
  //  - bg-paper-50 페이지 배경 (page.tsx) + bg-paper-card 리스트 + border-postal-navy/20
  //  - h1 ShieldOff text-stamp-red 잔존
  // ============================================================
  test('step-05-dark-tokens', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);
    const theme = themeFromProjectName(testInfo.project.name);

    let paperBgPresent = false;
    let paperCardPresent = false;
    let postalNavyBorderPresent = false;
    let stampRedTextPresent = false;
    let inkTextPresent = false;
    let inkSoftPresent = false;
    let bgWhiteCount = 0;
    let textGray900Count = 0;
    let pageBg = '';
    let h1Color = '';

    if (branch === 'blacklist') {
      // 페이지 배경 — bg-paper-50
      paperBgPresent = await page
        .locator('.bg-paper-50, main.bg-paper-50, div.bg-paper-50')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      // 리스트 카드 컨테이너 — bg-paper-card
      paperCardPresent = await page
        .locator('.bg-paper-card')
        .first()
        .isVisible({ timeout: 1000 })
        .catch(() => false);

      // border-postal-navy/20
      postalNavyBorderPresent = (await page.locator('[class*="border-postal-navy"]').count()) > 0;

      // text-stamp-red — h1 ShieldOff
      stampRedTextPresent = (await page.locator('[class*="text-stamp-red"], [class*="stamp-red"]').count()) > 0;

      // text-ink / text-ink-soft
      inkTextPresent = (await page.locator('[class*="text-ink"]').count()) > 0;
      inkSoftPresent = (await page.locator('[class*="text-ink-soft"]').count()) > 0;

      // 폐기 토큰 잔존 0 — bg-white / text-gray-900 (헤더/카드 직접 색은 0이어야 함)
      // 단, flowbite 내부 컴포넌트가 bg-white 를 쓸 수 있으니 BlacklistManager 트리 안에서만 카운트
      const blacklistTree = page.locator('div').filter({ has: page.getByRole('heading', { level: 1, name: /받지 않는 발신인/ }) });
      bgWhiteCount = await blacklistTree.locator('.bg-white').count().catch(() => 0);
      textGray900Count = await blacklistTree.locator('.text-gray-900').count().catch(() => 0);

      // computed page background
      pageBg = await page
        .locator('main, div.bg-paper-50, body')
        .first()
        .evaluate((el) => window.getComputedStyle(el).backgroundColor)
        .catch(() => '');

      // h1 color (ShieldOff 옆 텍스트는 ink, 아이콘은 stamp-red)
      h1Color = await page
        .getByRole('heading', { level: 1, name: /받지 않는 발신인/ })
        .first()
        .evaluate((el) => window.getComputedStyle(el).color)
        .catch(() => '');
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-dark-tokens',
    );

    test.info().annotations.push({
      type: 'dark-tokens',
      description: JSON.stringify(
        {
          branch,
          theme,
          paperBgPresent,
          paperCardPresent,
          postalNavyBorderPresent,
          stampRedTextPresent,
          inkTextPresent,
          inkSoftPresent,
          bgWhiteCount,
          textGray900Count,
          pageBg,
          h1Color,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-06: 모바일 sticky + 단축 라벨 — F-15-012
  //  - BlacklistListItem 의 차단 해제 버튼 라벨 — "차단 해제" 가 sm:inline 으로 모바일 hidden
  //  - 모바일에서는 Trash2 아이콘 + aria-label 만, 데스크톱에서는 텍스트 함께
  //  - 헤더 "총 N명" shrink-0
  // ============================================================
  test('step-06-mobile-sticky-short-label', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);
    const vp = viewportFromProjectName(testInfo.project.name);

    let removeBtnExists = false;
    let removeBtnAriaLabel: string | null = null;
    let labelHiddenOnMobile = false;
    let buttonMinHeight = '';
    let buttonMinWidth = '';

    if (branch === 'blacklist') {
      const removeBtn = page.locator('button[aria-label*="차단 해제"]').first();
      removeBtnExists = (await removeBtn.count()) > 0;

      if (removeBtnExists) {
        removeBtnAriaLabel = await removeBtn.getAttribute('aria-label').catch(() => null);

        // span.hidden.sm:inline — 모바일에서는 display:none, 데스크톱은 inline
        const labelSpan = removeBtn.locator('span.hidden, span[class*="hidden"]').first();
        if ((await labelSpan.count()) > 0) {
          const display = await labelSpan
            .evaluate((el) => window.getComputedStyle(el).display)
            .catch(() => '');
          labelHiddenOnMobile = display === 'none';
        }

        // 44px 터치 타겟
        const dim = await removeBtn
          .evaluate((el) => {
            const cs = window.getComputedStyle(el);
            return { minH: cs.minHeight, minW: cs.minWidth };
          })
          .catch(() => ({ minH: '', minW: '' }));
        buttonMinHeight = dim.minH;
        buttonMinWidth = dim.minW;
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-mobile-sticky-short-label',
    );

    test.info().annotations.push({
      type: 'mobile-short-label',
      description: JSON.stringify(
        {
          branch,
          vp,
          removeBtnExists,
          removeBtnAriaLabel,
          labelHiddenOnMobile,
          buttonMinHeight,
          buttonMinWidth,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-07: 페이지네이션 44px + focus 이동 — F-15-BUG-8/13
  //  - <nav aria-label="블랙리스트 페이지">
  //  - <Button min-h-[44px]> 이전 / 다음
  //  - <span aria-live="polite" aria-label="현재 N페이지, 총 M페이지">
  //  - 페이지 변경 후 listRef.current?.focus()
  //  - 시드 부족 (빈 상태) 시 nav 미렌더 → 빈 상태 캡처
  // ============================================================
  test('step-07-pagination-44px-focus', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    let navExists = false;
    let navAriaLabel: string | null = null;
    let prevBtnMinHeight = '';
    let nextBtnMinHeight = '';
    let pageCounterAriaLive: string | null = null;
    let pageCounterAriaLabel: string | null = null;
    let listFocusedAfter = false;

    if (branch === 'blacklist') {
      const nav = page.locator('nav[aria-label="블랙리스트 페이지"]').first();
      navExists = (await nav.count()) > 0;

      if (navExists) {
        navAriaLabel = await nav.getAttribute('aria-label').catch(() => null);

        const prevBtn = nav.locator('button:has-text("이전")').first();
        const nextBtn = nav.locator('button:has-text("다음")').first();

        prevBtnMinHeight = await prevBtn
          .evaluate((el) => window.getComputedStyle(el).minHeight)
          .catch(() => '');
        nextBtnMinHeight = await nextBtn
          .evaluate((el) => window.getComputedStyle(el).minHeight)
          .catch(() => '');

        const pageCounter = nav.locator('[aria-live="polite"]').first();
        if ((await pageCounter.count()) > 0) {
          pageCounterAriaLive = await pageCounter.getAttribute('aria-live').catch(() => null);
          pageCounterAriaLabel = await pageCounter.getAttribute('aria-label').catch(() => null);
        }

        // "다음" 클릭 시도 — disabled 가 아니면 페이지 이동 + listRef.focus()
        const isDisabled = await nextBtn.getAttribute('disabled').catch(() => null);
        if (isDisabled === null) {
          await nextBtn.click().catch(() => undefined);
          await page.waitForTimeout(800);

          // 활성 요소가 ul (블랙리스트 목록) 인지
          listFocusedAfter = await page
            .evaluate(() => {
              const ae = document.activeElement;
              return ae?.tagName === 'UL' && ae?.getAttribute('aria-label') === '블랙리스트 목록';
            })
            .catch(() => false);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-pagination-44px-focus',
    );

    test.info().annotations.push({
      type: 'pagination',
      description: JSON.stringify(
        {
          branch,
          navExists,
          navAriaLabel,
          prevBtnMinHeight,
          nextBtnMinHeight,
          pageCounterAriaLive,
          pageCounterAriaLabel,
          listFocusedAfter,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-08: ARIA — F-15-BUG-12
  //  - <ul aria-label="블랙리스트 목록"> tabIndex=-1
  //  - h1 옆 <ShieldOff aria-hidden="true">
  //  - 카드 <time dateTime={createdAt}> 시맨틱
  //  - 차단 해제 버튼 aria-label
  //  - aria-live="polite" 페이지 카운터
  //  - 빈 상태 EmptyView role="status" / 에러 role="alert"
  // ============================================================
  test('step-08-aria-semantic', async ({ page }, testInfo) => {
    const branch = await gotoBlacklist(page);

    let listAriaLabel: string | null = null;
    let listTabIndex: string | null = null;
    let h1IconAriaHidden: string | null = null;
    let timeTagSamples: string[] = [];
    let removeBtnAriaLabels: string[] = [];
    let ariaLiveCount = 0;
    let statusRoleCount = 0;
    let alertRoleCount = 0;

    if (branch === 'blacklist') {
      const ul = page.locator('ul[aria-label="블랙리스트 목록"]').first();
      const ulExists = (await ul.count()) > 0;
      if (ulExists) {
        listAriaLabel = await ul.getAttribute('aria-label').catch(() => null);
        listTabIndex = await ul.getAttribute('tabindex').catch(() => null);
      }

      // h1 의 ShieldOff 아이콘 — h1 첫 자식 svg
      const h1Icon = page
        .getByRole('heading', { level: 1, name: /받지 않는 발신인/ })
        .first()
        .locator('svg')
        .first();
      if ((await h1Icon.count()) > 0) {
        h1IconAriaHidden = await h1Icon.getAttribute('aria-hidden').catch(() => null);
      }

      // <time datetime> 샘플 (최대 3개)
      const timeLocators = await page.locator('time[datetime]').all();
      for (const t of timeLocators.slice(0, 3)) {
        const dt = await t.getAttribute('datetime').catch(() => null);
        if (dt) timeTagSamples.push(dt);
      }

      // 차단 해제 버튼 aria-label (최대 3개)
      const removeBtns = await page.locator('button[aria-label*="차단 해제"]').all();
      for (const b of removeBtns.slice(0, 3)) {
        const al = await b.getAttribute('aria-label').catch(() => null);
        if (al) removeBtnAriaLabels.push(al);
      }

      ariaLiveCount = await page.locator('[aria-live="polite"]').count().catch(() => 0);
      statusRoleCount = await page.locator('[role="status"]').count().catch(() => 0);
      alertRoleCount = await page.locator('[role="alert"]').count().catch(() => 0);
    }

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-aria-semantic',
    );

    test.info().annotations.push({
      type: 'aria-semantic',
      description: JSON.stringify(
        {
          branch,
          listAriaLabel,
          listTabIndex,
          h1IconAriaHidden,
          timeTagSamples,
          removeBtnAriaLabels,
          ariaLiveCount,
          statusRoleCount,
          alertRoleCount,
        },
        null,
        2,
      ),
    });
  });
});

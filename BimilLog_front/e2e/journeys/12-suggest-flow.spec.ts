import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-12-suggest-flow';
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
 * suggest 페이지 진입 + 라디오 그룹 mount 보장.
 * fail-soft — 마운트 실패해도 캡처는 진행.
 */
async function gotoSuggest(page: Page) {
  await page.goto('/suggest');
  await waitSettled(page, 1500);
  await page
    .locator('[role="radiogroup"]')
    .first()
    .waitFor({ state: 'attached', timeout: 5000 })
    .catch(() => undefined);
}

/**
 * 라디오 옵션을 클릭으로 선택하고 폼 등장 대기.
 * suggestionType 이 IMPROVEMENT 인 첫 옵션을 우선 선택 (기본 케이스).
 */
async function selectImprovement(page: Page): Promise<boolean> {
  const radios = page.getByRole('radio');
  const count = await radios.count().catch(() => 0);
  if (count < 2) return false;

  // OPTIONS[0] === IMPROVEMENT
  const first = radios.nth(0);
  await first.click({ trial: false }).catch(() => undefined);
  await page.waitForTimeout(700); // RAF + scrollIntoView smooth

  // 폼 textarea 마운트 대기 — id="suggest-content"
  return await page
    .locator('#suggest-content')
    .first()
    .isVisible({ timeout: 3000 })
    .catch(() => false);
}

async function selectError(page: Page): Promise<boolean> {
  const radios = page.getByRole('radio');
  if ((await radios.count().catch(() => 0)) < 2) return false;

  const second = radios.nth(1);
  await second.click({ trial: false }).catch(() => undefined);
  await page.waitForTimeout(700);

  return await page
    .locator('#suggest-content')
    .first()
    .isVisible({ timeout: 3000 })
    .catch(() => false);
}

/**
 * round-12-suggest-flow iter-1
 *  - 비인증 컨텍스트 + 백엔드 미가용 가정 (시드 토큰 없음).
 *  - 카드 → role=radiogroup 회귀 핵심.
 *  - 폼 자체와 카운터/카피/사후 카드는 클라이언트 단으로 검증 가능 (제출은 fail).
 *  - fail-soft: 모든 step 매트릭스 캡처 확보.
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-suggest-base                /suggest 진입 + 헤더 메타포
 *  step-02-radiogroup-aria             role=radiogroup/radio + Arrow 키 이동
 *  step-03-form-filled                 IMPROVEMENT 선택 + 본문 작성
 *  step-04-counter-near-limit          400자 입력 → 카운터 임계 색상
 *  step-05-anonymous-notice            비인증 카피 분기 ("익명으로 접수돼요")
 *  step-06-success-exits-sim           제출 시뮬레이션 시도 + 실패 시 폼 보존 캡처
 *  step-07-mobile-sticky-shortlabel    모바일 단축 라벨 + 폼 위치
 *  step-08-dark-tokens                 다크 토큰 회귀 (paper-aged/postal-navy/seal-gold)
 */
test.describe('round-12-suggest-flow', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /suggest 진입 + 헤더 메타포 ("아이디어를 들려주세요")
  // ============================================================
  test('step-01-suggest-base', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const heading = page
      .getByRole('heading', { level: 1, name: /아이디어를 들려주세요/ })
      .first();
    const headingVisible = await heading
      .isVisible({ timeout: 3000 })
      .catch(() => false);

    const groupHeading = page
      .getByRole('heading', { level: 2, name: /어떤 의견을 들려주시겠어요/ })
      .first();
    const groupVisible = await groupHeading
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-suggest-base',
    );

    test.info().annotations.push({
      type: 'header-metaphor',
      description: `headingVisible=${headingVisible} groupVisible=${groupVisible} url=${page.url()}`,
    });

    // sanity: 페이지 자체는 mount 됨
    expect(page.url()).toContain('/suggest');
  });

  // ============================================================
  // step-02: role=radiogroup / radio + Arrow 키 이동 (F-12-BUG-2 핵심)
  //  - 라디오 그룹 ARIA 속성 검증
  //  - Tab → 첫 라디오에 포커스 → ArrowRight → 두 번째 라디오 선택+포커스
  //  - aria-checked 변동 검증
  // ============================================================
  test('step-02-radiogroup-aria', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const group = page.locator('[role="radiogroup"]').first();
    const groupVisible = await group.isVisible({ timeout: 3000 }).catch(() => false);

    let radiosCount = 0;
    let labelledBy: string | null = null;
    let firstAriaChecked: string | null = null;
    let secondAriaCheckedAfterArrow: string | null = null;
    let firstTabIndex: string | null = null;

    if (groupVisible) {
      labelledBy = await group.getAttribute('aria-labelledby').catch(() => null);

      const radios = page.getByRole('radio');
      radiosCount = await radios.count().catch(() => 0);

      if (radiosCount >= 2) {
        firstAriaChecked = await radios
          .nth(0)
          .getAttribute('aria-checked')
          .catch(() => null);
        firstTabIndex = await radios.nth(0).getAttribute('tabindex').catch(() => null);

        // 첫 옵션에 포커스 후 ArrowRight → 두 번째 옵션 선택
        await radios.nth(0).focus();
        await page.waitForTimeout(150);
        await page.keyboard.press('ArrowRight');
        await page.waitForTimeout(400);

        secondAriaCheckedAfterArrow = await radios
          .nth(1)
          .getAttribute('aria-checked')
          .catch(() => null);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-radiogroup-aria',
    );

    test.info().annotations.push({
      type: 'radiogroup-aria',
      description: JSON.stringify(
        {
          groupVisible,
          radiosCount,
          labelledBy,
          firstAriaChecked,
          firstTabIndex,
          secondAriaCheckedAfterArrow,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-03: IMPROVEMENT 선택 + 본문 작성 (제목+본문 폼)
  //  - 폼 등장 + textarea 자동 포커스 + scrollIntoView 회귀
  //  - 카운터 표시 ("9/500" → "60/500")
  // ============================================================
  test('step-03-form-filled', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const opened = await selectImprovement(page);

    let typed = false;
    let counterText: string | null = null;
    let focusedId: string | null = null;
    if (opened) {
      const textarea = page.locator('#suggest-content').first();
      await textarea.fill(
        '비밀로그 사용 중에 떠오른 작은 아이디어를 적어봅니다. 이 글은 e2e 회귀 검증용 본문입니다.',
      );
      typed = true;
      await page.waitForTimeout(300);

      counterText = await page
        .locator('text=/\\d+\\s*\\/\\s*500/')
        .first()
        .innerText()
        .catch(() => null);

      focusedId = await page.evaluate(() =>
        document.activeElement instanceof HTMLElement
          ? document.activeElement.id || document.activeElement.tagName.toLowerCase()
          : null,
      );
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-form-filled',
    );

    test.info().annotations.push({
      type: 'form-filled',
      description: `opened=${opened} typed=${typed} counterText=${counterText} focusedId=${focusedId}`,
    });
  });

  // ============================================================
  // step-04: 카운터 임계 색상 + max validation (F-12-BUG-5)
  //  - 400자 (80%) 도달 → text-stamp-red font-semibold (isNearLimit)
  //  - 카운터 노출 + sr live region 보존 회귀
  // ============================================================
  test('step-04-counter-near-limit', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const opened = await selectImprovement(page);

    let len = 0;
    let counterColorClass: string | null = null;
    if (opened) {
      const textarea = page.locator('#suggest-content').first();
      // 400자 입력 — 한국어 어절 단위 + 줄바꿈으로 break-keep 도 함께 회귀.
      const sample =
        '비밀로그 건의 e2e 라운드 12 회귀용 임계 카운터 검증 본문입니다. ';
      // 약 30자 × 14 ≈ 420자
      const padded = sample.repeat(14).slice(0, 420);
      await textarea.fill(padded);
      await page.waitForTimeout(300);

      len = await textarea.evaluate((el) => (el as HTMLTextAreaElement).value.length);

      // counter element class 추출 (text-stamp-red 검증)
      counterColorClass = await page
        .locator('text=/\\d+\\s*\\/\\s*500/')
        .first()
        .getAttribute('class')
        .catch(() => null);

      // 카운터로 스크롤
      await page
        .locator('text=/\\d+\\s*\\/\\s*500/')
        .first()
        .scrollIntoViewIfNeeded()
        .catch(() => undefined);
      await page.waitForTimeout(200);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-counter-near-limit',
    );

    test.info().annotations.push({
      type: 'counter-threshold',
      description: `opened=${opened} length=${len} counterClass=${counterColorClass}`,
    });
  });

  // ============================================================
  // step-05: 비인증 카피 분기 — "익명으로 접수돼요"
  //  - reporterNoticeCopy: 비인증 = "익명으로 접수돼요"
  //  - 로그인 시 = "{memberName} 님 명의로 접수돼요" (시드 부족 시 비인증 분기만 캡처)
  // ============================================================
  test('step-05-anonymous-notice', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const opened = await selectImprovement(page);

    let anonNoticeVisible = false;
    let noticeText: string | null = null;
    if (opened) {
      const notice = page.getByText(/익명으로 접수돼요|.+ 님 명의로 접수돼요/).first();
      anonNoticeVisible = await notice
        .isVisible({ timeout: 2000 })
        .catch(() => false);
      if (anonNoticeVisible) {
        noticeText = (await notice.innerText().catch(() => '')) || null;
        await notice.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-anonymous-notice',
    );

    test.info().annotations.push({
      type: 'anonymous-notice',
      description: `opened=${opened} anonNoticeVisible=${anonNoticeVisible} noticeText=${noticeText}`,
    });
  });

  // ============================================================
  // step-06: 제출 시뮬 → 사후 SuccessExits 카드 / 실패 시 폼 보존 캡처
  //  - 백엔드 미가용 가정 → submitReportAction 실패 토스트 분기.
  //  - 폼 데이터 보존 회귀 (라운드 12 명세 — 성공 시에만 reset).
  //  - 시드 모드에서 성공 시: SuggestSuccessExits 카드 + "편지를 잘 받았어요" 헤더.
  // ============================================================
  test('step-06-success-exits-sim', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const opened = await selectImprovement(page);

    let submitClicked = false;
    let exitsCardVisible = false;
    let errorToastVisible = false;
    let formStillVisibleAfter = false;
    if (opened) {
      const textarea = page.locator('#suggest-content').first();
      await textarea.fill(
        'E2E 라운드 12 회귀 검증용 본문입니다. 백엔드 미가용 시 실패 토스트가 노출됩니다. 라운드 12 회귀.',
      );
      await page.waitForTimeout(200);

      const submitBtn = page.getByRole('button', { name: /의견 보내기|접수 중/ }).first();
      if (await submitBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
        await submitBtn.click().catch(() => undefined);
        submitClicked = true;
        await page.waitForTimeout(2500); // server action timeout

        exitsCardVisible = await page
          .getByText(/편지를 잘 받았어요/)
          .first()
          .isVisible({ timeout: 1500 })
          .catch(() => false);
        errorToastVisible = await page
          .getByText(/건의사항 접수 실패|오류가 발생|다시 시도해주세요/)
          .first()
          .isVisible({ timeout: 1500 })
          .catch(() => false);
        formStillVisibleAfter = await page
          .locator('#suggest-content')
          .first()
          .isVisible({ timeout: 800 })
          .catch(() => false);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-success-exits-sim',
    );

    test.info().annotations.push({
      type: 'submit-result',
      description: `opened=${opened} submitClicked=${submitClicked} exitsCardVisible=${exitsCardVisible} errorToastVisible=${errorToastVisible} formPreserved=${formStillVisibleAfter}`,
    });
  });

  // ============================================================
  // step-07: 모바일 단축 라벨 + 폼 위치 (F-12-BUG-6 자동 스크롤 회귀)
  //  - 모바일 프로젝트(mobile-375): "기능 제안" / "오류 신고" (sm:hidden)
  //  - 데스크톱 프로젝트(desktop-1280): "기능 개선 제안" / "오류 신고" (hidden sm:inline)
  //  - 카드 hover/active 토큰 (border-stamp-red ring-2) 시각 회귀
  // ============================================================
  test('step-07-mobile-sticky-shortlabel', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const vp = viewportFromProjectName(testInfo.project.name);
    const opened = await selectImprovement(page);

    // 라벨 분기 검증
    let shortLabelVisible = false;
    let fullLabelVisible = false;
    const shortLabel = page.getByText(/^기능 제안$/, { exact: true }).first();
    const fullLabel = page.getByText(/기능 개선 제안/).first();
    shortLabelVisible = await shortLabel.isVisible({ timeout: 1500 }).catch(() => false);
    fullLabelVisible = await fullLabel.isVisible({ timeout: 1500 }).catch(() => false);

    // 폼 위치 측정 — scrollIntoView 회귀
    let textareaInViewport = false;
    if (opened) {
      textareaInViewport = await page
        .locator('#suggest-content')
        .first()
        .evaluate((el) => {
          const rect = el.getBoundingClientRect();
          return rect.top >= 0 && rect.top <= window.innerHeight;
        })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-sticky-shortlabel',
    );

    test.info().annotations.push({
      type: 'label-and-position',
      description: `viewport=${vp} opened=${opened} shortLabel=${shortLabelVisible} fullLabel=${fullLabelVisible} textareaInVP=${textareaInViewport}`,
    });
  });

  // ============================================================
  // step-08: 다크 토큰 일관성 (paper-aged / postal-navy / seal-gold / stamp-red)
  //  - 다크 모드 프로젝트(*-dark) 에서 라디오 카드 + 폼 카드 + 안내 카드 모두 잉크 톤 회귀.
  //  - ERROR 카드 선택 → stamp-red iconBg + ring-stamp-red 시각.
  //  - 라이트/다크 양쪽에서 동일 위치/구조 캡처 → 토큰 일관성 비교.
  // ============================================================
  test('step-08-dark-tokens', async ({ page }, testInfo) => {
    await gotoSuggest(page);

    const opened = await selectError(page);

    // 폼 카드 헤더 (오류 신고 + Bug 아이콘)로 스크롤
    if (opened) {
      const formHeading = page
        .getByRole('heading', { name: /오류 신고/ })
        .first();
      await formHeading.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-dark-tokens',
    );

    test.info().annotations.push({
      type: 'dark-tokens',
      description: `theme=${themeFromProjectName(testInfo.project.name)} opened=${opened}`,
    });
  });
});

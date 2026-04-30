import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-7-board-write-edit-publish';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 백그라운드 polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

/**
 * Quill 에디터 ready 까지 대기. 백엔드 미가용/네트워크 영향 없이 client-only 초기화 결과 확인.
 *
 * 성공 조건: `.ql-editor` 또는 `.ql-toolbar` 가 visible.
 * 실패 시(=에디터 로드 실패 분기 또는 dynamic import 지연) → false 반환, 캡처는 그대로 진행 (fail-soft).
 */
async function waitForEditorReady(page: Page, ms = 6000): Promise<boolean> {
  const editor = page.locator('.ql-editor').first();
  return editor
    .waitFor({ state: 'visible', timeout: ms })
    .then(() => true)
    .catch(() => false);
}

/**
 * Quill 에디터 contenteditable 영역에 텍스트 입력.
 * Editor.tsx 가 dispatch 하는 onChange 가 반영될 시간을 약간 부여.
 */
async function typeIntoQuill(page: Page, text: string): Promise<void> {
  const editor = page.locator('.ql-editor').first();
  await editor.click();
  await editor.fill('');
  await editor.type(text, { delay: 10 });
  await page.waitForTimeout(300);
}

/**
 * round-7-board-write-edit-publish iter-1 — /board/write 진입 ~ 작성/미리보기/임시저장 배너 ~
 * 수정 페이지 prefill ~ 권한 없음 ~ 다크 에디터 ~ 모바일 sticky overlap.
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → 작성/수정 publish 요청은 토스트 에러로 흘러갈 수 있음.
 *    여정의 핵심 회귀 검증 포인트는 "폼 상태/UI 토큰/sticky 좌표/SafeHTML 미리보기/CDN 의존 제거" 시각 검증.
 *
 * 캡처 단계 (ux-proposal "인터랙션 캡처 지점" 기반, 약 12 step):
 *  step-01-write-base                    /board/write 비인증 진입 (AnonymousWriteNotice + WritePageHeader sticky + 빈 폼)
 *  step-01b-write-anonymous-notice       AnonymousWriteNotice 단독 영역 (스크롤 0 상태 강조)
 *  step-02-editor-filled                 제목 + Quill 본문 50자+ 입력 후 상태 (글자 수 카운터 노출)
 *  step-02b-editor-dark-tokens           다크 테마에서 Quill 영역 — bg-paper-50 / border-ink-soft 토큰 적용 검증 (B-7-008)
 *  step-03-publish-pending               "작성완료" 클릭 후 — 백엔드 미가용으로 disabled / 에러 토스트 / 폼 보존 확인
 *  step-03b-write-preview-safehtml       미리보기 토글 — SafeHTML prose 렌더 (B-7-006 동일 패턴 검증, write 측)
 *  step-04-write-mobile-sticky-overlap   모바일에서 WritePageHeader sticky + AuthHeader 스택 overlap 여부 (B-7-005)
 *  step-04b-draft-restore-banner         이전 임시저장 → "이어서 작성 / 새로 작성" 배너 노출 (B-7-003)
 *  step-05-edit-base                     /board/post/{id}/edit 직접 진입 — backend 미가용시 "게시글 정보를 찾을 수 없습니다" fallback
 *  step-05b-edit-no-permission           권한 없음 분기 — 명시적 카피 + 게시글로 돌아가기 버튼 (B-7-007)
 *  step-06-edit-saved-toast              edit 페이지 어떤 인터랙션 후 결과 — backend 미가용으로 토스트 또는 fallback 캡처
 */
test.describe('round-7-board-write-edit-publish', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 보장 — 인증 시드 없는 라운드. /board/write 는 비회원도 진입 가능.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /board/write 비인증 진입 base
  // ============================================================
  test('step-01-write-base', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // WritePageHeader 의 "글쓰기" 또는 Breadcrumb "글쓰기" 텍스트 확인 (둘 중 하나라도 OK)
    await page
      .getByText(/글쓰기|작성완료/)
      .first()
      .waitFor({ state: 'visible', timeout: 8000 })
      .catch(() => undefined);

    // Quill 에디터 ready 까지 한 번 대기 (실패해도 fail-soft)
    await waitForEditorReady(page, 6000);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-write-base',
    );

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'write-base-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // ============================================================
  // step-01b: AnonymousWriteNotice 영역 (비로그인 안내)
  // ============================================================
  test('step-01b-write-anonymous-notice', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // AnonymousWriteNotice 텍스트 후보들 — 비회원/익명/로그인/회원 키워드 중 하나라도
    const notice = page.getByText(/비회원|익명|로그인|회원으로/).first();
    await notice.waitFor({ state: 'visible', timeout: 4000 }).catch(() => undefined);

    // 페이지 최상단으로 스크롤 (notice 가 뷰포트 안에 들어가도록)
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01b-write-anonymous-notice',
    );
  });

  // ============================================================
  // step-02: 제목 + Quill 본문 입력 (서식/글자 수 카운터)
  // ============================================================
  test('step-02-editor-filled', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // 제목 input
    const titleInput = page.locator('#title');
    await titleInput.waitFor({ state: 'visible', timeout: 6000 }).catch(() => undefined);
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('라운드 7 UX 점검 글');
    }

    // Quill 에디터 ready 후 본문 입력
    const editorReady = await waitForEditorReady(page, 6000);
    if (editorReady) {
      await typeIntoQuill(
        page,
        '라운드 7 multi-agent loop 검증을 위한 본문 입력입니다. 임시저장 동작도 같이 확인합니다. 서식 보존도 검증.',
      );
    }

    // 글자 수 카운터가 노출되도록 약간 대기
    await page.waitForTimeout(400);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-editor-filled',
    );
  });

  // ============================================================
  // step-02b: 다크 테마에서 Quill 에디터 토큰 적용 검증
  // (mobile-375-dark / desktop-1280-dark 프로젝트에서 의미 — light 도 동일 페이지 캡처 OK)
  // B-7-008: globals.css 의 quill.snow.css npm import + 다크 토큰 (bg-paper-50 dark:bg-postal-navy/20)
  // ============================================================
  test('step-02b-editor-dark-tokens', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // 에디터 영역만 우선 보이도록 약간 입력 후 안정화
    const titleInput = page.locator('#title');
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('다크 토큰 점검');
    }

    const editorReady = await waitForEditorReady(page, 6000);
    if (editorReady) {
      await typeIntoQuill(page, '다크 모드에서 Quill 토큰 적용을 시각 검증합니다.');
    }

    // 에디터 영역으로 스크롤 — 카드 본문 안에 Quill 이 위치
    const quillRoot = page.locator('.ql-container').first();
    await quillRoot.scrollIntoViewIfNeeded().catch(() => undefined);
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02b-editor-dark-tokens',
    );
  });

  // ============================================================
  // step-03: "작성완료" 클릭 후 — backend 미가용 토스트 또는 disabled 분기
  // 비회원이면 비밀번호 입력 없이는 isFormValid=false 라 disabled 캡처 자체가 의미 있음.
  // ============================================================
  test('step-03-publish-pending', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // 제목 + 본문 입력
    const titleInput = page.locator('#title');
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('라운드 7 publish 검증');
    }
    if (await waitForEditorReady(page, 6000)) {
      await typeIntoQuill(
        page,
        '발행 시도 — 백엔드 미가용 환경에서 폼 보존/에러 토스트 캡처를 위한 임시 본문입니다.',
      );
    }

    // 비회원 비밀번호 input 도 채워서 isFormValid 통과 시도
    const passwordInput = page.locator('#password');
    if (await passwordInput.isVisible().catch(() => false)) {
      await passwordInput.fill('1234');
    }

    // 작성완료 버튼 클릭 시도
    const submitBtn = page.getByRole('button', { name: /작성완료|작성 중/ }).first();
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click().catch(() => undefined);
      // 토스트/disabled 라벨 변경 확인을 위한 짧은 대기
      await page.waitForTimeout(1500);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-publish-pending',
    );
  });

  // ============================================================
  // step-03b: 미리보기 토글 — write 폼의 prose 렌더
  // ============================================================
  test('step-03b-write-preview-safehtml', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    const titleInput = page.locator('#title');
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('미리보기 SafeHTML 검증');
    }
    if (await waitForEditorReady(page, 6000)) {
      await typeIntoQuill(
        page,
        '미리보기에서 HTML 서식이 그대로 렌더되어야 하며, escape 되어 텍스트로 보이면 안 됩니다.',
      );
    }

    // 미리보기 토글 버튼 — 모바일은 아이콘 only, 데스크톱은 "미리보기" 라벨
    const previewBtn = page
      .getByRole('button', { name: /미리보기|편집/ })
      .first();
    if (await previewBtn.isVisible().catch(() => false)) {
      await previewBtn.click().catch(() => undefined);
      await page.waitForTimeout(400);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03b-write-preview-safehtml',
    );
  });

  // ============================================================
  // step-04: 모바일 sticky overlap — WritePageHeader top:var(--app-header-height) 검증
  // mobile-* 프로젝트에서 의미. desktop 도 동일 페이지 캡처 (페이지 기준 검증).
  // 페이지를 약간 스크롤시켜 sticky 적용된 상태 캡처.
  // ============================================================
  test('step-04-write-mobile-sticky-overlap', async ({ page }, testInfo) => {
    await page.goto('/board/write');
    await waitSettled(page, 1500);

    // 본문 입력해 페이지 길이 확보
    const titleInput = page.locator('#title');
    if (await titleInput.isVisible().catch(() => false)) {
      await titleInput.fill('sticky 좌표 점검');
    }

    if (await waitForEditorReady(page, 6000)) {
      await typeIntoQuill(
        page,
        '모바일 sticky 헤더와 AuthHeader 의 좌표가 var(--app-header-height) 토큰으로 통일되어 있는지 시각 검증합니다.',
      );
    }

    // 약간 스크롤 → sticky 가 화면 상단에 고정된 채 본문이 미끄러지는 상태 캡처
    await page.evaluate(() => window.scrollBy(0, 200));
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-write-mobile-sticky-overlap',
    );
  });

  // ============================================================
  // step-04b: 임시저장 복구 배너 (B-7-003)
  // localStorage 의 bimillog_drafts 키에 직접 draft 시드 → 페이지 진입 시 배너 노출 검증.
  // ============================================================
  test('step-04b-draft-restore-banner', async ({ page }, testInfo) => {
    // 1) 빈 페이지에 우선 접근하여 origin 확보
    await page.goto('/board/write');
    await waitSettled(page, 800);

    // 2) localStorage 에 draft 직접 주입 (write 모드 → key="bimillog_drafts:write")
    //    useDraft 의 키 규칙은 변동 가능 — 안전하게 두 후보 키 모두 주입.
    await page.evaluate(() => {
      const draft = {
        title: '이전에 작성하던 라운드 7 편지',
        content:
          '<p>이전에 작성하던 본문입니다. 임시 보관함에서 이어서 작성을 검증합니다.</p>',
        savedAt: new Date().toISOString(),
      };
      try {
        localStorage.setItem('bimillog_drafts', JSON.stringify({ write: draft }));
        localStorage.setItem('bimillog_drafts:write', JSON.stringify(draft));
        localStorage.setItem(
          'bimillog_draft_write',
          JSON.stringify(draft),
        );
      } catch {
        /* ignored */
      }
    });

    // 3) 새로고침으로 useWriteForm 마운트 사이클 다시 실행 → 배너 분기 진입 가능
    await page.reload();
    await waitSettled(page, 1500);

    // 배너의 한국어 카피 — "이전에 작성하던 편지가 있어요"
    await page
      .getByText(/이전에 작성하던 편지|이어서 작성|새로 작성/)
      .first()
      .waitFor({ state: 'visible', timeout: 4000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04b-draft-restore-banner',
    );
  });

  // ============================================================
  // step-05: /board/post/{id}/edit 직접 진입 — backend 미가용시 fallback (post=null) 또는 권한 없음 분기
  // initialPost null → "게시글 정보를 찾을 수 없습니다." + "게시판으로 돌아가기" 노출 (EditPostClient 의 fallback)
  // ============================================================
  test('step-05-edit-base', async ({ page }, testInfo) => {
    await page.goto('/board/post/1/edit');
    await waitSettled(page, 2000);

    // 후보 텍스트들 — 케이스에 따라 분기 (회원이라 권한 통과시 폼, post null 이면 fallback,
    //  isLoading 끝나기 전이면 Spinner)
    await page
      .getByText(/게시글 수정|게시글 정보를 찾을 수 없습니다|게시글 불러오는 중|권한이 없습니다/)
      .first()
      .waitFor({ state: 'visible', timeout: 4000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-edit-base',
    );
  });

  // ============================================================
  // step-05b: 권한 없음 화면 (B-7-007)
  // backend 미가용 + 비인증 → 대부분 "게시글 정보를 찾을 수 없습니다" fallback 으로 빠지지만,
  // 만약 mock 응답으로 post 가 채워진다면 비인증/소유주 불일치 분기로 권한 없음 화면 노출.
  // 이 step 은 fail-soft 로 fallback 캡처도 의미.
  // ============================================================
  test('step-05b-edit-no-permission', async ({ page }, testInfo) => {
    await page.goto('/board/post/999999/edit');
    await waitSettled(page, 2000);

    // 권한 없음 카피 또는 게시글 없음 fallback 둘 중 하나
    await page
      .getByText(/권한이 없습니다|게시글 정보를 찾을 수 없습니다|게시글로 돌아가기|게시판으로 돌아가기/)
      .first()
      .waitFor({ state: 'visible', timeout: 4000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05b-edit-no-permission',
    );
  });

  // ============================================================
  // step-06: edit 페이지 인터랙션 — 백엔드 미가용시 토스트 또는 fallback 캡처
  // 정상 흐름이라면 본문 일부 수정 → "수정완료" → 토스트.
  // 본 환경에서는 polling 없이 fallback 메시지 캡처가 핵심.
  // ============================================================
  test('step-06-edit-saved-toast', async ({ page }, testInfo) => {
    await page.goto('/board/post/1/edit');
    await waitSettled(page, 2000);

    // 폼이 로드되었다면 본문에 약간 수정 후 수정완료 클릭 시도
    const titleInput = page.locator('#title');
    if (await titleInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      const current = (await titleInput.inputValue().catch(() => '')) || '제목';
      await titleInput.fill(current + ' (수정)');
    }

    if (await waitForEditorReady(page, 4000)) {
      const editor = page.locator('.ql-editor').first();
      await editor.click();
      await editor.type(' (수정됨)', { delay: 10 });
      await page.waitForTimeout(300);
    }

    const saveBtn = page.getByRole('button', { name: /수정완료|수정 중/ }).first();
    if (await saveBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
      await saveBtn.click().catch(() => undefined);
      await page.waitForTimeout(1500);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-edit-saved-toast',
    );

    // URL/토스트 텍스트 어노테이션
    const toastText = await page
      .getByRole('status')
      .first()
      .textContent()
      .catch(() => null);
    test.info().annotations.push({
      type: 'edit-saved-result',
      description: `url=${page.url()} toast=${toastText ?? 'null'}`,
    });
  });
});

import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-8-post-detail-comment';
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
 * fail-soft 진입 헬퍼.
 *  - backend 미가용시 `app/board/post/[id]/page.tsx` 가 fetch 실패 → `notFound()` →
 *    Next.js 404 화면이 노출된다. 그래도 페이지는 정상 렌더되므로 캡처 자체는 의미 있음.
 *  - 게시글 fetch 성공 시 `PostDetailClient` 마운트 → SubElement 들이 보이게 됨.
 *
 * 둘 중 어느 분기든 캡처를 진행하되, 분기 종류를 annotation 에 남겨 디자인 리뷰 시 참고하도록 한다.
 */
async function gotoPostDetail(page: Page, id: number | string): Promise<'detail' | 'notfound' | 'unknown'> {
  await page.goto(`/board/post/${id}`);
  await waitSettled(page, 1500);

  // notFound 분기 후보: "404", "찾을 수 없", "이 페이지를 찾을 수 없습니다", "Not Found"
  const notFoundMarker = page.getByText(/찾을 수 없|Not Found|404/i).first();
  // detail 분기 후보: 댓글 섹션 / PostHeader / "조회" / "추천" / "댓글"
  const detailMarker = page.getByText(/댓글|추천|조회/).first();

  const isNotFound = await notFoundMarker.isVisible({ timeout: 2500 }).catch(() => false);
  if (isNotFound) return 'notfound';

  const isDetail = await detailMarker.isVisible({ timeout: 2500 }).catch(() => false);
  if (isDetail) return 'detail';

  return 'unknown';
}

/**
 * Quill 에디터 ready 까지 대기. 백엔드 미가용/네트워크 영향 없이 client-only 초기화 결과 확인.
 */
async function waitForEditorReady(page: Page, ms = 6000): Promise<boolean> {
  const editor = page.locator('.ql-editor').first();
  return editor
    .waitFor({ state: 'visible', timeout: ms })
    .then(() => true)
    .catch(() => false);
}

async function typeIntoQuill(page: Page, text: string): Promise<void> {
  const editor = page.locator('.ql-editor').first();
  await editor.click();
  await editor.fill('');
  await editor.type(text, { delay: 10 });
  await page.waitForTimeout(300);
}

/**
 * round-8-post-detail-comment iter-1
 *  - /board/post/{id} 진입 ~ 추천(좋아요) ~ 댓글 작성/답글/수정/삭제 모달 ~ 다크/모바일 회귀.
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → Server Component 의 fetch 실패 → notFound() → 404 화면이 노출되는 케이스가 존재.
 *    이 경우에도 캡처 자체는 의미 있으며 (token/typography 회귀 검증 가능),
 *    "post 가 정상 prefill 된" 분기에서는 인터랙션 시도까지 진행한다 (fail-soft).
 *
 * 캡처 단계 (ux-proposal 의 9 step + 회귀 검증 추가, 약 12 step):
 *  step-01-detail-base                     /board/post/{id} 진입 base — PostHeader/PostContent/CommentSection 구조 또는 404 분기
 *  step-01b-popular-comment-card           인기 댓글 카드 단독 영역 (paper-aged + seal-gold ring 검증)
 *  step-02-like-active                     "추천" 버튼 클릭 후 — 옵티미스틱 토글 시각 검증 (B-8-001)
 *  step-03-comment-form-active             CommentForm Quill 에디터 활성 + 글자 카운터 노출
 *  step-04-comment-submitted               작성 시도 결과 (성공 → 토스트 / 실패 → 폼 보존)
 *  step-05-reply-form-open                 답글 폼 인라인 노출 — replyTo 헤더 + Quill (B-8-014 자동 포커스)
 *  step-06-comment-edit-mode               본인 댓글 수정 인라인 토글 (드롭다운 → 수정)
 *  step-07-delete-confirm-modal            DeleteConfirmModal 노출 (paper-aged + stamp-red 토큰 회귀)
 *  step-07b-password-modal-form            PasswordModal 노출 — form 래핑 + inputMode + sr-only label 회귀 (B-8-007)
 *  step-08-comment-deleted-state           삭제 댓글 마스킹 (MailX + dashed border + 이탈릭) — B-8-002
 *  step-09-post-more-dropdown              본인 게시글 PostActions 드롭다운 또는 "추천/북마크" 영역
 *  step-10-mobile-reply-indent             모바일 depth>=2 들여쓰기 + break-keep 가독성
 */
test.describe('round-8-post-detail-comment', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 라운드 8 은 인증 시드 없는 fail-soft 검증.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /board/post/1 진입 base (detail 또는 404)
  // ============================================================
  test('step-01-detail-base', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    // detail 분기인 경우 댓글 섹션이 마운트 될 때까지 약간 더 대기
    if (branch === 'detail') {
      await page
        .getByRole('heading', { name: /댓글|comment/i })
        .first()
        .waitFor({ state: 'visible', timeout: 4000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-detail-base',
    );

    test.info().annotations.push({
      type: 'detail-branch',
      description: `branch=${branch} url=${page.url()}`,
    });

    // 인터랙션 트리거 스캔 — 페이지가 의미 있게 마운트된 경우만 의미 있음
    if (branch === 'detail') {
      const triggers = await scanInteractionTriggers(page);
      test.info().annotations.push({
        type: 'detail-triggers',
        description: JSON.stringify(triggers, null, 2),
      });
    }
  });

  // ============================================================
  // step-01b: 인기 댓글 카드 단독 (paper-aged + seal-gold ring 회귀)
  // detail 분기일 때만 의미. 그 외에는 동일 스크린샷 (404) 으로 fail-soft.
  // ============================================================
  test('step-01b-popular-comment-card', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // PopularCommentCard 후보 — "인기 댓글" 섹션 헤더 또는 Sparkles 아이콘 영역
      const popular = page.getByText(/인기 댓글|인기댓글|원본 댓글로 이동/).first();
      await popular.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01b-popular-comment-card',
    );
  });

  // ============================================================
  // step-02: "추천" 버튼 클릭 후 옵티미스틱 토글 시각 (B-8-001)
  // ============================================================
  test('step-02-like-active', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      const likeBtn = page
        .getByRole('button', { name: /추천|좋아요|like/i })
        .first();
      if (await likeBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await likeBtn.click().catch(() => undefined);
        // 옵티미스틱 반영 + 토스트 노출까지 짧게 대기
        await page.waitForTimeout(800);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-like-active',
    );

    const toastText = await page
      .getByRole('status')
      .first()
      .textContent()
      .catch(() => null);
    test.info().annotations.push({
      type: 'like-result',
      description: `branch=${branch} toast=${toastText ?? 'null'}`,
    });
  });

  // ============================================================
  // step-03: CommentForm Quill 활성 + 글자 카운터
  // ============================================================
  test('step-03-comment-form-active', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // CommentForm 영역으로 스크롤 — "댓글 작성" / "한마디 남기기" / "작성" 버튼 등 후보
      const formAnchor = page
        .getByText(/댓글 작성|한마디 남기기|첫 번째 댓글/)
        .first();
      await formAnchor.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);

      if (await waitForEditorReady(page, 4000)) {
        await typeIntoQuill(page, '라운드 8 댓글 점검을 위한 본문 입력입니다.');
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-comment-form-active',
    );
  });

  // ============================================================
  // step-04: "작성" 클릭 후 결과 (성공 → 토스트 / 실패 → 폼 보존)
  // 비회원 + backend 미가용이면 대부분 토스트 에러 / 폼 reset 안 되어야 OK (B-8-005).
  // ============================================================
  test('step-04-comment-submitted', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      const formAnchor = page
        .getByText(/댓글 작성|한마디 남기기|첫 번째 댓글/)
        .first();
      await formAnchor.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);

      if (await waitForEditorReady(page, 4000)) {
        await typeIntoQuill(page, '라운드 8 댓글 작성 시도 — backend 미가용 케이스 회귀 검증.');
      }

      // 비회원 비밀번호 input 채우기 (있다면)
      const pw = page.locator('input[type="password"]').first();
      if (await pw.isVisible({ timeout: 1500 }).catch(() => false)) {
        await pw.fill('1234');
      }

      const submit = page.getByRole('button', { name: /^작성$|등록|작성 중/ }).first();
      if (await submit.isVisible({ timeout: 1500 }).catch(() => false)) {
        await submit.click().catch(() => undefined);
        await page.waitForTimeout(1500);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-comment-submitted',
    );
  });

  // ============================================================
  // step-05: 답글 폼 인라인 노출 — replyTo 헤더 + Quill (B-8-014)
  // detail 분기 + 댓글 1건 이상 prefill 시 의미. 그 외는 fail-soft.
  // ============================================================
  test('step-05-reply-form-open', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      const replyBtn = page
        .getByRole('button', { name: /^답글$|reply/i })
        .first();
      if (await replyBtn.isVisible({ timeout: 2500 }).catch(() => false)) {
        await replyBtn.click().catch(() => undefined);
        await page.waitForTimeout(500);

        // 답글 폼 헤더 — "@nickname 에게 답글" 등
        await page
          .getByText(/답글 작성|에게 답글|에 답글/)
          .first()
          .waitFor({ state: 'visible', timeout: 2000 })
          .catch(() => undefined);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-reply-form-open',
    );
  });

  // ============================================================
  // step-06: 본인 댓글 수정 인라인 토글
  //  비회원 시드 0이라 대부분 진입 불가 — 드롭다운 트리거가 없으면 그대로 캡처 (fail-soft).
  // ============================================================
  test('step-06-comment-edit-mode', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // MoreHorizontal 드롭다운 트리거 — aria-haspopup="menu" 또는 aria-label 에 "더보기"
      const more = page.locator('[aria-haspopup="menu"]').first();
      if (await more.isVisible({ timeout: 2000 }).catch(() => false)) {
        await more.click().catch(() => undefined);
        await page.waitForTimeout(300);

        const editItem = page.getByRole('menuitem', { name: /수정/ }).first();
        if (await editItem.isVisible({ timeout: 1500 }).catch(() => false)) {
          await editItem.click().catch(() => undefined);
          await page.waitForTimeout(500);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-comment-edit-mode',
    );
  });

  // ============================================================
  // step-07: DeleteConfirmModal 노출 (회원 댓글 분기) — paper-aged/stamp-red 토큰 회귀
  //  fail-soft: 진입 불가 시 그대로 캡처 (404 페이지 또는 빈 상태).
  // ============================================================
  test('step-07-delete-confirm-modal', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      const more = page.locator('[aria-haspopup="menu"]').first();
      if (await more.isVisible({ timeout: 2000 }).catch(() => false)) {
        await more.click().catch(() => undefined);
        await page.waitForTimeout(300);

        const delItem = page.getByRole('menuitem', { name: /삭제/ }).first();
        if (await delItem.isVisible({ timeout: 1500 }).catch(() => false)) {
          await delItem.click().catch(() => undefined);
          await page.waitForTimeout(500);

          // 모달 텍스트 — "정말 삭제" / "삭제하시겠" 등
          await page
            .getByText(/정말|삭제하시|삭제할까/)
            .first()
            .waitFor({ state: 'visible', timeout: 2000 })
            .catch(() => undefined);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-delete-confirm-modal',
    );
  });

  // ============================================================
  // step-07b: PasswordModal 노출 (익명 댓글 삭제 분기) — form 래핑/inputMode 회귀
  // 익명 댓글 시드가 없으면 모달이 안 열림 → fail-soft.
  // 직접 호출 가능한 trigger 가 없을 수 있으므로 step-07 와 동일하게 시도하되,
  // 모달이 password type input 을 가진 변형(PasswordModal) 인지 확인.
  // ============================================================
  test('step-07b-password-modal-form', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // 가능한 trigger 들을 순회
      const triggers = page.locator('[aria-haspopup="menu"]');
      const count = await triggers.count();
      for (let i = 0; i < Math.min(count, 3); i++) {
        const t = triggers.nth(i);
        if (!(await t.isVisible().catch(() => false))) continue;
        await t.click().catch(() => undefined);
        await page.waitForTimeout(200);

        const delItem = page.getByRole('menuitem', { name: /삭제/ }).first();
        if (await delItem.isVisible({ timeout: 1000 }).catch(() => false)) {
          await delItem.click().catch(() => undefined);
          await page.waitForTimeout(500);
          // 비밀번호 input 이 보이면 PasswordModal 분기 — break
          const pw = page.locator('input[type="password"]').first();
          if (await pw.isVisible({ timeout: 1500 }).catch(() => false)) {
            break;
          }
          // 다른 모달이면 dismiss 시도
          await page.keyboard.press('Escape').catch(() => undefined);
          await page.waitForTimeout(200);
        } else {
          await page.keyboard.press('Escape').catch(() => undefined);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07b-password-modal-form',
    );
  });

  // ============================================================
  // step-08: 삭제 댓글 마스킹 — comment.deleted=true 시 "삭제된 댓글입니다." (B-8-002)
  //  실제 삭제 흐름 진입 어렵 — 시드 데이터에 deleted=true 댓글이 있다면 노출됨.
  //  현재 환경에서는 fail-soft 캡처 (없으면 일반 detail or 404).
  // ============================================================
  test('step-08-comment-deleted-state', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // 마스킹 텍스트 후보 노출 시 그 위치로 스크롤
      const masked = page.getByText(/삭제된 댓글입니다|삭제된 댓글/).first();
      if (await masked.isVisible({ timeout: 1500 }).catch(() => false)) {
        await masked.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-comment-deleted-state',
    );
  });

  // ============================================================
  // step-09: PostActions 드롭다운 또는 추천/북마크 영역 (본인 게시글 분기)
  //  비회원 + backend 미가용 시 대부분 fail-soft 캡처.
  // ============================================================
  test('step-09-post-more-dropdown', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // PostHeader / PostActions 영역으로 스크롤
      const headerArea = page.getByText(/조회|추천|댓글/).first();
      await headerArea.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(200);

      // PostActions 의 본인 분기 트리거 (있다면)
      const postMore = page
        .getByRole('button', { name: /더보기|수정|삭제|공유/ })
        .first();
      if (await postMore.isVisible({ timeout: 1500 }).catch(() => false)) {
        await postMore.click().catch(() => undefined);
        await page.waitForTimeout(300);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-09-post-more-dropdown',
    );
  });

  // ============================================================
  // step-10: 모바일 depth>=2 들여쓰기 + break-keep 회귀
  //  모바일 프로젝트에서 의미. 데스크톱에서도 동일 페이지 캡처.
  //  댓글 시드 부재 시에도 페이지/카드 토큰 회귀 검증 가치 있음.
  // ============================================================
  test('step-10-mobile-reply-indent', async ({ page }, testInfo) => {
    const branch = await gotoPostDetail(page, 1);

    if (branch === 'detail') {
      // 댓글 영역으로 스크롤 (가능한 만큼)
      const commentList = page
        .getByText(/댓글|첫 번째 댓글|아직 댓글/)
        .first();
      await commentList.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);

      // depth>=2 답글이 있으면 그 위치로 추가 스크롤
      const reply = page.locator('[id^="comment-"]').nth(2);
      if (await reply.isVisible({ timeout: 1000 }).catch(() => false)) {
        await reply.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-10-mobile-reply-indent',
    );

    // sanity — 페이지 자체는 200/404 어느 분기든 mount 완료
    expect(page.url()).toContain('/board/post/');
  });
});

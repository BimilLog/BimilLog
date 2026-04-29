"use client";

import { Info } from "lucide-react";

/**
 * 비로그인 사용자에게 글쓰기 정책을 안내하는 인라인 알림.
 *
 * - role="alert" 로 스크린리더가 즉시 인지 가능
 * - 모바일/데스크톱 공통 노출 (sticky 영역 아래)
 */
export function AnonymousWriteNotice() {
  return (
    <div
      role="alert"
      data-testid="anonymous-write-info-modal"
      className="container mx-auto mt-3 max-w-4xl px-4"
    >
      <div className="flex items-start gap-3 rounded-lg border border-postal-navy/30 bg-paper-aged p-3 text-sm text-ink dark:border-postal-navy/40 dark:bg-postal-navy/20 dark:text-paper-100">
        <Info className="mt-0.5 h-5 w-5 flex-shrink-0 stroke-postal-navy dark:stroke-paper-100" />
        <div>
          <p className="font-medium">로그인이 필요할 수 있어요</p>
          <p className="mt-1 text-ink-soft dark:text-paper-100/80">
            로그인하면 게시글 수정/삭제가 자동으로 가능합니다. 비로그인으로 작성하시면
            4자리 숫자(1000~9999) 비밀번호가 필요합니다.
          </p>
        </div>
      </div>
    </div>
  );
}

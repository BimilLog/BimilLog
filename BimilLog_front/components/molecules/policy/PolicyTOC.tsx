"use client";

import React, { useEffect, useState } from "react";

export interface PolicyTOCItem {
  id: string;
  label: string;
}

interface PolicyTOCProps {
  items: PolicyTOCItem[];
  /** 모바일에서 details 기본 펼침 여부 (기본 false). */
  defaultOpen?: boolean;
}

/**
 * 정책 문서 목차.
 * - 데스크톱(lg+): sticky sidebar 좌측 컬럼.
 * - 모바일/태블릿: 헤더 직후 collapsible <details>.
 * - IntersectionObserver 로 현재 보이는 섹션 highlight.
 *
 * 라운드 14: F-14-011 / F-14-012 / F-14-BUG-7 한 번에 해소.
 */
export const PolicyTOC = React.memo(function PolicyTOC({
  items,
  defaultOpen = false,
}: PolicyTOCProps) {
  const [activeId, setActiveId] = useState<string | null>(items[0]?.id ?? null);

  useEffect(() => {
    if (typeof window === "undefined" || items.length === 0) return;

    const elements = items
      .map((item) => document.getElementById(item.id))
      .filter((el): el is HTMLElement => el !== null);

    if (elements.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        // 화면 상단에 가장 가까운 섹션 우선
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (visible[0]?.target.id) {
          setActiveId(visible[0].target.id);
        }
      },
      {
        // 헤더 높이 + 약간 여유
        rootMargin: "-96px 0px -65% 0px",
        threshold: [0, 0.2, 0.5, 1.0],
      }
    );

    elements.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [items]);

  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>, id: string) => {
    e.preventDefault();
    const el = document.getElementById(id);
    if (!el) return;
    el.scrollIntoView({ behavior: "smooth", block: "start" });
    // URL 해시 업데이트 (외부 공유 링크용)
    if (typeof window !== "undefined" && window.history.replaceState) {
      window.history.replaceState(null, "", `#${id}`);
    }
    setActiveId(id);
  };

  const renderList = (
    listClass: string,
    onItemClick: (e: React.MouseEvent<HTMLAnchorElement>, id: string) => void
  ) => (
    <ol className={listClass}>
      {items.map((item, idx) => {
        const isActive = activeId === item.id;
        return (
          <li key={item.id} className="break-keep">
            <a
              href={`#${item.id}`}
              onClick={(e) => onItemClick(e, item.id)}
              className={[
                "block rounded-md px-3 py-2 text-sm font-body transition-colors no-underline",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2",
                isActive
                  ? "bg-stamp-red/10 text-stamp-red font-semibold border-l-2 border-stamp-red"
                  : "text-ink-soft dark:text-muted-foreground hover:bg-paper-aged hover:text-ink dark:hover:text-foreground border-l-2 border-transparent",
              ].join(" ")}
            >
              <span className="text-stamp-red/60 mr-2 tabular-nums">
                {String(idx + 1).padStart(2, "0")}
              </span>
              {item.label}
            </a>
          </li>
        );
      })}
    </ol>
  );

  return (
    <>
      {/* Desktop: sticky sidebar */}
      <aside
        aria-label="목차"
        className="no-print hidden lg:block lg:sticky lg:top-24 lg:self-start"
      >
        <div className="bg-paper-aged border border-ink-soft rounded-xl p-4 shadow-brand-sm">
          <h2 className="font-display text-sm font-bold text-stamp-red dark:text-stamp-red/90 mb-3 px-3 tracking-wide uppercase">
            목차
          </h2>
          {renderList("space-y-1 list-none p-0", handleClick)}
        </div>
      </aside>

      {/* Mobile/Tablet: collapsible details */}
      <details
        open={defaultOpen}
        className="no-print lg:hidden bg-paper-aged border border-ink-soft rounded-xl overflow-hidden mb-6 group"
      >
        <summary className="cursor-pointer list-none px-4 py-3 flex items-center justify-between font-display text-sm font-bold text-stamp-red dark:text-stamp-red/90 select-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 rounded-xl">
          <span className="tracking-wide uppercase">목차 보기</span>
          <span aria-hidden="true" className="text-stamp-red transition-transform group-open:rotate-180">
            ▾
          </span>
        </summary>
        <div className="px-2 pb-2">
          {renderList("space-y-1 list-none p-0", (e, id) => {
            handleClick(e, id);
            // 모바일은 클릭 후 details 닫기
            const details = (e.currentTarget as HTMLElement).closest("details");
            if (details) details.removeAttribute("open");
          })}
        </div>
      </details>
    </>
  );
});

PolicyTOC.displayName = "PolicyTOC";

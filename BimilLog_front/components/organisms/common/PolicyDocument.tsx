import React from "react";
import { LegalDocumentHeader } from "@/components/organisms/common/LegalDocumentHeader";
import { PolicyTOC, type PolicyTOCItem } from "@/components/molecules/policy/PolicyTOC";

export interface PolicySection {
  id: string;
  heading: string;
  /** 본문은 server-rendered React 노드 (정적 JSX). */
  content: React.ReactNode;
}

export interface PolicyRevision {
  date: string;
  summary: string;
}

interface PolicyDocumentProps {
  title: string;
  /** 섹션 목록 (id 는 anchor + ToC 매칭용 — 영문 + 숫자 권장). */
  sections: PolicySection[];
  /** 시행일자 (YYYY년 MM월 DD일 형식 권장). */
  effectiveDate?: string;
  /** 개정 이력. 최신순 정렬 권장. */
  revisions?: PolicyRevision[];
  /** 본문 첫 단락 (intro). 회사 자기소개나 약관 요약. */
  intro?: React.ReactNode;
  /** 마지막 추가 콘텐츠 (예: 책임자 박스). */
  footer?: React.ReactNode;
}

/**
 * 정책 문서 공통 organism — privacy / terms 가 동일하게 사용.
 *
 * 라운드 14 핵심 작업:
 * - paper-card / paper-aged 토큰 일관 (F-14-BUG-2/3)
 * - 섹션 anchor + scroll-margin-top (F-14-BUG-7/F-14-012)
 * - ToC sticky sidebar + 모바일 details (F-14-011)
 * - break-keep 한국어 줄바꿈 (F-14-BUG-20)
 * - text-ink dark:text-foreground 본문 톤 (F-14-BUG-2)
 * - text-stamp-red dark:text-stamp-red/90 헤더 (F-14-BUG-21)
 * - font-body 본문 (F-14-024)
 * - .no-print / .policy-section 클래스 (F-14-BUG-4)
 * - server component (F-14-BUG-17)
 */
export function PolicyDocument({
  title,
  sections,
  effectiveDate,
  revisions,
  intro,
  footer,
}: PolicyDocumentProps) {
  const tocItems: PolicyTOCItem[] = sections.map((s) => ({
    id: s.id,
    label: s.heading,
  }));

  return (
    <main className="container mx-auto px-4 py-8 max-w-7xl">
      <div className="bg-paper-card rounded-2xl shadow-brand-xl border border-ink-soft overflow-hidden">
        <LegalDocumentHeader title={title} />

        <div className="px-4 sm:px-8 py-6 text-foreground">
          <div className="lg:grid lg:grid-cols-[260px_1fr] lg:gap-10">
            <PolicyTOC items={tocItems} />

            <article className="font-body leading-relaxed text-base sm:text-lg max-w-none">
              {intro && (
                <p className="text-ink dark:text-foreground leading-relaxed break-keep mb-6">
                  {intro}
                </p>
              )}

              {sections.map((section) => (
                <section
                  key={section.id}
                  id={section.id}
                  data-policy-section
                  className="policy-section mb-10 scroll-mt-24"
                >
                  <h2 className="font-display text-xl sm:text-2xl font-semibold text-stamp-red dark:text-stamp-red/90 mb-4 border-b border-stamp-red/20 pb-2 break-keep">
                    {section.heading}
                  </h2>
                  <div className="text-ink dark:text-foreground leading-relaxed break-keep space-y-4">
                    {section.content}
                  </div>
                </section>
              ))}

              {footer}

              {(effectiveDate || (revisions && revisions.length > 0)) && (
                <div className="mt-10 pt-6 border-t border-ink-soft flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  {effectiveDate && (
                    <div className="bg-paper-aged border border-ink-soft rounded-lg p-4 inline-block">
                      <p className="text-ink dark:text-foreground font-body font-medium break-keep">
                        시행일자: {effectiveDate}
                      </p>
                    </div>
                  )}

                  {revisions && revisions.length > 0 && (
                    <details className="no-print bg-paper-aged border border-ink-soft rounded-lg p-4 max-w-md">
                      <summary className="cursor-pointer font-body font-medium text-ink dark:text-foreground select-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red rounded">
                        개정 이력 보기 ({revisions.length}건)
                      </summary>
                      <ul className="mt-3 space-y-2 text-sm text-ink-soft dark:text-muted-foreground">
                        {revisions.map((rev, idx) => (
                          <li key={`${rev.date}-${idx}`} className="break-keep">
                            <span className="font-medium text-stamp-red dark:text-stamp-red/90 mr-2">
                              {rev.date}
                            </span>
                            {rev.summary}
                          </li>
                        ))}
                      </ul>
                    </details>
                  )}
                </div>
              )}
            </article>
          </div>
        </div>
      </div>
    </main>
  );
}

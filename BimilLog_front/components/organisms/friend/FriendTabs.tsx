"use client";

import React, { useCallback, useEffect, useMemo, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Users, UserPlus, Send, Sparkles } from "lucide-react";
import { FriendList } from "./FriendList";
import { ReceivedRequestList } from "./ReceivedRequestList";
import { SentRequestList } from "./SentRequestList";
import { RecommendedFriendList } from "./RecommendedFriendList";
import {
  useReceivedFriendRequests,
  useSentFriendRequests,
} from "@/hooks/api/useFriendQueries";
import type { PageResponse } from "@/types/common";
import type {
  Friend,
  ReceivedFriendRequest,
  SentFriendRequest,
  RecommendedFriend,
} from "@/types/domains/friend";

const tabs = [
  { id: 'friends', label: '내 친구', shortLabel: '친구', icon: Users },
  { id: 'recommended', label: '추천 친구', shortLabel: '추천', icon: Sparkles },
  { id: 'received', label: '받은 요청', shortLabel: '받은', icon: UserPlus },
  { id: 'sent', label: '보낸 요청', shortLabel: '보낸', icon: Send },
] as const;

type TabId = typeof tabs[number]['id'];

export interface FriendTabInitialData {
  friends?: PageResponse<Friend> | null;
  recommended?: PageResponse<RecommendedFriend> | null;
  received?: PageResponse<ReceivedFriendRequest> | null;
  sent?: PageResponse<SentFriendRequest> | null;
}

interface FriendTabsProps {
  initialData?: FriendTabInitialData;
  initialTab?: TabId;
}

const isValidTab = (tab: string | null): tab is TabId => {
  return tabs.some(t => t.id === tab);
};

/**
 * 친구 탭 네비게이션 컴포넌트
 *
 * 라운드 9 B-002 수정:
 * - URL `?tab=` 가 SSOT. 탭 클릭 시 router.replace 로 URL 만 갱신하고
 *   activeTab 상태는 URL 에서 파생 (useMemo).
 * - revalidatePath 가 제거되어 server component 가 재실행되지 않으므로
 *   연속 mutation (취소/거절/수락/삭제) 후에도 활성 탭이 유지된다.
 *
 * 라운드 9 ARIA: WAI-ARIA Tabs 패턴 (role="tablist"/"tab"/"tabpanel").
 */
export const FriendTabs: React.FC<FriendTabsProps> = React.memo(({ initialData, initialTab }) => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tabParam = searchParams.get('tab');

  // URL → activeTab 파생 (URL SSOT)
  const activeTab: TabId = useMemo(() => {
    if (isValidTab(tabParam)) return tabParam;
    return initialTab ?? 'friends';
  }, [tabParam, initialTab]);

  // 받은/보낸 요청 카운트 뱃지 (totalElements 활용 — 응답 신설 X)
  const { data: receivedCountData } = useReceivedFriendRequests(
    0,
    1,
    true,
    initialData?.received,
  );
  const { data: sentCountData } = useSentFriendRequests(
    0,
    1,
    true,
    initialData?.sent,
  );

  const counts: Partial<Record<TabId, number>> = useMemo(
    () => ({
      received: receivedCountData?.data?.totalElements ?? 0,
      sent: sentCountData?.data?.totalElements ?? 0,
    }),
    [receivedCountData, sentCountData],
  );

  // 탭 클릭 → URL 갱신
  const handleSelect = useCallback(
    (id: TabId) => {
      if (id === activeTab) return;
      router.replace(`/friends?tab=${id}`, { scroll: false });
    },
    [activeTab, router],
  );

  // 키보드 화살표 네비게이션 (WAI-ARIA Tabs)
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
      let nextIndex: number | null = null;
      if (event.key === 'ArrowRight') {
        nextIndex = (index + 1) % tabs.length;
      } else if (event.key === 'ArrowLeft') {
        nextIndex = (index - 1 + tabs.length) % tabs.length;
      } else if (event.key === 'Home') {
        nextIndex = 0;
      } else if (event.key === 'End') {
        nextIndex = tabs.length - 1;
      }

      if (nextIndex !== null) {
        event.preventDefault();
        const next = tabs[nextIndex];
        handleSelect(next.id);
        tabRefs.current[nextIndex]?.focus();
      }
    },
    [handleSelect],
  );

  // 알림으로 진입 시 invalid tab 파라미터면 friends 로 정정
  useEffect(() => {
    if (tabParam !== null && !isValidTab(tabParam)) {
      router.replace('/friends?tab=friends', { scroll: false });
    }
  }, [tabParam, router]);

  return (
    <div>
      {/* 탭 헤더 (sticky) */}
      <div className="sticky top-0 z-10 -mx-4 px-4 mb-6 bg-paper-50/95 dark:bg-paper-900/95 backdrop-blur-sm border-b border-postal-navy/20">
        <div
          role="tablist"
          aria-label="친구 탭"
          className="flex flex-wrap gap-1 sm:gap-2 overflow-x-auto"
        >
          {tabs.map(({ id, label, shortLabel, icon: Icon }, index) => {
            const isActive = activeTab === id;
            const count = counts[id];
            const showBadge = (id === 'received' || id === 'sent') && count !== undefined && count > 0;
            return (
              <button
                key={id}
                ref={(el) => { tabRefs.current[index] = el; }}
                type="button"
                role="tab"
                id={`friend-tab-${id}`}
                aria-selected={isActive}
                aria-controls={`friend-panel-${id}`}
                tabIndex={isActive ? 0 : -1}
                onClick={() => handleSelect(id)}
                onKeyDown={(e) => handleKeyDown(e, index)}
                className={`
                  inline-flex items-center gap-1.5 sm:gap-2 px-3 sm:px-4 py-3 font-medium text-sm
                  border-b-2 transition-colors break-keep min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1 focus-visible:ring-offset-paper-50
                  ${isActive
                    ? 'border-postal-navy text-postal-navy dark:text-ink-900'
                    : 'border-transparent text-ink-soft hover:text-postal-navy hover:border-postal-navy/30'
                  }
                `}
              >
                <Icon className="w-4 h-4 shrink-0" aria-hidden="true" />
                <span className="hidden sm:inline">{label}</span>
                <span className="sm:hidden">{shortLabel}</span>
                {showBadge && (
                  <span
                    className={`
                      inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-semibold
                      ${isActive
                        ? 'bg-postal-navy text-paper-50'
                        : 'bg-postal-navy/15 text-postal-navy dark:bg-postal-navy/30 dark:text-ink-900'
                      }
                    `}
                    aria-label={`${count}건`}
                  >
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* 탭 패널 */}
      <div className="min-h-[400px]">
        <div
          role="tabpanel"
          id={`friend-panel-friends`}
          aria-labelledby={`friend-tab-friends`}
          hidden={activeTab !== 'friends'}
        >
          {activeTab === 'friends' && <FriendList initialData={initialData?.friends} />}
        </div>
        <div
          role="tabpanel"
          id={`friend-panel-recommended`}
          aria-labelledby={`friend-tab-recommended`}
          hidden={activeTab !== 'recommended'}
        >
          {activeTab === 'recommended' && <RecommendedFriendList initialData={initialData?.recommended} />}
        </div>
        <div
          role="tabpanel"
          id={`friend-panel-received`}
          aria-labelledby={`friend-tab-received`}
          hidden={activeTab !== 'received'}
        >
          {activeTab === 'received' && <ReceivedRequestList initialData={initialData?.received} />}
        </div>
        <div
          role="tabpanel"
          id={`friend-panel-sent`}
          aria-labelledby={`friend-tab-sent`}
          hidden={activeTab !== 'sent'}
        >
          {activeTab === 'sent' && <SentRequestList initialData={initialData?.sent} />}
        </div>
      </div>
    </div>
  );
});

FriendTabs.displayName = "FriendTabs";

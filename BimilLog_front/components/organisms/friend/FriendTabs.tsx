"use client";

import React, { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { Users, UserPlus, Send, Sparkles } from "lucide-react";
import { FriendList } from "./FriendList";
import { ReceivedRequestList } from "./ReceivedRequestList";
import { SentRequestList } from "./SentRequestList";
import { RecommendedFriendList } from "./RecommendedFriendList";
import type { PageResponse } from "@/types/common";
import type { Friend, ReceivedFriendRequest, SentFriendRequest, RecommendedFriend } from "@/types/domains/friend";

const tabs = [
  { id: 'friends', label: '내 친구', icon: Users, color: 'text-blue-600' },
  { id: 'recommended', label: '추천 친구', icon: Sparkles, color: 'text-purple-600' },
  { id: 'received', label: '받은 요청', icon: UserPlus, color: 'text-green-600' },
  { id: 'sent', label: '보낸 요청', icon: Send, color: 'text-orange-600' },
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

/**
 * 탭 ID의 유효성 검사 타입 가드
 */
const isValidTab = (tab: string | null): tab is TabId => {
  return tabs.some(t => t.id === tab);
};

/**
 * 친구 탭 네비게이션 컴포넌트
 * - URL 쿼리 파라미터 ?tab=friends|recommended|received|sent 지원
 * - 알림에서 /friends?tab=received로 이동 가능
 * - 북마크/공유 가능한 URL 구조
 */
export const FriendTabs: React.FC<FriendTabsProps> = ({ initialData, initialTab }) => {
  const searchParams = useSearchParams();
  const tabParam = searchParams.get('tab') as string | null;

  // 초기 상태: URL 파라미터 우선, 그 다음 initialTab, 그 다음 기본값
  const [activeTab, setActiveTab] = useState<TabId>(() => {
    if (isValidTab(tabParam)) {
      return tabParam;
    }
    return initialTab || 'friends';
  });

  // URL 파라미터 변경 시 상태 동기화
  useEffect(() => {
    if (isValidTab(tabParam)) {
      setActiveTab(tabParam);
    } else if (tabParam === null) {
      // 파라미터가 없으면 기본값 유지
      setActiveTab('friends');
    }
    // 유효하지 않은 파라미터는 무시 (현재 상태 유지)
  }, [tabParam]);

  return (
    <div>
      {/* 탭 헤더 */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex flex-wrap gap-2" aria-label="친구 탭">
          {tabs.map(({ id, label, icon: Icon, color }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className={`
                flex items-center gap-2 px-4 py-3 font-medium text-sm
                border-b-2 transition-colors
                ${activeTab === id
                  ? `border-purple-600 ${color}`
                  : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                }
              `}
              aria-current={activeTab === id ? 'page' : undefined}
            >
              <Icon className="w-4 h-4" />
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* 탭 컨텐츠 */}
      <div className="min-h-[400px]">
        {activeTab === 'friends' && <FriendList initialData={initialData?.friends} />}
        {activeTab === 'recommended' && <RecommendedFriendList initialData={initialData?.recommended} />}
        {activeTab === 'received' && <ReceivedRequestList initialData={initialData?.received} />}
        {activeTab === 'sent' && <SentRequestList initialData={initialData?.sent} />}
      </div>
    </div>
  );
};

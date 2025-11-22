"use client";

import React, { useState } from "react";
import { Users, UserPlus, Send, Sparkles } from "lucide-react";
import { FriendList } from "./FriendList";
import { ReceivedRequestList } from "./ReceivedRequestList";
import { SentRequestList } from "./SentRequestList";
import { RecommendedFriendList } from "./RecommendedFriendList";

const tabs = [
  { id: 'friends', label: '내 친구', icon: Users, color: 'text-blue-600' },
  { id: 'recommended', label: '추천 친구', icon: Sparkles, color: 'text-purple-600' },
  { id: 'received', label: '받은 요청', icon: UserPlus, color: 'text-green-600' },
  { id: 'sent', label: '보낸 요청', icon: Send, color: 'text-orange-600' },
] as const;

type TabId = typeof tabs[number]['id'];

/**
 * 친구 탭 네비게이션 컴포넌트
 * 내 친구, 추천 친구, 받은 요청, 보낸 요청 탭 관리
 */
export const FriendTabs: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabId>('friends');

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
        {activeTab === 'friends' && <FriendList />}
        {activeTab === 'recommended' && <RecommendedFriendList />}
        {activeTab === 'received' && <ReceivedRequestList />}
        {activeTab === 'sent' && <SentRequestList />}
      </div>
    </div>
  );
};

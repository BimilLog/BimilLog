import { Metadata } from "next";
import { FriendTabs } from "@/components/organisms/friend/FriendTabs";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import {
  getMyFriendsServer,
  getReceivedRequestsServer,
  getSentRequestsServer,
  getRecommendedFriendsServer,
} from "@/lib/api/server";

/**
 * 메타데이터
 */
export const metadata: Metadata = {
  title: "친구 | BimilLog",
  description: "친구 관리, 추천 친구, 친구 요청",
};

type TabId = 'friends' | 'recommended' | 'received' | 'sent';

const validTabs: TabId[] = ['friends', 'recommended', 'received', 'sent'];

async function getFriendTabData(tab: TabId) {
  switch (tab) {
    case 'friends':
      return { friends: (await getMyFriendsServer(0, 20))?.data ?? null };
    case 'recommended':
      return { recommended: (await getRecommendedFriendsServer(0, 10))?.data ?? null };
    case 'received':
      return { received: (await getReceivedRequestsServer(0, 20))?.data ?? null };
    case 'sent':
      return { sent: (await getSentRequestsServer(0, 20))?.data ?? null };
  }
}

interface Props {
  searchParams: Promise<{ tab?: string }>;
}

/**
 * 친구 메인 페이지
 * 내 친구, 추천 친구, 받은/보낸 요청을 탭으로 관리
 */
export default async function FriendsPage({ searchParams }: Props) {
  const params = await searchParams;
  const tab: TabId = validTabs.includes(params.tab as TabId) ? (params.tab as TabId) : 'friends';
  const initialData = await getFriendTabData(tab);

  return (
    <MainLayout
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
      containerClassName="container mx-auto px-4"
    >
      <div className="py-8 max-w-4xl mx-auto">
        {/* 페이지 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground">친구</h1>
          <p className="text-muted-foreground mt-2">
            친구를 관리하고 새로운 친구를 추천받아보세요
          </p>
        </div>

        {/* 탭 컴포넌트 */}
        <FriendTabs initialData={initialData} initialTab={tab} />
      </div>
    </MainLayout>
  );
}

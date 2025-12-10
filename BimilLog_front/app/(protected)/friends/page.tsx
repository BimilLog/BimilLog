import { Metadata } from "next";
import { FriendTabs } from "@/components/organisms/friend/FriendTabs";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";

/**
 * 메타데이터
 */
export const metadata: Metadata = {
  title: "친구 | BimilLog",
  description: "친구 관리, 추천 친구, 친구 요청",
};

/**
 * 친구 메인 페이지
 * 내 친구, 추천 친구, 받은/보낸 요청을 탭으로 관리
 */
export default function FriendsPage() {
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
        <FriendTabs />
      </div>
    </MainLayout>
  );
}

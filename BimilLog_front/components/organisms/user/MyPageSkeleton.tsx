import { memo } from "react";
import { Card, CardContent, CardHeader } from "@/components";

/**
 * 마이페이지 로딩 스켈레톤
 * MyPageClient.tsx의 레이아웃에 대응:
 * - ProfileCard (프로필 카드)
 * - UserStatsSection (활동 통계)
 * - ProfileBadges (뱃지 영역)
 * - UserActivitySection (활동 내역 탭)
 */

/** 스켈레톤 블록 유틸리티 */
const Bone = ({ className }: { className: string }) => (
  <div className={`bg-gray-200 dark:bg-gray-700 rounded animate-pulse ${className}`} />
);

const MyPageSkeleton = memo(() => {
  return (
    <div className="py-8 space-y-6">
      {/* 프로필 카드 스켈레톤 */}
      <Card variant="elevated">
        <CardContent className="p-6">
          <div className="flex flex-col sm:flex-row items-center gap-6">
            {/* 프로필 이미지 */}
            <Bone className="h-20 w-20 rounded-full shrink-0" />

            {/* 프로필 정보 */}
            <div className="flex-1 text-center sm:text-left space-y-3">
              {/* 닉네임 + 역할 뱃지 */}
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <Bone className="h-6 w-28" />
                <Bone className="h-5 w-12 rounded-full" />
              </div>

              {/* 이메일 */}
              <Bone className="h-4 w-44 mx-auto sm:mx-0" />

              {/* 가입일 */}
              <Bone className="h-3 w-36 mx-auto sm:mx-0" />
            </div>

            {/* 버튼 영역 (닉네임 변경, 설정, 로그아웃) */}
            <div className="flex flex-col gap-2 shrink-0">
              <Bone className="h-9 w-28 rounded-lg" />
              <Bone className="h-9 w-28 rounded-lg" />
              <Bone className="h-9 w-28 rounded-lg" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 활동 통계 섹션 스켈레톤 */}
      <Card variant="elevated">
        <CardContent className="p-6">
          {/* 활동 레벨 표시 */}
          <div className="flex items-center gap-3 mb-6">
            <Bone className="h-6 w-6 rounded-full" />
            <Bone className="h-6 w-32" />
          </div>

          {/* 경험치 프로그레스 바 */}
          <Bone className="h-3 w-full rounded-full mb-6" />

          {/* 통계 카드 그리드 */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
            {Array.from({ length: 5 }).map((_, idx) => (
              <div key={idx} className="text-center space-y-2 p-3">
                <Bone className="h-5 w-5 mx-auto rounded" />
                <Bone className="h-6 w-10 mx-auto" />
                <Bone className="h-3 w-16 mx-auto" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* 뱃지 섹션 스켈레톤 */}
      <Card variant="elevated">
        <CardHeader className="border-b p-4">
          <Bone className="h-6 w-24" />
        </CardHeader>
        <CardContent className="p-6">
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-4">
            {Array.from({ length: 6 }).map((_, idx) => (
              <div key={idx} className="flex flex-col items-center gap-2 p-3">
                <Bone className="h-12 w-12 rounded-full" />
                <Bone className="h-3 w-14" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* 활동 내역 탭 스켈레톤 */}
      <Card variant="elevated">
        <CardHeader className="border-b p-4">
          {/* 탭 버튼들 */}
          <div className="flex gap-2">
            {["내 게시글", "내 댓글", "좋아요 글", "좋아요 댓글"].map((_, idx) => (
              <Bone key={idx} className="h-9 w-24 rounded-lg" />
            ))}
          </div>
        </CardHeader>
        <CardContent className="p-4">
          {/* 테이블 헤더 */}
          <div className="hidden sm:flex items-center gap-4 pb-3 border-b border-gray-100 dark:border-gray-800 mb-3">
            <Bone className="h-4 w-12" />
            <Bone className="h-4 w-1/2" />
            <Bone className="h-4 w-20" />
            <Bone className="h-4 w-16" />
          </div>

          {/* 활동 항목 리스트 */}
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, idx) => (
              <div key={idx} className="flex items-center gap-4 py-2">
                <Bone className="h-4 w-8 shrink-0" />
                <Bone className={`h-4 flex-1 ${idx % 2 === 0 ? "max-w-[70%]" : "max-w-[50%]"}`} />
                <Bone className="h-4 w-20 shrink-0" />
                <Bone className="h-4 w-12 shrink-0" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
});

MyPageSkeleton.displayName = "MyPageSkeleton";

export { MyPageSkeleton };

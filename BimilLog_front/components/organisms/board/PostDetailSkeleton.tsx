import { Card, CardHeader, CardContent } from "@/components";

export const PostDetailSkeleton = () => {
  return (
    <div className="min-h-screen bg-brand-gradient">
      <div className="container mx-auto px-4 py-8">
        {/* 브레드크럼 스켈레톤 */}
        <div className="mb-4">
          <div className="h-4 bg-gray-200 rounded w-48 animate-pulse"></div>
        </div>

        {/* 게시글 카드 스켈레톤 */}
        <Card variant="elevated" className="mb-8">
          <CardHeader className="border-b p-4 md:p-6">
            {/* 제목 스켈레톤 */}
            <div className="mb-4">
              <div className="h-8 bg-gray-200 rounded w-3/4 mb-2 animate-pulse"></div>
            </div>

            {/* 작성자 정보 스켈레톤 */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex flex-col space-y-2">
                {/* 작성자와 시간 */}
                <div className="flex items-center space-x-3">
                  <div className="h-4 bg-gray-200 rounded w-24 animate-pulse"></div>
                  <div className="h-4 bg-gray-200 rounded w-32 animate-pulse"></div>
                </div>

                {/* 통계 정보 */}
                <div className="flex items-center space-x-4">
                  <div className="h-4 bg-gray-200 rounded w-16 animate-pulse"></div>
                  <div className="h-4 bg-gray-200 rounded w-16 animate-pulse"></div>
                  <div className="h-4 bg-gray-200 rounded w-16 animate-pulse"></div>
                </div>
              </div>

              {/* 버튼 영역 스켈레톤 */}
              <div className="flex gap-2">
                <div className="h-9 bg-gray-200 rounded w-24 animate-pulse"></div>
                <div className="h-9 bg-gray-200 rounded w-24 animate-pulse"></div>
              </div>
            </div>
          </CardHeader>

          <CardContent className="p-6">
            {/* 본문 스켈레톤 */}
            <div className="space-y-3 mb-8">
              <div className="h-4 bg-gray-200 rounded w-full animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded w-full animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded w-5/6 animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded w-full animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded w-4/5 animate-pulse"></div>
            </div>

            {/* 추천/신고 버튼 스켈레톤 */}
            <div className="flex items-center justify-center gap-4 mt-8 pt-6 border-t">
              <div className="h-10 bg-gray-200 rounded w-32 animate-pulse"></div>
              <div className="h-10 bg-gray-200 rounded w-32 animate-pulse"></div>
            </div>
          </CardContent>
        </Card>

        {/* 댓글 섹션 스켈레톤 */}
        <Card variant="elevated">
          <CardHeader className="border-b p-4">
            <div className="h-6 bg-gray-200 rounded w-32 animate-pulse"></div>
          </CardHeader>
          <CardContent className="p-4">
            {/* 댓글 작성 폼 스켈레톤 */}
            <div className="mb-6">
              <div className="h-24 bg-gray-200 rounded w-full animate-pulse mb-3"></div>
              <div className="h-10 bg-gray-200 rounded w-24 animate-pulse"></div>
            </div>

            {/* 댓글 목록 스켈레톤 */}
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="border-b pb-4">
                  <div className="flex items-center space-x-2 mb-2">
                    <div className="h-4 bg-gray-200 rounded w-24 animate-pulse"></div>
                    <div className="h-4 bg-gray-200 rounded w-32 animate-pulse"></div>
                  </div>
                  <div className="h-4 bg-gray-200 rounded w-full mb-2 animate-pulse"></div>
                  <div className="h-4 bg-gray-200 rounded w-2/3 animate-pulse"></div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

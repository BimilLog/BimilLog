import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Mail,
  FileText,
  MessageCircle,
  Heart,
  ThumbsUp,
  TrendingUp,
  Award,
  Target,
  Calendar,
} from "lucide-react";

interface UserStatsProps {
  stats: {
    totalMessages: number;
    totalPosts: number;
    totalComments: number;
    totalLikedPosts: number;
    totalLikedComments: number;
  };
}

// 활동 레벨 계산 함수
const getActivityLevel = (
  totalActivity: number
): { level: string; badge: string; color: string } => {
  if (totalActivity >= 100) {
    return {
      level: "전설급",
      badge: "🏆",
      color: "from-yellow-400 to-orange-500",
    };
  } else if (totalActivity >= 50) {
    return { level: "고수", badge: "🔥", color: "from-red-400 to-pink-500" };
  } else if (totalActivity >= 20) {
    return {
      level: "활발함",
      badge: "⭐",
      color: "from-blue-400 to-purple-500",
    };
  } else if (totalActivity >= 5) {
    return {
      level: "초보자",
      badge: "🌱",
      color: "from-green-400 to-teal-500",
    };
  } else {
    return { level: "새싹", badge: "🌿", color: "from-gray-400 to-gray-500" };
  }
};

// 통계 카드 컴포넌트
const StatCard = ({
  icon,
  value,
  label,
  color,
  gradient,
  description,
}: {
  icon: React.ReactNode;
  value: number;
  label: string;
  color: string;
  gradient: string;
  description: string;
}) => (
  <Card className="group bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105 cursor-pointer">
    <CardContent className="p-6 text-center relative overflow-hidden">
      {/* 배경 그라디언트 효과 */}
      <div
        className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-5 group-hover:opacity-10 transition-opacity duration-300`}
      />

      {/* 아이콘 */}
      <div
        className={`w-14 h-14 bg-gradient-to-r ${gradient} rounded-xl flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform duration-300`}
      >
        <div className="text-white">{icon}</div>
      </div>

      {/* 수치 */}
      <div className="relative z-10">
        <p
          className={`text-3xl font-bold ${color} mb-1 group-hover:scale-110 transition-transform duration-300`}
        >
          {value.toLocaleString()}
        </p>
        <p className="text-sm font-medium text-gray-700 mb-2">{label}</p>
        <p className="text-xs text-gray-500">{description}</p>
      </div>

      {/* 호버 효과 */}
      <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
        <TrendingUp className="w-4 h-4 text-gray-400" />
      </div>
    </CardContent>
  </Card>
);

export const UserStats: React.FC<UserStatsProps> = ({ stats }) => {
  const totalActivity = stats.totalPosts + stats.totalComments;
  const totalLikes = stats.totalLikedPosts + stats.totalLikedComments;
  const activityLevel = getActivityLevel(totalActivity);

  return (
    <div className="space-y-6 mb-8">
      {/* 활동 레벨 카드 */}
      <Card className="bg-gradient-to-r from-pink-50 via-purple-50 to-indigo-50 border-0 shadow-lg">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div
                className={`w-16 h-16 bg-gradient-to-r ${activityLevel.color} rounded-xl flex items-center justify-center text-2xl`}
              >
                {activityLevel.badge}
              </div>
              <div>
                <h3 className="text-xl font-bold text-gray-800 mb-1">
                  활동 레벨: {activityLevel.level}
                </h3>
                <p className="text-gray-600">
                  총 {totalActivity}개의 활동 기록이 있습니다
                </p>
              </div>
            </div>
            <div className="text-right">
              <Badge
                className={`bg-gradient-to-r ${activityLevel.color} text-white border-0 px-3 py-1`}
              >
                <Award className="w-4 h-4 mr-1" />
                {activityLevel.level}
              </Badge>
            </div>
          </div>

          {/* 활동 진행도 바 */}
          <div className="mt-4">
            <div className="flex justify-between text-xs text-gray-600 mb-2">
              <span>활동 진행도</span>
              <span>
                {Math.min(100, (totalActivity / 100) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`bg-gradient-to-r ${activityLevel.color} h-2 rounded-full transition-all duration-500`}
                style={{
                  width: `${Math.min(100, (totalActivity / 100) * 100)}%`,
                }}
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              다음 레벨까지{" "}
              {Math.max(
                0,
                getNextLevelThreshold(totalActivity) - totalActivity
              )}
              개 활동 필요
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 통계 카드들 */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <StatCard
          icon={<Mail className="w-7 h-7" />}
          value={stats.totalMessages}
          label="받은 메시지"
          color="text-pink-600"
          gradient="from-pink-500 to-rose-600"
          description="롤링페이퍼 메시지"
        />

        <StatCard
          icon={<FileText className="w-7 h-7" />}
          value={stats.totalPosts}
          label="작성한 글"
          color="text-blue-600"
          gradient="from-blue-500 to-cyan-600"
          description="게시판 활동"
        />

        <StatCard
          icon={<MessageCircle className="w-7 h-7" />}
          value={stats.totalComments}
          label="작성한 댓글"
          color="text-green-600"
          gradient="from-green-500 to-emerald-600"
          description="소통 참여도"
        />

        <StatCard
          icon={<Heart className="w-7 h-7" />}
          value={stats.totalLikedPosts}
          label="추천한 글"
          color="text-red-600"
          gradient="from-red-500 to-pink-600"
          description="좋아요 표현"
        />

        <StatCard
          icon={<ThumbsUp className="w-7 h-7" />}
          value={stats.totalLikedComments}
          label="추천한 댓글"
          color="text-purple-600"
          gradient="from-purple-500 to-indigo-600"
          description="댓글 공감도"
        />
      </div>

      {/* 활동 요약 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardContent className="p-4 text-center">
            <div className="flex items-center justify-center space-x-1 mb-2">
              <Target className="w-5 h-5 text-blue-500" />
              <span className="font-medium text-gray-700">총 활동</span>
            </div>
            <p className="text-2xl font-bold text-blue-600">{totalActivity}</p>
            <p className="text-xs text-gray-500">게시글 + 댓글</p>
          </CardContent>
        </Card>

        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardContent className="p-4 text-center">
            <div className="flex items-center justify-center space-x-1 mb-2">
              <Heart className="w-5 h-5 text-red-500" />
              <span className="font-medium text-gray-700">총 추천</span>
            </div>
            <p className="text-2xl font-bold text-red-600">{totalLikes}</p>
            <p className="text-xs text-gray-500">글 + 댓글 추천</p>
          </CardContent>
        </Card>

        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
          <CardContent className="p-4 text-center">
            <div className="flex items-center justify-center space-x-1 mb-2">
              <Calendar className="w-5 h-5 text-purple-500" />
              <span className="font-medium text-gray-700">추천율</span>
            </div>
            <p className="text-2xl font-bold text-purple-600">
              {totalActivity > 0
                ? Math.round((totalLikes / totalActivity) * 100)
                : 0}
              %
            </p>
            <p className="text-xs text-gray-500">활동 대비 추천 비율</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

// 다음 레벨 임계값 계산
function getNextLevelThreshold(currentActivity: number): number {
  if (currentActivity < 5) return 5;
  if (currentActivity < 20) return 20;
  if (currentActivity < 50) return 50;
  if (currentActivity < 100) return 100;
  return Math.ceil(currentActivity / 100) * 100 + 100;
}

import React from "react";
import { Card, CardContent } from "@/components";
import { Badge } from "@/components";
import { Button } from "@/components";
import { Alert, AlertDescription } from "@/components";
import { StatCard, LoadingSpinner } from "@/components/atoms";
import {
  FileText,
  MessageCircle,
  Heart,
  ThumbsUp,
  Target,
  Trophy,
  Flame,
  Star,
  TreePine,
  Leaf,
  Award,
  AlertTriangle,
  RefreshCw,
  Info,
} from "lucide-react";
import { calculateActivityScore, getActivityLevel, getNextLevelThreshold } from "@/lib/utils/format";

interface UserStatsProps {
  stats: {
    totalMessages: number;
    totalPosts: number;
    totalComments: number;
    totalLikedPosts: number;
    totalLikedComments: number;
  };
  isLoading?: boolean;
  error?: string | null;
  partialErrors?: string[];
  onRetry?: () => void;
  className?: string;
}

const getActivityLevelWithIcons = (totalScore: number) => {
  const baseLevel = getActivityLevel(totalScore);
  
  let badge;
  if (totalScore >= 500) {
    badge = <Trophy className="w-6 h-6 stroke-yellow-500 fill-yellow-100" />;
  } else if (totalScore >= 250) {
    badge = <Flame className="w-6 h-6 stroke-orange-600 fill-orange-100" />;
  } else if (totalScore >= 100) {
    badge = <Star className="w-6 h-6 stroke-yellow-500 fill-yellow-100" />;
  } else if (totalScore >= 25) {
    badge = <TreePine className="w-6 h-6 stroke-green-600 fill-green-100" />;
  } else {
    badge = <Leaf className="w-6 h-6 stroke-green-600 fill-green-100" />;
  }
  
  return { ...baseLevel, badge };
};

export const UserStatsSection: React.FC<UserStatsProps> = ({
  stats,
  isLoading = false,
  error = null,
  partialErrors = [],
  onRetry,
  className,
}) => {
  const totalScore = calculateActivityScore(stats);
  const totalLikes = stats.totalLikedPosts + stats.totalLikedComments;
  const activityLevel = getActivityLevelWithIcons(totalScore);

  if (isLoading) {
    return (
      <div className={`space-y-6 mb-8 ${className || ""}`}>
        <Card variant="elevated">
          <CardContent className="p-6">
            <LoadingSpinner
              variant="default"
              size="md"
              message="통계 정보를 불러오는 중..."
              className="py-8"
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`space-y-6 mb-8 ${className || ""}`}>
        <Alert className="border-red-200 bg-red-50">
          <AlertTriangle className="h-4 w-4 stroke-red-600 fill-red-100" />
          <AlertDescription className="text-red-800">
            <div className="flex items-center justify-between">
              <span>{error}</span>
              {onRetry && (
                <Button onClick={onRetry} variant="outline" size="sm" className="ml-4">
                  <RefreshCw className="w-4 h-4 mr-2 stroke-slate-600 fill-slate-100" />
                  다시 시도
                </Button>
              )}
            </div>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className={`space-y-6 mb-8 ${className || ""}`}>
      {partialErrors.length > 0 && (
        <Alert className="border-yellow-200 bg-yellow-50">
          <Info className="h-4 w-4 stroke-yellow-500 fill-yellow-100" />
          <AlertDescription>
            <div className="text-yellow-800">
              <p className="font-medium mb-2">일부 통계 정보를 불러오지 못했습니다:</p>
              <ul className="list-disc list-inside text-sm space-y-1">
                {partialErrors.map((err, idx) => (
                  <li key={idx}>{err}</li>
                ))}
              </ul>
              {onRetry && (
                <Button onClick={onRetry} variant="outline" size="sm" className="mt-3">
                  <RefreshCw className="w-4 h-4 mr-2 stroke-slate-600 fill-slate-100" />
                  다시 시도
                </Button>
              )}
            </div>
          </AlertDescription>
        </Alert>
      )}

      <Card className="bg-gradient-to-r from-pink-50 via-purple-50 to-indigo-50 border-0 shadow-brand-lg">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div
                className={`w-16 h-16 bg-gradient-to-r ${activityLevel.color} rounded-xl flex items-center justify-center text-2xl`}
              >
                {activityLevel.badge}
              </div>
              <div>
                <h3 className="text-xl font-bold text-brand-primary mb-1">
                  활동 레벨: {activityLevel.level}
                </h3>
                <p className="text-brand-muted">총 {totalScore}점의 활동 점수가 있습니다</p>
              </div>
            </div>
            <div className="text-right">
              <Badge
                className={`bg-gradient-to-r ${activityLevel.color} text-white border-0 px-3 py-1`}
              >
                <Award className="w-4 h-4 mr-1 stroke-yellow-500 fill-yellow-100" />
                {activityLevel.level}
              </Badge>
            </div>
          </div>

          <div className="mt-4">
            <div className="flex justify-between text-xs text-brand-muted mb-2">
              <span>활동 진행도</span>
              <span>{Math.min(100, (totalScore / 500) * 100).toFixed(1)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`bg-gradient-to-r ${activityLevel.color} h-2 rounded-full transition-all duration-500`}
                style={{
                  width: `${Math.min(100, (totalScore / 500) * 100)}%`,
                }}
              />
            </div>
            <p className="text-xs text-brand-secondary mt-1">
              다음 레벨까지 {Math.max(0, getNextLevelThreshold(totalScore) - totalScore)}점 필요
            </p>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard
          icon={<FileText className="w-7 h-7 stroke-blue-600 fill-blue-100" />}
          value={stats.totalPosts}
          label="작성한 글"
          color="text-blue-600"
          gradient="from-blue-500 to-cyan-600"
          description="게시판 활동 (5점)"
        />

        <StatCard
          icon={<MessageCircle className="w-7 h-7 stroke-green-600 fill-green-100" />}
          value={stats.totalComments}
          label="작성한 댓글"
          color="text-green-600"
          gradient="from-green-500 to-emerald-600"
          description="소통 참여도 (2점)"
        />

        <StatCard
          icon={<Heart className="w-7 h-7 stroke-red-500 fill-red-100" />}
          value={stats.totalLikedPosts}
          label="추천한 글"
          color="text-red-600"
          gradient="from-red-500 to-pink-600"
          description="추천 표현 (1점)"
        />

        <StatCard
          icon={<ThumbsUp className="w-7 h-7 stroke-purple-600 fill-purple-100" />}
          value={stats.totalLikedComments}
          label="추천한 댓글"
          color="text-purple-600"
          gradient="from-purple-500 to-indigo-600"
          description="댓글 공감도 (1점)"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card variant="elevated">
          <CardContent className="p-4 text-center">
            <div className="flex items-center justify-center space-x-1 mb-2">
              <Target className="w-5 h-5 stroke-blue-600 fill-blue-100" />
              <span className="font-medium text-brand-primary">총 활동 점수</span>
            </div>
            <p className="text-2xl font-bold text-blue-600">{totalScore}점</p>
          </CardContent>
        </Card>

        <Card variant="elevated">
          <CardContent className="p-4 text-center">
            <div className="flex items-center justify-center space-x-1 mb-2">
              <Heart className="w-5 h-5 stroke-red-500 fill-red-100" />
              <span className="font-medium text-brand-primary">총 추천</span>
            </div>
            <p className="text-2xl font-bold text-red-600">{totalLikes}</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
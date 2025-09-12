import { formatKoreanDate } from "@/lib/utils/date";

export const getRelativeTime = (dateString: string): string => {
  const now = new Date();
  const date = new Date(dateString);
  const diffInMs = now.getTime() - date.getTime();
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  if (diffInMinutes < 1) return "방금 전";
  if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
  if (diffInHours < 24) return `${diffInHours}시간 전`;
  if (diffInDays < 7) return `${diffInDays}일 전`;
  return formatKoreanDate(dateString);
};

export const calculateActivityScore = (stats: {
  totalPosts: number;
  totalComments: number;
  totalLikedPosts: number;
  totalLikedComments: number;
}): number => {
  return (
    stats.totalPosts * 5 +
    stats.totalComments * 2 +
    stats.totalLikedPosts * 1 +
    stats.totalLikedComments * 1
  );
};

export const getActivityLevel = (
  totalScore: number
): { level: string; badge: React.ReactNode; color: string } => {
  if (totalScore >= 500) {
    return {
      level: "전설급",
      badge: null, // Will be filled by component
      color: "from-yellow-400 to-orange-500",
    };
  } else if (totalScore >= 250) {
    return {
      level: "고수",
      badge: null,
      color: "from-red-400 to-pink-500",
    };
  } else if (totalScore >= 100) {
    return {
      level: "활발함",
      badge: null,
      color: "from-blue-400 to-purple-500",
    };
  } else if (totalScore >= 25) {
    return {
      level: "초보자",
      badge: null,
      color: "from-green-400 to-teal-500",
    };
  } else {
    return {
      level: "새싹",
      badge: null,
      color: "from-gray-400 to-gray-500",
    };
  }
};

export const getNextLevelThreshold = (currentScore: number): number => {
  if (currentScore < 25) return 25;
  if (currentScore < 100) return 100;
  if (currentScore < 250) return 250;
  if (currentScore < 500) return 500;
  return Math.ceil(currentScore / 500) * 500 + 500;
};

export const formatNumber = (num: number): string => {
  return num.toLocaleString();
};
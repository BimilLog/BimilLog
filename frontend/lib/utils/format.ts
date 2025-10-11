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

/**
 * HTML 문자열에서 텍스트만 추출
 */
export function stripHtml(html: string): string {
  // <br> 태그를 줄바꿈으로 변환
  let result = html.replace(/<br\s*\/?>/gi, '\n');
  // <p> 태그 끝을 줄바꿈으로 변환
  result = result.replace(/<\/p>/gi, '\n');
  // 다른 HTML 태그들 제거
  result = result.replace(/<[^>]*>?/gm, '');
  // 연속된 줄바꿈을 정리 (3개 이상을 2개로)
  result = result.replace(/\n{3,}/g, '\n\n');
  return result;
}

/**
 * 사용자 이름에서 이니셜 추출 (최대 2자)
 * @param name 사용자 이름
 * @returns 이니셜 (예: "홍길동" -> "홍길", "John Doe" -> "JD")
 */
export function getInitials(name: string): string {
  if (!name || !name.trim()) return '';

  const trimmedName = name.trim();

  // 한글/한자 이름인 경우 (공백 없이 연속된 문자)
  if (/^[\u4e00-\u9fff\uac00-\ud7af]+$/.test(trimmedName)) {
    return trimmedName.slice(0, 2);
  }

  // 영문 이름인 경우 (공백으로 구분된 단어들)
  const words = trimmedName.split(/\s+/).filter(word => word.length > 0);
  if (words.length >= 2) {
    return (words[0][0] + words[1][0]).toUpperCase();
  }

  // 단일 단어인 경우 첫 2글자
  return trimmedName.slice(0, 2).toUpperCase();
}
/**
 * 날짜/시간 포맷팅 유틸리티 함수들
 * 프로젝트 전반에 걸쳐 일관된 날짜 표시를 위한 통합 유틸리티
 */

/**
 * 상대 시간 표시 (방금 전, 5분 전, 3시간 전 등)
 */
export function formatRelativeDate(dateString: string | null | undefined): string {
  if (!dateString) {
    return '날짜 정보 없음';
  }

  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return '날짜 오류';
    }

    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return "방금 전";

    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}분 전`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}시간 전`;

    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}일 전`;

    // 1주일 이상이면 절대 날짜로 표시
    return formatKoreanDate(dateString);
  } catch {
    return '날짜 오류';
  }
}

/**
 * 절대 시간 표시 (YYYY-MM-DD HH:mm)
 */
export function formatDateTime(dateTimeString: string | null | undefined): string {
  if (!dateTimeString) {
    return '날짜 정보 없음';
  }

  try {
    const date = new Date(dateTimeString);
    if (isNaN(date.getTime())) {
      return '날짜 오류';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");

    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch {
    return '날짜 오류';
  }
}

/**
 * 한국식 날짜 표시 (YYYY.MM.DD)
 */
export function formatKoreanDate(dateString: string | null | undefined): string {
  if (!dateString) {
    return '날짜 정보 없음';
  }

  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return '날짜 오류';
    }
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).replace(/\. /g, '.').slice(0, -1);
  } catch {
    return '날짜 오류';
  }
}

/**
 * 한국식 날짜 + 시간 표시 (YYYY.MM.DD HH:mm)
 */
export function formatKoreanDateTime(dateString: string | null | undefined): string {
  if (!dateString) {
    return '날짜 정보 없음';
  }

  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return '날짜 오류';
    }
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).replace(/\. /g, '.').slice(0, -3);
  } catch {
    return '날짜 오류';
  }
}

/**
 * 짧은 형태의 날짜 표시 (월.일)
 */
export function formatShortDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString("ko-KR", {
    month: "short",
    day: "numeric",
  });
}

/**
 * 날짜 차이 계산 (일 단위)
 */
export function getDaysBetween(date1: string | Date, date2: string | Date): number {
  const d1 = new Date(date1);
  const d2 = new Date(date2);
  const diffTime = Math.abs(d2.getTime() - d1.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

/**
 * 특정 일수 전/후 날짜 계산
 */
export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

/**
 * 기본 날짜 포맷 (YYYY.MM.DD) - formatKoreanDate의 별칭
 */
export function formatDate(dateString: string | null | undefined): string {
  return formatKoreanDate(dateString);
}

/**
 * 현재 시각을 ISO string으로 반환
 */
export function getNowISOString(): string {
  return new Date().toISOString();
}

/**
 * 시간 차이를 밀리초로 반환
 */
export function getTimeDifferenceInMs(date1: string | Date, date2: string | Date): number {
  return Math.abs(new Date(date2).getTime() - new Date(date1).getTime());
}
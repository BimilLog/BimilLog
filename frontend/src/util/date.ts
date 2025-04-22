/**
 * ISO 날짜 문자열을 "yyyy-mm-dd hh:mm" 형식으로 변환합니다.
 * @param dateString ISO 형식의 날짜 문자열
 * @returns "yyyy-mm-dd hh:mm" 형식의 문자열
 */
export function formatDateTime(dateString: string | undefined | null): string {
  if (!dateString) return "날짜 정보 없음";
  
  try {
    const date = new Date(dateString);
    
    // 유효하지 않은 날짜인지 확인
    if (isNaN(date.getTime())) {
      return "유효하지 않은 날짜";
    }
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 1을 더함, 2자리로 패딩
    const day = String(date.getDate()).padStart(2, '0'); // 2자리로 패딩
    const hours = String(date.getHours()).padStart(2, '0'); // 2자리로 패딩
    const minutes = String(date.getMinutes()).padStart(2, '0'); // 2자리로 패딩
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch (error) {
    console.error("날짜 변환 오류:", error);
    return "날짜 변환 오류";
  }
} 
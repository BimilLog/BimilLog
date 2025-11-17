// Blacklist-related type definitions

/**
 * 블랙리스트 DTO
 * 백엔드 BlacklistDTO와 일치
 */
export interface BlacklistDTO {
  id?: number;              // 블랙리스트 ID (삭제 시 필수)
  memberName: string;       // 차단한 사용자 이름 (1-8자, 필수)
  createdAt?: string;       // 차단한 날짜 (ISO 8601 형식)
}

/**
 * 블랙리스트 추가 요청
 */
export interface AddToBlacklistRequest {
  memberName: string;       // 차단할 사용자 이름
}

/**
 * 블랙리스트 삭제 요청
 */
export interface RemoveFromBlacklistRequest {
  id: number;              // 삭제할 블랙리스트 ID
}

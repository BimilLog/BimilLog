// Friend-related type definitions

/**
 * 친구 정보
 * 백엔드 Friend.FriendInfo와 매핑
 */
export interface Friend {
  friendMemberId: number;
  memberName: string;
  thumbnailImage?: string;
}

/**
 * 친구 요청 정보
 * 백엔드 FriendRequest 엔티티와 매핑
 */
export interface FriendRequest {
  id: number;
  senderMemberId: number;
  senderName: string;
  senderThumbnail?: string;
  receiverMemberId: number;
  receiverName: string;
  receiverThumbnail?: string;
  status: 'PENDING' | 'REJECTED';
  createdAt: string;
}

/**
 * 추천 친구 정보
 * 백엔드 RecommendedFriend와 매핑
 */
export interface RecommendedFriend {
  friendMemberId: number;        // 추천 친구 ID
  memberName: string;             // 추천 친구 닉네임
  thumbnailImage?: string;        // 프로필 이미지 (옵션)
  depth: 2 | 3;                   // 촌수 (2촌 또는 3촌)
  acquaintanceId?: number;        // 공통 친구 ID (3촌은 null)
  acquaintanceName?: string;      // 공통 친구 닉네임 (3촌은 null)
  manyAcquaintance: boolean;      // 공통 친구가 2명 이상인지
  introduce?: string;             // 소개 문구 ("홍길동의 친구", "홍길동 외 다수의 친구", 3촌은 null)
}

/**
 * 친구 요청 보내기 DTO
 */
export interface SendFriendRequestDTO {
  receiverId: number;
}

/**
 * 친구 관계 ID 응답
 */
export interface FriendshipIdResponse {
  id: number;
}

// Member-related type definitions

// 사용자 정보 타입 (백엔드 MemberInfoResponseDTO와 일치)
export interface Member {
  memberId: number            // 사용자 고유 ID
  settingId: number           // 알림 설정 ID (Setting 테이블과 연결)
  socialNickname: string      // 카카오에서 가져온 원본 닉네임
  thumbnailImage: string      // 카카오 프로필 이미지 URL
  memberName: string          // 서비스 내에서 사용하는 사용자명 (변경 가능)
  role: "USER" | "ADMIN"      // 사용자 권한 레벨
}

// 하위 호환성을 위한 User 타입 alias (deprecated)
/** @deprecated Use Member instead */
export type User = Member

// 설정 타입 - v2 백엔드 SettingDTO 호환
// 사용자별 알림 설정 (기본값: 모두 true)
export interface Setting {
  messageNotification: boolean      // 롤링페이퍼 메시지 알림
  commentNotification: boolean      // 댓글 알림
  postFeaturedNotification: boolean // 게시글 인기글 선정 알림
}

// 카카오 친구 타입
// 카카오 API에서 받아오는 친구 정보 구조
export interface KakaoFriend {
  id: number                        // 카카오 친구 ID
  uuid: string                      // 카카오 고유 식별자
  memberName: string                // 비밀로그 서비스 내 사용자명 (가입된 친구만)
  profile_nickname: string          // 카카오 닉네임
  profile_thumbnail_image: string   // 카카오 프로필 이미지
}

// 카카오 친구 목록 타입
// 카카오 API 응답 구조에 맞춰 설계
export interface KakaoFriendList {
  elements: KakaoFriend[]    // 친구 목록 배열
  total_count: number        // 전체 친구 수
}
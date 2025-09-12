// User-related type definitions

// 사용자 정보 타입 (백엔드 UserInfoResponseDTO와 일치)
export interface User {
  userId: number
  settingId: number
  socialNickname: string
  thumbnailImage: string
  userName: string
  role: "USER" | "ADMIN"
}

// 설정 타입 - v2 백엔드 SettingDTO 호환
export interface Setting {
  messageNotification: boolean
  commentNotification: boolean
  postFeaturedNotification: boolean
}

// 카카오 친구 타입
export interface KakaoFriend {
  id: number
  uuid: string
  userName: string
  profile_nickname: string
  profile_thumbnail_image: string
}

// 카카오 친구 목록 타입
export interface KakaoFriendList {
  elements: KakaoFriend[]
  total_count: number
}
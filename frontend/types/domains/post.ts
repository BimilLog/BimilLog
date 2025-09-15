// Post-related type definitions

// 게시글 타입 - v2 백엔드 FullPostResDTO 호환
// 게시글 상세 페이지에서 사용 (모든 정보 포함)
export interface Post {
  id: number         // v2: postId → id
  userId: number
  userName: string
  title: string
  content: string
  viewCount: number  // v2: views → viewCount
  likeCount: number  // v2: likes → likeCount
  commentCount: number // v2: 추가된 필드
  // 캐시 플래그 - 게시글의 인기도/성격 표시:
  // NOTICE: 공지사항
  // REALTIME: 실시간 인기 게시글
  // WEEKLY: 주간 인기 게시글
  // LEGEND: 레전드 게시글 (역대 최고 인기)
  postCacheFlag?: "NOTICE" | "REALTIME" | "WEEKLY" | "LEGEND"
  createdAt: string  // v2: Instant → ISO string
  updatedAt: string  // v2: Instant → ISO string
  isLiked: boolean   // v2: userLike → isLiked (현재 사용자의 좋아요 여부)
  isNotice: boolean  // v2: notice → isNotice (공지사항 여부)
  password?: number
}

// 간단한 게시글 타입 (목록용) - v2 백엔드 SimplePostResDTO 호환
// Post와 차이: updatedAt, isLiked, password 필드가 없음 (목록에서 불필요한 정보)
export interface SimplePost {
  id: number         // v2: postId → id
  userId: number
  userName: string
  title: string
  content: string    // v2: 추가된 필드 (간단한 내용 미리보기)
  commentCount: number
  likeCount: number  // v2: likes → likeCount
  viewCount: number  // v2: views → viewCount
  createdAt: string  // v2: Instant → ISO string
  postCacheFlag?: "NOTICE" | "REALTIME" | "WEEKLY" | "LEGEND"
  isNotice: boolean  // v2: _notice → isNotice
}
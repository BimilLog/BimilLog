// Post-related type definitions

// 게시글 타입 - v2 백엔드 FullPostResDTO 호환
export interface Post {
  id: number         // v2: postId → id
  userId: number
  userName: string
  title: string
  content: string
  viewCount: number  // v2: views → viewCount
  likeCount: number  // v2: likes → likeCount
  commentCount: number // v2: 추가된 필드
  postCacheFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  createdAt: string  // v2: Instant → ISO string
  updatedAt: string  // v2: Instant → ISO string
  isLiked: boolean   // v2: userLike → isLiked
  isNotice: boolean  // v2: notice → isNotice
  password?: number
}

// 간단한 게시글 타입 (목록용) - v2 백엔드 SimplePostResDTO 호환
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
  postCacheFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  isNotice: boolean  // v2: _notice → isNotice
}
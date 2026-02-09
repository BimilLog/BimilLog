// Post-related type definitions

// 게시글 특집 타입 - 백엔드 PostCacheFlag enum 호환
export type FeaturedType = 'REALTIME' | 'WEEKLY' | 'LEGEND' | 'NOTICE';

// 게시글 타입 - v2 백엔드 FullPostDTO 호환
// 게시글 상세 페이지에서 사용 (모든 정보 포함)
export interface Post {
  id: number         // v2: postId → id
  memberId: number
  memberName: string
  title: string
  content: string
  viewCount: number  // v2: views → viewCount
  likeCount: number  // v2: likes → likeCount
  commentCount: number // v2: 추가된 필드
  createdAt: string  // v2: Instant → ISO string
  updatedAt: string  // v2: Instant → ISO string
  liked: boolean     // v2: Jackson이 isLiked를 liked로 직렬화
  password?: number
  featuredType?: FeaturedType | null  // 공지/주간/레전드/실시간 (null = 일반 게시글)
}

// 간단한 게시글 타입 (목록용) - v2 백엔드 PostSimpleDetail 호환
// Post와 차이: updatedAt, liked, password 필드가 없음 (목록에서 불필요한 정보)
export interface SimplePost {
  id: number         // v2: postId → id
  memberId: number
  memberName: string
  title: string
  commentCount: number
  likeCount: number  // v2: likes → likeCount
  viewCount: number  // v2: views → viewCount
  createdAt: string  // v2: Instant → ISO string
  featuredType?: FeaturedType | null  // 공지/주간/레전드/실시간 (null = 일반 게시글)
}

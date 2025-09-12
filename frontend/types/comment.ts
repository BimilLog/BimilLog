// Comment-related type definitions

// 댓글 타입 - v2 백엔드 CommentDTO 호환
export interface Comment {
  id: number
  parentId?: number
  postId: number
  userId?: number
  userName: string
  content: string
  popular: boolean
  deleted: boolean
  likeCount: number  // v2: likes → likeCount
  createdAt: string
  userLike: boolean
}

// 간단한 댓글 타입 (목록용) - v2 백엔드 호환
export interface SimpleComment {
  id: number
  postId: number
  userName: string
  content: string
  likeCount: number  // v2: likes → likeCount
  userLike: boolean
  createdAt: string
}
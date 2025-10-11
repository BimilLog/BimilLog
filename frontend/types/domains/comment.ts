// Comment-related type definitions

// 댓글 타입 - v2 백엔드 CommentDTO 호환
export interface Comment {
  id: number
  parentId?: number       // 대댓글인 경우 부모 댓글 ID (계층 구조)
  postId: number
  memberId?: number       // 탈퇴한 사용자의 경우 null 가능
  memberName: string
  content: string
  popular: boolean        // 인기 댓글 여부 (좋아요 많이 받은 댓글)
  deleted: boolean        // 삭제된 댓글 여부 (소프트 삭제)
  likeCount: number       // v2: likes → likeCount (총 좋아요 수)
  createdAt: string
  userLike: boolean       // 현재 사용자가 이 댓글에 좋아요를 눌렀는지 여부
  replies?: Comment[]     // 계층 구조를 위한 대댓글 목록 (프론트엔드에서 추가)
}

// 간단한 댓글 타입 (목록용) - v2 백엔드 호환
// Comment와 차이: parentId, popular, deleted 필드가 없음 (마이페이지 댓글 목록용)
export interface SimpleComment {
  id: number
  postId: number
  memberName: string
  content: string
  likeCount: number       // v2: likes → likeCount
  userLike: boolean       // 현재 사용자의 좋아요 여부
  createdAt: string
}
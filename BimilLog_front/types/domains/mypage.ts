// MyPage-related type definitions

import { PageResponse } from '@/types/common';
import { SimplePost } from './post';
import { SimpleComment } from './comment';

/**
 * 사용자 댓글 활동
 * 백엔드: MemberActivityComment.java
 */
export interface MemberActivityComment {
  writeComments: PageResponse<SimpleComment>;
  likedComments: PageResponse<SimpleComment>;
}

/**
 * 사용자 게시글 활동
 * 백엔드: MemberActivityPost.java
 */
export interface MemberActivityPost {
  writePosts: PageResponse<SimplePost>;
  likedPosts: PageResponse<SimplePost>;
}

/**
 * 마이페이지 통합 데이터
 * 백엔드: MyPageDTO.java
 *
 * 작성글, 작성댓글, 추천글, 추천댓글을 1번의 API 호출로 조회
 */
export interface MyPageDTO {
  memberActivityComment: MemberActivityComment;
  memberActivityPost: MemberActivityPost;
}

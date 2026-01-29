import { useInfiniteQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { commentQuery } from '@/lib/api';
import type { Comment } from '@/types/domains/comment';
import { useMemo } from 'react';

/**
 * Comment 관련 Query hooks
 * 댓글 시스템에서 사용되는 데이터 조회 훅들
 */

export interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

/**
 * 댓글 트리 구조 빌드: parentId를 기반으로 평면 배열을 계층 구조로 변환
 */
const buildCommentTree = (comments: Comment[]): CommentWithReplies[] => {
  const commentMap = new Map<number, CommentWithReplies>();
  const rootComments: CommentWithReplies[] = [];

  // 모든 댓글을 Map에 저장하여 빠른 조회가 가능하도록 함
  comments.forEach((comment) => {
    commentMap.set(comment.id, { ...comment, replies: [] });
  });

  // 부모-자식 관계 설정: parentId가 있으면 해당 부모의 replies에 추가
  comments.forEach((comment) => {
    // 클로저 테이블에서 루트 댓글은 parentId가 자기 자신을 가리키거나 null
    if (!comment.parentId || comment.parentId === comment.id) {
      // parentId가 없거나 자기 자신을 가리키는 경우만 루트로 처리
      rootComments.push(commentMap.get(comment.id)!);
    } else if (commentMap.has(comment.parentId)) {
      // parentId가 존재하고 부모 댓글이 Map에 있는 경우 자식으로 추가
      const parent = commentMap.get(comment.parentId)!;
      const child = commentMap.get(comment.id)!;
      parent.replies!.push(child);
    }
  });

  return rootComments;
};

/**
 * 댓글 목록 조회 (무한 스크롤 지원) - BFF 통합 응답
 * 인기댓글 + 일반댓글을 단일 API로 조회
 * TanStack Query의 useInfiniteQuery를 사용하여 페이지네이션 구현
 */
export const useCommentsQuery = (postId: number) => {
  const query = useInfiniteQuery({
    queryKey: queryKeys.comment.list(postId),
    queryFn: ({ pageParam = 0 }) => commentQuery.getByPostId(postId, pageParam),
    enabled: !!postId && postId > 0,
    staleTime: 1 * 60 * 1000, // 1분 - mutation 후 빠른 동기화를 위해 짧게 설정
    gcTime: 5 * 60 * 1000, // 5분
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.success || !lastPage.data) return undefined;
      if (lastPage.data.commentInfoPage.last) return undefined;
      return allPages.length; // 다음 페이지 번호 반환
    },
  });

  // 인기 댓글 (첫 페이지에서만 제공)
  const popularComments = useMemo(() => {
    if (!query.data?.pages?.[0]) return [];
    const firstPage = query.data.pages[0];
    if (!firstPage.success || !firstPage.data) return [];
    return firstPage.data.popularCommentList || [];
  }, [query.data]);

  // 모든 페이지의 댓글을 평면 배열로 병합 후 트리 구조로 변환
  const comments = useMemo(() => {
    if (!query.data?.pages) return [];

    // 모든 페이지의 댓글을 평면 배열로 병합
    const allComments: Comment[] = [];
    query.data.pages.forEach((page) => {
      if (page.success && page.data?.commentInfoPage?.content) {
        allComments.push(...page.data.commentInfoPage.content);
      }
    });

    // 중복 제거 (같은 ID가 여러 페이지에 나타날 수 있음)
    const uniqueComments = allComments.reduce((acc, comment) => {
      const exists = acc.find((c) => c.id === comment.id);
      if (!exists) acc.push(comment);
      return acc;
    }, [] as Comment[]);

    // 트리 구조로 변환
    return buildCommentTree(uniqueComments);
  }, [query.data]);

  return {
    ...query,
    comments, // 계층 구조로 변환된 댓글 목록
    popularComments, // 인기 댓글 목록
  };
};
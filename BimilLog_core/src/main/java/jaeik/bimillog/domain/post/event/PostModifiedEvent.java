package jaeik.bimillog.domain.post.event;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;

/**
 * <h2>글 수정 캐시 이벤트</h2>
 * <p>게시글 수정 후 모든 JSON LIST 캐시의 제목을 갱신하기 위한 이벤트</p>
 *
 * @param postId      수정된 게시글 ID
 * @param updatedPost 수정된 게시글 요약 정보
 * @author Jaeik
 * @version 2.8.0
 */
public record PostModifiedEvent(Long postId, PostSimpleDetail updatedPost) {}

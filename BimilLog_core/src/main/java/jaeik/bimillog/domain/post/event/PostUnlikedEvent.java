package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 추천취소 캐시 이벤트</h2>
 * <p>게시글 추천취소 후 좋아요 카운터 -1 및 실시간 인기글 점수 -4.0을 반영하기 위한 이벤트</p>
 *
 * @param postId 추천취소된 게시글 ID
 * @author Jaeik
 * @version 2.8.0
 */
public record PostUnlikedEvent(Long postId) {}

package jaeik.bimillog.domain.post.event;

/**
 * <h2>게시글 상세 조회 캐시 이벤트</h2>
 * <p>게시글 상세 조회 시 조회수 증가 및 실시간 인기글 점수를 반영하기 위한 이벤트</p>
 *
 * @param postId    조회된 게시글 ID
 * @param viewerKey 조회자 식별 키 (중복 조회 방지용)
 * @author Jaeik
 * @version 2.8.0
 */
public record PostDetailViewedEvent(Long postId, String viewerKey) {}

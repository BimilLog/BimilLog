package jaeik.bimillog.domain.post.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * <h2>Post 도메인용 Comment 조회 Port</h2>
 * <p>Post 도메인에서 Comment 도메인의 데이터가 필요할 때 사용하는 인터페이스입니다.</p>
 * <p>헥사고날 아키텍처 원칙을 준수하여 Service가 UseCase 대신 Port에 의존하도록 합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommentQueryPort {

    /**
     * <h3>단일 게시글의 댓글 수 조회</h3>
     * <p>단일 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void countByPostId(Long postId);

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회하여 N+1 문제를 해결합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds);
}
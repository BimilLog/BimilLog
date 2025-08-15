package jaeik.growfarm.domain.comment.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * <h2>댓글 추천 조회 포트</h2>
 * <p>댓글 추천 엔티티 조회를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 조회 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikeQueryPort {

    /**
     * <h3>여러 댓글 ID에 대한 추천 수 조회</h3>
     * <p>주어진 댓글 ID 목록에 해당하는 각 댓글의 추천 수를 조회합니다.</p>
     *
     * @param commentIds 댓글 ID 목록
     * @return Map<Long, Long> 댓글 ID를 키로, 추천 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, Long> countByCommentIds(List<Long> commentIds);

    /**
     * <h3>사용자가 댓글에 추천을 눌렀는지 여부 확인</h3>
     * <p>주어진 댓글과 사용자가 이미 추천 관계인지 확인합니다.</p>
     * <p>성능 최적화: EXISTS 쿼리를 사용하여 효율적으로 존재 여부만 확인합니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isLikedByUser(Long commentId, Long userId);
}
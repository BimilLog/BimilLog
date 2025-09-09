package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.entity.CommentLike;

/**
 * <h2>CommentLikePort</h2>
 * <p>
 * 댓글 추천 시스템을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 댓글 추천 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * </p>
 * <p>
 * 이 포트는 댓글의 추천 기능을 관리합니다:
 * - 추천 정보 저장: 사용자가 댓글에 추천을 누를 때의 관계 저장
 * - 추천 취소: 이미 추천한 댓글에 대한 추천 취소 처리
 * - 추천 상태 확인: 특정 사용자가 댓글을 추천했는지 여부 확인
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 커뮤니티 활성화 - 좋은 댓글에 대한 사용자 피드백 수집
 * 2. 인기 댓글 식별 - 추천 수를 기반으로 한 댓글 정렬 및 노출
 * 3. 중복 추천 방지 - 동일 사용자의 중복 추천 차단
 * </p>
 * <p>
 * CommentService에서 댓글 추천/취소 로직 실행 시 사용됩니다.
 * CommentQueryController에서 댓글 목록 조회 시 추천 상태 확인을 위해 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikePort {

    /**
     * <h3>댓글 추천 관계 저장</h3>
     * <p>사용자와 댓글 간의 추천 관계를 저장합니다.</p>
     * <p>이미 추천 관계가 존재하는 경우 중복 추천을 방지하기 위해 사전에 검증이 필요합니다.</p>
     * <p>CommentService에서 사용자의 댓글 추천 요청을 처리할 때 호출됩니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    CommentLike save(CommentLike commentLike);

    /**
     * <h3>댓글 추천 관계 삭제</h3>
     * <p>특정 사용자와 댓글 간의 추천 관계를 삭제합니다.</p>
     * <p>추천 취소 기능을 구현하기 위해 사용되며, 존재하지 않는 관계 삭제 시도는 조용히 무시됩니다.</p>
     * <p>CommentService에서 사용자의 댓글 추천 취소 요청을 처리할 때 호출됩니다.</p>
     *
     * @param commentId 추천을 삭제할 댓글 ID
     * @param userId    추천을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteLikeByIds(Long commentId, Long userId);

    /**
     * <h3>댓글 추천 상태 확인</h3>
     * <p>특정 사용자가 해당 댓글을 이미 추천했는지 여부를 확인합니다.</p>
     * <p>중복 추천 방지와 UI 상태 표시를 위해 사용됩니다.</p>
     * <p>CommentService에서 추천/취소 로직 분기 처리 시 호출됩니다.</p>
     * <p>CommentQueryController에서 댓글 목록에 추천 상태를 포함하여 조회할 때 호출됩니다.</p>
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return boolean 추천을 눌렀으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean isLikedByUser(Long commentId, Long userId);
}
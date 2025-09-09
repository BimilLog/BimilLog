package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.post.entity.Post;

/**
 * <h2>CommentToPostPort</h2>
 * <p>
 * Comment 도메인에서 Post 도메인의 데이터에 접근하기 위한 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 도메인 간 의존성을 추상화하여 도메인의 독립성을 보장합니다.
 * </p>
 * <p>
 * 이 포트는 댓글 도메인이 게시글 도메인의 정보를 조회하는 기능을 제공합니다:
 * - 게시글 존재성 검증: 댓글 작성 대상 게시글의 유효성 확인
 * - 게시글 정보 조회: 댓글과 연관된 게시글의 세부 정보 획득
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 데이터 무결성 보장 - 존재하지 않는 게시글에 댓글 작성 방지
 * 2. 권한 검증 지원 - 삭제된 게시글이나 비공개 게시글에 대한 댓글 작성 제한
 * 3. 도메인 경계 유지 - Comment 도메인이 Post 도메인의 구현에 직접 의존하지 않음
 * </p>
 * <p>
 * CommentService에서 새 댓글 작성 시 게시글 존재성 검증을 위해 사용됩니다.
 * CommentService에서 댓글과 연관된 게시글 정보가 필요할 때 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentToPostPort {

    /**
     * <h3>게시글 ID로 게시글 엔티티 조회</h3>
     * <p>특정 ID에 해당하는 게시글 엔티티를 조회합니다.</p>
     * <p>댓글 작성 전 게시글 존재성 검증과 게시글 관련 비즈니스 로직 실행에 사용됩니다.</p>
     * <p>CommentService에서 댓글 작성 전 게시글 유효성 검증 시 호출됩니다.</p>
     * <p>CommentService에서 댓글 알림 발송 시 게시글 정보 조회를 위해 호출됩니다.</p>
     *
     * @param postId 게시글 ID
     * @return Post 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Post findById(Long postId);
}

package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentLike;

/**
 * <h2>댓글 추천 명령 포트</h2>
 * <p>댓글 추천 엔티티 생성/수정/삭제를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentLikeCommandPort {

    /**
     * <h3>댓글 추천 저장</h3>
     * <p>주어진 댓글 추천 엔티티를 저장합니다.</p>
     *
     * @param commentLike 저장할 댓글 추천 엔티티
     * @return CommentLike 저장된 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    CommentLike save(CommentLike commentLike);

    /**
     * <h3>댓글 추천 삭제</h3>
     * <p>주어진 댓글 추천 엔티티를 삭제합니다.</p>
     *
     * @param commentLike 삭제할 댓글 추천 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(CommentLike commentLike);
}
package jaeik.bimillog.domain.comment.application.port.out;

import jaeik.bimillog.domain.comment.application.service.CommentCommandService;

/**
 * <h2>댓글 삭제 포트</h2>
 * <p>댓글 도메인의 하드 삭제 작업을 담당하는 포트입니다.</p>
 * <p>자손이 없는 댓글의 완전 제거 (댓글과 관련 클로저 삭제)</p>
 * <p>소프트 삭제는 엔티티 메서드와 더티 체킹으로 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentDeletePort {

    /**
     * <h3>댓글 하드 삭제</h3>
     * <p>자손이 없는 댓글의 하드 삭제를 실행합니다.</p>
     * <p>댓글과 관련 클로저 테이블 레코드를 완전 제거합니다.</p>
     * <p>{@link CommentCommandService}에서 자손이 없는 댓글 삭제 시 호출됩니다.</p>
     *
     * @param commentId 삭제 처리할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId);
}
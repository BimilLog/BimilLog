package jaeik.growfarm.domain.comment.application.port.out;

import jaeik.growfarm.domain.comment.entity.CommentClosure;

/**
 * <h2>댓글 클로저 명령 포트</h2>
 * <p>댓글 클로저 엔티티 생성/수정/삭제를 위한 Out-Port</p>
 * <p>CQRS 패턴에 따른 명령 전용 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentClosureCommandPort {

    /**
     * <h3>댓글 클로저 저장</h3>
     * <p>주어진 댓글 클로저 엔티티를 저장합니다.</p>
     *
     * @param commentClosure 저장할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void save(CommentClosure commentClosure);

    /**
     * <h3>댓글 클로저 삭제</h3>
     * <p>주어진 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentClosure 삭제할 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void delete(CommentClosure commentClosure);

    /**
     * <h3>자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     *
     * @param commentId 자손 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByDescendantId(Long commentId);

    /**
     * <h3>여러 자손 ID로 댓글 클로저 삭제</h3>
     * <p>주어진 여러 자손 댓글 ID와 관련된 모든 댓글 클로저 엔티티를 삭제합니다.</p>
     * 
     * <p><strong>⚠️ TODO: 현재 미사용 메서드 - 게시글 삭제 시 성능 최적화용</strong></p>
     * <ul>
     *   <li><strong>목적</strong>: 게시글 삭제 시 해당 게시글의 모든 댓글 클로저를 배치로 삭제</li>
     *   <li><strong>현재 상황</strong>: CommentService.handlePostDeletedEvent에서 commentCommandPort.deleteAllByPostId만 사용</li>
     *   <li><strong>문제점</strong>: 댓글만 삭제하고 클로저는 CASCADE 의존 (데이터베이스 설정에 따라 다름)</li>
     *   <li><strong>성능 이점</strong>: 100개 댓글이 있는 게시글 삭제 시 100번 개별 삭제 대신 1번 배치 삭제</li>
     *   <li><strong>구현 방법</strong>:</li>
     *   <li>&nbsp;&nbsp;1. PostDeletedEvent 처리 시 해당 게시글의 모든 댓글 ID 조회</li>
     *   <li>&nbsp;&nbsp;2. 이 메서드로 클로저 배치 삭제</li>
     *   <li>&nbsp;&nbsp;3. 그 다음 댓글 배치 삭제</li>
     *   <li><strong>연결 지점</strong>: CommentService.handlePostDeletedEvent 메서드 개선</li>
     *   <li><strong>참고</strong>: 현재 개별 댓글 삭제는 deleteByDescendantId 사용 (CommentDomainService.deleteComment)</li>
     * </ul>
     *
     * @param commentIds 여러 자손 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByDescendantIds(java.util.List<Long> commentIds);
}
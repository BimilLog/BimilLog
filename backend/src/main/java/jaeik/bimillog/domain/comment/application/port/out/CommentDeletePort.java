package jaeik.bimillog.domain.comment.application.port.out;

/**
 * <h2>CommentDeletePort</h2>
 * <p>
 * 댓글 도메인의 삭제 작업을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 댓글 삭제 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * </p>
 * <p>
 * 이 포트는 댓글의 복잡한 삭제 정책을 처리합니다:
 * - 하드 삭제: 자손 댓글이 없는 경우 완전 제거
 * - 소프트 삭제: 자손 댓글이 있는 경우 내용만 익명화
 * - 계층 구조 유지: Closure Table을 활용한 댓글 트리 구조 보존
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 댓글 스레드 무결성 유지 - 부모 댓글 삭제 시 자손 댓글의 컨텍스트 보존
 * 2. 사용자 탈퇴 처리 - 댓글 기록은 유지하되 개인정보는 제거
 * 3. 게시글 삭제 시 연관 댓글 일괄 정리
 * </p>
 * <p>
 * CommentService에서 삭제 로직 실행 시 사용됩니다.
 * PostService에서 게시글 삭제 시 연관 댓글 정리를 위해 사용됩니다.
 * UserService에서 회원 탈퇴 시 댓글 처리를 위해 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentDeletePort {

    /**
     * <h3>게시글 삭제 시 연관 댓글 일괄 제거</h3>
     * <p>게시글이 삭제될 때 해당 게시글의 모든 댓글과 댓글 클로저를 완전히 제거합니다.</p>
     * <p>이 작업은 게시글과 댓글 간의 참조 무결성을 보장하고 고아 댓글 발생을 방지합니다.</p>
     * <p>PostService의 게시글 삭제 로직에서 cascade 삭제를 위해 호출됩니다.</p>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByPostId(Long postId);

    /**
     * <h3>댓글 삭제 정책 실행 (지능형 삭제)</h3>
     * <p>댓글의 계층 구조를 고려하여 최적의 삭제 방식을 자동으로 결정하고 실행합니다.</p>
     * <p>삭제 정책:</p>
     * <p>- 자손 댓글이 없는 경우: 하드 삭제 (댓글과 관련 클로저 완전 제거)</p>
     * <p>- 자손 댓글이 있는 경우: 소프트 삭제 (내용 익명화, 구조 보존)</p>
     * <p>이 방식으로 댓글 스레드의 무결성을 유지하면서 사용자 요청을 충족합니다.</p>
     * <p>CommentService의 개별 댓글 삭제 로직에서 호출됩니다.</p>
     *
     * @param commentId 삭제 처리할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId);

    /**
     * <h3>회원 탈퇴 시 댓글 데이터 정리</h3>
     * <p>사용자 탈퇴 시 개인정보 보호와 댓글 스레드 무결성을 동시에 만족하는 처리를 수행합니다.</p>
     * <p>처리 방식:</p>
     * <p>- 자손이 있는 댓글: 소프트 삭제로 내용만 익명화 ("삭제된 댓글입니다")</p>
     * <p>- 자손이 없는 댓글: 하드 삭제로 완전 제거</p>
     * <p>이를 통해 개인정보는 완전히 제거하되 댓글 스레드의 맥락은 보존합니다.</p>
     * <p>UserService의 회원 탈퇴 로직에서 개인정보 정리 단계에서 호출됩니다.</p>
     *
     * @param userId 탈퇴 처리할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);
}
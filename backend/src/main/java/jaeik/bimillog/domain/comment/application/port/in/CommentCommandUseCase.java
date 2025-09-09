package jaeik.bimillog.domain.comment.application.port.in;

import jaeik.bimillog.domain.comment.entity.Comment;

/**
 * <h2>댓글 명령 요구사항</h2>
 * <p>
 * 댓글 시스템의 모든 쓰기 작업을 정의하는 핵심 비즈니스 인터페이스입니다.
 * 계층형 댓글 구조를 지원하며 로그인 사용자와 익명 사용자 모두가 사용할 수 있습니다.
 * </p>
 * <p>
 * 이 인터페이스는 댓글 작성, 수정, 삭제, 추천 기능을 제공하며, 사용자 탈퇴 시 댓글 처리까지 담당합니다.
 * 클로저 테이블을 활용한 계층형 구조로 부모-자식 댓글 관계를 효율적으로 관리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface CommentCommandUseCase {

    /**
     * <h3>댓글 작성</h3>
     * <p>새로운 댓글을 작성하고 계층 구조에 맞게 저장합니다.</p>
     * <p>로그인 사용자는 별도 인증 없이 댓글을 작성할 수 있으며, 익명 사용자는 비밀번호를 설정해야 합니다.</p>
     * <p>부모 댓글이 있는 경우 클로저 테이블에 계층 관계를 자동으로 등록합니다.</p>
     * <p>CommentCommandController에서 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param postId 게시글 ID
     * @param parentId 부모 댓글 ID (대댓글인 경우)
     * @param content 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void writeComment(Long userId, Long postId, Long parentId, String content, Integer password);

    /**
     * <h3>댓글 수정</h3>
     * <p>기존 댓글의 내용을 수정합니다.</p>
     * <p>댓글 작성자만 수정이 가능하며, 익명 댓글의 경우 비밀번호 검증을 통해 본인 확인을 수행합니다.</p>
     * <p>삭제된 댓글이나 타인의 댓글 수정 시도 시 예외를 발생시킵니다.</p>
     * <p>CommentCommandController에서 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param commentId 수정할 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param content 새로운 댓글 내용
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void updateComment(Long commentId, Long userId, String content, Integer password);

    /**
     * <h3>댓글 삭제</h3>
     * <p>댓글을 삭제하며, 자식 댓글 존재 여부에 따라 소프트 삭제 또는 하드 삭제를 수행합니다.</p>
     * <p>자식 댓글이 있는 경우 소프트 삭제로 내용만 숨기고, 자식 댓글이 없는 경우 완전히 삭제합니다.</p>
     * <p>댓글 작성자만 삭제가 가능하며, 익명 댓글의 경우 비밀번호 검증을 통해 본인 확인을 수행합니다.</p>
     * <p>CommentCommandController에서 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 댓글
     * @param password 댓글 비밀번호 (익명 댓글인 경우)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteComment(Long commentId, Long userId, Integer password);

    /**
     * <h3>사용자 탈퇴 시 댓글 처리</h3>
     * <p>사용자 탈퇴 시 해당 사용자의 모든 댓글에 대해 비즈니스 규칙에 따른 적절한 처리를 수행합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화 (댓글 구조 유지를 위해)</p>
     * <p>자손이 없는 댓글: 하드 삭제 (불필요한 데이터 제거)</p>
     * <p>이 처리를 통해 댓글 계층 구조의 무결성을 보장하면서도 개인정보를 완전히 삭제합니다.</p>
     * <p>CommentRemoveListener에서 사용자 탈퇴 이벤트 발생 시 호출합니다.</p>
     *
     * @param userId 탈퇴하는 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void processUserCommentsOnWithdrawal(Long userId);

    /**
     * <h3>댓글 추천/취소</h3>
     * <p>댓글에 대한 추천을 토글 방식으로 처리합니다.</p>
     * <p>이미 추천한 댓글을 다시 누르면 추천이 취소되고, 추천하지 않은 댓글을 누르면 추천됩니다.</p>
     * <p>익명 사용자도 추천 기능을 사용할 수 있으며, 중복 추천 방지를 위해 세션 기반으로 관리합니다.</p>
     * <p>CommentCommandController에서 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param userId 사용자 ID (로그인한 경우), null인 경우 익명 사용자
     * @param commentId 추천/취소할 댓글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void likeComment(Long userId, Long commentId);

}

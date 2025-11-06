package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.in.global.listener.MemberWithdrawListener;
import jaeik.bimillog.in.post.web.PostCommandController;


/**
 * <h2>게시글 명령 유스케이스</h2>
 * <p>게시글 도메인의 명령 작업을 담당하는 유스케이스입니다.</p>
 * <p>게시글 작성, 수정, 삭제</p>
 * <p>익명/회원 게시글 권한 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCommandUseCase {

    /**
     * <h3>게시글 작성</h3>
     * <p>새로운 게시글을 생성하고 저장합니다.</p>
     * <p>익명/회원 게시글 모두 지원하며, 익명 게시글은 비밀번호 설정이 가능합니다.</p>
     * <p>{@link PostCommandController}에서 게시글 작성 API 처리 시 호출됩니다.</p>
     *
     * @param memberId   게시글 작성자의 사용자 ID (null이면 익명 게시글)
     * @param title    게시글 제목
     * @param content  게시글 본문 내용
     * @param password 게시글 비밀번호 (익명 게시글인 경우)
     * @return 생성된 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    Long writePost(Long memberId, String title, String content, Integer password);

    /**
     * <h3>게시글 수정</h3>
     * <p>기존 게시글의 제목과 내용을 수정합니다.</p>
     * <p>작성자 권한 검증 후 제목과 내용을 업데이트합니다.</p>
     * <p>{@link PostCommandController}에서 게시글 수정 API 처리 시 호출됩니다.</p>
     *
     * @param memberId  수정 요청 사용자 ID (작성자 검증용)
     * @param postId  수정할 게시글 ID
     * @param title   새로운 제목
     * @param content 새로운 내용
     * @author Jaeik
     * @since 2.0.0
     */
    void updatePost(Long memberId, Long postId, String title, String content, Integer password);

    /**
     * <h3>게시글 삭제</h3>
     * <p>게시글을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>작성자 권한 검증 후 게시글과 연관 데이터를 제거합니다.</p>
     * <p>{@link PostCommandController}에서 게시글 삭제 API 처리 시 호출됩니다.</p>
     *
     * @param memberId 삭제 요청 사용자 ID (작성자 검증용)
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePost(Long memberId, Long postId, Integer password);

    /**
     * <h3>회원 작성 게시글 일괄 삭제</h3>
     * <p>회원 탈퇴 시 해당 사용자가 작성한 모든 게시글을 삭제합니다.</p>
     * <p>{@link MemberWithdrawListener}에서 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param memberId 게시글을 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllPostsByMemberId(Long memberId);
}

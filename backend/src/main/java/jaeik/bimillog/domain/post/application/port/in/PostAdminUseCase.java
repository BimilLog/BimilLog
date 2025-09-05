package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.exception.PostCustomException;

/**
 * <h2>게시글 공지사항 유스케이스</h2>
 * <p>게시글의 공지사항 설정/해제와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>관리자 권한이 필요한 기능들을 포함합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostAdminUseCase {

    /**
     * <h3>게시글 공지 설정</h3>
     * <p>특정 게시글을 공지로 설정합니다.</p>
     * <p>관리자 권한이 필요하며, 설정 후 공지사항 캐시를 무효화합니다.</p>
     *
     * @param postId 공지로 설정할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    void setPostAsNotice(Long postId);

    /**
     * <h3>게시글 공지 해제</h3>
     * <p>게시글의 공지 설정을 해제합니다.</p>
     * <p>관리자 권한이 필요하며, 해제 후 공지사항 캐시를 무효화합니다.</p>
     *
     * @param postId 공지 설정을 해제할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    void unsetPostAsNotice(Long postId);
}
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
     * <h3>게시글 공지 토글</h3>
     * <p>게시글의 공지 설정을 토글합니다. 현재 공지이면 해제하고, 공지가 아니면 설정합니다.</p>
     * <p>관리자 권한이 필요하며, 순수한 DB 업데이트만 담당합니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    void togglePostNotice(Long postId);

    /**
     * <h3>게시글 공지 상태 확인</h3>
     * <p>게시글의 현재 공지 상태를 확인합니다.</p>
     * <p>캐시 동기화를 위해 현재 상태를 조회할 때 사용됩니다.</p>
     *
     * @param postId 확인할 게시글 ID
     * @return 공지 상태 (true: 공지, false: 일반)
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    boolean isPostNotice(Long postId);
}
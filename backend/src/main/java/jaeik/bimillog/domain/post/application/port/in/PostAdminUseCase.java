package jaeik.bimillog.domain.post.application.port.in;

import jaeik.bimillog.domain.post.application.service.PostAdminService;
import jaeik.bimillog.domain.post.exception.PostCustomException;

/**
 * <h2>게시글 관리자 유스케이스</h2>
 * <p>게시글 도메인의 관리자 전용 기능을 처리하는 유스케이스입니다.</p>
 * <p>공지사항 토글: 일반 게시글을 공지로 승격하거나 공지를 일반으로 전환</p>
 * <p>공지 상태 조회: 게시글의 현재 공지 여부 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostAdminUseCase {

    /**
     * <h3>게시글 공지사항 상태 토글</h3>
     * <p>게시글의 공지사항 상태를 현재 상태의 반대로 변경합니다.</p>
     * <p>일반 게시글이면 공지로 설정하고, 공지 게시글이면 일반으로 해제합니다.</p>
     * <p>{@link PostAdminService}에서 관리자의 공지 관리 요청 시 호출됩니다.</p>
     *
     * @param postId 공지 상태를 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    void togglePostNotice(Long postId);

}
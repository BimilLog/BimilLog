package jaeik.bimillog.domain.post.application.port.in;

/**
 * <h2>게시글 캐시 유스케이스</h2>
 * <p>게시글 캐시 관리와 관련된 비즈니스 로직을 처리하는 유스케이스 인터페이스</p>
 * <p>공지사항 설정/해제 시 캐시 동기화를 담당합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PostCacheUseCase {

    /**
     * <h3>공지사항 캐시 동기화</h3>
     * <p>게시글의 공지 상태에 따라 캐시를 동기화합니다.</p>
     * <p>공지 설정 시 캐시에 추가, 공지 해제 시 캐시에서 제거합니다.</p>
     *
     * @param postId 동기화할 게시글 ID
     * @param isNotice 현재 공지 상태 (true: 공지, false: 일반)
     * @author Jaeik
     * @since 2.0.0
     */
    void syncNoticeCache(Long postId, boolean isNotice);
}
package jaeik.bimillog.domain.post.application.port.in;

public interface PostCacheUseCase {

    /**
     * <h3>공지 캐시 삭제</h3>
     * <p>공지사항 관련 캐시를 삭제합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
     void deleteNoticeCache();

    /**
     * <h3>단일 공지사항 캐시 추가</h3>
     * <p>특정 게시글을 공지사항 캐시에 추가합니다.</p>
     *
     * @param postId 추가할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void addSingleNoticeToCache(Long postId);

    /**
     * <h3>단일 공지사항 캐시 제거</h3>
     * <p>특정 게시글을 공지사항 캐시에서 제거합니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removeSingleNoticeFromCache(Long postId);
}

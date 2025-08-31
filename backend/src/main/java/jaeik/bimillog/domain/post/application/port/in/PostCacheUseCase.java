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
}

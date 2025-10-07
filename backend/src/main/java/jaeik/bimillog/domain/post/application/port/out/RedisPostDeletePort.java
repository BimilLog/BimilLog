package jaeik.bimillog.domain.post.application.port.out;

import jaeik.bimillog.domain.post.entity.PostCacheFlag;

public interface RedisPostDeletePort {

    /**
     * <h3>단일 게시글 캐시 무효화</h3>
     * <p>특정 게시글의 캐시 데이터를 Redis에서 삭제합니다.</p>
     * <p>PostCommandService에서 라이트 어라운드 패턴으로 게시글 수정/삭제 시 호출됩니다.</p>
     *
     * @param postId 캐시를 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteSinglePostCache(Long postId);

    /**
     * <h3>postIds 저장소에서 단일 게시글 제거</h3>
     * <p>postIds 영구 저장소에서 게시글 ID를 제거합니다 (LREM).</p>
     * <p>공지사항 해제 시 호출됩니다.</p>
     *
     * @param type 캐시 유형 (NOTICE만 사용)
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removePostIdFromStorage(PostCacheFlag type, Long postId);

}

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

    /**
     * <h3>게시글 목록 캐시에서 단일 게시글 제거 (Hash 필드 삭제)</h3>
     * <p>Redis Hash에서 특정 postId의 PostSimpleDetail을 삭제합니다.</p>
     * <p>게시글 수정/삭제 시 목록 캐시 무효화를 위해 호출됩니다.</p>
     *
     * @param type 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removePostFromListCache(PostCacheFlag type, Long postId);

    /**
     * <h3>실시간 인기글 점수 저장소에서 게시글 제거</h3>
     * <p>score:realtime Sorted Set에서 특정 postId를 삭제합니다.</p>
     * <p>게시글 삭제 시 실시간 인기글 점수 정리를 위해 호출됩니다.</p>
     *
     * @param postId 제거할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void removePostIdFromRealtimeScore(Long postId);

    /**
     * <h3>게시글 목록 캐시 전체 삭제</h3>
     * <p>특정 캐시 유형의 posts:{type} Hash 전체를 삭제합니다.</p>
     * <p>스케줄러 재실행 시 기존 목록 캐시를 초기화하기 위해 호출됩니다.</p>
     *
     * @param type 삭제할 캐시 유형 (REALTIME, WEEKLY, LEGEND, NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    void clearPostListCache(PostCacheFlag type);

}

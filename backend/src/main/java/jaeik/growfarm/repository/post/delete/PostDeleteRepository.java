package jaeik.growfarm.repository.post.delete;

import org.springframework.stereotype.Repository;

/**
 * <h2>게시글 삭제 관리 저장소</h2>
 * <p>
 * 게시글 관련 삭제 관리 기능을 담당한다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Repository
public interface PostDeleteRepository {

    /**
     * <h3>게시글 삭제 및 Redis 캐시 동기화</h3>
     * <p>
     * 게시글을 삭제하고, 해당 게시글이 인기글(실시간/주간/레전드)인 경우 Redis 캐시에서도 즉시 해당 인기글 목록을 삭제한다.
     * </p>
     *
     * @param postId 삭제할 게시글 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deletePostWithCacheSync(Long postId);
}
package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.out.PostCacheCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostCacheSyncPort;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostDetail;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostNoticeEventListener {

    private final PostCacheCommandPort postCacheCommandPort;
    private final PostCacheSyncPort postCacheSyncPort;

    /**
     * <h3>게시글 공지 설정 이벤트 처리</h3>
     * <p>게시글이 공지로 설정되었을 때 해당 공지를 캐시에 추가합니다.</p>
     * <p>캐시 추가 실패 시 로그를 남기고 예외를 전파하지 않습니다.</p>
     *
     * @param event 게시글 공지 설정 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostSetAsNotice(PostSetAsNoticeEvent event) {
        try {
            log.info("Post (ID: {}) set as notice event received. Adding notice to cache.", event.postId());
            
            // 새로운 공지사항의 상세 정보를 DB에서 조회
            PostDetail postDetail = postCacheSyncPort.findPostDetail(event.postId());
            if (postDetail != null) {
                // 단건을 리스트로 감싸서 캐시에 추가
                postCacheCommandPort.cachePostsWithDetails(PostCacheFlag.NOTICE, List.of(postDetail));
            }
        } catch (Exception e) {
            log.error("Failed to add notice to cache for post (ID: {}): {}", event.postId(), e.getMessage(), e);
        }
    }

    /**
     * <h3>게시글 공지 해제 이벤트 처리</h3>
     * <p>게시글의 공지 설정이 해제되었을 때 해당 공지를 캐시에서 제거합니다.</p>
     * <p>캐시 제거 실패 시 로그를 남기고 예외를 전파하지 않습니다.</p>
     *
     * @param event 게시글 공지 해제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostUnsetAsNotice(PostUnsetAsNoticeEvent event) {
        try {
            log.info("Post (ID: {}) unset as notice event received. Removing notice from cache.", event.postId());
            // 공지 캐시에서만 삭제 (성능 최적화)
            postCacheCommandPort.deleteCache(null, event.postId(), PostCacheFlag.NOTICE);
        } catch (Exception e) {
            log.error("Failed to remove notice from cache for post (ID: {}): {}", event.postId(), e.getMessage(), e);
        }
    }
}

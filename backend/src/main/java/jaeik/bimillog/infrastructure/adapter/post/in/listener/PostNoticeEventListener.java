package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostNoticeEventListener {

    private final PostCacheUseCase postCacheUseCase;

    /**
     * <h3>게시글 공지 설정 이벤트 처리</h3>
     * <p>게시글이 공지로 설정되었을 때 공지 캐시를 삭제합니다.</p>
     * <p>캐시 삭제 실패 시 로그를 남기고 예외를 전파하지 않습니다.</p>
     *
     * @param event 게시글 공지 설정 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostSetAsNotice(PostSetAsNoticeEvent event) {
        try {
            log.info("Post (ID: {}) set as notice event received. Deleting notice cache.", event.postId());
            postCacheUseCase.deleteNoticeCache();
        } catch (Exception e) {
            log.error("Failed to delete notice cache for post (ID: {}): {}", event.postId(), e.getMessage(), e);
        }
    }

    /**
     * <h3>게시글 공지 해제 이벤트 처리</h3>
     * <p>게시글의 공지 설정이 해제되었을 때 공지 캐시를 삭제합니다.</p>
     * <p>캐시 삭제 실패 시 로그를 남기고 예외를 전파하지 않습니다.</p>
     *
     * @param event 게시글 공지 해제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    public void handlePostUnsetAsNotice(PostUnsetAsNoticeEvent event) {
        try {
            log.info("Post (ID: {}) unset as notice event received. Deleting notice cache.", event.postId());
            postCacheUseCase.deleteNoticeCache();
        } catch (Exception e) {
            log.error("Failed to delete notice cache for post (ID: {}): {}", event.postId(), e.getMessage(), e);
        }
    }
}

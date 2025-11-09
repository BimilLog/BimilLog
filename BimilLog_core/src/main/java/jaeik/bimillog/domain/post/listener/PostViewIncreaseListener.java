package jaeik.bimillog.domain.post.listener;

import jaeik.bimillog.domain.post.event.PostViewedEvent;
import jaeik.bimillog.domain.post.service.PostInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>PostViewIncreaseListener</h2>
 * <p>
 * PostViewedEvent를 비동기적으로 처리하여 게시글 조회수를 증가시키는 이벤트 리스너입니다.
 * </p>
 * <p>
 * PostInteractionService.incrementViewCount()으로 부터 실행
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewIncreaseListener {

    private final PostInteractionService postInteractionService;

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     *
     * @param event 게시글 조회 이벤트 (PostViewedEvent)
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostViewedEvent(PostViewedEvent event) {
        Long postId = event.postId();
        
        if (postId == null) {
            log.warn("게시글 조회 이벤트 처리 실패: postId가 null입니다");
            return;
        }
        
        try {
            postInteractionService.incrementViewCount(postId);
        } catch (Exception e) {
            log.error("게시글 조회수 증가 실패: postId={}", postId, e);
        }
    }
}
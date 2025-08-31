package jaeik.bimillog.infrastructure.adapter.post.event;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 조회 이벤트 리스너</h2>
 * <p>
 * PostViewedEvent를 비동기적으로 처리하여 조회수를 증가시킵니다.
 * CQRS 패턴을 준수하기 위해 Query 작업과 Command 작업을 분리합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewEventListener {

    private final PostInteractionUseCase postInteractionUseCase;

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     * <p>
     * 게시글 조회 시 발생하는 이벤트를 비동기적으로 처리하여 조회수를 증가시킵니다.
     * 쿠키 기반 중복 조회 방지 로직을 적용합니다.
     * </p>
     *
     * @param event 게시글 조회 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostViewedEvent(PostViewedEvent event) {
        try {
            postInteractionUseCase.incrementViewCountWithCookie(
                    event.getPostId(), 
                    event.getRequest(), 
                    event.getResponse()
            );
            log.debug("Post view count incremented asynchronously for postId: {}", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to increment view count for postId: {}", event.getPostId(), e);
        }
    }
}
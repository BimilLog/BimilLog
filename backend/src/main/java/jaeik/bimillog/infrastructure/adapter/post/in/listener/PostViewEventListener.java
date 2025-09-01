package jaeik.bimillog.infrastructure.adapter.post.in.listener;

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
     * 중복 조회 검증은 Controller에서 이미 처리되었습니다.
     * </p>
     * <p>
     * null postId나 잘못된 postId로 인한 예외는 정상적인 상황이 아니므로,
     * 이런 경우에는 로그를 남기고 조회수 증가를 건너뜁니다.
     * </p>
     *
     * @param event 게시글 조회 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async
    public void handlePostViewedEvent(PostViewedEvent event) {
        Long postId = event.getPostId();
        
        // null postId 경고 로그는 남기되, 서비스 호출은 진행 (서비스에서 처리)
        if (postId == null) {
            log.warn("게시글 조회 이벤트 처리 실패: postId가 null입니다");
        }
        
        try {
            postInteractionUseCase.incrementViewCount(postId);
        } catch (Exception e) {
            log.error("게시글 조회수 증가 실패: postId={}", postId, e);
        }
    }


}
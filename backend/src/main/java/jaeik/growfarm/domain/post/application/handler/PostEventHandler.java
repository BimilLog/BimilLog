package jaeik.growfarm.domain.post.application.handler;

import jaeik.growfarm.domain.post.application.port.out.DeletePostLikePort;
import jaeik.growfarm.global.event.PostDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 이벤트 핸들러</h2>
 * <p>게시글 관련 도메인 이벤트를 처리하는 이벤트 핸들러 클래스</p>
 * <p>비동기 방식으로 이벤트를 처리하여 성능을 최적화합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventHandler {

    private final DeletePostLikePort deletePostLikePort;

    /**
     * <h3>게시글 삭제 이벤트 처리</h3>
     * <p>게시글이 삭제되었을 때 해당 게시글의 모든 좋아요를 삭제합니다.</p>
     * <p>비동기로 처리되어 게시글 삭제 트랜잭션에 영향을 주지 않습니다.</p>
     *
     * @param event 게시글 삭제 이벤트
     * @since 2.0.0
     * @author Jaeik
     */
    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post deleted event received: postId={}", event.postId());
        
        try {
            deletePostLikePort.deleteAllByPostId(event.postId());
            log.info("All post likes deleted successfully for postId={}", event.postId());
        } catch (Exception e) {
            log.error("Failed to delete post likes for postId={}", event.postId(), e);
            // 이벤트 처리 실패 시 재시도 로직이나 별도 처리 필요시 추가
        }
    }
}
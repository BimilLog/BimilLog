package jaeik.growfarm.domain.post.infrastructure.adapter.in.listener;

import jaeik.growfarm.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.growfarm.global.event.PostDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 이벤트 리스너</h2>
 * <p>게시글 관련 도메인 이벤트를 처리하는 이벤트 리스너</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventListener {

    private final PostLikeCommandPort postLikeCommandPort;

    // TODO : 글 삭제 이벤트 발생시 순서를 파악할 필요 있음 현재 추천 삭제만 확인이 되는데 글을 삭제할 때 추천도 삭제되는지 확인 필요하고,
    //  추천은 괜찮지만 댓글을 삭제하기 전에 글이 삭제되면 안되듯이 다른 핸들러도 파악하여 2차 이벤트 발생과정을 파악해야 함.
    //  예를들어 회원탈퇴 이벤트가 발생했을 때 어떤 사이드 이펙트가 발생하는지 엔티티의 연관관계에서 에러가 발생하지는 않는지 확인 필요.
    /**
     * <h3>게시글 추천 삭제 핸들러</h3>
     * <p>게시글이 삭제되었을 때 해당 게시글의 모든 추천를 삭제합니다.</p>
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
        log.info("글 삭제 이벤트 청취 글 Id = {}", event.postId());
        
        try {
            postLikeCommandPort.deleteAllByPostId(event.postId());
            log.info("모든 글의 추천 삭제 완료 글 Id = {}", event.postId());
        } catch (Exception e) {
            log.error("글 추천 삭제 실패 = {}", event.postId(), e);
            // 이벤트 처리 실패 시 재시도 로직이나 별도 처리 필요시 추가
        }
    }
}
package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.event.PostViewedEvent;
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
 * 헥사고날 아키텍처에서 이벤트 기반 비동기 처리를 통해 사용자 경험을 최적화하고,
 * CQRS 패턴에 따라 조회(Query)와 명령(Command) 작업을 완전히 분리합니다.
 * </p>
 * <p>
 * 사용자가 게시글을 클릭해서 상세 내용을 보는 상황에서 다음 흐름으로 실행됩니다:
 * 1. 사용자 클릭 → PostQueryController.getPost() 호출 → 게시글 내용 즉시 반환
 * 2. PostViewedEvent 발행 → 본 리스너가 비동기 수신 → PostInteractionService.incrementViewCount() 실행
 * 3. 조회수 DB 업데이트는 백그라운드에서 처리되어 사용자 응답 시간에 영향 없음
 * </p>
 * <p>
 * 이를 통해 사용자는 빠른 게시글 조회 경험을 얻고, 시스템은 인기도 측정을 위한 정확한 조회수를 비동기로 수집합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostViewIncreaseListener {

    private final PostInteractionUseCase postInteractionUseCase;

    /**
     * <h3>게시글 조회 이벤트 처리</h3>
     * <p>
     * 사용자가 게시글 상세 페이지를 조회할 때 PostQueryController에서 발행한 PostViewedEvent를 수신하여
     * 백그라운드에서 조회수를 증가시킵니다. 중복 조회 검증은 이미 Controller에서 쿠키 기반으로 완료되었습니다.
     * </p>
     * <p>
     * 게시글 조회 응답은 이미 사용자에게 전달된 후이므로, 여기서 발생하는 DB 업데이트나 예외는 
     * 사용자 경험에 영향을 주지 않고 시스템 모니터링 목적으로 로그만 남깁니다.
     * </p>
     * <p>
     * 인기도 측정, 트렌드 분석, 통계 대시보드 등에 활용될 정확한 조회수 데이터를 수집하는 역할을 합니다.
     * </p>
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
            postInteractionUseCase.incrementViewCount(postId);
        } catch (Exception e) {
            log.error("게시글 조회수 증가 실패: postId={}", postId, e);
        }
    }


}
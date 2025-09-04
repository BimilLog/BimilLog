package jaeik.bimillog.infrastructure.adapter.post.in.listener;

import jaeik.bimillog.domain.post.application.port.in.PostCacheUseCase;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 공지사항 이벤트 리스너</h2>
 * <p>게시글 공지사항 설정/해제 이벤트를 비동기적으로 처리하여 캐시를 관리합니다.</p>
 * <p>이벤트 기반 아키텍처를 통해 도메인 간 결합도를 낮춥니다.</p>
 * <p>헥사고널 아키텍처 Primary Adapter</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostNoticeListener {

    private final PostCacheUseCase postCacheUseCase;

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
            log.info("게시글 공지사항 설정 이벤트 수신: postId={}, 캐시에 공지사항 추가 중", event.postId());
            postCacheUseCase.addNoticeToCache(event.postId());
        } catch (Exception e) {
            log.error("게시글 공지사항 캐시 추가 실패: postId={}, error={}", event.postId(), e.getMessage(), e);
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
            log.info("게시글 공지사항 해제 이벤트 수신: postId={}, 캐시에서 공지사항 제거 중", event.postId());
            postCacheUseCase.removeNoticeFromCache(event.postId());
        } catch (Exception e) {
            log.error("게시글 공지사항 캐시 제거 실패: postId={}, error={}", event.postId(), e.getMessage(), e);
        }
    }
}

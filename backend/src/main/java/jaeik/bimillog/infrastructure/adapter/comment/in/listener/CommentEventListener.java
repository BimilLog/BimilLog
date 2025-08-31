package jaeik.bimillog.infrastructure.adapter.comment.in.listener;

import jaeik.bimillog.domain.comment.application.port.out.CommentCommandPort;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 이벤트 리스너</h2>
 * <p>댓글 도메인과 관련된 도메인 이벤트를 처리하는 리스너</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentCommandPort commentCommandPort;

    /**
     * <h3>사용자 탈퇴 이벤트 핸들러</h3>
     * <p>사용자 탈퇴 이벤트를 수신하여 해당 사용자의 댓글을 익명화 처리합니다.</p>
     * <p>개인정보보호법 및 GDPR 준수를 위한 필수 기능입니다.</p>
     *
     * @param event 사용자 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("User (ID: {}) withdrawn event received. Anonymizing comments.", event.userId());
        commentCommandPort.anonymizeUserComments(event.userId());
    }

}
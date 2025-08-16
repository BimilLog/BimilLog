package jaeik.growfarm.infrastructure.adapter.comment.in.listener;

import jaeik.growfarm.domain.comment.application.port.out.CommentCommandPort;
import jaeik.growfarm.domain.post.event.PostDeletedEvent;
import jaeik.growfarm.domain.auth.event.UserWithdrawnEvent;
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

    /**
     * <h3>게시글 삭제 이벤트 핸들러</h3>
     * <p>게시글 삭제 이벤트를 수신하여 해당 게시글의 모든 댓글을 삭제합니다.</p>
     * <p>DB CASCADE 대신 이벤트 방식을 사용하여 명시적이고 제어 가능한 삭제 프로세스를 구현합니다.</p>
     * 
     * <p><strong>삭제 순서:</strong> commentCommandPort.deleteAllByPostId()가 댓글과 관련 클로저를 함께 처리합니다.</p>
     *
     * @param event 게시글 삭제 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handlePostDeletedEvent(PostDeletedEvent event) {
        log.info("Post (ID: {}) deleted event received. Deleting all comments.", event.postId());
        commentCommandPort.deleteAllByPostId(event.postId());
    }
}
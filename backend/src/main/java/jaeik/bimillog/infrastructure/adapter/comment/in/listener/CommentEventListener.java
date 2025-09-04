package jaeik.bimillog.infrastructure.adapter.comment.in.listener;

import jaeik.bimillog.domain.admin.event.AdminWithdrawRequestedEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.service.CommentCommandService;
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

    private final CommentCommandService commentCommandService;

    /**
     * <h3>사용자 탈퇴 이벤트 핸들러</h3>
     * <p>사용자 탈퇴 이벤트를 수신하여 해당 사용자의 댓글을 적절히 처리합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화, 자손이 없는 댓글: 하드 삭제</p>
     *
     * @param event 사용자 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handleUserWithdrawnEvent(UserWithdrawnEvent event) {
        log.info("사용자 탈퇴 이벤트 수신 (사용자 ID: {}). 댓글 처리를 진행합니다.", event.userId());
        commentCommandService.processUserCommentsOnWithdrawal(event.userId());
    }

    /**
     * <h3>관리자 강제 탈퇴 이벤트 핸들러</h3>
     * <p>관리자 강제 탈퇴 이벤트를 수신하여 해당 사용자의 댓글을 적절히 처리합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화, 자손이 없는 댓글: 하드 삭제</p>
     *
     * @param event 관리자 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener
    public void handleAdminWithdrawRequestedEvent(AdminWithdrawRequestedEvent event) {
        log.info("관리자 강제 탈퇴 이벤트 수신 (사용자 ID: {}). 댓글 처리를 진행합니다.", event.userId());
        commentCommandService.processUserCommentsOnWithdrawal(event.userId());
    }

}
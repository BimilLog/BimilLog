package jaeik.bimillog.infrastructure.adapter.comment.in.listener;

import jaeik.bimillog.domain.admin.event.AdminWithdrawEvent;
import jaeik.bimillog.domain.auth.event.UserWithdrawnEvent;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>댓글 삭제 리스너</h2>
 * <p>사용자 탈퇴 및 강제 탈퇴 이벤트를 처리하는 리스너입니다.</p>
 * <p>댓글 삭제 이벤트 리스너 구현체입니다.</p>
 * <p>사용자 자발적/강제 탈퇴 시 댓글 익명화 및 삭제</p>
 * <p>이벤트 기반 비동기 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentRemoveListener {

    private final CommentCommandUseCase commentCommandUseCase;

    /**
     * <h3>탈퇴 이벤트 처리로 댓글 삭제</h3>
     * <p>사용자 탈퇴 및 강제 탈퇴 이벤트 발생 시 해당 사용자의 댓글을 삭제합니다.</p>
     * <p>자손이 있는 댓글: 소프트 삭제 + 익명화</p>
     * <p>자손이 없는 댓글: 하드 삭제</p>
     * <p>{@link UserWithdrawnEvent}, {@link AdminWithdrawEvent} 이벤트 발생시 탈퇴로 인한 댓글 삭제 흐름에서 호출됩니다.</p>
     *
     * @param event 사용자 탈퇴 또는 강제 탈퇴 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @Transactional
    @EventListener({UserWithdrawnEvent.class, AdminWithdrawEvent.class})
    public void handleCommentProcessingEvents(Object event) {
        Long userId;
        String eventType;
        
        if (event instanceof UserWithdrawnEvent withdrawnEvent) {
            userId = withdrawnEvent.userId();
            eventType = "사용자 탈퇴";
        } else if (event instanceof AdminWithdrawEvent adminEvent) {
            userId = adminEvent.userId();
            eventType = "관리자 강제 탈퇴";
        } else {
            log.warn("지원하지 않는 이벤트 타입: {}", event.getClass().getSimpleName());
            return;
        }
        
        log.info("{} 이벤트 수신 - 댓글 처리 시작: 사용자 ID={}", eventType, userId);
        
        try {
            commentCommandUseCase.processUserCommentsOnWithdrawal(userId);
            log.info("{} 댓글 처리 완료 - 사용자 ID={}", eventType, userId);
        } catch (Exception e) {
            log.error("{} 댓글 처리 실패 - 사용자 ID={}, error: {}", 
                    eventType, userId, e.getMessage(), e);
            throw e;
        }
    }
}
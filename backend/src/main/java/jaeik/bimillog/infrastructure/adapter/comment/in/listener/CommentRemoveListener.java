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
 * <h2>댓글 처리 이벤트 리스너</h2>
 * <p>사용자 탈퇴 시 댓글 익명화 및 삭제를 처리하는 리스너입니다.</p>
 * <p>Comment 도메인의 데이터 정리 책임을 담당합니다.</p>
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
     * <h3>댓글 처리 이벤트 핸들러</h3>
     * <p>사용자 탈퇴 또는 관리자 강제 탈퇴 이벤트를 수신하여 해당 사용자의 댓글을 처리합니다.</p>
     * <p>처리 방식:</p>
     * <ul>
     *   <li>자손이 있는 댓글: 소프트 삭제 + 익명화</li>
     *   <li>자손이 없는 댓글: 하드 삭제</li>
     * </ul>
     * <p>처리 대상 이벤트:</p>
     * <ul>
     *   <li>UserWithdrawnEvent: 사용자 자발적 탈퇴 시</li>
     *   <li>AdminWithdrawEvent: 관리자 강제 탈퇴 시</li>
     * </ul>
     *
     * @param event 사용자 탈퇴 또는 관리자 강제 탈퇴 이벤트
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
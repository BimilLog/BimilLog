package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.auth.application.port.in.TokenUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawListener {

    private final SocialWithdrawUseCase socialWithdrawUseCase;
    private final SseUseCase sseUseCase;
    private final FcmUseCase fcmUseCase;
    private final NotificationCommandUseCase notificationCommandUseCase;
    private final CommentCommandUseCase commentCommandUseCase;
    private final PostCommandUseCase postCommandUseCase;
    private final TokenUseCase tokenUseCase;
    private final PaperCommandUseCase paperCommandUseCase;
    private final AdminCommandUseCase adminCommandUseCase;
    private final UserCommandUseCase userCommandUseCase;

    /**
     * <h3>사용자 탈퇴 이벤트 처리</h3>
     * <p>사용자가 회원 탈퇴하거나 관리자에 의해 강제 탈퇴될 때 발생하는 이벤트를 처리합니다.</p>
     * <p>모든 관련 데이터를 순차적으로 정리합니다: SSE 연결, 소셜 계정 연동 해제, 댓글 처리, 게시글 삭제,
     * 토큰 무효화, FCM 토큰 삭제, 알림 삭제, 롤링페이퍼 메시지 삭제, 신고 기록 삭제, 계정 정보 삭제</p>
     *
     * @param userWithdrawnEvent 회원 탈퇴 이벤트 (userId, socialId, provider 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Async
    @EventListener
    @Transactional
    public void userWithdraw(UserWithdrawnEvent userWithdrawnEvent) {
        Long userId = userWithdrawnEvent.userId();
        String socialId = userWithdrawnEvent.socialId();
        SocialProvider provider = userWithdrawnEvent.provider();

        sseUseCase.deleteEmitters(userId, null);
        socialWithdrawUseCase.unlinkSocialAccount(provider, socialId);
        commentCommandUseCase.processUserCommentsOnWithdrawal(userId);
        postCommandUseCase.deleteAllPostsByUserId(userId); // 구현 필요
        tokenUseCase.deleteTokens(userId, null);
        fcmUseCase.deleteFcmTokens(userId, null);
        notificationCommandUseCase.deleteAllNotification(userId);
        paperCommandUseCase.deleteMessageInMyPaper(userId, null);
        adminCommandUseCase.deleteAllReportsByUserId(userId);
        userCommandUseCase.removeUserAccount(userId);
        SecurityContextHolder.clearContext();
    }
}

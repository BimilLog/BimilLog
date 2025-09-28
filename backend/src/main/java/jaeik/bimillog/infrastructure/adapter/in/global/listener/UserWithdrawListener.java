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
import jaeik.bimillog.domain.user.entity.SocialProvider;
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

    @Async
    @EventListener
    @Transactional
    public void userWithdraw(UserWithdrawnEvent userWithdrawnEvent) {
        Long userId = userWithdrawnEvent.userId();
        String socialId = userWithdrawnEvent.socialId();
        SocialProvider provider = userWithdrawnEvent.provider();

        sseUseCase.deleteAllEmitterByUserId(userId);
        socialWithdrawUseCase.unlinkSocialAccount(provider, socialId);
        commentCommandUseCase.processUserCommentsOnWithdrawal(userId);
        postCommandUseCase.deleteAllPostsByUserId(userId); // 구현 필요
        tokenUseCase.deleteTokens(userId, null);
        fcmUseCase.deleteFcmTokens(userId);
        notificationCommandUseCase.deleteAllNotification(userId); // 구현 필요
        paperCommandUseCase.deleteAllMessagesByUserId(userId); // 구현 필요
        adminCommandUseCase.deleteAllReportsByUserId(userId); // 구현 필요
        userCommandUseCase.removeUserAccount(userId); // 구현 필요
        SecurityContextHolder.clearContext();
    }
}

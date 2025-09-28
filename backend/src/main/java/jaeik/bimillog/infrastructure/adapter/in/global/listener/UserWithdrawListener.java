package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.admin.application.port.in.AdminCommandUseCase;
import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.auth.application.port.in.UserBanUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserCommandUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.event.UserWithdrawnEvent;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawListener {
    private final SocialWithdrawUseCase socialWithdrawUseCase;
    private final NotificationSseUseCase notificationSseUseCase;
    private final NotificationFcmUseCase notificationFcmUseCase;
    private final NotificationCommandUseCase notificationCommandUseCase;
    private final CommentCommandUseCase commentCommandUseCase;
    private final PostCommandUseCase postCommandUseCase;
    private final UserBanUseCase userBanUseCase;
    private final PaperCommandUseCase paperCommandUseCase;
    private final AdminCommandUseCase adminCommandUseCase;
    private final UserCommandUseCase userCommandUseCase;

    @Async
    @EventListener
    @Transactional
    public void userWithdraw(UserWithdrawnEvent userWithdrawnEvent) {
        CustomUserDetails userDetails = userWithdrawnEvent.userDetails();
        Long userId = userWithdrawnEvent.userDetails().getUserId();
        Long tokenId = userWithdrawnEvent.userDetails().getTokenId();
        SocialProvider provider = userWithdrawnEvent.userDetails().getSocialProvider();
        String socialId = userWithdrawnEvent.userDetails().getSocialId();

        notificationSseUseCase.deleteEmitterByUserIdAndTokenId(userId, tokenId); // 전체 기기 Emitter 제거 메서드 필요
        socialWithdrawUseCase.unlinkSocialAccount(provider, socialId);
        commentCommandUseCase.processUserCommentsOnWithdrawal(userId);
        postCommandUseCase.deleteAllPostsByUserId(userId); // 구현 필요
        userBanUseCase.deleteAllTokensByUserId(userId);
        notificationFcmUseCase.deleteFcmTokens(userId);
        notificationCommandUseCase.deleteAllNotification(userDetails); // 구현 필요
        paperCommandUseCase.deleteAllMessagesByUserId(userId); // 구현 필요
        adminCommandUseCase.deleteAllReportsByUserId(userId); // 구현 필요
        userCommandUseCase.deleteUser(userId); // 구현 필요
    }
}

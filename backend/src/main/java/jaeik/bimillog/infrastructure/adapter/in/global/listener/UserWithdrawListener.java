package jaeik.bimillog.infrastructure.adapter.in.global.listener;

import jaeik.bimillog.domain.auth.application.port.in.SocialWithdrawUseCase;
import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.post.application.port.in.PostCommandUseCase;
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

    @Async
    @EventListener
    @Transactional
    public void userWithdraw(UserWithdrawnEvent userWithdrawnEvent) {
        CustomUserDetails userDetails = userWithdrawnEvent.userDetails();
        Long userId = userWithdrawnEvent.userDetails().getUserId();
        Long tokenId = userWithdrawnEvent.userDetails().getTokenId();
        SocialProvider provider = userWithdrawnEvent.userDetails().getSocialProvider();
//        String socialId = userWithdrawnEvent.userDetails(); userDetail에 socialId 추가 필요

        notificationSseUseCase.deleteEmitterByUserIdAndTokenId(userId, tokenId); // 전체 기기 Emitter 제거 메서드 필요
        // socialWithdrawUseCase.unlinkSocialAccount(provider, socialId);
        commentCommandUseCase.processUserCommentsOnWithdrawal(userId);
        // postCommandUseCase. 특정 유저의 전체 글, 전체 댓글 제거 메서드 필요
        // 특정 사용자의 모든 토큰을 정리하는 Auth도메인의 유스케이스 필요
        notificationFcmUseCase.deleteFcmTokens(userId);
        notificationCommandUseCase.deleteAllNotification(userDetails); // 구현 필요 현재 구현체가 없음
        // 특정 사용자의 모든 메시지를 삭제하는 Message 도메인의 유스케이스 필요
        // 특정 사용자의 모든 Report를 삭제하는 Admin 도메인의 유스케이스 필요
        // 특정 사용자를 삭제하는 User 도메인의 유스케이스 필요
        // 특정 사용자의 설정을 삭제하는 User 도메인의 유스케이스 필요
    }
}

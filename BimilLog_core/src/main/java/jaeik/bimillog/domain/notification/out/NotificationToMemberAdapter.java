package jaeik.bimillog.domain.notification.out;

import jaeik.bimillog.domain.member.service.MemberQueryService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationToMemberAdapter {
    private final MemberQueryService memberQueryService;

    public List<String> fcmEligibleFcmTokens(Long memberId, NotificationType type) {
        return memberQueryService.fcmEligibleFcmTokens(memberId, type);
    }
}

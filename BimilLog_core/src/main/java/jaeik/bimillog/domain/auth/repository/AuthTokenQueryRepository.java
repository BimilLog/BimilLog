package jaeik.bimillog.domain.auth.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.auth.entity.QAuthToken;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.member.entity.QSetting;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthTokenQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMember member = QMember.member;
    private final QSetting setting = QSetting.setting;
    private final QAuthToken authToken = QAuthToken.authToken;

    /**
     * <h3>알림 수신 자격이 있는 FCM 토큰 조회</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는 경우 해당 사용자의 모든 FCM 토큰 문자열을 조회합니다.</p>
     *
     * @param memberId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 자격이 있는 경우 FCM 토큰 문자열 목록, 없는 경우 빈 목록
     * @author Jaeik
     * @since 2.1.0
     */
    public List<String> fcmEligibleFcmTokens(Long memberId, NotificationType type) {
        return jpaQueryFactory
                .select(authToken.fcmRegistrationToken)
                .from(authToken)
                .join(authToken.member, member)
                .join(member.setting, setting)
                .where(
                        member.id.eq(memberId),
                        authToken.fcmRegistrationToken.isNotNull(),
                        notificationTypeCondition(type, setting)
                )
                .fetch();
    }

    /**
     * <h3>알림 타입별 조건 생성</h3>
     * <p>알림 타입에 따라 적절한 BooleanExpression을 생성합니다.</p>
     *
     * @param type 알림 타입
     * @param qSetting 설정 엔티티 Q클래스
     * @return 알림 타입에 해당하는 조건 표현식
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression notificationTypeCondition(NotificationType type, QSetting qSetting) {
        // ADMIN, INITIATE는 항상 true (항상 전송)
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return Expressions.TRUE;
        }

        return switch (type) {
            case MESSAGE -> qSetting.messageNotification.isTrue();
            case COMMENT -> qSetting.commentNotification.isTrue();
            case POST_FEATURED_WEEKLY, POST_FEATURED_LEGEND, POST_FEATURED_REALTIME -> qSetting.postFeaturedNotification.isTrue();
            case FRIEND -> qSetting.friendSendNotification.isTrue();
            default -> Expressions.FALSE;
        };
    }
}

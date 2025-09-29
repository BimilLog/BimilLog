package jaeik.bimillog.infrastructure.adapter.out.notification;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.QFcmToken;
import jaeik.bimillog.domain.user.entity.QSetting;
import jaeik.bimillog.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>알림 유틸리티 어댑터</h2>
 * <p>알림 전송 자격 확인과 FCM 토큰 조회를 담당하는 어댑터입니다.</p>
 * <p>FCM 전송 자격 토큰 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationUtilAdapter implements NotificationUtilPort {

    private final JPAQueryFactory queryFactory;

    /**
     * <h3>알림 수신 자격 확인</h3>
     * <p>주어진 사용자 ID와 알림 유형에 따라 사용자가 알림을 수신할 자격이 있는지 확인합니다.</p>
     *
     * @param userId 확인할 사용자의 ID
     * @param type 확인할 알림 유형
     * @return 알림 수신이 가능하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean SseEligibleForNotification(Long userId, NotificationType type) {
        QUser qUser = QUser.user;
        QSetting qSetting = QSetting.setting;
        
        return queryFactory
            .select(qUser)
            .from(qUser)
            .join(qUser.setting, qSetting)
            .where(
                qUser.id.eq(userId),
                notificationTypeCondition(type, qSetting)
            )
            .fetchFirst() != null;
    }

    /**
     * <h3>알림 수신 자격이 있는 FCM 토큰 조회</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는 경우 해당 사용자의 모든 FCM 토큰을 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 자격이 있는 경우 FCM 토큰 목록, 없는 경우 빈 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<FcmToken> FcmEligibleFcmTokens(Long userId, NotificationType type) {
        QUser qUser = QUser.user;
        QSetting qSetting = QSetting.setting;
        QFcmToken qFcmToken = QFcmToken.fcmToken;
        
        return queryFactory
            .selectFrom(qFcmToken)
            .join(qFcmToken.user, qUser)
            .join(qUser.setting, qSetting)
            .where(
                qUser.id.eq(userId),
                notificationTypeCondition(type, qSetting)
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
        // ADMIN, INITIATE는 항상 true
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return Expressions.TRUE;
        }
        
        return switch (type) {
            case PAPER -> qSetting.messageNotification.isTrue();
            case COMMENT -> qSetting.commentNotification.isTrue();
            case POST_FEATURED -> qSetting.postFeaturedNotification.isTrue();
            default -> Expressions.FALSE;
        };
    }
}
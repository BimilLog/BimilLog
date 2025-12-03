package jaeik.bimillog.domain.notification.repository;

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
public class NotificationUtilRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * <h3>알림 수신 자격 확인</h3>
     * <p>주어진 사용자 ID와 알림 유형에 따라 사용자가 알림을 수신할 자격이 있는지 확인합니다.</p>
     *
     * @param memberId 확인할 사용자의 ID
     * @param type 확인할 알림 유형
     * @return 알림 수신이 가능하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean SseEligibleForNotification(Long memberId, NotificationType type) {
        QMember qMember = QMember.member;
        QSetting qSetting = QSetting.setting;
        
        return queryFactory
            .select(qMember)
            .from(qMember)
            .join(qMember.setting, qSetting)
            .where(
                qMember.id.eq(memberId),
                notificationTypeCondition(type, qSetting)
            )
            .fetchFirst() != null;
    }

    /**
     * <h3>알림 수신 자격이 있는 FCM 토큰 조회</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는 경우 해당 사용자의 모든 FCM 토큰 문자열을 조회합니다.</p>
     * <p>v2.4: AuthToken 테이블에서 fcmRegistrationToken 컬럼 조회 (fcm_token 테이블 통합)</p>
     *
     * @param memberId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 자격이 있는 경우 FCM 토큰 문자열 목록, 없는 경우 빈 목록
     * @author Jaeik
     * @since 2.1.0
     */
    public List<String> FcmEligibleFcmTokens(Long memberId, NotificationType type) {
        QMember qMember = QMember.member;
        QSetting qSetting = QSetting.setting;
        QAuthToken qAuthToken = QAuthToken.authToken;

        return queryFactory
            .select(qAuthToken.fcmRegistrationToken)
            .from(qAuthToken)
            .join(qAuthToken.member, qMember)
            .join(qMember.setting, qSetting)
            .where(
                qMember.id.eq(memberId),
                qAuthToken.fcmRegistrationToken.isNotNull(),
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
        // ADMIN, INITIATE는 항상 true (항상 전송)
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return Expressions.TRUE;
        }

        return switch (type) {
            case MESSAGE -> qSetting.messageNotification.isTrue();
            case COMMENT -> qSetting.commentNotification.isTrue();
            case POST_FEATURED -> qSetting.postFeaturedNotification.isTrue();
            case FRIEND -> qSetting.friendSendNotification.isTrue();
            default -> Expressions.FALSE;
        };
    }
}

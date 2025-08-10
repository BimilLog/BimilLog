package jaeik.growfarm.repository.notification;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.notification.QNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>알림 레포지터리 공통 기능</h2>
 * <p>
 * 알림 관련 레포지터리들의 공통 메서드들을 제공하는 추상 클래스
 * SRP: 공통 기능 제공만 담당
 * OCP: 하위 레포지터리들의 확장 기반 제공
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public abstract class NotificationBaseRepository {

    protected final JPAQueryFactory jpaQueryFactory;
    protected final QNotification notification = QNotification.notification;

    /**
     * <h3>사용자별 알림 기본 조건</h3>
     * <p>공통으로 사용되는 사용자별 알림 필터링 조건을 제공합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 공통 조건
     */
    protected com.querydsl.core.types.dsl.BooleanExpression getUserNotificationCondition(Long userId) {
        return notification.users.id.eq(userId);
    }
}
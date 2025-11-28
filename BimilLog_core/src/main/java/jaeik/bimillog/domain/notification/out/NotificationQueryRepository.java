package jaeik.bimillog.domain.notification.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.entity.QNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>알림 조회 어댑터</h2>
 * <p>알림 조회 작업을 담당하는 영속성 어댑터입니다.</p>
 * <p>알림 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final QNotification notification = QNotification.notification;
    private final QMember member = QMember.member;

    /**
     * <h3>알림 목록 조회</h3>
     * <p>지정된 사용자의 알림 목록을 최신순으로 조회합니다.</p>
     *
     * @param memberId 사용자 ID
     * @return 알림 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationList(Long memberId) {

        return jpaQueryFactory
                .selectFrom(notification)
                .innerJoin(notification.member, member).fetchJoin()
                .where(notification.member.id.eq(memberId))
                .orderBy(notification.createdAt.desc())
                .fetch();
    }
}

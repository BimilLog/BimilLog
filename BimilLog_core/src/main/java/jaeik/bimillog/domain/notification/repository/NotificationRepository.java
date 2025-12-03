package jaeik.bimillog.domain.notification.repository;

import jaeik.bimillog.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 기본 레포지토리</h2>
 * <p>알림 엔티티의 기본 CRUD 기능을 제공하는 레포지토리입니다.</p>
 * <p>더티체킹을 활용한 읽음 처리, 사용자별 알림 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * <h3>사용자 ID와 알림 ID 목록으로 알림 조회</h3>
     * <p>특정 사용자의 알림 중에서 지정된 ID 목록에 해당하는 알림들을 조회합니다.</p>
     * <p>더티체킹을 위한 엔티티 조회에 사용됩니다.</p>
     *
     * @param ids 조회할 알림 ID 목록
     * @param memberId 사용자 ID
     * @return 조회된 알림 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> findAllByIdInAndMemberId(List<Long> ids, Long memberId);

    /**
     * <h3>사용자 ID와 알림 ID 목록으로 알림 삭제</h3>
     * <p>특정 사용자의 알림 중에서 지정된 ID 목록에 해당하는 알림들을 삭제합니다.</p>
     *
     * @param ids 삭제할 알림 ID 목록
     * @param memberId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByIdInAndMemberId(List<Long> ids, Long memberId);

    /**
     * <h3>사용자 ID로 모든 알림 삭제</h3>
     * <p>특정 사용자의 모든 알림을 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 해당 사용자의 모든 알림을 정리하는데 사용됩니다.</p>
     *
     * @param memberId 삭제할 알림들의 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByMemberId(Long memberId);

    /**
     * <h3>알림 목록 조회</h3>
     * <p>지정된 사용자의 알림 목록을 최신순으로 조회합니다.</p>
     *
     * @param memberId 사용자 ID
     * @return 알림 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Query("SELECT n FROM Notification n JOIN FETCH n.member m WHERE m.id = :memberId ORDER BY n.createdAt DESC")
    List<Notification> getNotificationList(@Param("memberId") Long memberId);
}

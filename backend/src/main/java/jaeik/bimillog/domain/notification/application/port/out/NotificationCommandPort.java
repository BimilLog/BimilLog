package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>알림 명령 포트</h2>
 * <p>
 * 헥사고날 아키텍처에서 알림 도메인의 명령형 데이터 저장소 연동을 정의하는 Secondary Port입니다.
 * 새로운 알림 생성과 기존 알림의 상태 변경에 대한 외부 어댑터 인터페이스를 제공합니다.
 * </p>
 * <p>
 * 댓글 작성, 롤링페이퍼 메시지 작성 등의 이벤트 발생 시 알림을 저장하고,
 * 사용자의 알림 관리 요청에 따른 읽음/삭제 상태 변경을 처리합니다.
 * </p>
 * <p>NotificationCommandService에서 사용되며, NotificationCommandAdapter에 의해 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandPort {
    /**
     * <h3>알림 저장</h3>
     * <p>다양한 비즈니스 이벤트 발생 시 새로운 알림을 데이터베이스에 영구 저장합니다.</p>
     * <p>댓글 작성, 롤링페이퍼 메시지 작성, 인기글 등극 등의 이벤트에 대응하여 알림을 생성합니다.</p>
     * <p>NotificationCommandService에서 이벤트 리스너 호출 시 사용됩니다.</p>
     *
     * @param user 알림을 받을 사용자 엔티티 (사용자 정보 포함)
     * @param type 알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED 등)
     * @param content 알림 내용 텍스트
     * @param url 알림 클릭 시 이동할 URL (게시글, 롤링페이퍼 상세 페이지)
     * @author Jaeik
     * @since 2.0.0
     */
    void save(User user, NotificationType type, String content, String url);

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>사용자의 알림 관리 요청에 따라 여러 알림의 상태를 읽음 처리하거나 물리적으로 삭제합니다.</p>
     * <p>전체 알림 또는 선택된 알림에 대해 일괄 업데이트를 수행하며, 트랜잭션 내에서 일관성을 보장합니다.</p>
     * <p>NotificationCommandService에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param userId        대상 사용자 ID (알림 소유자 확인용)
     * @param updateCommand 알림 업데이트 정보 (작업 타입과 대상 알림 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void batchUpdate(Long userId, NotificationUpdateVO updateCommand);
}

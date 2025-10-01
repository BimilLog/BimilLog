package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.domain.member.entity.member.Member;

/**
 * <h2>알림 명령 포트</h2>
 * <p>알림 도메인의 명령 작업을 담당하는 포트입니다.</p>
 * <p>알림 저장, 알림 일괄 업데이트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandPort {
    /**
     * <h3>알림 저장</h3>
     * <p>비즈니스 이벤트 발생 시 새로운 알림을 데이터베이스에 저장합니다.</p>
     * <p>댓글 작성, 롤링페이퍼 메시지 작성, 인기글 등극 이벤트에 대응합니다.</p>
     * <p>{@link NotificationCommandService}에서 이벤트 리스너 호출 시 사용됩니다.</p>
     *
     * @param member 알림을 받을 사용자 엔티티
     * @param type 알림 유형 (COMMENT, PAPER_PLANT, POST_FEATURED 등)
     * @param content 알림 내용 텍스트
     * @param url 알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    void save(Member member, NotificationType type, String content, String url);

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>사용자의 알림 관리 요청에 따라 여러 알림의 상태를 읽음 처리하거나 삭제합니다.</p>
     * <p>전체 알림 또는 선택된 알림에 대해 일괄 업데이트를 수행합니다.</p>
     * <p>{@link NotificationCommandService}에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param memberId        대상 사용자 ID
     * @param updateCommand 알림 업데이트 정보 (작업 타입과 대상 알림 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void batchUpdate(Long memberId, NotificationUpdateVO updateCommand);

    /**
     * <h3>사용자의 모든 알림 삭제</h3>
     * <p>특정 사용자의 모든 알림을 데이터베이스에서 완전히 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 호출되어 해당 사용자의 모든 알림 데이터를 정리합니다.</p>
     * <p>{@link NotificationCommandService}에서 회원 탈퇴 이벤트 처리 시 호출됩니다.</p>
     *
     * @param memberId 알림을 삭제할 대상 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByMemberId(Long memberId);
}

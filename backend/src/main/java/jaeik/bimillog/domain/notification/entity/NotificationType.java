package jaeik.bimillog.domain.notification.entity;

import lombok.Getter;

/**
 * <h2>알림 유형</h2>
 * <p>알림의 유형을 정의하는 열거형</p>
 * <p>롤링페이퍼 메시지, 댓글, 인기글 선정, 관리자 공지 등 다양한 비즈니스 이벤트별로 알림을 분류하여 관리하기 위한 열거형</p>
 * <p>각 알림 유형은 사용자 설정의 특정 필드와 연결되어 알림 수신 여부를 제어합니다</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
public enum NotificationType {

    PAPER("messageNotification"), // 롤링페이퍼에 메시지가 달렸을 때
    COMMENT("commentNotification"), // 게시글에 댓글이 달렸을 때
    POST_FEATURED("postFeaturedNotification"), // 주간, 전설 인기글이 되었을 때
    ADMIN(""), // 관리자 알림 (설정 연동 없음)
    INITIATE(""); // SSE 초기화 용도 (설정 연동 없음)


    private final String settingField;

    NotificationType(String settingField) {
        this.settingField = settingField;
    }

}


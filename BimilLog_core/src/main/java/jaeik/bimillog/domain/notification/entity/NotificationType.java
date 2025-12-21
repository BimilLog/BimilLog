package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>알림 유형</h2>
 * <p>알림의 유형을 정의하는 열거형입니다.</p>
 * <p>롤링페이퍼 메시지, 댓글, 인기글 선정, 관리자 공지 분류</p>
 * <p>각 알림 유형은 NotificationUtilAdapter에서 사용자 설정 필드와 연결되어 수신 여부 제어</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public enum NotificationType {
    MESSAGE, // 롤링페이퍼에 메시지가 달렸을 때 (Setting.messageNotification과 연결)
    COMMENT, // 게시글에 댓글이 달렸을 때 (Setting.commentNotification과 연결)
    POST_FEATURED_WEEKLY, // 주간 인기글에 선정되었을 때 (Setting.postFeaturedNotification과 연결)
    POST_FEATURED_LEGEND, // 전설 게시글(명예의 전당)에 등극했을 때 (Setting.postFeaturedNotification과 연결)
    POST_FEATURED_REALTIME, // 실시간 인기글에 선정되었을 때 (Setting.postFeaturedNotification과 연결)
    ADMIN, // 관리자 알림 (설정 연동 없음)
    INITIATE, // SSE 초기화 용도 (설정 연동 없음)
    FRIEND; // 친구 요청

    public String getFCMTitle(String relatedMemberName) {
        switch (this) {
            case COMMENT -> {
                return relatedMemberName + "님이 댓글을 남겼습니다!";
            }
            case MESSAGE -> {
                return "롤링페이퍼에 메시지가 작성되었어요!";
            }
            case POST_FEATURED_WEEKLY -> {
                return "축하합니다! 주간 인기글에 선정되었습니다!";
            }
            case POST_FEATURED_LEGEND -> {
                return "축하합니다! 명예의 전당에 등극했습니다!";
            }
            case POST_FEATURED_REALTIME -> {
                return "축하합니다! 실시간 인기글에 선정되었습니다!";
            }
            case FRIEND -> {
                return "새로운 친구 요청이 도착했어요!";
            }
            default -> {
                return "";
            }
        }
    }

    public String getFCMBody(String relatedMemberName, String postTitle) {
        switch (this) {
            case COMMENT, MESSAGE -> {
                return "지금 확인해보세요!";
            }
            case POST_FEATURED_WEEKLY -> {
                return String.format("회원님의 게시글 %s 이 주간 인기 게시글로 선정되었습니다.", postTitle);
            }
            case POST_FEATURED_LEGEND -> {
                return String.format("회원님의 게시글 %s 이 명예의 전당에 등극했습니다.", postTitle);
            }
            case POST_FEATURED_REALTIME -> {
                return String.format("회원님의 게시글 %s 이 실시간 인기글로 선정되었습니다.", postTitle);
            }
            case FRIEND -> {
                return String.format("%s님 에게서 친구 요청이 도착했습니다.", relatedMemberName);
            }
            default -> {
                return "";
            }
        }
    }
}


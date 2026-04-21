package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>알림 유형</h2>
 * <p>알림의 유형을 정의하는 열거형입니다.</p>
 * <p>각 타입은 SSE 메시지 / URL 경로 패턴과 FCM 문구를 보유합니다.</p>
 *
 * @author Jaeik
 * @version 2.8.0
 */
public enum NotificationType {
    MESSAGE("/rolling-paper/%s", "롤링페이퍼에 메시지가 작성되었어요!"),
    COMMENT("/board/post/%s", "%s님이 댓글을 남겼습니다!"),
    POST_FEATURED_WEEKLY("/board/post/%s", null),
    POST_FEATURED_LEGEND("/board/post/%s", null),
    POST_FEATURED_REALTIME("/board/post/%s", null),
    ADMIN(null, null),
    INITIATE(null, null),
    FRIEND("/friends?tab=received", null);

    private final String urlPathPattern;
    private final String ssePattern;

    NotificationType(String urlPathPattern, String ssePattern) {
        this.urlPathPattern = urlPathPattern;
        this.ssePattern = ssePattern;
    }

    /**
     * <h3>SSE/알림 이동 URL 생성</h3>
     * <p>타입에 정의된 경로 패턴에 {@code args}를 채워 baseUrl에 붙입니다.</p>
     * <p>경로 패턴이 없으면 baseUrl을 그대로 반환합니다.</p>
     */
    public String buildUrl(String baseUrl, Object... args) {
        if (urlPathPattern == null) {
            return baseUrl;
        }
        return baseUrl + (args.length == 0 ? urlPathPattern : urlPathPattern.formatted(args));
    }

    /**
     * <h3>SSE 메시지 생성</h3>
     * <p>타입에 고정된 메시지 패턴을 {@code args}로 포매팅해 반환합니다.</p>
     * <p>패턴이 없는 타입(POST_FEATURED_*, FRIEND 등)은 이벤트에서 직접 메시지를 공급하므로 호출하지 않습니다.</p>
     */
    public String buildSseMessage(Object... args) {
        if (ssePattern == null) {
            throw new IllegalStateException(this + "는 SSE 메시지 패턴이 없습니다. 이벤트에서 직접 공급하세요.");
        }
        return args.length == 0 ? ssePattern : ssePattern.formatted(args);
    }

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

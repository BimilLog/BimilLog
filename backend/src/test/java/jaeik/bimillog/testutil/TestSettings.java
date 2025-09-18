package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;

/**
 * <h2>미리 정의된 테스트 설정 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 설정 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestSettings {

    // 미리 정의된 설정 인스턴스들
    public static final Setting DEFAULT;
    public static final Setting ALL_DISABLED;
    public static final Setting MESSAGE_ONLY;
    public static final Setting COMMENT_ONLY;
    public static final Setting POST_FEATURED_ONLY;

    static {
        // 설정들 (테스트용)
        DEFAULT = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        ALL_DISABLED = Setting.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        MESSAGE_ONLY = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(false)
                .build();

        COMMENT_ONLY = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        POST_FEATURED_ONLY = Setting.builder()
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();
    }

    /**
     * 설정 복사본 생성 (객체 수정이 필요한 경우)
     */
    public static Setting copy(Setting setting) {
        return Setting.builder()
                .messageNotification(setting.isMessageNotification())
                .commentNotification(setting.isCommentNotification())
                .postFeaturedNotification(setting.isPostFeaturedNotification())
                .build();
    }

    /**
     * 특정 ID를 가진 설정 복사본 생성
     */
    public static Setting copyWithId(Setting setting, Long id) {
        return Setting.builder()
                .id(id)
                .messageNotification(setting.isMessageNotification())
                .commentNotification(setting.isCommentNotification())
                .postFeaturedNotification(setting.isPostFeaturedNotification())
                .build();
    }

    /**
     * 커스터마이징된 설정 생성
     */
    public static Setting custom(boolean messageNotification,
                               boolean commentNotification,
                               boolean postFeaturedNotification) {
        return Setting.builder()
                .messageNotification(messageNotification)
                .commentNotification(commentNotification)
                .postFeaturedNotification(postFeaturedNotification)
                .build();
    }

    // Private constructor to prevent instantiation
    private TestSettings() {}
}
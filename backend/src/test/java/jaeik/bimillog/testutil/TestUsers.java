package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;

/**
 * <h2>미리 정의된 테스트 사용자 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 사용자 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 * <p>모든 사용자는 기본 Setting을 포함하여 생성됨</p>
 *
 * @author Jaeik
 * @version 3.0.0
 */
public class TestUsers {

    // 미리 정의된 사용자 인스턴스들 (Setting 포함)
    public static final User USER1;
    public static final User USER2;
    public static final User USER3;

    static {
        // 사용자들 (테스트용) - 기본 Setting 포함
        USER1 = User.builder()
                .socialId("kakao123456")
                .provider(SocialProvider.KAKAO)
                .userName("testUser1")
                .socialNickname("테스트유저1")
                .thumbnailImage("http://example.com/profile1.jpg")
                .role(UserRole.USER)
                .setting(createDefaultSetting())
                .build();

        USER2 = User.builder()
                .socialId("kakao789012")
                .provider(SocialProvider.KAKAO)
                .userName("testUser2")
                .socialNickname("테스트유저2")
                .thumbnailImage("http://example.com/profile2.jpg")
                .role(UserRole.USER)
                .setting(createDefaultSetting())
                .build();

        USER3 = User.builder()
                .socialId("kakao345678")
                .provider(SocialProvider.KAKAO)
                .userName("testUser3")
                .socialNickname("테스트유저3")
                .thumbnailImage("http://example.com/profile3.jpg")
                .role(UserRole.USER)
                .setting(createDefaultSetting())
                .build();
    }

    /**
     * 기존 사용자를 복사하며 특정 ID 설정
     */
    public static User copyWithId(User user, Long id) {
        return User.builder()
                .id(id)
                .socialId(user.getSocialId())
                .provider(user.getProvider())
                .userName(user.getUserName())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .role(user.getRole())
                .setting(user.getSetting())
                .build();
    }

    /**
     * 특정 소셜 ID를 가진 사용자 생성
     */
    public static User withSocialId(String socialId) {
        return User.builder()
                .socialId(socialId)
                .provider(SocialProvider.KAKAO)
                .userName(USER1.getUserName())
                .socialNickname(USER1.getSocialNickname())
                .thumbnailImage(USER1.getThumbnailImage())
                .role(USER1.getRole())
                .setting(USER1.getSetting())
                .build();
    }

    /**
     * 특정 role을 가진 사용자 생성 (ADMIN 생성 시 사용)
     */
    public static User withRole(UserRole role) {
        Setting setting = role == UserRole.ADMIN
                ? createSetting(false, false, false) // 관리자는 기본적으로 알림 비활성화
                : createDefaultSetting();

        return User.builder()
                .socialId(role == UserRole.ADMIN ? "kakao999999" : USER1.getSocialId())
                .provider(USER1.getProvider())
                .userName(role == UserRole.ADMIN ? "adminUser" : USER1.getUserName())
                .socialNickname(role == UserRole.ADMIN ? "관리자" : USER1.getSocialNickname())
                .thumbnailImage(role == UserRole.ADMIN ? "http://example.com/admin.jpg" : USER1.getThumbnailImage())
                .role(role)
                .setting(setting)
                .build();
    }

    /**
     * 특정 알림 설정을 가진 사용자 생성
     */
    public static User withNotificationSettings(boolean messageNotification,
                                                boolean commentNotification,
                                                boolean postFeaturedNotification) {
        return User.builder()
                .socialId(USER1.getSocialId())
                .provider(USER1.getProvider())
                .userName(USER1.getUserName())
                .socialNickname(USER1.getSocialNickname())
                .thumbnailImage(USER1.getThumbnailImage())
                .role(USER1.getRole())
                .setting(createSetting(messageNotification, commentNotification, postFeaturedNotification))
                .build();
    }

    /**
     * 고유한 사용자 생성 (타임스탬프 기반)
     * 통합 테스트에서 고유한 사용자가 필요한 경우 사용
     */
    public static User createUnique() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return User.builder()
                .socialId("unique_" + timestamp)
                .provider(SocialProvider.KAKAO)
                .userName("user_" + timestamp)
                .socialNickname("테스트유저_" + timestamp)
                .thumbnailImage(USER1.getThumbnailImage())
                .role(UserRole.USER)
                .setting(createDefaultSetting())
                .build();
    }

    /**
     * 고유한 사용자 생성 (접두사 지정)
     * @param prefix 사용자 식별 접두사
     */
    public static User createUniqueWithPrefix(String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return User.builder()
                .socialId(prefix + "_" + timestamp)
                .provider(SocialProvider.KAKAO)
                .userName(prefix + "_" + timestamp)
                .socialNickname(prefix + "_소셜닉네임")
                .thumbnailImage(USER1.getThumbnailImage())
                .role(UserRole.USER)
                .setting(createDefaultSetting())
                .build();
    }

    // ==================== Setting 관련 메서드 ====================

    /**
     * 기본 설정 생성 (모든 알림 활성화)
     */
    private static Setting createDefaultSetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }

    /**
     * 커스텀 설정 생성
     */
    public static Setting createSetting(boolean messageNotification,
                                       boolean commentNotification,
                                       boolean postFeaturedNotification) {
        return Setting.builder()
                .messageNotification(messageNotification)
                .commentNotification(commentNotification)
                .postFeaturedNotification(postFeaturedNotification)
                .build();
    }

    /**
     * 메시지 알림만 활성화된 설정
     */
    public static Setting createMessageOnlySetting() {
        return createSetting(true, false, false);
    }

    /**
     * 댓글 알림만 활성화된 설정
     */
    public static Setting createCommentOnlySetting() {
        return createSetting(false, true, false);
    }

    /**
     * 게시글 추천 알림만 활성화된 설정
     */
    public static Setting createPostFeaturedOnlySetting() {
        return createSetting(false, false, true);
    }

    /**
     * 모든 알림 비활성화된 설정
     */
    public static Setting createAllDisabledSetting() {
        return createSetting(false, false, false);
    }
}
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
        USER1 = createUser("kakao123456", "testUser1", "테스트유저1", "http://example.com/profile1.jpg", UserRole.USER, createDefaultSetting());
        USER2 = createUser("kakao789012", "testUser2", "테스트유저2", "http://example.com/profile2.jpg", UserRole.USER, createDefaultSetting());
        USER3 = createUser("kakao345678", "testUser3", "테스트유저3", "http://example.com/profile3.jpg", UserRole.USER, createDefaultSetting());
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
                .setting(cloneSetting(user.getSetting()))
                .build();
    }

    /**
     * 특정 소셜 ID를 가진 사용자 생성
     */
    public static User withSocialId(String socialId) {
        String generatedUserName = USER1.getUserName() + "_" + socialId;
        return createUser(
                socialId,
                generatedUserName,
                USER1.getSocialNickname(),
                USER1.getThumbnailImage(),
                USER1.getRole(),
                cloneSetting(USER1.getSetting())
        );
    }

    /**
     * 특정 role을 가진 사용자 생성 (ADMIN 생성 시 사용)
     */
    public static User withRole(UserRole role) {
        Setting setting = role == UserRole.ADMIN
                ? createSetting(false, false, false) // 관리자는 기본적으로 알림 비활성화
                : createDefaultSetting();

        return createUser(
                role == UserRole.ADMIN ? "kakao999999" : USER1.getSocialId(),
                role == UserRole.ADMIN ? "adminUser" : USER1.getUserName(),
                role == UserRole.ADMIN ? "관리자" : USER1.getSocialNickname(),
                role == UserRole.ADMIN ? "http://example.com/admin.jpg" : USER1.getThumbnailImage(),
                role,
                setting
        );
    }

    /**
     * 고유한 사용자 생성 (타임스탬프 기반)
     * 통합 테스트에서 고유한 사용자가 필요한 경우 사용
     */
    public static User createUnique() {
        return createUniqueInternal("user", "테스트유저", true);
    }

    /**
     * 고유한 사용자 생성 (접두사 지정)
     * @param prefix 사용자 식별 접두사
     */
    public static User createUniqueWithPrefix(String prefix) {
        return createUniqueInternal(prefix, prefix + "_소셜닉네임", false);
    }

    private static User createUniqueInternal(String prefix,
                                             String nicknamePrefix,
                                             boolean appendTimestampToNickname) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nickname = appendTimestampToNickname ? nicknamePrefix + '_' + timestamp : nicknamePrefix;
        return createUser(
                prefix + "_" + timestamp,
                prefix + "_" + timestamp,
                nickname,
                USER1.getThumbnailImage(),
                UserRole.USER,
                createDefaultSetting()
        );
    }

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
     * 모든 알림 비활성화된 설정
     */
    public static Setting createAllDisabledSetting() {
        return createSetting(false, false, false);
    }

    private static Setting cloneSetting(Setting original) {
        if (original == null) {
            return null;
        }
        return Setting.builder()
                .messageNotification(original.isMessageNotification())
                .commentNotification(original.isCommentNotification())
                .postFeaturedNotification(original.isPostFeaturedNotification())
                .build();
    }

    private static User createUser(String socialId,
                                   String userName,
                                   String socialNickname,
                                   String thumbnail,
                                   UserRole role,
                                   Setting setting) {
        return User.builder()
                .socialId(socialId)
                .provider(SocialProvider.KAKAO)
                .userName(userName)
                .socialNickname(socialNickname)
                .thumbnailImage(thumbnail)
                .role(role)
                .setting(setting)
                .build();
    }
}

package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.user.SocialProvider;
import jaeik.bimillog.domain.user.entity.user.User;
import jaeik.bimillog.domain.user.entity.user.UserRole;

import java.util.function.Consumer;

/**
 * <h2>미리 정의된 테스트 사용자 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 사용자 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 * <p>모든 사용자는 기본 Setting을 포함하여 생성됨</p>
 *
 * @author Jaeik
 * @version 3.1.0
 */
public class TestUsers {

    // 미리 정의된 사용자 인스턴스들 (Setting 포함)
    public static final User USER1;
    public static final User USER2;
    public static final User USER3;

    static {
        USER1 = createUser(builder -> {
            builder.socialId("kakao123456");
            builder.userName("testUser1");
            builder.socialNickname("테스트유저1");
            builder.thumbnailImage("http://example.com/profile1.jpg");
        });

        USER2 = createUser(builder -> {
            builder.socialId("kakao789012");
            builder.userName("testUser2");
            builder.socialNickname("테스트유저2");
            builder.thumbnailImage("http://example.com/profile2.jpg");
        });

        USER3 = createUser(builder -> {
            builder.socialId("kakao345678");
            builder.userName("testUser3");
            builder.socialNickname("테스트유저3");
            builder.thumbnailImage("http://example.com/profile3.jpg");
        });
    }

    /**
     * 기본 템플릿을 기반으로 한 사용자 빌더 제공
     */
    public static User.UserBuilder builder() {
        return User.builder()
                .socialId("kakao-template")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .thumbnailImage("http://example.com/profile.jpg")
                .role(UserRole.USER)
                .setting(createAllEnabledSetting());
    }

    /**
     * 템플릿 기반 사용자 생성 (커스터마이징 람다 적용)
     */
    public static User createUser(Consumer<User.UserBuilder> customizer) {
        User.UserBuilder builder = builder();
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.build();
    }

    /**
     * 기존 사용자를 복사하며 특정 ID 설정
     */
    public static User copyWithId(User user, Long id) {
        return builder()
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
        return createUser(builder -> {
            builder.socialId(socialId);
            builder.userName(generatedUserName);
            builder.socialNickname(USER1.getSocialNickname());
            builder.thumbnailImage(USER1.getThumbnailImage());
        });
    }

    /**
     * 특정 role을 가진 사용자 생성 (ADMIN 생성 시 사용)
     */
    public static User withRole(UserRole role) {
        return createUser(builder -> {
            builder.role(role);
            if (role == UserRole.ADMIN) {
                builder.socialId("kakao999999");
                builder.userName("adminUser");
                builder.socialNickname("관리자");
                builder.thumbnailImage("http://example.com/admin.jpg");
                builder.setting(createAllDisabledSetting());
            } else {
                builder.setting(createAllEnabledSetting());
            }
        });
    }

    /**
     * 고유한 사용자 생성 (타임스탬프 기반)
     * 통합 테스트에서 고유한 사용자가 필요한 경우 사용
     */
    public static User createUnique() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return createUser(builder -> {
            builder.socialId("user_" + timestamp);
            builder.userName("user_" + timestamp);
            builder.socialNickname("테스트유저_" + timestamp);
        });
    }

    /**
     * 고유한 사용자 생성 (접두사 지정)
     * @param prefix 사용자 식별 접두사
     */
    public static User createUniqueWithPrefix(String prefix) {
        return createUniqueWithPrefix(prefix, null);
    }

    /**
     * 고유한 사용자 생성 (접두사 및 커스터마이징 지원)
     * @param prefix 사용자 식별 접두사
     * @param customizer 사용자 정의 빌더 커스터마이저
     */
    public static User createUniqueWithPrefix(String prefix, Consumer<User.UserBuilder> customizer) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return createUser(builder -> {
            builder.socialId(prefix + "_" + timestamp);
            builder.userName(prefix + "_" + timestamp);
            builder.socialNickname(prefix + "_소셜닉네임");
            if (customizer != null) {
                customizer.accept(builder);
            }
        });
    }

    /**
     * 기본 설정 생성 (모든 알림 활성화)
     */
    public static Setting createAllEnabledSetting() {
        return createSetting(true, true, true);
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
}

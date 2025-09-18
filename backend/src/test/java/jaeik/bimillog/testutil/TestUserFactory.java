package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;

/**
 * <h2>테스트 사용자 생성 팩토리</h2>
 * <p>테스트에서 사용할 User 엔티티를 생성하는 유틸리티 클래스</p>
 * <p>중복되는 사용자 생성 코드를 중앙화하여 DRY 원칙 준수</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestUserFactory {

    private static final String DEFAULT_SOCIAL_ID = "kakao123456";
    private static final SocialProvider DEFAULT_PROVIDER = SocialProvider.KAKAO;
    private static final String DEFAULT_USER_NAME = "testUser";
    private static final String DEFAULT_SOCIAL_NICKNAME = "테스트유저";
    private static final String DEFAULT_THUMBNAIL_IMAGE = "http://example.com/profile.jpg";
    private static final UserRole DEFAULT_ROLE = UserRole.USER;

    /**
     * 기본 테스트 사용자 생성 (ID 없음)
     */
    public static User createUser() {
        return User.builder()
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(DEFAULT_USER_NAME)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 특정 ID를 가진 테스트 사용자 생성
     */
    public static User createUserWithId(Long id) {
        return User.builder()
                .id(id)
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(DEFAULT_USER_NAME)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 특정 사용자명을 가진 테스트 사용자 생성
     */
    public static User createUserWithUserName(String userName) {
        return User.builder()
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(userName)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 특정 ID와 사용자명을 가진 테스트 사용자 생성
     */
    public static User createUserWithIdAndUserName(Long id, String userName) {
        return User.builder()
                .id(id)
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(userName)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 관리자 권한을 가진 테스트 사용자 생성
     */
    public static User createAdminUser() {
        return User.builder()
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName("adminUser")
                .socialNickname("관리자")
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(UserRole.ADMIN)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 관리자 권한을 가진 특정 ID 테스트 사용자 생성
     */
    public static User createAdminUserWithId(Long id) {
        return User.builder()
                .id(id)
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName("adminUser")
                .socialNickname("관리자")
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(UserRole.ADMIN)
                .setting(TestSettingFactory.createDefaultSetting())
                .build();
    }

    /**
     * 특정 설정을 가진 테스트 사용자 생성
     */
    public static User createUserWithSetting(Setting setting) {
        return User.builder()
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(DEFAULT_USER_NAME)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .setting(setting)
                .build();
    }

    /**
     * 설정 없는 테스트 사용자 생성 (Integration Test용)
     */
    public static User createUserWithoutSetting() {
        return User.builder()
                .socialId(DEFAULT_SOCIAL_ID)
                .provider(DEFAULT_PROVIDER)
                .userName(DEFAULT_USER_NAME)
                .socialNickname(DEFAULT_SOCIAL_NICKNAME)
                .thumbnailImage(DEFAULT_THUMBNAIL_IMAGE)
                .role(DEFAULT_ROLE)
                .build();
    }

    /**
     * 빌더 패턴으로 커스터마이징 가능한 테스트 사용자 생성
     */
    public static TestUserBuilder builder() {
        return new TestUserBuilder();
    }

    /**
     * 테스트 사용자 빌더 클래스
     */
    public static class TestUserBuilder {
        private Long id;
        private String socialId = DEFAULT_SOCIAL_ID;
        private SocialProvider provider = DEFAULT_PROVIDER;
        private String userName = DEFAULT_USER_NAME;
        private String socialNickname = DEFAULT_SOCIAL_NICKNAME;
        private String thumbnailImage = DEFAULT_THUMBNAIL_IMAGE;
        private UserRole role = DEFAULT_ROLE;
        private Setting setting = TestSettingFactory.createDefaultSetting();

        public TestUserBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public TestUserBuilder withSocialId(String socialId) {
            this.socialId = socialId;
            return this;
        }

        public TestUserBuilder withProvider(SocialProvider provider) {
            this.provider = provider;
            return this;
        }

        public TestUserBuilder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public TestUserBuilder withSocialNickname(String socialNickname) {
            this.socialNickname = socialNickname;
            return this;
        }

        public TestUserBuilder withThumbnailImage(String thumbnailImage) {
            this.thumbnailImage = thumbnailImage;
            return this;
        }

        public TestUserBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }

        public TestUserBuilder withSetting(Setting setting) {
            this.setting = setting;
            return this;
        }

        public TestUserBuilder withoutSetting() {
            this.setting = null;
            return this;
        }

        public User build() {
            return User.builder()
                    .id(id)
                    .socialId(socialId)
                    .provider(provider)
                    .userName(userName)
                    .socialNickname(socialNickname)
                    .thumbnailImage(thumbnailImage)
                    .role(role)
                    .setting(setting)
                    .build();
        }
    }
}
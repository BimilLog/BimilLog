package jaeik.growfarm.infrastructure.adapter.user.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.BlackList;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserCommandAdapter;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>UserCommandAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 UserCommandAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 CRUD 동작 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user",
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.setting",
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.blacklist"
})
@Import(UserCommandAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class UserCommandAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SettingRepository settingRepository;
    
    @Autowired
    private BlackListRepository blackListRepository;

    @Autowired
    private UserCommandAdapter userCommandAdapter;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        settingRepository.deleteAll();
        blackListRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 사용자 저장")
    void shouldSaveNewUser_WhenValidUserProvided() {
        // Given: 새로운 사용자 엔티티
        Setting setting = Setting.createSetting();
        setting = settingRepository.save(setting);  // 먼저 설정 저장
        
        User newUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("카카오유저")
                .thumbnailImage("http://example.com/image.jpg")
                .role(UserRole.USER)
                .setting(setting)
                .build();

        // When: 사용자 저장
        User savedUser = userCommandAdapter.save(newUser);

        // Then: 사용자가 올바르게 저장되었는지 검증
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getSocialId()).isEqualTo("kakao123");
        assertThat(savedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(savedUser.getUserName()).isEqualTo("testUser");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        
        // DB에 실제로 저장되었는지 확인
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("정상 케이스 - 기존 사용자 정보 업데이트")
    void shouldUpdateExistingUser_WhenUserModified() {
        // Given: 기존 사용자 생성 및 저장
        Setting setting = Setting.createSetting();
        setting = settingRepository.save(setting);
        
        User existingUser = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("oldUserName")
                .socialNickname("old nickname")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        existingUser = userRepository.save(existingUser);

        // 사용자 정보 수정
        existingUser.updateUserName("newUserName");
        existingUser.updateUserInfo("new nickname", "http://new-image.com/image.jpg");

        // When: 수정된 사용자 저장
        User updatedUser = userCommandAdapter.save(existingUser);

        // Then: 사용자 정보가 올바르게 업데이트되었는지 검증
        assertThat(updatedUser.getId()).isEqualTo(existingUser.getId());
        assertThat(updatedUser.getUserName()).isEqualTo("newUserName");
        assertThat(updatedUser.getSocialNickname()).isEqualTo("new nickname");
        assertThat(updatedUser.getThumbnailImage()).isEqualTo("http://new-image.com/image.jpg");
        
        // DB에서 다시 조회하여 확인
        Optional<User> foundUser = userRepository.findById(updatedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserName()).isEqualTo("newUserName");
        assertThat(foundUser.get().getSocialNickname()).isEqualTo("new nickname");
    }

    @Test
    @DisplayName("정상 케이스 - 설정 저장")
    void shouldSaveSetting_WhenValidSettingProvided() {
        // Given: 새로운 설정 엔티티
        Setting newSetting = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        // When: 설정 저장
        Setting savedSetting = userCommandAdapter.save(newSetting);

        // Then: 설정이 올바르게 저장되었는지 검증
        assertThat(savedSetting).isNotNull();
        assertThat(savedSetting.getId()).isNotNull();
        assertThat(savedSetting.isMessageNotification()).isFalse();
        assertThat(savedSetting.isCommentNotification()).isTrue();
        assertThat(savedSetting.isPostFeaturedNotification()).isFalse();
        
        // DB에 실제로 저장되었는지 확인
        Optional<Setting> foundSetting = settingRepository.findById(savedSetting.getId());
        assertThat(foundSetting).isPresent();
        assertThat(foundSetting.get().isCommentNotification()).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 기존 설정 업데이트")
    void shouldUpdateExistingSetting_WhenSettingModified() {
        // Given: 기존 설정 생성 및 저장
        Setting existingSetting = Setting.createSetting();  // 기본값: 모두 true
        existingSetting = settingRepository.save(existingSetting);

        // 설정 수정
        Setting modifiedSetting = Setting.builder()
                .id(existingSetting.getId())
                .messageNotification(false)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        // When: 수정된 설정 저장
        Setting updatedSetting = userCommandAdapter.save(modifiedSetting);

        // Then: 설정이 올바르게 업데이트되었는지 검증
        assertThat(updatedSetting.getId()).isEqualTo(existingSetting.getId());
        assertThat(updatedSetting.isMessageNotification()).isFalse();
        assertThat(updatedSetting.isCommentNotification()).isFalse();
        assertThat(updatedSetting.isPostFeaturedNotification()).isTrue();
        
        // DB에서 다시 조회하여 확인
        Optional<Setting> foundSetting = settingRepository.findById(updatedSetting.getId());
        assertThat(foundSetting).isPresent();
        assertThat(foundSetting.get().isMessageNotification()).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 삭제")
    void shouldDeleteUser_WhenValidIdProvided() {
        // Given: 기존 사용자 생성 및 저장
        Setting setting = Setting.createSetting();
        setting = settingRepository.save(setting);
        
        User existingUser = User.builder()
                .socialId("kakao789")
                .provider(SocialProvider.KAKAO)
                .userName("userToDelete")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        existingUser = userRepository.save(existingUser);
        Long userId = existingUser.getId();

        // 삭제 전 사용자 존재 확인
        assertThat(userRepository.findById(userId)).isPresent();

        // When: 사용자 삭제
        userCommandAdapter.deleteById(userId);

        // Then: 사용자가 삭제되었는지 검증
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 블랙리스트 저장")
    void shouldSaveBlackList_WhenValidBlackListProvided() {
        // Given: 새로운 블랙리스트 엔티티
        BlackList blackList = BlackList.createBlackList("kakaoBlocked123", SocialProvider.KAKAO);

        // When: 블랙리스트 저장
        userCommandAdapter.save(blackList);

        // Then: 블랙리스트가 올바르게 저장되었는지 검증
        boolean isBlackListed = blackListRepository
                .existsByProviderAndSocialId(SocialProvider.KAKAO, "kakaoBlocked123");
        
        assertThat(isBlackListed).isTrue();
    }

    @Test
    @DisplayName("트랜잭션 - 사용자와 설정 함께 저장")
    void shouldSaveUserWithSetting_WhenBothProvided() {
        // Given: 설정을 포함한 새로운 사용자
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        User userWithSetting = User.builder()
                .socialId("kakaoWithSetting")
                .provider(SocialProvider.KAKAO)
                .userName("userWithSetting")
                .role(UserRole.USER)
                .setting(setting)
                .build();

        // When: 사용자 저장 (설정도 cascade로 함께 저장)
        User savedUser = userCommandAdapter.save(userWithSetting);

        // Then: 사용자와 설정 모두 저장되었는지 검증
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getSetting()).isNotNull();
        assertThat(savedUser.getSetting().getId()).isNotNull();
        
        // DB에서 직접 확인
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getSetting()).isNotNull();
        assertThat(foundUser.get().getSetting().isCommentNotification()).isFalse();
    }

    @Test
    @DisplayName("경계값 - 중복 닉네임으로 사용자 저장 시 예외")
    void shouldThrowException_WhenDuplicateUserNameProvided() {
        // Given: 이미 존재하는 닉네임을 가진 사용자
        Setting setting1 = Setting.createSetting();
        setting1 = settingRepository.save(setting1);
        
        User existingUser = User.builder()
                .socialId("kakao111")
                .provider(SocialProvider.KAKAO)
                .userName("duplicateUser")
                .role(UserRole.USER)
                .setting(setting1)
                .build();
        userRepository.save(existingUser);

        // 동일한 닉네임을 가진 다른 사용자
        Setting setting2 = Setting.createSetting();
        setting2 = settingRepository.save(setting2);
        
        User duplicateUser = User.builder()
                .socialId("kakao222")
                .provider(SocialProvider.KAKAO)
                .userName("duplicateUser")  // 중복 닉네임
                .role(UserRole.USER)
                .setting(setting2)
                .build();

        // When & Then: 중복 닉네임으로 저장 시 예외 발생
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // 중복 닉네임 검증 로직이 누락되어 DB 제약조건 위반 발생 가능
        // 수정 필요: UserCommandAdapter.save() 메서드에 중복 검증 추가
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userCommandAdapter.save(duplicateUser)
        );
    }

    @Test
    @DisplayName("경계값 - 중복 소셜 정보로 블랙리스트 저장 시 예외")
    void shouldThrowException_WhenDuplicateBlackListProvided() {
        // Given: 이미 존재하는 블랙리스트
        BlackList existingBlackList = BlackList.createBlackList("kakaoBlocked456", SocialProvider.KAKAO);
        blackListRepository.save(existingBlackList);

        // 동일한 소셜 정보를 가진 블랙리스트
        BlackList duplicateBlackList = BlackList.createBlackList("kakaoBlocked456", SocialProvider.KAKAO);

        // When & Then: 중복 소셜 정보로 저장 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userCommandAdapter.save(duplicateBlackList)
        );
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 사용자 삭제")
    void shouldNotThrowException_WhenDeletingNonExistentUser() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentId = 999L;

        // When & Then: 존재하지 않는 사용자 삭제 시 예외가 발생하지 않아야 함
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> userCommandAdapter.deleteById(nonExistentId)
        );
    }

    @Test
    @DisplayName("DB 매핑 - 복잡한 사용자 엔티티 저장 및 조회")
    void shouldSaveComplexUserEntity_WhenAllFieldsProvided() {
        // Given: 모든 필드를 포함한 복잡한 사용자 엔티티
        Setting complexSetting = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();

        User complexUser = User.builder()
                .socialId("complexKakaoId")
                .provider(SocialProvider.KAKAO)
                .userName("complexUser")
                .socialNickname("복잡한 카카오 닉네임")
                .thumbnailImage("https://complex-image-url.com/profile.jpg")
                .role(UserRole.USER)
                .setting(complexSetting)
                .build();

        // When: 복잡한 사용자 저장
        User savedComplexUser = userCommandAdapter.save(complexUser);

        // Then: 모든 필드가 올바르게 저장되었는지 검증
        assertThat(savedComplexUser.getId()).isNotNull();
        assertThat(savedComplexUser.getSocialId()).isEqualTo("complexKakaoId");
        assertThat(savedComplexUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(savedComplexUser.getUserName()).isEqualTo("complexUser");
        assertThat(savedComplexUser.getSocialNickname()).isEqualTo("복잡한 카카오 닉네임");
        assertThat(savedComplexUser.getThumbnailImage()).isEqualTo("https://complex-image-url.com/profile.jpg");
        assertThat(savedComplexUser.getRole()).isEqualTo(UserRole.USER);
        
        // 설정 필드 검증
        assertThat(savedComplexUser.getSetting()).isNotNull();
        assertThat(savedComplexUser.getSetting().getId()).isNotNull();
        assertThat(savedComplexUser.getSetting().isMessageNotification()).isFalse();
        assertThat(savedComplexUser.getSetting().isCommentNotification()).isTrue();
        assertThat(savedComplexUser.getSetting().isPostFeaturedNotification()).isFalse();
        
        // 기본 엔티티 필드 검증
        assertThat(savedComplexUser.getCreatedAt()).isNotNull();
        assertThat(savedComplexUser.getModifiedAt()).isNotNull();
        
        // DB에서 직접 조회하여 재확인
        Optional<User> foundUser = userRepository.findById(savedComplexUser.getId());
        assertThat(foundUser).isPresent();
        User dbUser = foundUser.get();
        assertThat(dbUser.getSocialNickname()).isEqualTo("복잡한 카카오 닉네임");
        assertThat(dbUser.getThumbnailImage()).isEqualTo("https://complex-image-url.com/profile.jpg");
    }
}
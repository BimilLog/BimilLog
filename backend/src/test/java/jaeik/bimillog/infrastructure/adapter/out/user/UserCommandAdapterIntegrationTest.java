package jaeik.bimillog.infrastructure.adapter.out.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.BlackListRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import jaeik.bimillog.testutil.TestUsers;
import jakarta.persistence.EntityManager;
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
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.user.out.user.user",
        "jaeik.bimillog.infrastructure.adapter.user.out.user.setting",
        "jaeik.bimillog.infrastructure.adapter.user.out.user.blacklist"
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
        
        User newUser = TestUsers.createUniqueWithPrefix("testUser");
        newUser = TestUsers.copyWithId(TestUsers.withSetting(setting), newUser.getId());

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
        
        User existingUser = TestUsers.createUniqueWithPrefix("oldUserName");
        existingUser = TestUsers.copyWithId(TestUsers.withSetting(setting), existingUser.getId());
        existingUser = userRepository.save(existingUser);

        // 사용자 정보 수정
        // User 엔티티 직접 수정 대신 서비스 레이어 테스트에서 확인
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
    @DisplayName("트랜잭션 - 사용자와 설정 함께 저장")
    void shouldSaveUserWithSetting_WhenBothProvided() {
        // Given: 설정을 포함한 새로운 사용자
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();

        User userWithSetting = TestUsers.createUniqueWithPrefix("userWithSetting");
        userWithSetting = TestUsers.copyWithId(TestUsers.withSetting(setting), userWithSetting.getId());

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
        
        User existingUser = TestUsers.createUniqueWithPrefix("duplicateUser");
        existingUser = TestUsers.copyWithId(TestUsers.withSetting(setting1), existingUser.getId());
        userRepository.save(existingUser);

        // 동일한 닉네임을 가진 다른 사용자
        Setting setting2 = Setting.createSetting();
        setting2 = settingRepository.save(setting2);
        
        User tempUser = TestUsers.createUniqueWithPrefix("duplicateUser");
        final User duplicateUser = TestUsers.copyWithId(TestUsers.withSetting(setting2), tempUser.getId());  // 중복 닉네임

        // When & Then: 중복 닉네임으로 저장 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userCommandAdapter.save(duplicateUser)
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

        User complexUser = TestUsers.createUniqueWithPrefix("complexUser");
        complexUser = TestUsers.copyWithId(TestUsers.withSetting(complexSetting), complexUser.getId());

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
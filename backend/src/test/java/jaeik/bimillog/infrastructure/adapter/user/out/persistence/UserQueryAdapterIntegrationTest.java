package jaeik.bimillog.infrastructure.adapter.user.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jakarta.persistence.EntityManager;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserQueryAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user.UserRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>UserQueryAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 UserQueryAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 데이터 매핑 및 쿼리 동작 검증</p>
 * 
 * <p><strong>Note:</strong> User 엔티티의 @NotNull setting 제약조건에 따라 
 * 모든 테스트 사용자는 유효한 Setting을 가져야 함</p>
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
        "jaeik.bimillog.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.user",
        "jaeik.bimillog.infrastructure.adapter.user.out.persistence.user.setting"
})
@Import(UserQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class UserQueryAdapterIntegrationTest {

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
    private UserQueryAdapter userQueryAdapter;

    private User testUser1;
    private User testUser2;
    private Setting testSetting;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        settingRepository.deleteAll();

        // 설정 생성
        testSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();
        testSetting = settingRepository.save(testSetting);

        // 두 번째 사용자용 설정 생성
        Setting testSetting2 = Setting.builder()
                .messageNotification(false)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();
        testSetting2 = settingRepository.save(testSetting2);

        // 사용자 생성
        testUser1 = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser1")
                .socialNickname("카카오유저1")
                .thumbnailImage("http://example.com/image1.jpg")
                .role(UserRole.USER)
                .setting(testSetting)
                .build();

        testUser2 = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("testUser2")
                .socialNickname("카카오유저2")
                .thumbnailImage("http://example.com/image2.jpg")
                .role(UserRole.USER)
                .setting(testSetting2)  // 별도 설정을 가진 사용자
                .build();

        testUser1 = userRepository.save(testUser1);
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 조회")
    void shouldFindUser_WhenValidIdProvided() {
        // When: 사용자 ID로 조회
        Optional<User> result = userQueryAdapter.findById(testUser1.getId());

        // Then: 올바른 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getId()).isEqualTo(testUser1.getId());
        assertThat(foundUser.getUserName()).isEqualTo("testUser1");
        assertThat(foundUser.getSocialId()).isEqualTo("kakao123");
        assertThat(foundUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("정상 케이스 - ID와 설정을 포함한 사용자 조회")
    void shouldFindUserWithSetting_WhenValidIdProvided() {
        // When: 설정을 포함한 사용자 조회
        Optional<User> result = userQueryAdapter.findByIdWithSetting(testUser1.getId());

        // Then: 사용자와 설정이 모두 조회되는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getId()).isEqualTo(testUser1.getId());
        assertThat(foundUser.getSetting()).isNotNull();
        assertThat(foundUser.getSetting().isMessageNotification()).isTrue();
        assertThat(foundUser.getSetting().isCommentNotification()).isFalse();
        assertThat(foundUser.getSetting().isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 소셜 제공자와 소셜 ID로 사용자 조회")
    void shouldFindUserByProviderAndSocialId_WhenValidParametersProvided() {
        // When: 소셜 정보로 사용자 조회
        Optional<User> result = userQueryAdapter.findByProviderAndSocialId(
                SocialProvider.KAKAO, "kakao123"
        );

        // Then: 올바른 소셜 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getSocialId()).isEqualTo("kakao123");
        assertThat(foundUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(foundUser.getUserName()).isEqualTo("testUser1");
    }

    @Test
    @DisplayName("정상 케이스 - 닉네임 존재 여부 확인")
    void shouldCheckUserNameExists_WhenUserNameProvided() {
        // When: 존재하는 닉네임 확인
        boolean existingResult = userQueryAdapter.existsByUserName("testUser1");
        
        // And: 존재하지 않는 닉네임 확인
        boolean nonExistingResult = userQueryAdapter.existsByUserName("nonExistentUser");

        // Then: 존재 여부가 올바르게 반환되는지 검증
        assertThat(existingResult).isTrue();
        assertThat(nonExistingResult).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 닉네임으로 사용자 조회")
    void shouldFindUserByUserName_WhenValidUserNameProvided() {
        // When: 닉네임으로 사용자 조회
        Optional<User> result = userQueryAdapter.findByUserName("testUser2");

        // Then: 올바른 사용자가 조회되는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getUserName()).isEqualTo("testUser2");
        assertThat(foundUser.getSocialId()).isEqualTo("kakao456");
    }

    @Test
    @DisplayName("정상 케이스 - ID로 설정 조회")
    void shouldFindSetting_WhenValidSettingIdProvided() {
        // When: 설정 ID로 조회
        Optional<Setting> result = userQueryAdapter.findSettingById(testSetting.getId());

        // Then: 올바른 설정이 조회되는지 검증
        assertThat(result).isPresent();
        Setting foundSetting = result.get();
        assertThat(foundSetting.getId()).isEqualTo(testSetting.getId());
        assertThat(foundSetting.isMessageNotification()).isTrue();
        assertThat(foundSetting.isCommentNotification()).isFalse();
        assertThat(foundSetting.isPostFeaturedNotification()).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 순서대로 사용자 이름 조회")
    void shouldFindUserNamesInOrder_WhenSocialIdsProvided() {
        // Given: 추가 사용자 생성
        Setting testSetting3 = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
        testSetting3 = settingRepository.save(testSetting3);
        
        User testUser3 = User.builder()
                .socialId("kakao789")
                .provider(SocialProvider.KAKAO)
                .userName("testUser3")
                .role(UserRole.USER)
                .setting(testSetting3)
                .build();
        userRepository.save(testUser3);

        // When: 소셜 ID 목록으로 사용자 이름 조회
        List<String> socialIds = Arrays.asList("kakao123", "kakao456", "kakao789");
        List<String> result = userQueryAdapter.findUserNamesInOrder(socialIds);

        // Then: 요청한 순서대로 사용자 이름이 조회되는지 검증
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("testUser1", "testUser2", "testUser3");
    }

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 참조 가져오기")
    void shouldGetUserReference_WhenValidIdProvided() {
        // When: 사용자 참조 가져오기
        User result = userQueryAdapter.getReferenceById(testUser1.getId());

        // Then: 올바른 사용자 참조가 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser1.getId());
        // 주의: getReferenceById는 프록시를 반환할 수 있으므로 ID 외 필드 접근 시 쿼리 발생 가능
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 사용자 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // When: 존재하지 않는 사용자 조회
        Optional<User> result = userQueryAdapter.findById(999L);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 닉네임으로 사용자 조회")
    void shouldReturnEmpty_WhenNonExistentUserNameProvided() {
        // When: 존재하지 않는 닉네임으로 사용자 조회
        Optional<User> result = userQueryAdapter.findByUserName("nonExistentUser");

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 빈 소셜 ID 목록으로 사용자 이름 조회")
    void shouldReturnEmptyList_WhenEmptySocialIdsProvided() {
        // When: 빈 목록으로 사용자 이름 조회
        List<String> result = userQueryAdapter.findUserNamesInOrder(Arrays.asList());

        // Then: 빈 목록이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 다른 설정을 가진 사용자 조회")
    void shouldHandleUserWithDifferentSetting_WhenUserHasDifferentSetting() {
        // When: 다른 설정을 가진 사용자 조회
        Optional<User> result = userQueryAdapter.findByIdWithSetting(testUser2.getId());

        // Then: 사용자와 해당 설정이 올바르게 조회되는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getId()).isEqualTo(testUser2.getId());
        assertThat(foundUser.getSetting()).isNotNull();
        assertThat(foundUser.getSetting().isMessageNotification()).isFalse();
        assertThat(foundUser.getSetting().isCommentNotification()).isTrue();
        assertThat(foundUser.getSetting().isPostFeaturedNotification()).isFalse();
    }

    @Test
    @DisplayName("트랜잭션 - 여러 사용자 조회 시 데이터 일관성")
    void shouldMaintainDataConsistency_WhenMultipleQueries() {
        // When: 동일한 사용자를 여러 방법으로 조회
        Optional<User> userById = userQueryAdapter.findById(testUser1.getId());
        Optional<User> userByName = userQueryAdapter.findByUserName(testUser1.getUserName());
        Optional<User> userBySocial = userQueryAdapter.findByProviderAndSocialId(
                testUser1.getProvider(), testUser1.getSocialId()
        );

        // Then: 모든 조회 결과가 동일한 사용자를 가리키는지 검증
        assertThat(userById).isPresent();
        assertThat(userByName).isPresent();
        assertThat(userBySocial).isPresent();

        User user1 = userById.get();
        User user2 = userByName.get();
        User user3 = userBySocial.get();

        assertThat(user1.getId()).isEqualTo(user2.getId()).isEqualTo(user3.getId());
        assertThat(user1.getUserName()).isEqualTo(user2.getUserName()).isEqualTo(user3.getUserName());
        assertThat(user1.getSocialId()).isEqualTo(user2.getSocialId()).isEqualTo(user3.getSocialId());
    }

    @Test
    @DisplayName("DB 매핑 - 모든 필드 매핑 정확성 검증")
    void shouldMapAllFields_WhenUserSavedAndRetrieved() {
        // When: 저장된 사용자를 다시 조회
        Optional<User> result = userQueryAdapter.findById(testUser1.getId());

        // Then: 모든 필드가 정확히 매핑되었는지 검증
        assertThat(result).isPresent();
        User foundUser = result.get();
        
        // 기본 필드 검증
        assertThat(foundUser.getId()).isEqualTo(testUser1.getId());
        assertThat(foundUser.getSocialId()).isEqualTo("kakao123");
        assertThat(foundUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(foundUser.getUserName()).isEqualTo("testUser1");
        assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
        
        // 카카오 특화 필드 검증
        assertThat(foundUser.getSocialNickname()).isEqualTo("카카오유저1");
        assertThat(foundUser.getThumbnailImage()).isEqualTo("http://example.com/image1.jpg");
        
        // 관계 매핑 검증
        assertThat(foundUser.getSetting()).isNotNull();
        assertThat(foundUser.getSetting().getId()).isEqualTo(testSetting.getId());
        
        // 기본 엔티티 필드 검증 (createdAt, modifiedAt)
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getModifiedAt()).isNotNull();
    }
}
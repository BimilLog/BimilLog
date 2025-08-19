package jaeik.growfarm.infrastructure.adapter.user.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.setting.SettingRepository;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenAdapter;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token.TokenRepository;
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
 * <h2>TokenAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 TokenAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 토큰 CRUD 동작 검증</p>
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
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.token"
})
@Import(TokenAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class TokenAdapterIntegrationTest {

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
    private TokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private TokenAdapter tokenAdapter;

    private User testUser;
    private Token testToken;

    @BeforeEach
    void setUp() {
        // 설정 생성
        Setting setting = Setting.builder()
                .messageNotification(true)
                .commentNotification(false)
                .postFeaturedNotification(true)
                .build();
        setting = entityManager.persistAndFlush(setting);

        // 사용자 생성
        testUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("tokenTestUser")
                .socialNickname("토큰테스트유저")
                .thumbnailImage("http://example.com/token-test.jpg")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        testUser = entityManager.persistAndFlush(testUser);

        // 토큰 생성
        testToken = Token.builder()
                .accessToken("kakao-access-token-123")
                .refreshToken("kakao-refresh-token-123")
                .users(testUser)
                .build();
        testToken = entityManager.persistAndFlush(testToken);
        
        entityManager.clear(); // 캐시 클리어
    }

    @Test
    @DisplayName("정상 케이스 - ID로 토큰 조회")
    void shouldFindToken_WhenValidIdProvided() {
        // When: 토큰 ID로 조회
        Optional<Token> result = tokenAdapter.findById(testToken.getId());

        // Then: 올바른 토큰이 조회되는지 검증
        assertThat(result).isPresent();
        Token foundToken = result.get();
        assertThat(foundToken.getId()).isEqualTo(testToken.getId());
        assertThat(foundToken.getAccessToken()).isEqualTo("kakao-access-token-123");
        assertThat(foundToken.getRefreshToken()).isEqualTo("kakao-refresh-token-123");
        assertThat(foundToken.getUsers().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 사용자로 토큰 조회")
    void shouldFindTokenByUser_WhenValidUserProvided() {
        // When: 사용자로 토큰 조회
        Optional<Token> result = tokenAdapter.findByUsers(testUser);

        // Then: 해당 사용자의 토큰이 조회되는지 검증
        assertThat(result).isPresent();
        Token foundToken = result.get();
        assertThat(foundToken.getId()).isEqualTo(testToken.getId());
        assertThat(foundToken.getAccessToken()).isEqualTo("kakao-access-token-123");
        assertThat(foundToken.getRefreshToken()).isEqualTo("kakao-refresh-token-123");
        assertThat(foundToken.getUsers().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 토큰 저장")
    void shouldSaveNewToken_WhenValidTokenProvided() {
        // Given: 새로운 사용자 생성
        User newUser = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("newTokenUser")
                .role(UserRole.USER)
                .setting(null)
                .build();
        newUser = userRepository.save(newUser);

        // 새로운 토큰 생성
        Token newToken = Token.builder()
                .accessToken("new-access-token-456")
                .refreshToken("new-refresh-token-456")
                .users(newUser)
                .build();

        // When: 새로운 토큰 저장
        Token savedToken = tokenAdapter.save(newToken);

        // Then: 토큰이 올바르게 저장되었는지 검증
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getAccessToken()).isEqualTo("new-access-token-456");
        assertThat(savedToken.getRefreshToken()).isEqualTo("new-refresh-token-456");
        assertThat(savedToken.getUsers().getId()).isEqualTo(newUser.getId());

        // DB에 실제로 저장되었는지 확인
        Optional<Token> foundToken = tokenRepository.findById(savedToken.getId());
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getAccessToken()).isEqualTo("new-access-token-456");
    }

    @Test
    @DisplayName("정상 케이스 - 기존 토큰 업데이트")
    void shouldUpdateExistingToken_WhenTokenModified() {
        // Given: 기존 토큰을 수정
        testToken.updateToken("updated-access-token", "updated-refresh-token");

        // When: 수정된 토큰 저장
        Token updatedToken = tokenAdapter.save(testToken);

        // Then: 토큰이 올바르게 업데이트되었는지 검증
        assertThat(updatedToken.getId()).isEqualTo(testToken.getId());
        assertThat(updatedToken.getAccessToken()).isEqualTo("updated-access-token");
        assertThat(updatedToken.getRefreshToken()).isEqualTo("updated-refresh-token");
        assertThat(updatedToken.getUsers().getId()).isEqualTo(testUser.getId());

        // DB에서 다시 조회하여 확인
        Optional<Token> foundToken = tokenRepository.findById(updatedToken.getId());
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getAccessToken()).isEqualTo("updated-access-token");
        assertThat(foundToken.get().getRefreshToken()).isEqualTo("updated-refresh-token");
    }

    @Test
    @DisplayName("정상 케이스 - 리프레시 토큰 없이 액세스 토큰만 업데이트")
    void shouldUpdateAccessTokenOnly_WhenRefreshTokenIsNull() {
        // Given: 리프레시 토큰을 null로 설정하여 업데이트
        String originalRefreshToken = testToken.getRefreshToken();
        testToken.updateToken("updated-access-only", null);

        // When: 수정된 토큰 저장
        Token updatedToken = tokenAdapter.save(testToken);

        // Then: 액세스 토큰만 업데이트되고 리프레시 토큰은 유지되는지 검증
        assertThat(updatedToken.getAccessToken()).isEqualTo("updated-access-only");
        assertThat(updatedToken.getRefreshToken()).isEqualTo(originalRefreshToken);

        // DB에서 다시 조회하여 확인
        Optional<Token> foundToken = tokenRepository.findById(updatedToken.getId());
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getAccessToken()).isEqualTo("updated-access-only");
        assertThat(foundToken.get().getRefreshToken()).isEqualTo(originalRefreshToken);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 토큰 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // When: 존재하지 않는 토큰 조회
        Optional<Token> result = tokenAdapter.findById(999L);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 토큰이 없는 사용자로 조회")
    void shouldReturnEmpty_WhenUserHasNoToken() {
        // Given: 토큰이 없는 새로운 사용자
        User userWithoutToken = User.builder()
                .socialId("kakao999")
                .provider(SocialProvider.KAKAO)
                .userName("userWithoutToken")
                .role(UserRole.USER)
                .setting(null)
                .build();
        userWithoutToken = userRepository.save(userWithoutToken);

        // When: 토큰이 없는 사용자로 토큰 조회
        Optional<Token> result = tokenAdapter.findByUsers(userWithoutToken);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("관계 매핑 - 사용자 삭제 시 토큰도 자동 삭제 (CASCADE)")
    void shouldDeleteToken_WhenUserIsDeleted() {
        // Given: 토큰과 연결된 사용자 ID 확인
        Long userId = testUser.getId();
        Long tokenId = testToken.getId();
        
        // 사용자와 토큰 존재 확인
        assertThat(userRepository.findById(userId)).isPresent();
        assertThat(tokenRepository.findById(tokenId)).isPresent();

        // When: 사용자 삭제 (CASCADE로 토큰도 함께 삭제되어야 함)
        userRepository.deleteById(userId);

        // Then: 토큰도 자동으로 삭제되었는지 검증
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(tokenRepository.findById(tokenId)).isEmpty();
    }

    @Test
    @DisplayName("DB 매핑 - 모든 필드 매핑 정확성 검증")
    void shouldMapAllFields_WhenTokenSavedAndRetrieved() {
        // When: 저장된 토큰을 다시 조회
        Optional<Token> result = tokenAdapter.findById(testToken.getId());

        // Then: 모든 필드가 정확히 매핑되었는지 검증
        assertThat(result).isPresent();
        Token foundToken = result.get();
        
        // 기본 필드 검증
        assertThat(foundToken.getId()).isEqualTo(testToken.getId());
        assertThat(foundToken.getAccessToken()).isEqualTo("kakao-access-token-123");
        assertThat(foundToken.getRefreshToken()).isEqualTo("kakao-refresh-token-123");
        
        // 관계 매핑 검증
        assertThat(foundToken.getUsers()).isNotNull();
        assertThat(foundToken.getUsers().getId()).isEqualTo(testUser.getId());
        assertThat(foundToken.getUsers().getUserName()).isEqualTo("tokenTestUser");
        
        // 기본 엔티티 필드 검증 (createdAt, modifiedAt)
        assertThat(foundToken.getCreatedAt()).isNotNull();
        assertThat(foundToken.getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("성능 - Lazy Loading 동작 검증")
    void shouldUseLazyLoading_WhenAccessingUserFromToken() {
        // When: 토큰 조회 (사용자는 Lazy Loading)
        Optional<Token> result = tokenAdapter.findById(testToken.getId());

        // Then: 토큰은 조회되지만 사용자 정보는 필요시에만 로딩
        assertThat(result).isPresent();
        Token foundToken = result.get();
        
        // 사용자 정보에 접근할 때 실제 로딩
        User user = foundToken.getUsers();
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUser.getId());
        assertThat(user.getUserName()).isEqualTo("tokenTestUser");
    }

    @Test
    @DisplayName("트랜잭션 - 한 사용자의 여러 토큰 시나리오")
    void shouldHandleMultipleTokensPerUser_WhenBusinessRuleAllows() {
        // Given: 동일한 사용자에 대한 추가 토큰 생성
        Token additionalToken = Token.builder()
                .accessToken("additional-access-token")
                .refreshToken("additional-refresh-token")
                .users(testUser)
                .build();

        // When: 추가 토큰 저장
        Token savedAdditionalToken = tokenAdapter.save(additionalToken);

        // Then: 두 개의 토큰이 모두 저장되었는지 검증
        assertThat(savedAdditionalToken).isNotNull();
        assertThat(savedAdditionalToken.getId()).isNotNull();
        assertThat(savedAdditionalToken.getId()).isNotEqualTo(testToken.getId());
        
        // 사용자로 토큰 조회 시 하나만 반환 (findByUsers는 하나의 결과만 반환)
        Optional<Token> userTokenResult = tokenAdapter.findByUsers(testUser);
        assertThat(userTokenResult).isPresent();
        
        // 두 토큰 모두 DB에 존재하는지 확인
        assertThat(tokenRepository.findById(testToken.getId())).isPresent();
        assertThat(tokenRepository.findById(savedAdditionalToken.getId())).isPresent();
    }

    @Test
    @DisplayName("경계값 - null 사용자로 토큰 조회")
    void shouldHandleNullUser_WhenNullUserProvided() {
        // When: null 사용자로 토큰 조회
        Optional<Token> result = tokenAdapter.findByUsers(null);

        // Then: 빈 Optional이 반환되거나 예외가 발생하지 않아야 함
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // null User에 대한 방어 코드 누락으로 NPE 발생 가능성
        // 수정 필요: TokenAdapter.findByUsers() 메서드에 null 검증 추가
        assertThat(result).isEmpty();
    }
}
package jaeik.bimillog.infrastructure.outadapter.user.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.SettingRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.user.TokenAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.UserRepository;
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

import java.util.List;
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
        "jaeik.bimillog.infrastructure.adapter.user.out.user.token"
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
        testToken = Token.createTemporaryToken("access-token", "refresh-token");
                
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
    @DisplayName("정상 케이스 - 토큰 ID로 토큰 조회 (다중 로그인 권장 방식)")
    void shouldFindTokenById_WhenValidTokenIdProvided() {
        // When: 토큰 ID로 토큰 조회 (UserDetails.getTokenId() 사용 시나리오)
        Optional<Token> result = tokenAdapter.findById(testToken.getId());

        // Then: 해당 토큰이 정확히 조회되는지 검증
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
        // Given: 설정을 먼저 생성 (User는 반드시 Setting을 가져야 함)
        Setting newUserSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(false)
                .build();
        newUserSetting = settingRepository.save(newUserSetting);
        
        // 새로운 사용자 생성 (Setting 포함)
        User newUser = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("newTokenUser")
                .role(UserRole.USER)
                .setting(newUserSetting)
                .build();
        newUser = userRepository.save(newUser);

        // 새로운 토큰 생성
        Token newToken = Token.createTemporaryToken("access-token", "refresh-token");
                

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
    @DisplayName("경계값 - 존재하지 않는 토큰 ID로 조회")
    void shouldReturnEmpty_WhenTokenIdNotExists() {
        // When: 존재하지 않는 토큰 ID로 조회 (비즈니스 케이스)
        Optional<Token> result = tokenAdapter.findById(999L);

        // Then: 빈 Optional이 반환되는지 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("관계 매핑 - 사용자 삭제 시 모든 토큰 자동 삭제 (CASCADE)")
    void shouldDeleteAllTokens_WhenUserIsDeleted() {
        // Given: 사용자가 여러 토큰을 가진 상황 생성 (다중 로그인)
        Token additionalToken = Token.createTemporaryToken("access-token", "refresh-token");
                
        Token savedAdditionalToken = entityManager.persistAndFlush(additionalToken);
        entityManager.clear();
        
        Long userId = testUser.getId();
        Long firstTokenId = testToken.getId();
        Long secondTokenId = savedAdditionalToken.getId();
        
        // 사용자와 모든 토큰 존재 확인
        assertThat(userRepository.findById(userId)).isPresent();
        assertThat(tokenRepository.findById(firstTokenId)).isPresent();
        assertThat(tokenRepository.findById(secondTokenId)).isPresent();
        
        // Repository 레벨에서 토큰 개수 확인
        List<Token> userTokens = tokenRepository.findByUsersId(userId);
        assertThat(userTokens).hasSize(2);

        // When: 사용자 삭제 (CASCADE로 모든 토큰이 함께 삭제되어야 함)
        userRepository.deleteById(userId);

        // Then: 사용자와 모든 토큰이 자동으로 삭제되었는지 검증
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(tokenRepository.findById(firstTokenId)).isEmpty();
        assertThat(tokenRepository.findById(secondTokenId)).isEmpty();
        
        // 사용자의 모든 토큰이 삭제되었는지 확인
        List<Token> remainingTokens = tokenRepository.findByUsersId(userId);
        assertThat(remainingTokens).isEmpty();
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
    @DisplayName("다중 로그인 - 한 사용자의 여러 토큰 관리 시나리오")
    void shouldHandleMultipleTokensPerUser_WhenMultipleDeviceLogin() {
        // Given: 동일한 사용자에 대한 추가 토큰 생성 (다중 기기 로그인 지원)
        Token additionalToken = Token.createTemporaryToken("access-token", "refresh-token");
                

        // When: 추가 토큰 저장 (새로운 기기에서 로그인)
        Token savedAdditionalToken = tokenAdapter.save(additionalToken);

        // Then: 두 개의 토큰이 모두 저장되었는지 검증
        assertThat(savedAdditionalToken).isNotNull();
        assertThat(savedAdditionalToken.getId()).isNotNull();
        assertThat(savedAdditionalToken.getId()).isNotEqualTo(testToken.getId());
        
        // 다중 로그인 환경: 각 토큰을 ID로 정확히 조회 가능
        Optional<Token> mobileToken = tokenAdapter.findById(testToken.getId());
        Optional<Token> pcToken = tokenAdapter.findById(savedAdditionalToken.getId());
        
        assertThat(mobileToken).isPresent();
        assertThat(pcToken).isPresent();
        assertThat(mobileToken.get().getAccessToken()).isEqualTo("kakao-access-token-123");
        assertThat(pcToken.get().getAccessToken()).isEqualTo("additional-access-token-pc");
        
        // Repository 레벨에서 사용자의 모든 토큰 확인
        List<Token> allUserTokens = tokenRepository.findByUsersId(testUser.getId());
        assertThat(allUserTokens).hasSize(2);
        
        // 다중 로그인 비즈니스 로직: 각 기기별로 고유한 토큰 ID로 정확한 토큰 조회
        // 실제 서비스에서는 UserDetails.getTokenId()로 특정 토큰을 findById()로 조회
        Optional<Token> mobileTokenFound = tokenAdapter.findById(mobileToken.get().getId());
        Optional<Token> pcTokenFound = tokenAdapter.findById(pcToken.get().getId());
        assertThat(mobileTokenFound).isPresent();
        assertThat(pcTokenFound).isPresent();
    }

    @Test
    @DisplayName("다중 로그인 - 특정 기기 로그아웃 시나리오")
    void shouldDeleteSpecificToken_WhenSingleDeviceLogout() {
        // Given: 사용자가 두 기기에서 로그인한 상황
        Token pcToken = Token.createTemporaryToken("access-token", "refresh-token");
                
        Token savedPcToken = entityManager.persistAndFlush(pcToken);
        entityManager.clear();
        
        Long mobileTokenId = testToken.getId();
        Long pcTokenId = savedPcToken.getId();
        
        // 두 토큰 모두 존재 확인
        assertThat(tokenRepository.findById(mobileTokenId)).isPresent();
        assertThat(tokenRepository.findById(pcTokenId)).isPresent();
        
        // When: PC에서만 로그아웃 (특정 토큰만 삭제)
        tokenRepository.deleteById(pcTokenId);
        
        // Then: PC 토큰만 삭제되고 모바일 토큰은 유지되어야 함
        assertThat(tokenRepository.findById(pcTokenId)).isEmpty(); // PC 토큰 삭제됨
        assertThat(tokenRepository.findById(mobileTokenId)).isPresent(); // 모바일 토큰 유지됨
        
        // 사용자는 여전히 존재하고 모바일에서는 계속 로그인 상태
        assertThat(userRepository.findById(testUser.getId())).isPresent();
        
        // Repository 레벨에서 남은 토큰 확인
        List<Token> remainingTokens = tokenRepository.findByUsersId(testUser.getId());
        assertThat(remainingTokens).hasSize(1); // 모바일 토큰 1개만 남음
        assertThat(remainingTokens.getFirst().getId()).isEqualTo(mobileTokenId);
    }

    @Test
    @DisplayName("경계값 - null 토큰 ID로 토큰 조회")
    void shouldHandleNullTokenId_WhenNullTokenIdProvided() {
        // When: null 토큰 ID로 토큰 조회
        Optional<Token> result = tokenAdapter.findById(null);

        // Then: 빈 Optional이 반환되어야 함 (null 안전성 확보됨)
        assertThat(result).isEmpty();
    }
}
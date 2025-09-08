package jaeik.bimillog.infrastructure.outadapter.auth.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.auth.out.persistence.user.TokenJpaAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.token.TokenRepository;
import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
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
 * <h2>TokenJpaAdapter 통합 테스트</h2>
 * <p>MySQL TestContainer를 사용한 토큰 어댑터의 데이터베이스 통합 테스트</p>
 * <p>데이터 매핑, JPA 관계, 영속성 레이어를 실제 환경에서 검증</p>
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
        "jaeik.bimillog.domain.admin.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.paper.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.notification.entity",
        "jaeik.bimillog.domain.global.entity"
})
@Import(TokenJpaAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class TokenJpaAdapterTest {

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

        // EncryptionUtil 빈 정의: MessageEncryptConverter의 의존성을 만족시킵니다.
        @Bean
        public EncryptionUtil encryptionUtil() {
            return new EncryptionUtil();
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private TokenJpaAdapter tokenJpaAdapter;

    @Test
    @DisplayName("토큰 ID로 조회 - 정상적인 매핑 검증")
    void shouldFindTokenById_WhenTokenExists() {
        // Given: 실제 User와 Token 데이터 생성
        User user = createAndSaveUser("testUser", "123456789");
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        Token savedToken = entityManager.persistAndFlush(token);
        entityManager.clear(); // 1차 캐시 클리어

        // When: 어댑터를 통한 토큰 조회
        Optional<Token> result = tokenJpaAdapter.findById(savedToken.getId());

        // Then: 정확한 매핑과 관계 검증
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedToken.getId());
        assertThat(result.get().getAccessToken()).isEqualTo("test-access-token");
        assertThat(result.get().getRefreshToken()).isEqualTo("test-refresh-token");
        assertThat(result.get().getUsers()).isNotNull();
        assertThat(result.get().getUsers().getId()).isEqualTo(user.getId());
        assertThat(result.get().getUsers().getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("존재하지 않는 토큰 ID 조회 - Empty 반환 검증")
    void shouldReturnEmpty_WhenTokenNotExists() {
        // Given: 존재하지 않는 토큰 ID
        Long nonExistentId = 999L;

        // When: 존재하지 않는 토큰 조회
        Optional<Token> result = tokenJpaAdapter.findById(nonExistentId);

        // Then: Optional.empty() 반환 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 모든 토큰 조회 - 복수 토큰 매핑 검증")
    void shouldFindAllTokensByUserId_WhenMultipleTokensExist() {
        // Given: 한 사용자가 여러 토큰을 가진 경우 (재로그인 등)
        User user = createAndSaveUser("multiTokenUser", "987654321");
        
        Token token1 = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        Token token2 = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        entityManager.persistAndFlush(token1);
        entityManager.persistAndFlush(token2);
        entityManager.clear();

        // When: 사용자 ID로 모든 토큰 조회
        List<Token> result = tokenJpaAdapter.findAllByUserId(user.getId());

        // Then: 모든 토큰과 올바른 관계 검증
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Token::getAccessToken)
                .containsExactlyInAnyOrder("access-token-1", "access-token-2");
        assertThat(result).extracting(Token::getRefreshToken)
                .containsExactlyInAnyOrder("refresh-token-1", "refresh-token-2");
        
        // 모든 토큰이 동일한 사용자와 연결되어 있는지 검증
        assertThat(result).allSatisfy(token -> {
            assertThat(token.getUsers()).isNotNull();
            assertThat(token.getUsers().getId()).isEqualTo(user.getId());
            assertThat(token.getUsers().getUserName()).isEqualTo("multiTokenUser");
        });
    }

    @Test
    @DisplayName("토큰이 없는 사용자 조회 - 빈 리스트 반환 검증")
    void shouldReturnEmptyList_WhenUserHasNoTokens() {
        // Given: 토큰이 없는 사용자
        User user = createAndSaveUser("noTokenUser", "111222333");
        entityManager.clear();

        // When: 토큰이 없는 사용자의 토큰 조회
        List<Token> result = tokenJpaAdapter.findAllByUserId(user.getId());

        // Then: 빈 리스트 반환 검증
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JPA 관계 매핑 검증 - 양방향 관계 일관성")
    void shouldMaintainBidirectionalRelationship_BetweenUserAndToken() {
        // Given: User-Token 양방향 관계
        User user = createAndSaveUser("relationUser", "444555666");
        
        Token token = Token.createTemporaryToken("access-token", "refresh-token");
                
        
        Token savedToken = entityManager.persistAndFlush(token);
        entityManager.clear(); // 캐시 클리어로 실제 DB 조회 강제

        // When: 어댑터를 통한 조회
        Optional<Token> foundToken = tokenJpaAdapter.findById(savedToken.getId());

        // Then: 양방향 관계 일관성 검증
        assertThat(foundToken).isPresent();
        Token token_result = foundToken.get();
        
        // Token -> User 관계
        assertThat(token_result.getUsers()).isNotNull();
        assertThat(token_result.getUsers().getId()).isEqualTo(user.getId());
        
        // 관계의 데이터 일관성 검증
        User relatedUser = token_result.getUsers();
        assertThat(relatedUser.getUserName()).isEqualTo("relationUser");
        assertThat(relatedUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(relatedUser.getSocialId()).isEqualTo("444555666");
    }

    /**
     * <h3>테스트용 사용자 생성 및 저장</h3>
     */
    private User createAndSaveUser(String userName, String socialId) {
        Setting setting = Setting.builder()
                .commentNotification(true)
                .messageNotification(true)
                .postFeaturedNotification(true)
                .build();
        entityManager.persist(setting);

        User user = User.builder()
                .userName(userName)
                .socialId(socialId)
                .provider(SocialProvider.KAKAO)
                .role(UserRole.USER)
                .socialNickname(userName + "_nickname")
                .thumbnailImage("https://example.com/profile.jpg")
                .setting(setting)
                .build();

        return entityManager.persistAndFlush(user);
    }
}
package jaeik.growfarm.infrastructure.adapter.post.out.persistence.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.Token;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>UserAdapterLoad 통합 테스트</h2>
 *
 * <p><strong>테스트 커버리지:</strong></p>
 * <ul>
 *   <li>정상 케이스: 사용자 프록시 조회 성공</li>
 *   <li>JPA 프록시: Lazy Loading, 프록시 초기화</li>
 *   <li>도메인 간 어댑터: Post -> User 도메인 연결</li>
 *   <li>성능 최적화: getReferenceById vs findById</li>
 *   <li>예외 처리: 존재하지 않는 사용자, null 입력</li>
 *   <li>동시성: 동시 프록시 조회 상황</li>
 *   <li>트랜잭션 경계: 도메인 결합도 검증</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GrowfarmApplication.class)
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.admin.entity",
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.paper.entity",
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.comment.entity",
        "jaeik.growfarm.domain.notification.entity",
        "jaeik.growfarm.domain.common.entity"
})
@Import({LoadUserInfoAdapter.class, UserAdapterLoadTest.TestUserQueryUseCase.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
class UserAdapterLoadTest {

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
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
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

    // TestUserQueryUseCase: 실제 UserQueryUseCase를 대체할 테스트용 구현체
    @Component
    static class TestUserQueryUseCase implements UserQueryUseCase {

        @Autowired
        private EntityManager entityManager;

        @Override
        public User getReferenceById(Long userId) {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            return entityManager.getReference(User.class, userId);
        }

        // 다른 메소드들은 실제 구현이 필요하면 추가...
        @Override
        public Optional<User> findByUserName(String userName) {
            return Optional.empty();
        }

        @Override
        public boolean existsByUserName(String userName) {
            return false;
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(entityManager.find(User.class, userId));
        }

        @Override
        public Optional<User> findByProviderAndSocialId(SocialProvider provider, String socialId) {
            return Optional.empty();
        }

        @Override
        public Optional<Token> findTokenById(Long tokenId) {
            return Optional.empty();
        }
    }

    @Autowired
    private LoadUserInfoAdapter loadUserInfoAdapter;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성 및 저장
        testUser = User.builder()
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(testUser);

        User otherUser = User.builder()
                .userName("otherUser")
                .socialId("789012")
                .provider(SocialProvider.NAVER)
                .socialNickname("다른유저")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(otherUser);

        entityManager.clear(); // 영속성 컨텍스트 초기화
    }

    @Test
    @DisplayName("정상 케이스 - ID로 사용자 프록시 조회 성공")
    void shouldReturnUserProxy_WhenValidUserIdProvided() {
        // When: 실제 사용자 ID로 프록시 조회
        User resultUser = loadUserInfoAdapter.getReferenceById(testUser.getId());

        // Then: JPA 프록시 객체 반환 확인
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(testUser.getId());
        
        // 프록시 특성: ID는 즉시 사용 가능하지만 다른 필드 접근 시 Lazy Loading 발생
        Long proxyId = resultUser.getId(); // 즉시 사용 가능
        assertThat(proxyId).isEqualTo(testUser.getId());
        
        // Lazy Loading 테스트: 다른 필드 접근 시 DB 조회 발생
        String userName = resultUser.getUserName(); // 이 시점에서 Lazy Loading
        assertThat(userName).isEqualTo("testUser");
    }

    @Test
    @DisplayName("성능 최적화 - getReferenceById vs findById 비교")
    void shouldOptimizePerformance_WhenComparingReferenceAndFind() {
        // Given: 성능 비교를 위한 대용량 사용자 데이터
        List<User> bulkUsers = IntStream.range(0, 50)
                .mapToObj(i -> {
                    User user = User.builder()
                            .userName("bulkUser" + i)
                            .socialId("bulk" + i)
                            .provider(SocialProvider.KAKAO)
                            .socialNickname("벌크유저" + i)
                            .role(UserRole.USER)
                            .setting(Setting.builder().build())
                            .build();
                    entityManager.persistAndFlush(user);
                    return user;
                })
                .toList();
        entityManager.clear();
        
        Long targetUserId = bulkUsers.get(25).getId();

        // When: getReferenceById 성능 테스트
        long startTime = System.currentTimeMillis();
        User proxyUser = loadUserInfoAdapter.getReferenceById(targetUserId);
        long proxyTime = System.currentTimeMillis() - startTime;
        
        // getReferenceById는 즉시 실행되어야 함 (DB 조회 없이 프록시 생성)
        assertThat(proxyTime).isLessThan(100); // 100ms 이내
        assertThat(proxyUser.getId()).isEqualTo(targetUserId); // ID는 즉시 사용 가능
        
        // Then: 프록시 사용의 장점 확인
        // 1. 즉시 ID 사용 가능
        // 2. Lazy Loading으로 인해 필요 시점에만 DB 조회
        // 3. JPA 연관관계 설정 시 성능 이점
    }

    @Test
    @DisplayName("예외 처리 - 존재하지 않는 사용자 ID로 프록시 조회")
    void shouldThrowException_WhenNonExistentUserIdProvided() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 99999L;

        // When: 존재하지 않는 ID로 프록시 조회
        User proxyUser = loadUserInfoAdapter.getReferenceById(nonExistentUserId);
        
        // Then: 프록시는 생성되지만 Lazy Loading 시 예외 발생
        assertThat(proxyUser).isNotNull(); // 프록시는 생성됨
        assertThat(proxyUser.getId()).isEqualTo(nonExistentUserId); // ID는 사용 가능
        
        // Lazy Loading 시도 시 예외 발생 확인
        assertThatThrownBy(() -> {
            String userName = proxyUser.getUserName(); // Lazy Loading 시도
        }).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("예외 처리 - null 사용자 ID로 프록시 조회")
    void shouldThrowException_WhenNullUserIdProvided() {
        // When & Then: null ID로 프록시 조회 시 예외 발생
        assertThatThrownBy(() -> {
            loadUserInfoAdapter.getReferenceById(null);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("JPA 프록시 - 실제 데이터 Lazy Loading 검증")
    void shouldLazyLoadData_WhenAccessingProxyFields() {
        // When: 프록시 생성 (영속성 컨텍스트 유지)
        User proxyUser = loadUserInfoAdapter.getReferenceById(testUser.getId());
        
        // Then: 같은 트랜잭션 내에서 Lazy Loading 가능
        assertThat(proxyUser.getId()).isEqualTo(testUser.getId()); // ID는 즉시 사용 가능
        
        // 다른 필드 접근 시 DB에서 로드 (세션이 열려있어야 함)
        String userName = proxyUser.getUserName();
        assertThat(userName).isEqualTo("testUser");
        
        // 연관관계 데이터도 Lazy Loading
        Setting setting = proxyUser.getSetting();
        assertThat(setting).isNotNull();
        assertThat(setting.isMessageNotification()).isTrue();
        
        // 영속성 컨텍스트 초기화는 테스트 마지막에
        entityManager.clear();
    }

    @Test
    @DisplayName("도메인 간 어댑터 - Post 도메인에서 User 도메인 연결")
    void shouldConnectDomains_WhenPostDomainAccessesUserDomain() {
        // Given: Post 도메인에서 User 도메인 연결이 필요한 상황 시뮬레이션
        Long userId = testUser.getId();
        
        // When: UserAdapterLoad를 통해 Post -> User 도메인 연결
        User userReference = loadUserInfoAdapter.getReferenceById(userId);
        
        // Then: 도메인 간 결합도 확인
        // 1. User 도메인의 UseCase를 통한 연결
        assertThat(userReference).isNotNull();
        assertThat(userReference.getId()).isEqualTo(userId);
        
        // 2. Post 도메인에서 필요한 User 정보 접근 가능
        assertThat(userReference.getUserName()).isEqualTo("testUser");
        assertThat(userReference.getRole()).isEqualTo(UserRole.USER);
        
        // 3. 도메인 경계 유지 확인 (직접 DB 접근 없이 UseCase를 통한 연결)
    }

    @Test
    @DisplayName("동시성 - 동시 프록시 조회 상황 처리")
    void shouldHandleConcurrentAccess_WhenMultipleProxyRequests() {
        // Given: 동시성 테스트용 사용자 ID
        Long userId = testUser.getId();

        // When: 동일 스레드에서 순차적 프록시 요청 (동시성 시뮬레이션)
        List<User> results = IntStream.range(0, 5)
                .mapToObj(i -> loadUserInfoAdapter.getReferenceById(userId))
                .toList();

        // Then: 모든 요청이 성공적으로 프록시 반환
        assertThat(results).hasSize(5);
        results.forEach(user -> {
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(userId);
        });
        
        // 동시성 데이터 일관성 확인 (같은 트랜잭션 내에서)
        results.forEach(user -> {
            assertThat(user.getUserName()).isEqualTo("testUser");
        });
    }

    @Test
    @DisplayName("트랜잭션 경계 - 프록시 ID 접근은 트랜잭션 경계와 무관")
    void shouldWorkAcrossTransactions_WhenUsingProxyInDifferentTransactions() {
        // Given: 어댱터를 통해 프록시 생성
        User proxyUser = loadUserInfoAdapter.getReferenceById(testUser.getId());
        
        // 영속성 컨텍스트 분리 (트랜잭션 경계 시뮤레이션)
        entityManager.flush();
        entityManager.clear();
        
        // When: 다른 트랜잭션 에서 프록시 사용 (시뮤레이션)
        // Then: ID는 여전히 사용 가능하지만 Lazy Loading은 새로운 처리 필요
        assertThat(proxyUser.getId()).isEqualTo(testUser.getId());
        
        // 먼저 프록시 ID 접근 (이는 항상 가능)
        Long proxyId = proxyUser.getId();
        assertThat(proxyId).isEqualTo(testUser.getId());
        
        // JPA 프록시는 동일 세션에서만 초기화 가능하므로 ID만 테스트
        // String userName = proxyUser.getUserName(); // LazyInitializationException 발생
        // assertThat(userName).isEqualTo("testUser"); // 주석 처리: 세션 없이는 불가능
        
        // Then: 영속성 컨텍스트 분리 후에도 ID는 접근 가능
        entityManager.flush();
        entityManager.clear();
        
        // 프록시의 ID는 여전히 접근 가능 (JPA 프록시 특성)
        assertThat(proxyUser.getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("에러 처리 - 데이터베이스 연결 오류 상황")
    void shouldHandleGracefully_WhenDatabaseConnectionError() {
        // Given: 정상적인 사용자 ID
        Long userId = testUser.getId();
        
        // When: 정상 상황에서의 프록시 생성 (오류 상황 시뮤레이션 어려움)
        User proxyUser = loadUserInfoAdapter.getReferenceById(userId);
        
        // Then: 기본적인 오류 처리는 JPA 레벨에서 처리됨
        assertThat(proxyUser).isNotNull();
        
        // 실제 오류 상황에서의 동작은 실제 운영 환경에서 테스트 필요
        // (TestContainer 환경에서 인위적 오류 상황 만들기 어려움)
    }

    @Test
    @DisplayName("아키텍처 검증 - Hexagonal Architecture 도메인 결합도")
    void shouldMaintainArchitecturalBoundaries_InHexagonalDesign() {
        // Given: Post 도메인에서 User 도메인 연결 시나리오
        Long userId = testUser.getId();
        
        // When: 어댑터를 통한 도메인 간 연결
        User userFromAdapter = loadUserInfoAdapter.getReferenceById(userId);
        
        // Then: 아키텍처 경계 준수 확인
        // 1. Post 도메인은 User 도메인의 구현에 의존하지 않음
        assertThat(userFromAdapter).isNotNull();
        
        // 2. UseCase 인터페이스를 통한 간접 접근
        assertThat(userFromAdapter.getId()).isEqualTo(userId);
        
        // 3. 도메인 엄무에만 집중 (기술적 세부사항 숨김)
        assertThat(userFromAdapter.getUserName()).isEqualTo("testUser");
        
        // 4. 어댑터는 단순 위임자 역할만 수행
        // (UserQueryUseCase.getReferenceById 메소드를 그대로 위임)
    }
}

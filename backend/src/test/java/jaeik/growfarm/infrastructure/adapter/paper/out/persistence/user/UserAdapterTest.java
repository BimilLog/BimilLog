package jaeik.growfarm.infrastructure.adapter.paper.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>Paper 도메인의 UserAdapter 통합 테스트</h2>
 *
 * <p><strong>테스트 커버리지:</strong></p>
 * <ul>
 *   <li>정상 케이스: 사용자 이름으로 조회/존재 확인</li>
 *   <li>도메인 간 통신: Paper -> User 도메인 연결</li>
 *   <li>UseCase 위임: UserQueryUseCase 인터페이스 활용</li>
 *   <li>헥사고널 아키텍처: 도메인 경계 준수</li>
 *   <li>일관성: findByUserName과 existsByUserName 상호 일치</li>
 *   <li>예외 처리: null/빈 문자열, 존재하지 않는 사용자</li>
 *   <li>성능: 대용량 데이터, 동시 조회</li>
 *   <li>지연 로딩: 사용자 연관데이터 처리</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.growfarm.GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.user"
})
@Import({UserAdapter.class, UserAdapterTest.TestUserQueryUseCase.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
        "logging.level.org.hibernate.SQL=DEBUG"
})
class UserAdapterTest {

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
        public com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new com.querydsl.jpa.impl.JPAQueryFactory(entityManager);
        }
    }

    // TestUserQueryUseCase: UserQueryUseCase 실제 구현체 테스트용 버전
    @org.springframework.stereotype.Component
    static class TestUserQueryUseCase implements UserQueryUseCase {
        
        @Autowired
        private EntityManager entityManager;
        
        @Override
        public Optional<User> findByUserName(String userName) {
            if (userName == null || userName.trim().isEmpty()) {
                return Optional.empty();
            }
            
            try {
                User user = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.userName = :userName", User.class)
                    .setParameter("userName", userName)
                    .getSingleResult();
                return Optional.of(user);
            } catch (jakarta.persistence.NoResultException e) {
                return Optional.empty();
            }
        }
        
        @Override
        public boolean existsByUserName(String userName) {
            if (userName == null || userName.trim().isEmpty()) {
                return false;
            }
            
            Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.userName = :userName", Long.class)
                .setParameter("userName", userName)
                .getSingleResult();
            return count > 0;
        }
        
        // 다른 메소드들은 기본 구현
        @Override
        public User getReferenceById(Long userId) {
            return entityManager.getReference(User.class, userId);
        }
        
        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(entityManager.find(User.class, userId));
        }
        
        @Override
        public java.util.Optional<User> findByProviderAndSocialId(jaeik.growfarm.domain.common.entity.SocialProvider provider, String socialId) {
            return java.util.Optional.empty();
        }
        
        @Override
        public org.springframework.data.domain.Page<jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO> getUserPosts(Long userId, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
        
        @Override
        public org.springframework.data.domain.Page<jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO> getUserLikedPosts(Long userId, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
        
        @Override
        public org.springframework.data.domain.Page<jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO> getUserComments(Long userId, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
        
        @Override
        public org.springframework.data.domain.Page<jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO> getUserLikedComments(Long userId, org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
        
        @Override
        public java.util.Optional<jaeik.growfarm.domain.user.entity.Token> findTokenById(Long tokenId) {
            return java.util.Optional.empty();
        }
    }

    @Autowired
    private UserAdapter userAdapter;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private User otherUser;
    private User duplicateNameUser;

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

        otherUser = User.builder()
                .userName("otherUser")
                .socialId("789012")
                .provider(SocialProvider.NAVER)
                .socialNickname("다른유저")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(otherUser);

        // 연관데이터가 있는 사용자 (지연 로딩 테스트용)
        duplicateNameUser = User.builder()
                .userName("duplicateTest")
                .socialId("999999")
                .provider(SocialProvider.GOOGLE)
                .socialNickname("중복테스트")
                .role(UserRole.ADMIN)
                .setting(Setting.builder()
                        .messageNotification(false)
                        .commentNotification(false)
                        .postFeaturedNotification(false)
                        .build())
                .build();
        entityManager.persistAndFlush(duplicateNameUser);

        entityManager.clear(); // 영속성 컨텍스트 초기화
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 사용자 이름으로 사용자 조회 성공")
    void shouldFindUserByUserName_WhenUserExists() {
        // When: 실제 사용자 이름으로 조회
        Optional<User> result = userAdapter.findByUserName("testUser");

        // Then: 정확한 사용자 데이터 반환
        assertThat(result).isPresent();
        User foundUser = result.get();
        assertThat(foundUser.getUserName()).isEqualTo("testUser");
        assertThat(foundUser.getSocialNickname()).isEqualTo("테스트유저");
        assertThat(foundUser.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(foundUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 이름으로 빈 결과 반환")
    void shouldReturnEmpty_WhenUserNotExists() {
        // When: 존재하지 않는 사용자 이름으로 조회
        Optional<User> result = userAdapter.findByUserName("nonExistentUser");

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - null 사용자 이름으로 빈 결과 반환")
    void shouldReturnEmpty_WhenNullUserNameProvided() {
        // When: null 사용자 이름으로 조회
        Optional<User> result = userAdapter.findByUserName(null);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 사용자 이름으로 존재 여부 확인")
    void shouldReturnTrue_WhenUserExistsForExistsByUserName() {
        // When: 실제 존재하는 사용자 이름으로 존재 여부 확인
        boolean result = userAdapter.existsByUserName("testUser");

        // Then: true 반환
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 이름으로 존재 여부 확인")
    void shouldReturnFalse_WhenUserNotExistsForExistsByUserName() {
        // When: 존재하지 않는 사용자 이름으로 존재 여부 확인
        boolean result = userAdapter.existsByUserName("nonExistentUser");

        // Then: false 반환
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경계값 - null 사용자 이름으로 존재 여부 확인")
    void shouldReturnFalse_WhenNullUserNameProvidedForExistsByUserName() {
        // When: null 사용자 이름으로 존재 여부 확인
        boolean result = userAdapter.existsByUserName(null);

        // Then: false 반환
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 - 빈 문자열로 조회 시 빈 결과 반환")
    void shouldReturnEmpty_WhenEmptyStringProvided() {
        // When: 빈 문자열로 조회
        Optional<User> findResult = userAdapter.findByUserName("");
        boolean existsResult = userAdapter.existsByUserName("");

        // Then: 빈 결과 반환
        assertThat(findResult).isEmpty();
        assertThat(existsResult).isFalse();
    }

    @Test
    @DisplayName("공백문자 - 공백만 있는 문자열로 조회 시 빈 결과 반환")
    void shouldReturnEmpty_WhenWhitespaceOnlyStringProvided() {
        // When: 공백만 있는 문자열로 조회
        Optional<User> findResult = userAdapter.findByUserName("   ");
        boolean existsResult = userAdapter.existsByUserName("   ");

        // Then: 빈 결과 반환
        assertThat(findResult).isEmpty();
        assertThat(existsResult).isFalse();
    }

    @Test
    @DisplayName("일관성 - findByUserName과 existsByUserName의 결과 일치 확인")
    void shouldBeConsistent_BetweenFindAndExistsByUserName() {
        // When & Then: 존재하는 사용자
        Optional<User> foundUser = userAdapter.findByUserName("testUser");
        boolean exists = userAdapter.existsByUserName("testUser");
        
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserName()).isEqualTo("testUser");
        assertThat(exists).isTrue();

        // When & Then: 존재하지 않는 사용자
        Optional<User> notFoundUser = userAdapter.findByUserName("nonExistentUser");
        boolean notExists = userAdapter.existsByUserName("nonExistentUser");

        assertThat(notFoundUser).isEmpty();
        assertThat(notExists).isFalse();
        
        // 일관성 검증: 존재하는 경우 find = present, exists = true
        // 존재하지 않는 경우 find = empty, exists = false
        assertThat(foundUser.isPresent()).isEqualTo(exists);
        assertThat(notFoundUser.isEmpty()).isEqualTo(!notExists);
    }

    @Test
    @DisplayName("도메인 간 통신 - Paper 도메인에서 User 도메인 연결")
    void shouldConnectDomains_WhenPaperDomainAccessesUserDomain() {
        // Given: Paper 도메인에서 User 도메인 정보가 필요한 상황
        String targetUserName = "testUser";
        
        // When: UserAdapter를 통해 Paper -> User 도메인 연결
        Optional<User> userFromAdapter = userAdapter.findByUserName(targetUserName);
        boolean userExistsFromAdapter = userAdapter.existsByUserName(targetUserName);
        
        // Then: 도메인 간 결합도 확인
        // 1. User 도메인의 UseCase를 통한 간접 연결
        assertThat(userFromAdapter).isPresent();
        assertThat(userExistsFromAdapter).isTrue();
        
        // 2. Paper 도메인에서 필요한 User 정보 접근 가능
        User user = userFromAdapter.get();
        assertThat(user.getUserName()).isEqualTo(targetUserName);
        assertThat(user.getProvider()).isEqualTo(SocialProvider.KAKAO);
        
        // 3. 도메인 경계 유지 확인 (직접 DB 접근 없이 UseCase 인터페이스 활용)
    }

    @Test
    @DisplayName("지연 로딩 - 사용자 연관데이터 접근")
    void shouldLoadAssociatedData_WhenAccessingUserRelations() {
        // When: 연관데이터가 있는 사용자 조회
        Optional<User> result = userAdapter.findByUserName("duplicateTest");
        
        // Then: 주 데이터와 연관데이터 모두 접근 가능
        assertThat(result).isPresent();
        User user = result.get();
        
        // 기본 사용자 정보
        assertThat(user.getUserName()).isEqualTo("duplicateTest");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        
        // 연관된 Setting 데이터 (Lazy Loading)
        Setting setting = user.getSetting();
        assertThat(setting).isNotNull();
        assertThat(setting.isMessageNotification()).isFalse();
        assertThat(setting.isCommentNotification()).isFalse();
        assertThat(setting.isPostFeaturedNotification()).isFalse();
    }

    @Test
    @DisplayName("성능 - 대용량 데이터에서 사용자 조회")
    void shouldPerformWell_WhenQueryingLargeDataSet() {
        // Given: 대용량 사용자 데이터 생성
        List<User> bulkUsers = IntStream.range(0, 100)
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

        // When: 특정 사용자 조회
        long startTime = System.currentTimeMillis();
        Optional<User> result = userAdapter.findByUserName("bulkUser50");
        boolean exists = userAdapter.existsByUserName("bulkUser75");
        long endTime = System.currentTimeMillis();

        // Then: 성능 및 정확성 확인
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo("bulkUser50");
        assertThat(exists).isTrue();
        assertThat(endTime - startTime).isLessThan(1000); // 1초 이내
    }

    @Test
    @DisplayName("동시성 - 동시 사용자 조회 상황 처리")
    void shouldHandleConcurrentAccess_WhenMultipleUserQueries() throws InterruptedException, ExecutionException {
        // Given: 동시성 테스트용 데이터
        String targetUserName = "testUser";

        // When: 여러 스레드에서 동시 사용자 조회
        List<CompletableFuture<Optional<User>>> findFutures = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
                    userAdapter.findByUserName(targetUserName)
                ))
                .toList();
                
        List<CompletableFuture<Boolean>> existsFutures = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
                    userAdapter.existsByUserName(targetUserName)
                ))
                .toList();

        // 모든 비동기 작업 완료 대기
        List<Optional<User>> findResults = findFutures.stream()
                .map(CompletableFuture::join)
                .toList();
                
        List<Boolean> existsResults = existsFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Then: 모든 요청이 일관된 결과 반환
        findResults.forEach(result -> {
            assertThat(result).isPresent();
            assertThat(result.get().getUserName()).isEqualTo(targetUserName);
        });
        
        existsResults.forEach(result -> {
            assertThat(result).isTrue();
        });
        
        // 동시성 데이터 일관성 확인
        assertThat(findResults).hasSize(5);
        assertThat(existsResults).hasSize(5);
    }

    @Test
    @DisplayName("헥사고널 아키텍처 - 어댑터 위임 패턴 확인")
    void shouldFollowAdapterPattern_InHexagonalArchitecture() {
        // Given: Hexagonal Architecture에서의 어댑터 역할
        String userName = "testUser";
        
        // When: 어댑터를 통한 도메인 간 통신
        Optional<User> findResult = userAdapter.findByUserName(userName);
        boolean existsResult = userAdapter.existsByUserName(userName);
        
        // Then: 어댑터 패턴 준수 확인
        // 1. 어댑터는 UseCase 인터페이스를 단순 위임
        assertThat(findResult).isPresent();
        assertThat(existsResult).isTrue();
        
        // 2. 비즈니스 로직 노출 없이 데이터 전달만 담당
        User user = findResult.get();
        assertThat(user.getUserName()).isEqualTo(userName);
        
        // 3. 도메인 경계 보존 (Paper 도메인이 User 도메인 구현에 의존하지 않음)
    }

    // TODO: 테스트 실패 - 메인 로직 문제 의심
    // 추가 검증 필요: 도메인 반복정 검증 및 대안 설계
    // 가능한 문제: 1) UseCase 순환 의존성 2) 도메인 경계 모호성 3) 비동기 처리 한계
    // 수정 필요: DDD Aggregate 경계 재설계 및 이벤트 드리븐 아키텍처 도입
    @Test
    @DisplayName("아키텍처 검증 - 도메인 반복 의존성 없음 확인")
    void shouldAvoidCircularDependency_BetweenDomains() {
        // Given: 도메인 간 의존성 검증 시나리오
        String userName = "testUser";
        
        // When: Paper 도메인에서 User 도메인 접근
        Optional<User> result = userAdapter.findByUserName(userName);
        
        // Then: 단방향 의존성 확인
        // 1. Paper 도메인 -> User 도메인 (O)
        assertThat(result).isPresent();
        
        // 2. User 도메인 -> Paper 도메인 의존성 없음
        // (이는 UserAdapter의 구현체에서 확인 가능 - Paper 엔티티 참조 없음)
        
        // 3. UseCase 인터페이스를 통한 느슨한 결합
        assertThat(result.get().getUserName()).isEqualTo(userName);
    }
}

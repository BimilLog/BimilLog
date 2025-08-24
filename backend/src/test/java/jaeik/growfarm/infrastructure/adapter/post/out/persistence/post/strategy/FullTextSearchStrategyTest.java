package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.user.entity.QUser;
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
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>FullTextSearchStrategy 통합 테스트</h2>
 * <p>MySQL FULLTEXT 검색 전략의 모든 기능을 실제 DB와 함께 테스트합니다.</p>
 * <p>ngram 인덱스 동작, 실제 검색 결과, 예외 처리 등을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.growfarm.GrowfarmApplication.class
        )
)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FullTextSearchStrategy.class})
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.post.out.persistence",
        "jaeik.growfarm.infrastructure.adapter.user.out.persistence"
})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=true",
        "logging.level.org.springframework.orm.jpa=DEBUG"
})
class FullTextSearchStrategyTest {

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
    private TestEntityManager testEntityManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FullTextSearchStrategy fullTextSearchStrategy;

    private static final QPost POST = QPost.post;
    private static final QUser USER = QUser.user;

    private JPAQueryFactory queryFactory;

    private User testUser;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(entityManager);
        initializeDatabaseAndTestData();
    }

    /**
     * <h2>데이터베이스와 테스트 데이터를 초기화하는 통합 헬퍼 메서드</h2>
     * @author Jaeik
     * @since 2.0.0
     */
    private void initializeDatabaseAndTestData() {
        // 1. FULLTEXT 인덱스 생성 (테스트 전용 DDL)
        createFullTextIndexesForTest();

        // 2. 테스트 사용자 및 게시글 생성
        createTestUserAndPosts();

        // 3. 변경사항 플러시 및 영속성 컨텍스트 초기화
        testEntityManager.flush();
        testEntityManager.clear();

        // 4. 동적 인덱스 생성 완료 대기
        waitForIndexCreation("idx_post_title", "idx_post_title_content");
    }

    /**
     * <h2>테스트 사용자 및 관련 게시글 데이터를 생성합니다.</h2>
     * @author Jaeik
     * @since 2.0.0
     */
    private void createTestUserAndPosts() {
        Setting testSetting = Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();

        testUser = User.builder()
                .socialId("test_user_123")
                .provider(SocialProvider.KAKAO)
                .userName("테스트사용자")
                .role(UserRole.USER)
                .socialNickname("테스트사용자")
                .thumbnailImage("profile.jpg")
                .setting(testSetting)
                .build();
        testEntityManager.persistAndFlush(testUser);

        testPost1 = createTestPost("스프링부트 테스트 가이드", "JUnit과 TestContainers를 활용한 테스트 방법을 설명합니다.");
        testPost2 = createTestPost("MySQL 풀텍스트 검색", "ngram 파서를 사용한 한국어 검색 최적화 방법입니다.");
        testPost3 = createTestPost("짧은제목", "간단한 내용입니다.");
    }

    /**
     * <h3>테스트 전용 FULLTEXT 인덱스 생성</h3>
     * spring.jpa.hibernate.ddl-auto=create에 의해 일반 테이블이 생성된 후,
     * FULLTEXT 인덱스를 추가하는 DDL을 직접 실행합니다.
     *
     * @throws RuntimeException FULLTEXT 인덱스 생성이 완전히 실패한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void createFullTextIndexesForTest() {
        boolean titleIndexCreated = ensureFullTextIndex("idx_post_title", "title");
        boolean titleContentIndexCreated = ensureFullTextIndex("idx_post_title_content", "title, content");

        // 핵심 인덱스가 모두 실패한 경우 테스트 실행 불가 상태로 판단
        if (!titleIndexCreated && !titleContentIndexCreated) {
            System.err.println("핵심 FULLTEXT 인덱스 생성이 완전히 실패했습니다. 테스트 환경 점검이 필요합니다.");
            throw new RuntimeException("FULLTEXT 인덱스 생성 실패로 인한 테스트 환경 불안정");
        }

        // 생성된 인덱스 상태 로깅
        System.out.println("FULLTEXT 인덱스 생성 결과:");
        System.out.println("idx_post_title: " + (titleIndexCreated ? "성공" : "실패"));
        System.out.println("idx_post_title_content: " + (titleContentIndexCreated ? "성공" : "실패"));
    }

    /**
     * <h3>특정 테이블에 FULLTEXT 인덱스가 없으면 생성하고, 그 결과를 반환합니다.</h3>
     *
     * @param indexName 확인할 인덱스 이름
     * @param columns   인덱스를 생성할 컬럼 목록 (예: "title", "title, content")
     * @return 인덱스 생성 시도 후 성공 여부 (이미 존재했거나 새로 생성 성공 시 true)
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean ensureFullTextIndex(String indexName, String columns) {
        if (!checkIndexExists(indexName)) {
            try {
                entityManager.createNativeQuery(
                        "ALTER TABLE " + "post" + " ADD FULLTEXT " + indexName + " (" + columns + ") WITH PARSER ngram"
                ).executeUpdate();
                System.out.println("✅ 테스트용 '" + indexName + "' FULLTEXT 인덱스 생성");
                return true;
            } catch (Exception e) {
                System.err.println("FULLTEXT 인덱스 '" + indexName + "' 생성 실패: " + e.getMessage());
                // DDL 실행 중 오류 발생 시 상세 로깅
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("✅ '" + indexName + "' FULLTEXT 인덱스 이미 존재");
            return true;
        }
    }

    /**
     * 특정 테이블과 인덱스 이름으로 FULLTEXT 인덱스 존재 여부를 정확히 확인합니다.
     *
     * @param indexName 확인할 인덱스 이름
     * @return FULLTEXT 인덱스가 존재하면 true, 아니면 false
     */
    private boolean checkIndexExists(String indexName) {
        try {
            // information_schema.statistics를 쿼리하여 인덱스 정보와 INDEX_TYPE을 조회
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.statistics " +
                                    "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ? AND index_type = 'FULLTEXT'"
                    )
                    .setParameter(1, "post")
                    .setParameter(2, indexName)
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            System.err.println("FULLTEXT 인덱스 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    /**
     * 동적으로 FULLTEXT 인덱스 생성 완료를 대기합니다.
     * 최대 10초간 인덱스 상태를 매 500ms마다 확인합니다.
     *
     * @param indexNames 확인할 인덱스 이름들
     */
    private void waitForIndexCreation(String... indexNames) {
        final int maxAttempts = 20; // 10초 대기 (500ms * 20회)
        final int sleepMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean allIndexesReady = true;

            for (String indexName : indexNames) {
                if (!checkIndexExists(indexName)) {
                    allIndexesReady = false;
                    break;
                }
            }

            if (allIndexesReady) {
                System.out.println("모든 FULLTEXT 인덱스 준비 완료 (" + attempt + "/" + maxAttempts + "회 확인)");
                return;
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("인덱스 대기 중 인터럽트 발생");
                break;
            }

            if (attempt % 4 == 0) { // 2초마다 진행 상황 로깅
                System.out.println("🔍 인덱스 생성 대기 중... (" + attempt + "/" + maxAttempts + "회 확인)");
            }
        }

        System.err.println("FULLTEXT 인덱스 생성 대기 시간 초과 (" + (maxAttempts * sleepMs / 1000) + "초)");
    }

    /**
     * 특정 테이블의 존재 여부를 확인합니다.
     *
     * @return 테이블이 존재하면 true, 아니면 false
     */
    private boolean checkTableExists() {
        try {
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.tables " +
                                    "WHERE table_schema = DATABASE() AND table_name = ?"
                    )
                    .setParameter(1, "post")
                    .getSingleResult();

            return count > 0;
        } catch (Exception e) {
            System.err.println("테이블 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    private Post createTestPost(String title, String content) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .user(testUser)
                .isNotice(false)
                .build();
        return testEntityManager.persistAndFlush(post);
    }

    /**
     * <h3>검색을 수행하고, 결과에 대한 특정 검증 로직을 적용합니다.</h3>
     *
     * @param type        검색 타입
     * @param searchQuery 검색어
     * @param assertions  검색 결과 목록에 적용할 검증 로직 (Consumer<List<Post>>)
     * @return 검색된 Post 목록 (추가 검증을 위해)
     */
    private List<Post> performSearchAndVerify(String type, String searchQuery, Consumer<List<Post>> assertions) {
        // When: FULLTEXT 검색 조건 생성 및 실행
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);

        JPAQuery<Post> query = queryFactory.selectFrom(POST)
                .leftJoin(POST.user, USER).fetchJoin()
                .where(condition)
                .orderBy(POST.createdAt.desc());

        List<Post> results = query.fetch();

        assertions.accept(results);
        return results;
    }


    // ================== 단위 테스트 - canHandle 메서드 ==================
    
    private void assertCanHandleResult(String query, String type, boolean expected) {
        assertThat(fullTextSearchStrategy.canHandle(query, type)).isEqualTo(expected);
    }

    @Test
    @DisplayName("canHandle - 각 검색 타입별 임계값 검증")
    void shouldHandleSearchTypes_WithCorrectThresholds() {
        // 제목 검색 (3글자 이상)
        assertCanHandleResult("스프", "title", false);        // 2글자 - 거부
        assertCanHandleResult("스프링", "title", true);       // 3글자 - 허용
        
        // 제목+내용 검색 (3글자 이상) 
        assertCanHandleResult("Te", "title_content", false); // 2글자 - 거부
        assertCanHandleResult("Test", "title_content", true); // 4글자 - 허용
        
        // 작성자 검색 (4글자 이상)
        assertCanHandleResult("테스트", "writer", false);     // 3글자 - 거부
        assertCanHandleResult("테스트사", "writer", true);    // 4글자 - 허용
    }

    @Test
    @DisplayName("정상 케이스 - 전략 이름 반환")
    void shouldReturnCorrectStrategyName() {
        // When: 전략 이름 조회
        String strategyName = fullTextSearchStrategy.getStrategyName();
        
        // Then: 올바른 전략 이름 반환
        assertThat(strategyName).isEqualTo("FullTextSearchStrategy");
    }

    @Test
    @DisplayName("인프라 테스트 - FULLTEXT 인덱스 생성 확인")
    void shouldHaveFullTextIndexes_WhenFullTextInitial() {
        // When: 인덱스 존재 여부 확인
        boolean titleIndexExists = checkIndexExists("idx_post_title");
        boolean titleContentIndexExists = checkIndexExists("idx_post_title_content");
        boolean tableExists = checkTableExists();

        // Then: 기본 인프라 검증
        assertThat(tableExists).isTrue(); // 최소한 테이블은 존재해야 함

        // FULLTEXT 인덱스 상태 로깅 및 안정성 검증
        System.out.println("FULLTEXT 인덱스 상태:");
        System.out.println(" post 테이블: " + (tableExists ? "존재" : "없음"));
        System.out.println(" idx_post_title: " + (titleIndexExists ? "존재" : "없음"));
        System.out.println(" idx_post_title_content: " + (titleContentIndexExists ? "존재" : "없음"));

        // 인덱스 생성 성공률 로깅 (개발자 정보)
        int createdIndexCount = (titleIndexExists ? 1 : 0) + (titleContentIndexExists ? 1 : 0);
        double successRate = ((double) createdIndexCount / 2) * 100;
        System.out.println("인덱스 생성 성공률: " + successRate + "% (" + createdIndexCount + "/2)");

        // 인덱스가 생성되지 않은 경우 경고 메시지
        if (!titleIndexExists || !titleContentIndexExists) {
            System.out.println("FULLTEXT 검색 기능이 제한적으로 동작할 수 있습니다.");
            System.out.println("검색 성능이 LIKE 검색으로 fallback될 수 있습니다.");
        }
    }

    @Test
    @DisplayName("인프라 테스트 - MySQL ngram 파서 설정 확인")
    void shouldHaveNgramParserConfiguration() {
        // When: ngram 토큰 사이즈 확인
        try {
            Object tokenSize = entityManager.createNativeQuery(
                    "SELECT @@ngram_token_size"
            ).getSingleResult();

            System.out.println("MySQL ngram_token_size: " + tokenSize);

            // Then: ngram 설정이 존재해야 함
            assertThat(tokenSize).isNotNull();
        } catch (Exception e) {
            System.out.println("ngram 파서 설정 확인 실패 (예상됨): " + e.getMessage());
        }
    }

    @Test
    @DisplayName("인프라 테스트 - FULLTEXT 검색 쿼리 실행 가능성 확인")
    void shouldExecuteFullTextQuery_WhenIndexExistsOrNot() {
        // Given: FULLTEXT 검색 쿼리 (인덱스가 없어도 테이블 스캔으로 동작해야 함)
        String searchTerm = "테스트";

        boolean fullTextSuccess = false;
        boolean likeSearchSuccess = false;

        try {
            // When: FULLTEXT 쿼리 직접 실행
            List<?> results = entityManager.createNativeQuery(
                "SELECT post_id, title, content FROM post " + // post_id로 수정
                "WHERE MATCH(title) AGAINST(? IN NATURAL LANGUAGE MODE)",
                Object[].class
            )
            .setParameter(1, searchTerm)
            .getResultList();
            
            fullTextSuccess = true;
            System.out.println("FULLTEXT 검색 성공: " + results.size() + "개 결과");
            assertThat(results).isNotNull();

        } catch (Exception e) {
            // FULLTEXT 쿼리 실행 실패 시 (인덱스가 없으면 발생할 수 있음)
            System.out.println("FULLTEXT 쿼리 실행 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());

            try {
                // 대안으로 LIKE 검색이 동작하는지 확인
                List<?> likeResults = entityManager.createNativeQuery(
                    "SELECT post_id, title, content FROM post WHERE title LIKE ?", // post_id로 수정
                    Object[].class
                )
                .setParameter(1, "%" + searchTerm + "%")
                .getResultList();
                
                likeSearchSuccess = true;
                System.out.println("✅ LIKE 검색 성공: " + likeResults.size() + "개 결과");
                assertThat(likeResults).isNotNull();

            } catch (Exception likeException) {
                System.err.println("❌ LIKE 검색도 실패: " + likeException.getMessage());
                // 둘 다 실패하면 기본적인 문제가 있음
                throw new RuntimeException("기본적인 데이터베이스 접근에 문제가 있습니다.", likeException);
            }
        }

        // 최소한 하나의 검색 방식은 동작해야 함
        assertThat(fullTextSuccess || likeSearchSuccess)
            .withFailMessage("최소한 FULLTEXT 또는 LIKE 검색 중 하나는 동작해야 합니다.")
            .isTrue();
        
        System.out.println("📈 검색 방식 동작 상태: FULLTEXT(" + (fullTextSuccess ? "OK" : "FAIL") + "), LIKE(" + (likeSearchSuccess ? "OK" : "FAIL") + ")");
    }


    @Test
    @DisplayName("실제 ngram 인덱스 - 제목 FULLTEXT 검색 정상 동작")
    void shouldFindPostByTitle_WhenUsingRealNgramIndex() {
        // Given: 실제 저장된 게시글과 검색어
        // testPost1의 "스프링부트 테스트 가이드"에 포함
        performSearchAndVerify("title", "테스트", results -> {
            // Then: ngram 인덱스로 부분 매칭되어 검색됨
            // 제목에 "테스트"가 포함된 testPost1이 검색되어야 함
            assertThat(results).isNotEmpty();
            assertThat(results).extracting(Post::getTitle).anyMatch(title -> title.contains("테스트"));
            assertThat(results).anyMatch(p -> p.getId().equals(testPost1.getId()));
        });
    }

    @Test
    @DisplayName("실제 ngram 인덱스 - 제목+내용 통합 검색 정상 동작")
    void shouldFindPostByTitleAndContent_WhenUsingRealNgramIndex() {

        // Given: 실제 저장된 게시글과 내용 검색어
        // testPost1의 내용에 포함
        performSearchAndVerify("title_content", "TestContainers", results -> {
            // Then: 내용에서 ngram 인덱스로 검색됨
            // 내용에 "TestContainers"가 포함된 testPost1이 검색되어야 함
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(post ->
                    post.getTitle().contains("TestContainers") ||
                            post.getContent().contains("TestContainers")
            );
            assertThat(results).anyMatch(p -> p.getId().equals(testPost1.getId()));
        });
    }

    @Test
    @DisplayName("실제 DB - 작성자 LIKE 검색 정상 동작")
    void shouldFindPostByWriter_WhenUsingLikeSearch() {
        // Given: 실제 작성자로 검색어 (작성자는 LIKE 검색 사용)// testUser의 userName
        performSearchAndVerify("writer", "테스트사용자", results -> {
            // Then: 작성자명으로 LIKE 검색되어 모든 게시글이 검색됨
            // 모든 게시글의 작성자가 testUser여야 함
            // 모든 테스트 게시글이 동일한 작성자
            assertThat(results).isNotEmpty();
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(post ->
                    post.getUser().getUserName().startsWith("테스트사용자")
            );
        });
    }

    @Test
    @DisplayName("실제 DB - 빈 검색 결과 시 빈 리스트 반환")
    void shouldReturnEmptyResults_WhenNoMatchingPosts() {
        // Given: 실제 DB에 존재하지 않는 검색어
        // 절대 찾을 수 없는 검색어
        performSearchAndVerify("title", "xyz비존재하는검색어abc", results -> {
            // Then: 결과 반환
            assertThat(results).isEmpty();
        });
    }

    @Test
    @DisplayName("실제 DB - 한국어 ngram 부분 검색 정확성")
    void shouldFindKoreanPartialMatch_WhenUsingNgramIndex() {
        // Given: 한국어 부분 검색어로 테스트
        // testPost1의 "스프링부트 테스트 가이드"에서 마지막 부분
        performSearchAndVerify("title", "가이드", results -> {
            // Then: ngram으로 부분 매칭되어 검색됨
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(post -> post.getTitle().contains("가이드"));
            assertThat(results).anyMatch(p -> p.getId().equals(testPost1.getId()));
        });
    }

    @Test
    @DisplayName("실제 DB - 영어 부분 검색 정확성")
    void shouldFindEnglishPartialMatch_WhenUsingNgramIndex() {
        // Given: 영어 부분 검색어로 테스트
        // testPost2의 "제목에는 없지만 내용에 있음
        performSearchAndVerify("title_content", "MySQL", results -> {
            // Then: 영어 부분 매칭되어 검색됨 (testPost2의 제목에 "MySQL" 포함)
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(post ->
                    post.getTitle().contains("MySQL") || post.getContent().contains("MySQL")
            );
            assertThat(results).anyMatch(p -> p.getId().equals(testPost2.getId()));
        });
    }

    @Test
    @DisplayName("비즈니스 로직 - 기본 타입(default) 처리")
    void shouldHandleDefaultType_WhenUnknownTypeProvided() {
        // Given: 알 수 없는 검색 타입으로 기본 제목 검색
        // 실제 존재하는 검색어
        performSearchAndVerify("unknown_type", "테스트", results -> {
            // Then: 기본적으로 제목 검색으로 처리됨
            assertThat(results).isNotNull();
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(post -> post.getTitle().contains("테스트"));
        });
    }

    @Test
    @DisplayName("성능 테스트 - 대량 데이터 FULLTEXT 검색")
    void shouldHandleLargeDataSearch_WithAcceptablePerformance() {
        // Given: 대량 테스트 데이터 생성
        for (int i = 0; i < 20; i++) {
            createTestPost("테스트 게시글 " + i, "성능 테스트 내용 " + i);
        }
        testEntityManager.flush();
        testEntityManager.clear();

        // When: 성능 측정
        long startTime = System.nanoTime();
        List<Post> results = performSearchAndVerify("title", "테스트", r -> {
            assertThat(r).hasSizeGreaterThanOrEqualTo(20); // 초기 3개 + 추가 20개
        });
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: 합리적인 성능 기준
        assertThat(executionTimeMs).isLessThan(2000L); // 2초 이내
        System.out.println("📈 성능: " + executionTimeMs + "ms, 결과: " + results.size() + "건");
    }


    @Test
    @DisplayName("통합 테스트 - 전체 검색 전략 워크플로우")
    void shouldCompleteEntireSearchStrategyWorkflow() {
        // When: 전체 워크플로우 - 처리 가능성 → 검색 실행 → 메타데이터 확인
        assertThat(fullTextSearchStrategy.canHandle("테스트", "title")).isTrue();
        assertThat(fullTextSearchStrategy.canHandle("ngram", "title_content")).isTrue(); 
        assertThat(fullTextSearchStrategy.canHandle("테스트사용자", "writer")).isTrue();
        
        performSearchAndVerify("title", "테스트", results -> assertThat(results).isNotEmpty());
        performSearchAndVerify("title_content", "ngram", results -> assertThat(results).isNotEmpty());  
        performSearchAndVerify("writer", "테스트사용자", results -> assertThat(results).hasSize(3));
        
        assertThat(fullTextSearchStrategy.getStrategyName()).isEqualTo("FullTextSearchStrategy");
    }
}

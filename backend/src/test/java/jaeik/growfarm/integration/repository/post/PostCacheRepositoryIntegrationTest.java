package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.UserRole;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.post.cache.PostCacheRepository;
import jaeik.growfarm.repository.post.cache.PostCacheRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


import static org.assertj.core.api.Assertions.*;

/**
 * <h2>PostPopularRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 인기글 관리 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.2.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostCacheRepositoryImpl.class,
    jaeik.growfarm.global.config.QueryDSLConfig.class,
    jaeik.growfarm.global.security.EncryptionUtil.class
})
@TestPropertySource(properties = {
    "message.secret=testkey1234567890testkey1234567890"
})
@DisplayName("PostPopularRepository 통합 테스트")
class PostCacheRepositoryIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.use_sql_comments", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostCacheRepository postCacheRepository;

    private Users testUser1;
    private Users testUser2;
    private Users testUser3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        Setting setting1 = Setting.createSetting();
        entityManager.persistAndFlush(setting1);
        
        testUser1 = Users.builder()
                .userName("인기글작성자1")
                .kakaoId(12345L)
                .role(UserRole.USER)
                .setting(setting1)
                .build();
        entityManager.persistAndFlush(testUser1);

        Setting setting2 = Setting.createSetting();
        entityManager.persistAndFlush(setting2);
        
        testUser2 = Users.builder()
                .userName("인기글작성자2")
                .kakaoId(67890L)
                .role(UserRole.USER)
                .setting(setting2)
                .build();
        entityManager.persistAndFlush(testUser2);

        Setting setting3 = Setting.createSetting();
        entityManager.persistAndFlush(setting3);
        
        testUser3 = Users.builder()
                .userName("좋아요사용자")
                .kakaoId(11111L)
                .role(UserRole.USER)
                .setting(setting3)
                .build();
        entityManager.persistAndFlush(testUser3);

        entityManager.clear();
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 성공")
    void updateRealtimePopularPosts_Success() {
        // Given - 1일 이내 게시글들 생성
        Instant now = Instant.now();
        
        // 오늘 작성된 게시글들
        createPostWithLikes("오늘의 인기글 1", now.minus(2, ChronoUnit.HOURS), testUser1, 15);
        createPostWithLikes("오늘의 인기글 2", now.minus(4, ChronoUnit.HOURS), testUser2, 25);
        createPostWithLikes("오늘의 인기글 3", now.minus(6, ChronoUnit.HOURS), testUser1, 5);
        
        // 어제 작성된 게시글 (1일 이내)
        createPostWithLikes("어제의 인기글", now.minus(20, ChronoUnit.HOURS), testUser2, 30);
        
        // 2일 전 게시글 (1일 초과, 포함되지 않아야 함)
        createPostWithLikes("오래된 글", now.minus(2, ChronoUnit.DAYS), testUser1, 50);

        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSizeLessThanOrEqualTo(5); // 최대 5개
        
        // 좋아요 수가 많은 순서로 정렬되어 있는지 확인
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).getLikes()).isGreaterThanOrEqualTo(result.get(i + 1).getLikes());
            }
        }

        // 실제 인기글 플래그가 설정되었는지 DB에서 확인
        List<Post> realtimePopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).hasSameSizeAs(result);
    }

    @Test
    @DisplayName("주간 인기글 선정 - 성공")
    void updateWeeklyPopularPosts_Success() {
        // Given - 7일 이내 게시글들 생성
        Instant now = Instant.now();
        
        createPostWithLikes("이번주 인기글 1", now.minus(2, ChronoUnit.DAYS), testUser1, 40);
        createPostWithLikes("이번주 인기글 2", now.minus(5, ChronoUnit.DAYS), testUser2, 35);
        createPostWithLikes("이번주 인기글 3", now.minus(7, ChronoUnit.DAYS), testUser1, 45);
        
        // 8일 전 게시글 (7일 초과, 포함되지 않아야 함)
        createPostWithLikes("저번주 글", now.minus(8, ChronoUnit.DAYS), testUser2, 60);

        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateWeeklyPopularPosts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSizeLessThanOrEqualTo(5); // 최대 5개
        
        // 주간 인기글 플래그 확인
        List<Post> weeklyPopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.WEEKLY)
                .getResultList();
        
        assertThat(weeklyPopularPosts).hasSameSizeAs(result);
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 성공")
    void updateLegendPosts_Success() {
        // Given - 20개 이상 좋아요를 받은 게시글들 생성
        createPostWithLikes("레전드 게시글 1", Instant.now().minus(10, ChronoUnit.DAYS), testUser1, 25);
        createPostWithLikes("레전드 게시글 2", Instant.now().minus(5, ChronoUnit.DAYS), testUser2, 30);
        createPostWithLikes("레전드 게시글 3", Instant.now().minus(1, ChronoUnit.DAYS), testUser1, 22);
        
        // 20개 미만 좋아요 (레전드가 되지 못함)
        createPostWithLikes("일반 게시글", Instant.now().minus(3, ChronoUnit.DAYS), testUser2, 15);

        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateLegendPosts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 20개 이상 좋아요받은 게시글 3개
        
        // 모든 결과가 20개 이상 좋아요인지 확인
        result.forEach(post -> 
            assertThat(post.getLikes()).isGreaterThanOrEqualTo(20)
        );
        
        // 레전드 플래그 확인
        List<Post> legendPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.LEGEND)
                .getResultList();
        
        assertThat(legendPosts).hasSize(3);
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 결과 없음")
    void updateRealtimePopularPosts_NoResults() {
        // Given - 2일 전 게시글만 있음 (1일 초과)
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        createPostWithLikes("오래된 글", twoDaysAgo, testUser1, 10);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isEmpty();
        
        // 실시간 인기글 플래그가 설정된 게시글이 없는지 확인
        List<Post> realtimePopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).isEmpty();
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 조건에 맞는 게시글 없음")
    void updateLegendPosts_NoQualifiedPosts() {
        // Given - 20개 미만 좋아요만 있는 게시글들
        createPostWithLikes("일반글 1", Instant.now(), testUser1, 10);
        createPostWithLikes("일반글 2", Instant.now(), testUser2, 15);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateLegendPosts();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("인기글 플래그 초기화 및 재설정 확인")
    void popularFlagReset_And_Reapply() {
        // Given - 기존에 REALTIME 플래그가 있는 게시글 생성
        Post existingPopularPost = Post.builder()
                .title("기존 인기글")
                .content("기존 인기글 내용")
                .views(100)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(existingPopularPost);

        // 새로운 인기글 후보 생성
        Post newPopularPost = createPostWithLikes("새 인기글", Instant.now(), testUser2, 50);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isNotNull();
        
        // 기존 인기글의 플래그가 제거되었는지 확인
        Post updatedExistingPost = entityManager.find(Post.class, existingPopularPost.getId());
        assertThat(updatedExistingPost.getPostCacheFlag()).isNull();
        
        // 새로운 인기글의 플래그가 설정되었는지 확인
        Post updatedNewPost = entityManager.find(Post.class, newPopularPost.getId());
        assertThat(updatedNewPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 정확히 5개 제한 확인")
    void updateRealtimePopularPosts_ExactlyFiveLimit() {
        // Given - 10개의 게시글 생성 (모두 1일 이내)
        Instant now = Instant.now();
        
        for (int i = 0; i < 10; i++) {
            createPostWithLikes("인기글 " + i, now.minus(i, ChronoUnit.HOURS), 
                               i % 2 == 0 ? testUser1 : testUser2, 10 + i);
        }
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).hasSize(5); // 정확히 5개만 선정
        
        // 좋아요 수 내림차순 확인
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getLikes()).isGreaterThanOrEqualTo(result.get(i + 1).getLikes());
        }

        // DB에서 REALTIME 플래그 확인
        List<Post> realtimePopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).hasSize(5);
    }

    @Test
    @DisplayName("주간 인기글 선정 - 정확한 날짜 경계 확인")
    void updateWeeklyPopularPosts_ExactDateBoundary() {
        // Given
        Instant now = Instant.now();
        Instant exactlySevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant sevenDaysAndOneSecondAgo = exactlySevenDaysAgo.minus(1, ChronoUnit.SECONDS);
        
        // 정확히 7일 전 (포함되어야 함)
        createPostWithLikes("정확히 7일 전", exactlySevenDaysAgo, testUser1, 30);
        
        // 7일 1초 전 (포함되지 않아야 함)
        createPostWithLikes("7일 1초 전", sevenDaysAndOneSecondAgo, testUser2, 35);
        
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateWeeklyPopularPosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("정확히 7일 전");
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 정확히 20개 좋아요 경계값 확인")
    void updateLegendPosts_ExactTwentyLikesBoundary() {
        // Given
        createPostWithLikes("정확히 20개", Instant.now(), testUser1, 20);
        createPostWithLikes("19개", Instant.now(), testUser2, 19);
        createPostWithLikes("21개", Instant.now(), testUser1, 21);
        
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateLegendPosts();

        // Then
        assertThat(result).hasSize(2); // 20개와 21개만 포함
        
        List<String> titles = result.stream()
                .map(SimplePostResDTO::getTitle)
                .toList();
        assertThat(titles).containsExactlyInAnyOrder("정확히 20개", "21개");
    }

    @Test
    @DisplayName("주간 인기글 선정 - 결과 없음")
    void updateWeeklyPopularPosts_NoResults() {
        // Given - 8일 전 게시글만 있음
        Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);
        createPostWithLikes("오래된 글", eightDaysAgo, testUser1, 10);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateWeeklyPopularPosts();

        // Then
        assertThat(result).isEmpty();
        
        // 주간 인기글 플래그가 설정된 게시글이 없는지 확인
        List<Post> weeklyPopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.WEEKLY)
                .getResultList();
        
        assertThat(weeklyPopularPosts).isEmpty();
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 인기글 업데이트")
    void updatePopularPosts_ConcurrentAccess() throws InterruptedException {
        // Given - 테스트 데이터 생성
        Instant now = Instant.now();
        for (int i = 0; i < 10; i++) {
            createPostWithLikes("동시성 테스트 글 " + i, now.minus(i, ChronoUnit.HOURS), 
                               testUser1, 10 + i);
        }
        entityManager.clear();

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - 동시에 인기글 업데이트 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 발생 가능하지만 테스트는 계속 진행
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 최소 하나는 성공해야 함
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 최종 상태 확인
        List<Post> realtimePopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PostCacheFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("대용량 데이터 성능 테스트")
    void updatePopularPosts_PerformanceTest() {
        // Given - 대량의 게시글 생성 (100개)
        Instant now = Instant.now();
        
        for (int i = 0; i < 100; i++) {
            createPostWithLikes("성능테스트 글 " + i, now.minus(i, ChronoUnit.MINUTES), 
                               i % 3 == 0 ? testUser1 : i % 3 == 1 ? testUser2 : testUser3, 
                               i % 50); // 다양한 좋아요 수
        }
        entityManager.clear();

        long startTime = System.currentTimeMillis();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSizeLessThanOrEqualTo(5);
        assertThat(executionTime).isLessThan(5000); // 5초 이내 완료
        
        System.out.println("실행 시간: " + executionTime + "ms");
    }

    @Test
    @DisplayName("플래그 혼재 상황 테스트")
    void updatePopularPosts_MixedFlags() {
        // Given - 다양한 플래그를 가진 게시글들 생성
        Instant now = Instant.now();
        
        Post realtimePost = Post.builder()
                .title("기존 실시간 인기글")
                .content("내용")
                .views(10)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(realtimePost);
        
        Post weeklyPost = Post.builder()
                .title("기존 주간 인기글")
                .content("내용")
                .views(10)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(weeklyPost);
        
        Post legendPost = Post.builder()
                .title("기존 레전드 글")
                .content("내용")
                .views(10)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.LEGEND)
                .user(testUser3)
                .build();
        entityManager.persistAndFlush(legendPost);

        // 새로운 인기글 후보
        createPostWithLikes("새로운 인기글", now, testUser1, 50);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isNotNull();
        
        // 기존 REALTIME 플래그만 제거되고, WEEKLY, LEGEND는 유지되어야 함
        Post updatedRealtimePost = entityManager.find(Post.class, realtimePost.getId());
        Post updatedWeeklyPost = entityManager.find(Post.class, weeklyPost.getId());
        Post updatedLegendPost = entityManager.find(Post.class, legendPost.getId());
        
        assertThat(updatedRealtimePost.getPostCacheFlag()).isNull();
        assertThat(updatedWeeklyPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.WEEKLY);
        assertThat(updatedLegendPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.LEGEND);
    }

    @Test
    @DisplayName("공지글 제외 확인")
    void updatePopularPosts_ExcludeNotices() {
        // Given - 공지글과 일반글 생성
        Instant now = Instant.now();
        
        Post notice = Post.builder()
                .title("공지사항")
                .content("공지 내용")
                .views(1000)
                .isNotice(true) // 공지글
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(notice);
        
        // 공지글에 많은 좋아요 추가
        for (int i = 0; i < 100; i++) {
            Setting noticeLikeSetting = Setting.createSetting();
            entityManager.persistAndFlush(noticeLikeSetting);
            
            Users likeUser = Users.builder()
                    .userName("공지좋아요유저" + i)
                    .kakaoId(200000L + i)
                    .role(UserRole.USER)
                    .setting(noticeLikeSetting)
                    .build();
            entityManager.persistAndFlush(likeUser);
            
            PostLike postLike = PostLike.builder()
                    .post(notice)
                    .user(likeUser)
                    .build();
            entityManager.persistAndFlush(postLike);
        }
        
        // 일반글 생성
        createPostWithLikes("일반 글", now, testUser2, 10);
        entityManager.clear();

        // When
        List<SimplePostResDTO> result = postCacheRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("일반 글");
        
        // 공지글은 인기글 플래그가 설정되지 않아야 함
        Post updatedNotice = entityManager.find(Post.class, notice.getId());
        assertThat(updatedNotice.getPostCacheFlag()).isNull();
    }

    /**
     * <h3>좋아요가 있는 게시글을 생성하는 헬퍼 메서드</h3>
     * <p>
     * 테스트용 게시글과 지정된 수의 좋아요를 생성합니다.
     * </p>
     * 
     * @param title 게시글 제목
     * @param createdAt 게시글 생성 시간
     * @param author 게시글 작성자
     * @param likeCount 좋아요 개수
     * @return 생성된 Post 엔티티
     * @author Jaeik
     * @version 1.1.0
     */
    private Post createPostWithLikes(String title, Instant createdAt, Users author, int likeCount) {
        Post post = Post.builder()
                .title(title)
                .content(title + " 내용")
                .views(10)
                .isNotice(false)
                .user(author)
                .build();
        
        // createdAt 설정을 위해 직접 SQL 사용
        entityManager.persistAndFlush(post);
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE post SET created_at = ? WHERE post_id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, post.getId())
                .executeUpdate();

        // 좋아요 생성 - 성능 최적화를 위해 배치 처리
        createLikesForPost(post, likeCount);

        return post;
    }

    /**
     * <h3>게시글에 좋아요를 배치로 생성하는 헬퍼 메서드</h3>
     * <p>
     * 대량의 좋아요 생성 시 성능을 고려한 배치 처리 방식을 사용합니다.
     * </p>
     * 
     * @param post 좋아요를 추가할 게시글
     * @param likeCount 생성할 좋아요 개수
     * @author Jaeik
     * @version 1.1.0
     */
    private void createLikesForPost(Post post, int likeCount) {
        for (int i = 0; i < likeCount; i++) {
            // 임시 설정 생성
            Setting likeSetting = Setting.createSetting();
            entityManager.persistAndFlush(likeSetting);
            
            // 임시 사용자 생성 (좋아요용)
            Users likeUser = Users.builder()
                    .userName("좋아요유저" + post.getId() + "_" + i)
                    .kakaoId(100000L + post.getId() * 1000 + i)
                    .role(UserRole.USER)
                    .setting(likeSetting)
                    .build();
            entityManager.persistAndFlush(likeUser);
            
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(likeUser)
                    .build();
            entityManager.persistAndFlush(postLike);
            
            // 메모리 관리를 위해 주기적으로 flush
            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
                // post 참조 재로드
                post = entityManager.find(Post.class, post.getId());
            }
        }
    }


}
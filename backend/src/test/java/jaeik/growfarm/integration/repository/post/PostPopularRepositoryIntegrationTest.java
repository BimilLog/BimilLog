package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.post.popular.PostPopularRepository;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>PostPopularRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 인기글 관리 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    jaeik.growfarm.repository.post.popular.PostPopularRepositoryImpl.class,
    jaeik.growfarm.repository.comment.CommentRepository.class
})
@DisplayName("PostPopularRepository 통합 테스트")
class PostPopularRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
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
    private PostPopularRepository postPopularRepository;

    private Users testUser1;
    private Users testUser2;
    private Users testUser3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("인기글작성자1")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("인기글작성자2")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        testUser3 = Users.builder()
                .userName("좋아요사용자")
                .kakaoId(11111L)
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
        Post todayPost1 = createPostWithLikes("오늘의 인기글 1", now.minus(2, ChronoUnit.HOURS), testUser1, 15);
        Post todayPost2 = createPostWithLikes("오늘의 인기글 2", now.minus(4, ChronoUnit.HOURS), testUser2, 25);
        Post todayPost3 = createPostWithLikes("오늘의 인기글 3", now.minus(6, ChronoUnit.HOURS), testUser1, 5);
        
        // 어제 작성된 게시글 (1일 이내)
        Post yesterdayPost = createPostWithLikes("어제의 인기글", now.minus(20, ChronoUnit.HOURS), testUser2, 30);
        
        // 2일 전 게시글 (1일 초과, 포함되지 않아야 함)
        Post oldPost = createPostWithLikes("오래된 글", now.minus(2, ChronoUnit.DAYS), testUser1, 50);

        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

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
                .setParameter("flag", PopularFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).hasSameSizeAs(result);
    }

    @Test
    @DisplayName("주간 인기글 선정 - 성공")
    void updateWeeklyPopularPosts_Success() {
        // Given - 7일 이내 게시글들 생성
        Instant now = Instant.now();
        
        Post post1 = createPostWithLikes("이번주 인기글 1", now.minus(2, ChronoUnit.DAYS), testUser1, 40);
        Post post2 = createPostWithLikes("이번주 인기글 2", now.minus(5, ChronoUnit.DAYS), testUser2, 35);
        Post post3 = createPostWithLikes("이번주 인기글 3", now.minus(7, ChronoUnit.DAYS), testUser1, 45);
        
        // 8일 전 게시글 (7일 초과, 포함되지 않아야 함)
        Post oldPost = createPostWithLikes("저번주 글", now.minus(8, ChronoUnit.DAYS), testUser2, 60);

        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateWeeklyPopularPosts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSizeLessThanOrEqualTo(5); // 최대 5개
        
        // 주간 인기글 플래그 확인
        List<Post> weeklyPopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PopularFlag.WEEKLY)
                .getResultList();
        
        assertThat(weeklyPopularPosts).hasSameSizeAs(result);
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 성공")
    void updateLegendPosts_Success() {
        // Given - 20개 이상 좋아요를 받은 게시글들 생성
        Post legendPost1 = createPostWithLikes("레전드 게시글 1", Instant.now().minus(10, ChronoUnit.DAYS), testUser1, 25);
        Post legendPost2 = createPostWithLikes("레전드 게시글 2", Instant.now().minus(5, ChronoUnit.DAYS), testUser2, 30);
        Post legendPost3 = createPostWithLikes("레전드 게시글 3", Instant.now().minus(1, ChronoUnit.DAYS), testUser1, 22);
        
        // 20개 미만 좋아요 (레전드가 되지 못함)
        Post normalPost = createPostWithLikes("일반 게시글", Instant.now().minus(3, ChronoUnit.DAYS), testUser2, 15);

        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateLegendPosts();

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
                .setParameter("flag", PopularFlag.LEGEND)
                .getResultList();
        
        assertThat(legendPosts).hasSize(3);
    }

    @Test
    @DisplayName("실시간 인기글 선정 - 결과 없음")
    void updateRealtimePopularPosts_NoResults() {
        // Given - 2일 전 게시글만 있음 (1일 초과)
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        Post oldPost = createPostWithLikes("오래된 글", twoDaysAgo, testUser1, 10);
        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isEmpty();
        
        // 실시간 인기글 플래그가 설정된 게시글이 없는지 확인
        List<Post> realtimePopularPosts = entityManager.getEntityManager()
                .createQuery("SELECT p FROM Post p WHERE p.popularFlag = :flag", Post.class)
                .setParameter("flag", PopularFlag.REALTIME)
                .getResultList();
        
        assertThat(realtimePopularPosts).isEmpty();
    }

    @Test
    @DisplayName("레전드 게시글 선정 - 조건에 맞는 게시글 없음")
    void updateLegendPosts_NoQualifiedPosts() {
        // Given - 20개 미만 좋아요만 있는 게시글들
        Post post1 = createPostWithLikes("일반글 1", Instant.now(), testUser1, 10);
        Post post2 = createPostWithLikes("일반글 2", Instant.now(), testUser2, 15);
        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateLegendPosts();

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
                .popularFlag(PopularFlag.REALTIME)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(existingPopularPost);

        // 새로운 인기글 후보 생성
        Post newPopularPost = createPostWithLikes("새 인기글", Instant.now(), testUser2, 50);
        entityManager.clear();

        // When
        List<SimplePostDTO> result = postPopularRepository.updateRealtimePopularPosts();

        // Then
        assertThat(result).isNotNull();
        
        // 기존 인기글의 플래그가 제거되었는지 확인
        Post updatedExistingPost = entityManager.find(Post.class, existingPopularPost.getId());
        assertThat(updatedExistingPost.getPopularFlag()).isNull();
        
        // 새로운 인기글의 플래그가 설정되었는지 확인
        Post updatedNewPost = entityManager.find(Post.class, newPopularPost.getId());
        assertThat(updatedNewPost.getPopularFlag()).isEqualTo(PopularFlag.REALTIME);
    }

    /**
     * 좋아요가 있는 게시글을 생성하는 헬퍼 메서드
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
                .createNativeQuery("UPDATE post SET created_at = ? WHERE id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, post.getId())
                .executeUpdate();

        // 좋아요 생성
        for (int i = 0; i < likeCount; i++) {
            // 임시 사용자 생성 (좋아요용)
            Users likeUser = Users.builder()
                    .userName("좋아요유저" + i)
                    .kakaoId(100000L + i)
                    .build();
            entityManager.persistAndFlush(likeUser);
            
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(likeUser)
                    .build();
            entityManager.persistAndFlush(postLike);
        }

        return post;
    }
}
package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.cache;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.FullPostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post.PostJpaRepository;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.postlike.PostLikeJpaRepository;
import jaeik.growfarm.util.RedisContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.connection.RedisConnection; // Correct import
import org.springframework.boot.test.context.TestConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <h2>PostCacheSyncAdapter 테스트</h2>
 * <p>PostCacheSyncAdapter가 인기 게시글 조회 및 상세 조회 기능을 정확히 수행하는지 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest
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
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post",
        "jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.postlike"
})
@Import({PostCacheSyncAdapter.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostCacheSyncAdapterTest extends RedisContainer {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");


    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
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
    private PostCacheSyncAdapter postCacheSyncAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private PostLikeJpaRepository postLikeJpaRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;
    private User otherUser; // 좋아요 누를 다른 사용자

    @BeforeEach
    void setUp() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            Assertions.assertNotNull(connection);
            connection.serverCommands().flushAll();
        } catch (Exception e) {
            // Log the exception or rethrow it if necessary
            System.err.println("Error flushing Redis: " + e.getMessage());
        }
        entityManager.clear(); // 영속성 컨텍스트 초기화

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
    }

    private Post createAndSavePost(String title, String content, int views, PostCacheFlag flag, Instant createdAt) {
        Post post = Post.builder()
                .user(testUser)
                .title(title)
                .content(content)
                .views(views)
                .isNotice(false)
                .password(1234)
                .postCacheFlag(flag)
                .createdAt(createdAt)
                .modifiedAt(Instant.now())
                .build();
        return postJpaRepository.save(post);
    }

    private void addLikesToPost(Post post, int count) {
        IntStream.range(0, count).forEach(i -> {
            User liker = User.builder()
                    .userName("liker" + i)
                    .socialId("social" + i)
                    .provider(SocialProvider.GOOGLE)
                    .socialNickname("좋아요맨" + i)
                    .role(UserRole.USER)
                    .setting(Setting.builder().build())
                    .build();
            entityManager.persistAndFlush(liker);

            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(liker)
                    .build();
            postLikeJpaRepository.save(postLike);
        });
        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 게시글 조회 (지난 1일)")
    void shouldFindRealtimePopularPosts() {
        // Given: 최근 1일 이내 게시글 및 오래된 게시글, 좋아요 추가
        Post recentPost1 = createAndSavePost("최근 인기 게시글1", "내용", 10, PostCacheFlag.REALTIME, Instant.now().minus(10, ChronoUnit.HOURS));
        Post recentPost2 = createAndSavePost("최근 인기 게시글2", "내용", 5, PostCacheFlag.REALTIME, Instant.now().minus(20, ChronoUnit.HOURS));
        createAndSavePost("오래된 게시글", "내용", 100, PostCacheFlag.REALTIME, Instant.now().minus(2, ChronoUnit.DAYS));

        addLikesToPost(recentPost1, 5);
        addLikesToPost(recentPost2, 10); // 얘가 더 인기 많음

        entityManager.flush();
        entityManager.clear();

        // When: 실시간 인기 게시글 조회
        List<SimplePostResDTO> popularPosts = postCacheSyncAdapter.findRealtimePopularPosts();

        // Then: 최근 1일 이내 게시글 중 좋아요 순으로 정렬되어 조회되는지 확인 (5개 제한)
        assertThat(popularPosts).hasSize(2);
        assertThat(popularPosts.get(0).getTitle()).isEqualTo("최근 인기 게시글2"); // 좋아요 10개
        assertThat(popularPosts.get(1).getTitle()).isEqualTo("최근 인기 게시글1"); // 좋아요 5개
    }

    @Test
    @DisplayName("정상 케이스 - 주간 인기 게시글 조회 (지난 7일)")
    void shouldFindWeeklyPopularPosts() {
        // Given: 최근 7일 이내 게시글 및 오래된 게시글, 좋아요 추가
        Post weekPost1 = createAndSavePost("주간 인기 게시글1", "내용", 20, PostCacheFlag.WEEKLY, Instant.now().minus(3, ChronoUnit.DAYS));
        Post weekPost2 = createAndSavePost("주간 인기 게시글2", "내용", 15, PostCacheFlag.WEEKLY, Instant.now().minus(5, ChronoUnit.DAYS));
        createAndSavePost("아주 오래된 게시글", "내용", 200, PostCacheFlag.WEEKLY, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(weekPost1, 10);
        addLikesToPost(weekPost2, 12); // 얘가 더 인기 많음

        entityManager.flush();
        entityManager.clear();

        // When: 주간 인기 게시글 조회
        List<SimplePostResDTO> popularPosts = postCacheSyncAdapter.findWeeklyPopularPosts();

        // Then: 최근 7일 이내 게시글 중 좋아요 순으로 정렬되어 조회되는지 확인 (5개 제한)
        assertThat(popularPosts).hasSize(2);
        assertThat(popularPosts.get(0).getTitle()).isEqualTo("주간 인기 게시글2"); // 좋아요 12개
        assertThat(popularPosts.get(1).getTitle()).isEqualTo("주간 인기 게시글1"); // 좋아요 10개
    }

    @Test
    @DisplayName("정상 케이스 - 전설의 게시글 조회 (추천 20개 이상)")
    void shouldFindLegendaryPosts() {
        // Given: 추천 20개 이상 게시글 2개, 20개 미만 게시글 1개
        Post legendPost1 = createAndSavePost("전설의 게시글1", "내용", 50, PostCacheFlag.LEGEND, Instant.now().minus(30, ChronoUnit.DAYS));
        Post legendPost2 = createAndSavePost("전설의 게시글2", "내용", 60, PostCacheFlag.LEGEND, Instant.now().minus(60, ChronoUnit.DAYS));
        Post normalPost = createAndSavePost("일반 게시글", "내용", 5, PostCacheFlag.LEGEND, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(legendPost1, 25); // 25개
        addLikesToPost(legendPost2, 30); // 30개
        addLikesToPost(normalPost, 15); // 15개

        entityManager.flush();
        entityManager.clear();

        // When: 전설의 게시글 조회
        List<SimplePostResDTO> legendaryPosts = postCacheSyncAdapter.findLegendaryPosts();

        // Then: 추천 20개 이상 게시글 중 좋아요 순으로 정렬되어 조회되는지 확인 (50개 제한)
        assertThat(legendaryPosts).hasSize(2);
        assertThat(legendaryPosts.get(0).getTitle()).isEqualTo("전설의 게시글2");
        assertThat(legendaryPosts.get(1).getTitle()).isEqualTo("전설의 게시글1");
    }

    @Test
    @DisplayName("경계값 - 인기 게시글이 없는 경우 빈 목록 반환")
    void shouldReturnEmptyList_WhenNoPopularPosts() {
        // Given: 게시글이 없는 상태 (setup에서 flushAll 했으므로 비어있음)
        // When: 인기 게시글 조회
        List<SimplePostResDTO> realtimePosts = postCacheSyncAdapter.findRealtimePopularPosts();
        List<SimplePostResDTO> weeklyPosts = postCacheSyncAdapter.findWeeklyPopularPosts();
        List<SimplePostResDTO> legendaryPosts = postCacheSyncAdapter.findLegendaryPosts();

        // Then: 빈 목록 반환 확인
        assertThat(realtimePosts).isEmpty();
        assertThat(weeklyPosts).isEmpty();
        assertThat(legendaryPosts).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 조회")
    void shouldFindPostDetail_WhenValidPostIdProvided() {
        // Given: 게시글 저장 및 좋아요 추가
        Post post = createAndSavePost("상세 조회 게시글", "상세 내용", 10, PostCacheFlag.REALTIME, Instant.now());
        addLikesToPost(post, 3); // 좋아요 3개

        entityManager.flush();
        entityManager.clear();

        // When: 게시글 상세 조회
        FullPostResDTO postDetail = postCacheSyncAdapter.findPostDetail(post.getId());

        // Then: 상세 정보 및 좋아요 수 일치 확인
        assertThat(postDetail).isNotNull();
        assertThat(postDetail.getTitle()).isEqualTo("상세 조회 게시글");
        assertThat(postDetail.getContent()).isEqualTo("상세 내용");
        assertThat(postDetail.getLikeCount()).isEqualTo(3);
        assertThat(postDetail.getUserName()).isEqualTo(testUser.getUserName());
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 게시글 ID로 상세 조회 시 null 반환")
    void shouldReturnNull_WhenNonExistentPostIdProvidedForDetail() {
        // Given: 존재하지 않는 게시글 ID
        Long nonExistentPostId = 999L;

        // When: 상세 조회
        FullPostResDTO postDetail = postCacheSyncAdapter.findPostDetail(nonExistentPostId);

        // Then: null 반환 확인
        assertNull(postDetail);
    }
}

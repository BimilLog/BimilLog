package jaeik.bimillog.infrastructure.adapter.post.post;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.post.PostCacheSyncAdapter;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostLikeRepository;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <h2>PostCacheSyncAdapter 테스트</h2>
 * <p>PostCacheSyncAdapter가 인기 게시글 조회 기능을 정확히 수행하는지 테스트합니다.</p>
 * <p>TestContainers를 사용하여 MySQL과 Redis 컨테이너와 함께 통합 테스트를 수행합니다.</p>
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
@Import({PostCacheSyncAdapter.class, TestContainersConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "logging.level.org.springframework.orm.jpa=DEBUG",
        "logging.level.org.springframework.transaction=DEBUG"
})
class PostCacheSyncAdapterTest {

    @Autowired
    private PostCacheSyncAdapter postCacheSyncAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            if (connection != null) {
                connection.serverCommands().flushAll();
            }
        } catch (Exception e) {
            System.err.println("Redis flush warning: " + e.getMessage());
        }
        
        try {
            postLikeRepository.deleteAll();
            postRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            System.err.println("데이터베이스 초기화 경고: " + e.getMessage());
        }

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
        
        Post savedPost = postRepository.save(post);
        
        try {
            java.lang.reflect.Field createdAtField = savedPost.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(savedPost, createdAt);
            entityManager.persistAndFlush(savedPost);
        } catch (Exception e) {
            System.err.println("createdAt 설정 실패: " + e.getMessage());
        }
        
        return savedPost;
    }

    private void addLikesToPost(Post post, int count) {
        for (int i = 0; i < count; i++) {
            User liker = User.builder()
                    .userName("좋아요맨_" + post.getId() + "_" + i)
                    .socialId("social_" + post.getId() + "_" + i + "_" + System.currentTimeMillis())
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
            postLikeRepository.save(postLike);
        }
        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 실시간 인기 게시글 조회 (지난 1일)")
    void shouldFindRealtimePopularPosts() {
        // Given
        Post recentPost1 = createAndSavePost("최근 인기 게시글1", "내용", 10, PostCacheFlag.REALTIME, Instant.now().minus(10, ChronoUnit.HOURS));
        Post recentPost2 = createAndSavePost("최근 인기 게시글2", "내용", 5, PostCacheFlag.REALTIME, Instant.now().minus(20, ChronoUnit.HOURS));
        createAndSavePost("오래된 게시글", "내용", 100, PostCacheFlag.REALTIME, Instant.now().minus(2, ChronoUnit.DAYS));

        addLikesToPost(recentPost1, 5);
        addLikesToPost(recentPost2, 10);

        entityManager.flush();
        entityManager.clear();

        // When
        List<PostSearchResult> popularPosts = postCacheSyncAdapter.findRealtimePopularPosts();

        // Then
        assertThat(popularPosts).hasSize(2);
        assertThat(popularPosts.get(0).getTitle()).isEqualTo("최근 인기 게시글2"); // 좋아요 10개
        assertThat(popularPosts.get(1).getTitle()).isEqualTo("최근 인기 게시글1"); // 좋아요 5개
    }

    @Test
    @DisplayName("정상 케이스 - 주간 인기 게시글 조회 (지난 7일)")
    void shouldFindWeeklyPopularPosts() {
        // Given
        Post weekPost1 = createAndSavePost("주간 인기 게시글1", "내용", 20, PostCacheFlag.WEEKLY, Instant.now().minus(3, ChronoUnit.DAYS));
        Post weekPost2 = createAndSavePost("주간 인기 게시글2", "내용", 15, PostCacheFlag.WEEKLY, Instant.now().minus(5, ChronoUnit.DAYS));
        createAndSavePost("오래된 게시글", "내용", 200, PostCacheFlag.WEEKLY, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(weekPost1, 10);
        addLikesToPost(weekPost2, 12);

        entityManager.flush();
        entityManager.clear();

        // When
        List<PostSearchResult> popularPosts = postCacheSyncAdapter.findWeeklyPopularPosts();

        // Then
        assertThat(popularPosts).hasSize(2);
        assertThat(popularPosts.get(0).getTitle()).isEqualTo("주간 인기 게시글2"); // 좋아요 12개
        assertThat(popularPosts.get(1).getTitle()).isEqualTo("주간 인기 게시글1"); // 좋아요 10개
    }

    @Test
    @DisplayName("정상 케이스 - 전설의 게시글 조회 (추천 20개 이상)")
    void shouldFindLegendaryPosts() {
        // Given
        Post legendPost1 = createAndSavePost("전설의 게시글1", "내용", 50, PostCacheFlag.LEGEND, Instant.now().minus(30, ChronoUnit.DAYS));
        Post legendPost2 = createAndSavePost("전설의 게시글2", "내용", 60, PostCacheFlag.LEGEND, Instant.now().minus(60, ChronoUnit.DAYS));
        Post normalPost = createAndSavePost("일반 게시글", "내용", 5, PostCacheFlag.LEGEND, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(legendPost1, 25);
        addLikesToPost(legendPost2, 30);
        addLikesToPost(normalPost, 15);

        entityManager.flush();
        entityManager.clear();

        // When
        List<PostSearchResult> legendaryPosts = postCacheSyncAdapter.findLegendaryPosts();

        // Then
        assertThat(legendaryPosts).hasSize(2);
        assertThat(legendaryPosts.get(0).getTitle()).isEqualTo("전설의 게시글2");
        assertThat(legendaryPosts.get(1).getTitle()).isEqualTo("전설의 게시글1");
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 조회")
    void shouldFindPostDetail_WhenValidPostIdProvided() {
        // Given
        Post post = createAndSavePost("상세 조회 게시글", "상세 내용", 10, PostCacheFlag.REALTIME, Instant.now());
        addLikesToPost(post, 3);

        entityManager.flush();
        entityManager.clear();

        // When
        PostDetail postDetail = postCacheSyncAdapter.findPostDetail(post.getId());

        // Then
        assertThat(postDetail).isNotNull();
        assertThat(postDetail.title()).isEqualTo("상세 조회 게시글");
        assertThat(postDetail.content()).isEqualTo("상세 내용");
        assertThat(postDetail.likeCount()).isEqualTo(3);
        assertThat(postDetail.userName()).isEqualTo(testUser.getUserName());
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 게시글 ID로 상세 조회 시 null 반환")
    void shouldReturnNull_WhenNonExistentPostIdProvidedForDetail() {
        // Given
        Long nonExistentPostId = 999L;

        // When
        PostDetail postDetail = postCacheSyncAdapter.findPostDetail(nonExistentPostId);

        // Then
        assertNull(postDetail);
    }
}
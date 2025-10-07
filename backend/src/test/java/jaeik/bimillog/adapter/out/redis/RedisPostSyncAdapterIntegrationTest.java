package jaeik.bimillog.adapter.out.redis;

import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.adapter.out.post.PostLikeRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.PostRepository;
import jaeik.bimillog.testutil.RedisTestHelper;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestMembers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <h2>RedisPostSyncAdapter 테스트</h2>
 * <p>PostCacheSyncAdapter가 인기 게시글 조회 기능을 정확히 수행하는지 테스트합니다.</p>
 * <p>TestContainers를 사용하여 MySQL과 Redis 컨테이너와 함께 통합 테스트를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("tc")
@Testcontainers
@Import(TestContainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Tag("tc")
class RedisPostSyncAdapterIntegrationTest {

    @Autowired
    private PostQueryPort postQueryPort;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Member testMember;

    private void persistAndFlush(Object entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);

        // DB 초기화
        try {
            postLikeRepository.deleteAll();
            postRepository.deleteAll();
            entityManager.flush();
            // clear() 제거: 테스트에서 엔티티를 계속 사용하므로 분리하지 않음
        } catch (Exception e) {
            System.err.println("데이터베이스 초기화 경고: " + e.getMessage());
        }

        // 테스트 사용자 준비 (KakaoToken과 Setting을 포함하여 영속화)
        testMember = TestMembers.createUniqueWithPrefix("redis");
        TestFixtures.persistMemberWithDependencies(entityManager, testMember);
        entityManager.flush();
    }

    private Post createAndSavePost(String title, String content, int views, PostCacheFlag flag, Instant createdAt) {
        Post post = Post.builder()
                .member(testMember)
                .title(title)
                .content(content)
                .views(views)
                .password(1234)
                .createdAt(createdAt)
                .modifiedAt(Instant.now())
                .build();
        
        Post savedPost = postRepository.save(post);
        
        try {
            java.lang.reflect.Field createdAtField = savedPost.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(savedPost, createdAt);
            entityManager.flush();
        } catch (Exception e) {
            System.err.println("createdAt 설정 실패: " + e.getMessage());
        }
        
        return savedPost;
    }

    private void addLikesToPost(Post post, int count) {
        for (int i = 0; i < count; i++) {
            Member liker = TestMembers.withSocialId("social_" + post.getId() + "_" + i + "_" + System.currentTimeMillis());
            TestFixtures.persistMemberWithDependencies(entityManager, liker);
            entityManager.flush();

            PostLike postLike = PostLike.builder()
                    .post(post)
                    .member(liker)
                    .build();
            postLikeRepository.save(postLike);
        }
        entityManager.flush();
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
        List<PostSimpleDetail> popularPosts = postQueryPort.findWeeklyPopularPosts();

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
        List<PostSimpleDetail> legendaryPosts = postQueryPort.findLegendaryPosts();

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
        PostDetail postDetail = postQueryPort.findPostDetailWithCounts(post.getId(), null).orElse(null);

        // Then
        assertThat(postDetail).isNotNull();
        assertThat(postDetail.getTitle()).isEqualTo("상세 조회 게시글");
        assertThat(postDetail.getContent()).isEqualTo("상세 내용");
        assertThat(postDetail.getLikeCount()).isEqualTo(3);
        assertThat(postDetail.getMemberName()).isEqualTo(testMember.getMemberName());
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 게시글 ID로 상세 조회 시 null 반환")
    void shouldReturnNull_WhenNonExistentPostIdProvidedForDetail() {
        // Given
        Long nonExistentPostId = 999L;

        // When
        PostDetail postDetail = postQueryPort.findPostDetailWithCounts(nonExistentPostId, null).orElse(null);

        // Then
        assertNull(postDetail);
    }
}

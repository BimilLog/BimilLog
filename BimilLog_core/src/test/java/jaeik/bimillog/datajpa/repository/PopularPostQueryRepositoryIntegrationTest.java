package jaeik.bimillog.datajpa.repository;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.entity.jpa.PostLike;
import jaeik.bimillog.domain.post.repository.PostLikeRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.testutil.TestMembers;
import jaeik.bimillog.testutil.config.H2TestConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <h2>PostQueryAdapter 인기 게시글 조회 통합 테스트</h2>
 * <p>주간/전설 인기 게시글 DB 조회 기능을 정확히 수행하는지 테스트합니다.</p>
 * <p>H2 인메모리 DB 환경에서 통합 테스트를 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest
@Tag("datajpa-h2")
@ActiveProfiles("h2test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostQueryRepository.class, H2TestConfiguration.class})
class PopularPostQueryRepositoryIntegrationTest {

    @Autowired
    private PostQueryRepository postQueryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    // 최대 좋아요 수(30) + 여유분
    private static final int MEMBER_POOL_SIZE = 35;

    private Member testMember;
    private final List<Member> memberPool = new ArrayList<>();

    @BeforeEach
    void setUp() {
        testMember = TestMembers.createUniqueWithPrefix("popular");
        TestFixtures.persistMemberWithDependencies(entityManager, testMember);

        // 좋아요용 회원 풀 사전 생성 (UUID 기반 고유 ID로 충돌 방지)
        memberPool.clear();
        String poolKey = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < MEMBER_POOL_SIZE; i++) {
            Member m = TestMembers.withSocialId("pool_" + poolKey + "_" + i);
            TestFixtures.persistMemberWithDependencies(entityManager, m);
            memberPool.add(m);
        }

        entityManager.flush();
    }

    private Post createAndSavePost(String title, String content, int views, Instant createdAt) {
        Post post = Post.builder()
                .member(testMember)
                .title(title)
                .content(content)
                .views(views)
                .password(1234)
                .memberName(testMember.getMemberName())
                .build();

        Post savedPost = postRepository.save(post);

        // JPA Auditing이 createdAt을 현재 시간으로 설정하므로, JPQL로 덮어씀
        entityManager.createQuery("UPDATE Post p SET p.createdAt = :date WHERE p.id = :id")
                .setParameter("date", createdAt)
                .setParameter("id", savedPost.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        return postRepository.findById(savedPost.getId()).orElseThrow();
    }

    private void addLikesToPost(Post post, int count) {
        List<PostLike> likes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            likes.add(PostLike.builder()
                    .post(post)
                    .member(memberPool.get(i))
                    .build());
        }
        postLikeRepository.saveAll(likes);

        // 프로덕션 코드(postRepository.incrementLikeCount)와 동일한 방식으로 likeCount 동기화
        entityManager.createQuery("UPDATE Post p SET p.likeCount = p.likeCount + :count WHERE p.id = :id")
                .setParameter("count", count)
                .setParameter("id", post.getId())
                .executeUpdate();
        entityManager.flush();
    }

    @Test
    @DisplayName("정상 케이스 - 주간 인기 게시글 조회 (지난 7일)")
    void shouldFindWeeklyPopularPosts() {
        // Given
        Post weekPost1 = createAndSavePost("주간 인기 게시글1", "내용", 20, Instant.now().minus(3, ChronoUnit.DAYS));
        Post weekPost2 = createAndSavePost("주간 인기 게시글2", "내용", 15, Instant.now().minus(5, ChronoUnit.DAYS));
        createAndSavePost("오래된 게시글", "내용", 200, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(weekPost1, 10);
        addLikesToPost(weekPost2, 12);

        entityManager.flush();
        entityManager.clear();

        // When
        List<PostSimpleDetail> popularPosts = postQueryRepository.selectPostSimpleDetails(
                PostQueryType.WEEKLY_SCHEDULER.condition(),
                PageRequest.of(0, PostQueryType.WEEKLY_SCHEDULER.getLimit()),
                PostQueryType.WEEKLY_SCHEDULER.getOrders()
        ).getContent();

        // Then
        assertThat(popularPosts).hasSize(2);
        assertThat(popularPosts.getFirst().getTitle()).isEqualTo("주간 인기 게시글2"); // 좋아요 12개
        assertThat(popularPosts.get(0).getId()).isNotNull();
        assertThat(popularPosts.get(0).getMemberId()).isEqualTo(testMember.getId());
        assertThat(popularPosts.get(1).getTitle()).isEqualTo("주간 인기 게시글1"); // 좋아요 10개
        assertThat(popularPosts.get(1).getId()).isNotNull();
        assertThat(popularPosts.get(1).getMemberId()).isEqualTo(testMember.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 전설의 게시글 조회 (추천 20개 이상)")
    void shouldFindLegendaryPosts() {
        // Given
        Post legendPost1 = createAndSavePost("전설의 게시글1", "내용", 50, Instant.now().minus(30, ChronoUnit.DAYS));
        Post legendPost2 = createAndSavePost("전설의 게시글2", "내용", 60, Instant.now().minus(60, ChronoUnit.DAYS));
        Post normalPost = createAndSavePost("일반 게시글", "내용", 5, Instant.now().minus(10, ChronoUnit.DAYS));

        addLikesToPost(legendPost1, 25);
        addLikesToPost(legendPost2, 30);
        addLikesToPost(normalPost, 15);

        entityManager.flush();
        entityManager.clear();

        // When
        List<PostSimpleDetail> legendaryPosts = postQueryRepository.selectPostSimpleDetails(
                PostQueryType.LEGEND_SCHEDULER.condition(),
                PageRequest.of(0, PostQueryType.LEGEND_SCHEDULER.getLimit()),
                PostQueryType.LEGEND_SCHEDULER.getOrders()
        ).getContent();

        // Then
        assertThat(legendaryPosts).hasSize(2);
        assertThat(legendaryPosts.getFirst().getTitle()).isEqualTo("전설의 게시글2");
        assertThat(legendaryPosts.get(0).getId()).isNotNull();
        assertThat(legendaryPosts.get(0).getMemberId()).isEqualTo(testMember.getId());
        assertThat(legendaryPosts.get(1).getTitle()).isEqualTo("전설의 게시글1");
        assertThat(legendaryPosts.get(1).getId()).isNotNull();
        assertThat(legendaryPosts.get(1).getMemberId()).isEqualTo(testMember.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 최근 인기 게시글 조회 (1시간 이내, 조회수+추천수*30 정렬)")
    void shouldFindRecentPopularPosts() {
        // Given: 1시간 이내 게시글 3개 (인기도 다르게 설정)
        Post recentPost1 = createAndSavePost("최근 인기글1", "내용", 100, Instant.now().minus(30, ChronoUnit.MINUTES));
        Post recentPost2 = createAndSavePost("최근 인기글2", "내용", 10, Instant.now().minus(20, ChronoUnit.MINUTES));
        Post recentPost3 = createAndSavePost("최근 인기글3", "내용", 50, Instant.now().minus(10, ChronoUnit.MINUTES));
        // 2시간 전 게시글 (조회되면 안 됨)
        createAndSavePost("오래된 게시글", "내용", 500, Instant.now().minus(2, ChronoUnit.HOURS));

        // recentPost2에 좋아요 5개 추가 → 인기도: 10 + (5*30) = 160
        addLikesToPost(recentPost2, 5);
        // recentPost1: 인기도 100, recentPost3: 인기도 50

        entityManager.flush();
        entityManager.clear();

        // When
        Page<PostSimpleDetail> result = postQueryRepository.selectPostSimpleDetails(
                PostQueryType.REALTIME_FALLBACK.condition(),
                PageRequest.of(0, 5),
                PostQueryType.REALTIME_FALLBACK.getOrders()
        );

        // Then
        assertThat(result.getContent()).hasSize(3);
        // 인기도 순: recentPost2(160) > recentPost1(100) > recentPost3(50)
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("최근 인기글2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("최근 인기글1");
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("최근 인기글3");
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 상세 조회")
    void shouldFindPostDetail_WhenValidPostIdProvided() {
        // Given
        Post post = createAndSavePost("상세 조회 게시글", "상세 내용", 10, Instant.now());
        addLikesToPost(post, 3);

        entityManager.flush();
        entityManager.clear();

        // When
        Post postEntity = postRepository.findById(post.getId()).orElse(null);
        PostDetail postDetail = postEntity != null ? PostDetail.from(postEntity, false) : null;

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
        Long nonExistentPostId = 999999L;

        // When
        Post postEntity = postRepository.findById(nonExistentPostId).orElse(null);
        PostDetail postDetail = postEntity != null ? PostDetail.from(postEntity, false) : null;

        // Then
        assertNull(postDetail);
    }
}

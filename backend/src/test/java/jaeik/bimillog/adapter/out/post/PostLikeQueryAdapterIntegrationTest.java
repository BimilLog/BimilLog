package jaeik.bimillog.adapter.out.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.infrastructure.adapter.out.post.PostLikeQueryAdapter;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostLikeQueryAdapter 통합 테스트</h2>
 * <p>추천수 집계 및 존재 여부 조회 로직을 검증합니다.</p>
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = jaeik.bimillog.BimilLogApplication.class
        )
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2test")
@Import({PostLikeQueryAdapter.class, H2TestConfiguration.class})
@Tag("integration")
class PostLikeQueryAdapterIntegrationTest {

    @Autowired
    private PostLikeQueryAdapter postLikeQueryAdapter;

    @Autowired
    private TestEntityManager testEntityManager;

    private Member author;
    private Member likerOne;
    private Member likerTwo;

    @BeforeEach
    void setUp() {
        author = testEntityManager.persistAndFlush(TestMembers.copyWithId(TestMembers.MEMBER_1, null));
        likerOne = testEntityManager.persistAndFlush(TestMembers.copyWithId(TestMembers.MEMBER_2, null));
        likerTwo = testEntityManager.persistAndFlush(TestMembers.copyWithId(TestMembers.MEMBER_3, null));
    }

    @Test
    @DisplayName("게시글 ID 목록으로 추천수가 집계된다")
    void shouldReturnLikeCountsForPostIds() {
        Post postOne = persistPost("첫 번째 게시글");
        Post postTwo = persistPost("두 번째 게시글");
        Post postThree = persistPost("세 번째 게시글");

        persistLike(postOne, likerOne);
        persistLike(postOne, likerTwo);

        persistLike(postTwo, likerOne);

        testEntityManager.flush();
        testEntityManager.clear();

        Map<Long, Integer> likeCounts = postLikeQueryAdapter.findLikeCountsByPostIds(
                List.of(postOne.getId(), postTwo.getId(), postThree.getId())
        );

        assertThat(likeCounts.get(postOne.getId())).isEqualTo(2);
        assertThat(likeCounts.get(postTwo.getId())).isEqualTo(1);
        assertThat(likeCounts.get(postThree.getId())).isZero();
    }

    @Test
    @DisplayName("비어있는 입력은 빈 결과를 반환한다")
    void shouldReturnEmptyMapWhenPostIdsEmpty() {
        Map<Long, Integer> likeCounts = postLikeQueryAdapter.findLikeCountsByPostIds(List.of());

        assertThat(likeCounts).isEmpty();
    }

    @Test
    @DisplayName("추천 존재 여부를 빠르게 확인한다")
    void shouldCheckExistenceByPostIdAndMemberId() {
        Post post = persistPost("존재 여부 테스트 게시글");
        persistLike(post, likerOne);

        testEntityManager.flush();
        testEntityManager.clear();

        boolean exists = postLikeQueryAdapter.existsByPostIdAndUserId(post.getId(), likerOne.getId());
        boolean notExists = postLikeQueryAdapter.existsByPostIdAndUserId(post.getId(), likerTwo.getId());

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    private Post persistPost(String title) {
        Post post = Post.createPost(author, title, title + " 내용", 1234);
        return testEntityManager.persistAndFlush(post);
    }

    private void persistLike(Post post, Member member) {
        PostLike postLike = PostLike.builder()
                .post(post)
                .member(member)
                .build();
        testEntityManager.persist(postLike);
    }
}

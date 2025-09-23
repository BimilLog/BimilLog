package jaeik.bimillog.infrastructure.adapter.out.post;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostLikeCommandAdapter 테스트</h2>
 * <p>게시글 추천 명령 어댑터의 핵심 기능을 테스트합니다.</p>
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
@Import({PostLikeCommandAdapter.class, TestContainersConfiguration.class})
class PostLikeCommandAdapterIntegrationTest {

    @Autowired
    private PostLikeCommandAdapter postLikeCommandAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 저장")
    void shouldSave_WhenValidPostLike() {
        // Given
        User user = TestUsers.USER1;
        entityManager.persistAndFlush(user);

        Post post = TestFixtures.createPost(user, "테스트 게시글", "내용");
        entityManager.persistAndFlush(post);

        PostLike postLike = TestFixtures.createPostLike(post, user);

        // When
        postLikeCommandAdapter.save(postLike);
        entityManager.flush();
        PostLike savedLike = entityManager.find(PostLike.class, postLike.getId());

        // Then
        assertThat(savedLike).isNotNull();
        assertThat(savedLike.getPost().getId()).isEqualTo(post.getId());
        assertThat(savedLike.getUser().getId()).isEqualTo(user.getId());

        // 실제 데이터베이스에 저장되었는지 확인
        PostLike foundLike = entityManager.find(PostLike.class, savedLike.getId());
        assertThat(foundLike).isNotNull();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 삭제")
    void shouldDeleteByUserAndPost_WhenValidPostAndUser() {
        // Given
        User user = TestUsers.USER1;
        entityManager.persistAndFlush(user);

        Post post = TestFixtures.createPost(user, "테스트 게시글", "내용");
        entityManager.persistAndFlush(post);

        PostLike postLike = TestFixtures.createPostLike(post, user);
        entityManager.persistAndFlush(postLike);

        // When
        postLikeCommandAdapter.deleteByUserAndPost(user, post);
        entityManager.flush();

        // Then
        PostLike foundLike = entityManager.find(PostLike.class, postLike.getId());
        assertThat(foundLike).isNull();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID로 모든 추천 삭제")
    void shouldDeleteAllByPostId_WhenValidPostIdProvided() {
        // Given: 게시글과 여러 사용자의 추천들
        User user1 = TestUsers.USER1;
        entityManager.persistAndFlush(user1);

        User user2 = TestUsers.USER2;
        entityManager.persistAndFlush(user2);

        Post post = TestFixtures.createPost(user1, "테스트 게시글", "내용");
        entityManager.persistAndFlush(post);

        PostLike postLike1 = TestFixtures.createPostLike(post, user1);
        PostLike postLike2 = TestFixtures.createPostLike(post, user2);
        
        entityManager.persist(postLike1);
        entityManager.persist(postLike2);
        entityManager.flush();

        // When: 게시글 ID로 모든 추천 삭제
        postLikeCommandAdapter.deleteAllByPostId(post.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 모든 추천이 삭제되었는지 확인
        PostLike foundLike1 = entityManager.find(PostLike.class, postLike1.getId());
        PostLike foundLike2 = entityManager.find(PostLike.class, postLike2.getId());
        
        assertThat(foundLike1).isNull();
        assertThat(foundLike2).isNull();
    }
}
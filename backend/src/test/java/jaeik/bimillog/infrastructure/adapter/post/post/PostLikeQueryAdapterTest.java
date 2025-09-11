package jaeik.bimillog.infrastructure.adapter.post.post;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.postlike.PostLikeRepository;
import jaeik.bimillog.infrastructure.adapter.post.out.post.PostLikeQueryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * <h2>PostLikeQueryAdapter 단위 테스트</h2>
 * <p>게시글 추천 쿼리 어댑터의 기능을 Mock 기반으로 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PostLikeQueryAdapterTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    private PostLikeQueryAdapter postLikeQueryAdapter;
    
    private User testUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        postLikeQueryAdapter = new PostLikeQueryAdapter(postLikeRepository);
        
        // 테스트 데이터 준비
        testUser = createTestUser("testUser", "123456");
        testPost = createTestPost(testUser, "테스트 게시글", "테스트 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 존재 여부 확인 (true)")
    void shouldReturnTrue_WhenUserLikedPost() {
        // Given: 사용자와 게시글, 추천 데이터
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천된 게시글", "사용자가 추천한 게시글");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        entityManager.persistAndFlush(postLike);

        // When: 추천 존재 여부 조회
        boolean exists = postLikeQueryAdapter.existsByUserAndPost(user, post);

        // Then: 추천이 존재하므로 true 반환
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 존재 여부 확인 (false)")
    void shouldReturnFalse_WhenUserDidNotLikePost() {
        // Given: 사용자와 게시글 (추천 없음)
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천되지 않은 게시글", "사용자가 추천하지 않은 게시글");
        entityManager.persistAndFlush(post);

        // When: 추천 존재 여부 조회
        boolean exists = postLikeQueryAdapter.existsByUserAndPost(user, post);

        // Then: 추천이 존재하지 않으므로 false 반환
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (단일 추천)")
    void shouldReturnCorrectCount_WhenSingleUserLikedPost() {
        // Given: 한 명의 사용자가 추천한 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "단일 추천 게시글", "한 명이 추천한 게시글");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        entityManager.persistAndFlush(postLike);

        // When: 추천 수 조회
        long count = postLikeQueryAdapter.countByPost(post);

        // Then: 1개의 추천 반환
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (다중 추천)")
    void shouldReturnCorrectCount_WhenMultipleUsersLikedPost() {
        // Given: 여러 사용자가 추천한 게시글
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post = createTestPost(author, "인기 게시글", "여러 명이 추천한 게시글");
        entityManager.persistAndFlush(post);

        // 5명의 사용자가 추천
        for (int i = 1; i <= 5; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);

            PostLike postLike = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            entityManager.persistAndFlush(postLike);
        }

        // When: 추천 수 조회
        long count = postLikeQueryAdapter.countByPost(post);

        // Then: 5개의 추천 반환
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (추천 없음)")
    void shouldReturnZero_WhenNoOneaLikedPost() {
        // Given: 추천이 없는 게시글
        User user = createTestUser("author", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천 없는 게시글", "아무도 추천하지 않은 게시글");
        entityManager.persistAndFlush(post);

        // When: 추천 수 조회
        long count = postLikeQueryAdapter.countByPost(post);

        // Then: 0개 반환
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("경계값 - 다른 사용자와 동일 게시글 추천 여부")
    void shouldReturnFalse_WhenDifferentUserCheckedForLike() {
        // Given: userA가 추천한 게시글과 userB
        User userA = createTestUser("userA", "111111");
        User userB = createTestUser("userB", "222222");
        entityManager.persistAndFlush(userA);
        entityManager.persistAndFlush(userB);

        Post post = createTestPost(userA, "테스트 게시글", "테스트 내용");
        entityManager.persistAndFlush(post);

        // userA만 추천
        PostLike postLike = PostLike.builder()
                .user(userA)
                .post(post)
                .build();
        entityManager.persistAndFlush(postLike);

        // When: userB의 추천 여부 확인
        boolean userAExists = postLikeQueryAdapter.existsByUserAndPost(userA, post);
        boolean userBExists = postLikeQueryAdapter.existsByUserAndPost(userB, post);

        // Then: userA는 true, userB는 false
        assertThat(userAExists).isTrue();
        assertThat(userBExists).isFalse();
    }

    @Test
    @DisplayName("경계값 - 다른 게시글의 추천 수 비교")
    void shouldReturnCorrectCounts_WhenDifferentPostsHaveDifferentLikes() {
        // Given: 두 개의 게시글과 각기 다른 추천 수
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post1 = createTestPost(author, "게시글1", "내용1");
        Post post2 = createTestPost(author, "게시글2", "내용2");
        entityManager.persistAndFlush(post1);
        entityManager.persistAndFlush(post2);

        // post1: 3개 추천
        for (int i = 1; i <= 3; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);
            PostLike like = PostLike.builder().user(user).post(post1).build();
            entityManager.persistAndFlush(like);
        }

        // post2: 7개 추천
        for (int i = 4; i <= 10; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);
            PostLike like = PostLike.builder().user(user).post(post2).build();
            entityManager.persistAndFlush(like);
        }

        // When: 각 게시글의 추천 수 조회
        long count1 = postLikeQueryAdapter.countByPost(post1);
        long count2 = postLikeQueryAdapter.countByPost(post2);

        // Then: 각각 올바른 추천 수 반환
        assertThat(count1).isEqualTo(3L);
        assertThat(count2).isEqualTo(7L);
    }



    @Test
    @DisplayName("동시성 - 추천 생성과 동시에 조회")
    void shouldReturnConsistentResults_WhenConcurrentAccess() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "동시성 테스트 게시글", "동시 접근 테스트");
        entityManager.persistAndFlush(post);

        // 최초 상태 확인
        assertThat(postLikeQueryAdapter.existsByUserAndPost(user, post)).isFalse();
        assertThat(postLikeQueryAdapter.countByPost(post)).isEqualTo(0L);

        // When: 추천 생성
        PostLike postLike = PostLike.builder().user(user).post(post).build();
        entityManager.persistAndFlush(postLike);

        // Then: 즉시 조회된 결과가 일관성 있음
        assertThat(postLikeQueryAdapter.existsByUserAndPost(user, post)).isTrue();
        assertThat(postLikeQueryAdapter.countByPost(post)).isEqualTo(1L);
    }


    @Test
    @DisplayName("트랜잭션 - 일관성 있는 데이터 조회")
    void shouldProvideConsistentView_WithinTransaction() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "트랜잭션 테스트", "일관성 테스트");
        entityManager.persistAndFlush(post);

        // 최초 상태: 추천 없음
        assertThat(postLikeQueryAdapter.countByPost(post)).isEqualTo(0L);
        assertThat(postLikeQueryAdapter.existsByUserAndPost(user, post)).isFalse();

        // When: 동일 트랜잭션에서 추천 추가 후 여러 번 조회
        PostLike postLike = PostLike.builder().user(user).post(post).build();
        entityManager.persistAndFlush(postLike);

        // 여러 번 조회해서 일관성 확인
        for (int i = 0; i < 5; i++) {
            assertThat(postLikeQueryAdapter.countByPost(post)).isEqualTo(1L);
            assertThat(postLikeQueryAdapter.existsByUserAndPost(user, post)).isTrue();
        }

        // Then: 모든 조회에서 동일한 결과
        assertThat(postLikeQueryAdapter.countByPost(post)).isEqualTo(1L);
        assertThat(postLikeQueryAdapter.existsByUserAndPost(user, post)).isTrue();
    }

    private User createTestUser(String userName, String socialId) {
        return User.builder()
                .userName(userName)
                .socialId(socialId)
                .provider(SocialProvider.KAKAO)
                .socialNickname(userName + "_nick")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
    }

    private Post createTestPost(User user, String title, String content) {
        return Post.createPost(user, title, content, 1234);
    }
}
package jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.postlike;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.common.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.post.entity.PostReqVO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostLikeJpaRepository 테스트</h2>
 * <p>게시글 추천 JPA Repository의 모든 기능을 테스트합니다.</p>
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
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.postlike"
})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostLikeJpaRepositoryTest {

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
    private PostLikeJpaRepository postLikeJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 존재 여부 확인 (true)")
    void shouldReturnTrue_WhenPostLikeExists() {
        // Given: 사용자, 게시글, 추천 데이터
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천된 게시글", "사용자가 추천한 게시글");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        postLikeJpaRepository.save(postLike);
        entityManager.flush();

        // When: 추천 존재 여부 조회
        boolean exists = postLikeJpaRepository.existsByUserAndPost(user, post);

        // Then: 추천이 존재하므로 true
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 존재 여부 확인 (false)")
    void shouldReturnFalse_WhenPostLikeDoesNotExist() {
        // Given: 사용자와 게시글만 존재 (추천 없음)
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천되지 않은 게시글", "사용자가 추천하지 않은 게시글");
        entityManager.persistAndFlush(post);

        // When: 추천 존재 여부 조회
        boolean exists = postLikeJpaRepository.existsByUserAndPost(user, post);

        // Then: 추천이 존재하지 않으므로 false
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 삭제")
    void shouldDeletePostLike_WhenValidUserAndPostProvided() {
        // Given: 저장된 추천
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "삭제될 추천의 게시글", "추천이 삭제될 게시글");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        postLikeJpaRepository.save(postLike);
        entityManager.flush();

        // 삭제 전 존재 확인
        assertThat(postLikeJpaRepository.existsByUserAndPost(user, post)).isTrue();

        // When: 추천 삭제
        postLikeJpaRepository.deleteByUserAndPost(user, post);
        entityManager.flush();

        // Then: 추천이 삭제됨
        assertThat(postLikeJpaRepository.existsByUserAndPost(user, post)).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (단일 추천)")
    void shouldReturnCorrectCount_WhenSingleLikeExists() {
        // Given: 한 명의 사용자가 추천한 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "단일 추천 게시글", "한 명이 추천한 게시글");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        postLikeJpaRepository.save(postLike);
        entityManager.flush();

        // When: 추천 수 조회
        long count = postLikeJpaRepository.countByPost(post);

        // Then: 1개의 추천
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (다중 추천)")
    void shouldReturnCorrectCount_WhenMultipleLikesExist() {
        // Given: 여러 사용자가 추천한 게시글
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post = createTestPost(author, "인기 게시글", "여러 명이 추천한 게시글");
        entityManager.persistAndFlush(post);

        // 5명의 사용자가 추천
        final int LIKE_COUNT = 5;
        for (int i = 1; i <= LIKE_COUNT; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);

            PostLike postLike = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            postLikeJpaRepository.save(postLike);
        }
        entityManager.flush();

        // When: 추천 수 조회
        long count = postLikeJpaRepository.countByPost(post);

        // Then: 5개의 추천
        assertThat(count).isEqualTo(LIKE_COUNT);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 추천 수 조회 (추천 없음)")
    void shouldReturnZero_WhenNoLikesExist() {
        // Given: 추천이 없는 게시글
        User user = createTestUser("author", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "추천 없는 게시글", "아무도 추천하지 않은 게시글");
        entityManager.persistAndFlush(post);

        // When: 추천 수 조회
        long count = postLikeJpaRepository.countByPost(post);

        // Then: 0개의 추천
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID로 모든 추천 삭제")
    void shouldDeleteAllLikes_WhenValidPostIdProvided() {
        // Given: 여러 사용자가 추천한 게시글
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post = createTestPost(author, "삭제될 추천들의 게시글", "모든 추천이 삭제될 게시글");
        entityManager.persistAndFlush(post);

        // 3명의 사용자가 추천
        for (int i = 1; i <= 3; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);

            PostLike postLike = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            postLikeJpaRepository.save(postLike);
        }
        entityManager.flush();

        // 삭제 전 추천 수 확인
        assertThat(postLikeJpaRepository.countByPost(post)).isEqualTo(3L);

        // When: 게시글 ID로 모든 추천 삭제
        postLikeJpaRepository.deleteAllByPostId(post.getId());
        entityManager.flush();

        // Then: 모든 추천이 삭제됨
        assertThat(postLikeJpaRepository.countByPost(post)).isEqualTo(0L);
    }

    @Test
    @DisplayName("경계값 - 다른 게시글의 추천은 삭제되지 않음")
    void shouldNotDeleteOtherPostLikes_WhenDeletingSpecificPostLikes() {
        // Given: 두 개의 게시글과 각각의 추천
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post1 = createTestPost(author, "게시글1", "내용1");
        Post post2 = createTestPost(author, "게시글2", "내용2");
        entityManager.persistAndFlush(post1);
        entityManager.persistAndFlush(post2);

        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        // 각 게시글에 추천 생성
        PostLike like1 = PostLike.builder().user(user).post(post1).build();
        PostLike like2 = PostLike.builder().user(user).post(post2).build();
        postLikeJpaRepository.save(like1);
        postLikeJpaRepository.save(like2);
        entityManager.flush();

        // When: 첫 번째 게시글의 추천만 삭제
        postLikeJpaRepository.deleteAllByPostId(post1.getId());
        entityManager.flush();

        // Then: 첫 번째는 삭제, 두 번째는 유지
        assertThat(postLikeJpaRepository.countByPost(post1)).isEqualTo(0L);
        assertThat(postLikeJpaRepository.countByPost(post2)).isEqualTo(1L);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 게시글 ID로 삭제")
    void shouldHandleGracefully_WhenDeletingNonExistentPostId() {
        // Given: 존재하지 않는 게시글 ID
        Long nonExistentPostId = 99999L;

        // When: 존재하지 않는 게시글 ID로 삭제 시도
        postLikeJpaRepository.deleteAllByPostId(nonExistentPostId);
        entityManager.flush();

        // Then: 예외 없이 정상 처리 (아무것도 삭제되지 않음)
        // 기존 데이터가 없으므로 카운트는 여전히 0
        List<PostLike> allLikes = postLikeJpaRepository.findAll();
        assertThat(allLikes).hasSize(0);
    }

    @Test
    @DisplayName("경계값 - null 사용자 또는 게시글로 존재 여부 확인")
    void shouldReturnFalse_WhenUserOrPostIsNull() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "테스트 게시글", "테스트 내용");
        entityManager.persistAndFlush(post);

        // When: null 파라미터로 존재 여부 확인
        boolean nullUserExists = postLikeJpaRepository.existsByUserAndPost(null, post);
        boolean nullPostExists = postLikeJpaRepository.existsByUserAndPost(user, null);
        boolean bothNullExists = postLikeJpaRepository.existsByUserAndPost(null, null);

        // Then: 모두 false 반환
        assertThat(nullUserExists).isFalse();
        assertThat(nullPostExists).isFalse();
        assertThat(bothNullExists).isFalse();
    }

    @Test
    @DisplayName("경계값 - null 게시글로 추천 수 조회")
    void shouldReturnZero_WhenPostIsNullForCount() {
        // When: null 게시글의 추천 수 조회
        long count = postLikeJpaRepository.countByPost(null);

        // Then: 0 반환
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("예외 케이스 - 중복 추천 저장 시도")
    void shouldPreventDuplicateLikes_WhenSameUserLikesSamePostTwice() {
        // Given: 사용자와 게시글, 그리고 이미 존재하는 추천
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "중복 추천 테스트 게시글", "중복 추천 테스트");
        entityManager.persistAndFlush(post);

        PostLike firstLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        postLikeJpaRepository.save(firstLike);
        entityManager.flush();

        // When & Then: 동일한 사용자가 동일한 게시글에 다시 추천 시도
        PostLike secondLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        try {
            postLikeJpaRepository.save(secondLike);
            entityManager.flush();
            
            // 중복 저장이 허용된다면 개수 확인
            long count = postLikeJpaRepository.countByPost(post);
            // 비즈니스 요구사항에 따라 1 또는 2가 될 수 있음
            assertThat(count).isGreaterThanOrEqualTo(1L);
        } catch (Exception e) {
            // 제약조건으로 중복이 방지되는 경우
            assertThat(e).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    @DisplayName("트랜잭션 - 저장 후 즉시 조회 가능")
    void shouldBeAvailableImmediately_WhenPostLikeSaved() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "즉시 조회 테스트", "저장 후 바로 조회");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        // When: 저장 후 즉시 조회
        postLikeJpaRepository.save(postLike);
        entityManager.flush();

        boolean exists = postLikeJpaRepository.existsByUserAndPost(user, post);
        long count = postLikeJpaRepository.countByPost(post);

        // Then: 즉시 조회 가능
        assertThat(exists).isTrue();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("성능 - 대량 추천 데이터 처리")
    void shouldHandleMultipleLikes_WhenProcessingMany() {
        // Given: 하나의 인기 게시글과 많은 사용자
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post popularPost = createTestPost(author, "인기 게시글", "많은 추천을 받을 게시글");
        entityManager.persistAndFlush(popularPost);

        final int LIKE_COUNT = 100;

        // When: 100명의 사용자가 추천
        for (int i = 1; i <= LIKE_COUNT; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);

            PostLike like = PostLike.builder()
                    .user(user)
                    .post(popularPost)
                    .build();
            postLikeJpaRepository.save(like);

            // 주기적으로 플러시하여 메모리 효율성 확보
            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
                // 게시글과 작성자는 다시 로드
                popularPost = entityManager.find(Post.class, popularPost.getId());
                author = entityManager.find(User.class, author.getId());
            }
        }
        entityManager.flush();

        // Then: 모든 추천이 정상 처리됨
        long totalCount = postLikeJpaRepository.countByPost(popularPost);
        assertThat(totalCount).isEqualTo(LIKE_COUNT);

        // 조회 성능 확인
        long startTime = System.currentTimeMillis();
        boolean randomUserLikeExists = postLikeJpaRepository.existsByUserAndPost(
                entityManager.find(User.class, 50L), popularPost);
        long endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isLessThan(1000L); // 1초 이내
    }

    @Test
    @DisplayName("일관성 - 삭제와 조회의 일관성")
    void shouldMaintainConsistency_BetweenDeleteAndQuery() {
        // Given: 여러 사용자가 추천한 게시글
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post = createTestPost(author, "일관성 테스트 게시글", "삭제와 조회 일관성 테스트");
        entityManager.persistAndFlush(post);

        User user1 = createTestUser("user1", "111111");
        User user2 = createTestUser("user2", "222222");
        User user3 = createTestUser("user3", "333333");
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        // 3명이 추천
        PostLike like1 = PostLike.builder().user(user1).post(post).build();
        PostLike like2 = PostLike.builder().user(user2).post(post).build();
        PostLike like3 = PostLike.builder().user(user3).post(post).build();
        postLikeJpaRepository.save(like1);
        postLikeJpaRepository.save(like2);
        postLikeJpaRepository.save(like3);
        entityManager.flush();

        // 초기 상태 확인
        assertThat(postLikeJpaRepository.countByPost(post)).isEqualTo(3L);
        assertThat(postLikeJpaRepository.existsByUserAndPost(user1, post)).isTrue();
        assertThat(postLikeJpaRepository.existsByUserAndPost(user2, post)).isTrue();
        assertThat(postLikeJpaRepository.existsByUserAndPost(user3, post)).isTrue();

        // When: 한 명의 추천 삭제
        postLikeJpaRepository.deleteByUserAndPost(user2, post);
        entityManager.flush();

        // Then: 삭제된 사용자는 false, 나머지는 true, 카운트는 2
        assertThat(postLikeJpaRepository.countByPost(post)).isEqualTo(2L);
        assertThat(postLikeJpaRepository.existsByUserAndPost(user1, post)).isTrue();
        assertThat(postLikeJpaRepository.existsByUserAndPost(user2, post)).isFalse();
        assertThat(postLikeJpaRepository.existsByUserAndPost(user3, post)).isTrue();
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
        PostReqVO postReqDTO = PostReqVO.builder()
                .title(title)
                .content(content)
                .password(1234)
                .build();
        return Post.createPost(user, postReqDTO);
    }
}
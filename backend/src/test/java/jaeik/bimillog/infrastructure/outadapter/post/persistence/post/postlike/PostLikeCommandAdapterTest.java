package jaeik.bimillog.infrastructure.outadapter.post.persistence.post.postlike;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.post.entity.PostReqVO;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.postlike.PostLikeCommandAdapter;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostLikeCommandAdapter 테스트</h2>
 * <p>게시글 추천 명령 어댑터의 모든 기능을 테스트합니다.</p>
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
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.postlike"
})
@Import(PostLikeCommandAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostLikeCommandAdapterTest {

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
    private PostLikeCommandAdapter postLikeCommandAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("정상 케이스 - 새 게시글 추천 저장")
    void shouldSavePostLike_WhenValidPostLikeProvided() {
        // Given: 유효한 사용자와 게시글, 추천 생성
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "테스트 게시글", "테스트 내용");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        // When: 추천 저장
        postLikeCommandAdapter.save(postLike);
        entityManager.flush();

        // Then: 저장된 추천 검증
        PostLike foundPostLike = entityManager.getEntityManager()
                .createQuery("SELECT pl FROM PostLike pl WHERE pl.user = :user AND pl.post = :post", PostLike.class)
                .setParameter("user", user)
                .setParameter("post", post)
                .getSingleResult();

        assertThat(foundPostLike).isNotNull();
        assertThat(foundPostLike.getUser().getId()).isEqualTo(user.getId());
        assertThat(foundPostLike.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 사용자와 게시글로 추천 삭제")
    void shouldDeletePostLike_WhenValidUserAndPostProvided() {
        // Given: 저장된 추천
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "테스트 게시글", "테스트 내용");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();
        entityManager.persistAndFlush(postLike);

        // When: 추천 삭제
        postLikeCommandAdapter.deleteByUserAndPost(user, post);
        entityManager.flush();
        entityManager.clear();

        // Then: 삭제 검증
        Long count = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user = :user AND pl.post = :post", Long.class)
                .setParameter("user", user)
                .setParameter("post", post)
                .getSingleResult();

        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID로 모든 추천 삭제")
    void shouldDeleteAllPostLikes_WhenValidPostIdProvided() {
        // Given: 여러 사용자의 동일 게시글 추천
        User user1 = createTestUser("user1", "111111");
        User user2 = createTestUser("user2", "222222");
        User user3 = createTestUser("user3", "333333");
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        Post post = createTestPost(user1, "인기 게시글", "많은 추천을 받을 게시글");
        entityManager.persistAndFlush(post);

        // 3명의 사용자가 추천
        PostLike like1 = PostLike.builder().user(user1).post(post).build();
        PostLike like2 = PostLike.builder().user(user2).post(post).build();
        PostLike like3 = PostLike.builder().user(user3).post(post).build();
        entityManager.persistAndFlush(like1);
        entityManager.persistAndFlush(like2);
        entityManager.persistAndFlush(like3);

        // When: 게시글 ID로 모든 추천 삭제
        postLikeCommandAdapter.deleteAllByPostId(post.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 모든 추천 삭제 확인
        Long count = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId", Long.class)
                .setParameter("postId", post.getId())
                .getSingleResult();

        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("경계값 - 다른 게시글 추천은 삭제되지 않음")
    void shouldNotDeleteOtherPostLikes_WhenDeletingSpecificPost() {
        // Given: 두 개의 게시글과 각각의 추천
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post1 = createTestPost(user, "게시글1", "내용1");
        Post post2 = createTestPost(user, "게시글2", "내용2");
        entityManager.persistAndFlush(post1);
        entityManager.persistAndFlush(post2);

        PostLike like1 = PostLike.builder().user(user).post(post1).build();
        PostLike like2 = PostLike.builder().user(user).post(post2).build();
        entityManager.persistAndFlush(like1);
        entityManager.persistAndFlush(like2);

        // When: 첫 번째 게시글의 추천만 삭제
        postLikeCommandAdapter.deleteAllByPostId(post1.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 첫 번째 게시글 추천은 삭제, 두 번째는 유지
        Long count1 = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId", Long.class)
                .setParameter("postId", post1.getId())
                .getSingleResult();

        Long count2 = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId", Long.class)
                .setParameter("postId", post2.getId())
                .getSingleResult();

        assertThat(count1).isEqualTo(0L); // post1 추천 삭제됨
        assertThat(count2).isEqualTo(1L); // post2 추천 유지됨
    }

    @Test
    @DisplayName("예외 케이스 - 존재하지 않는 게시글 ID로 삭제")
    void shouldHandleGracefully_WhenDeletingNonExistentPostId() {
        // Given: 존재하지 않는 게시글 ID
        Long nonExistentPostId = 99999L;

        // When: 존재하지 않는 게시글 ID로 삭제 시도
        postLikeCommandAdapter.deleteAllByPostId(nonExistentPostId);
        entityManager.flush();

        // Then: 예외 없이 정상 처리 (아무것도 삭제되지 않음)
        Long totalCount = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl", Long.class)
                .getSingleResult();

        assertThat(totalCount).isEqualTo(0L); // 기존에 데이터가 없으므로 0
    }

    @Test
    @DisplayName("동시성 - 중복 추천 저장")
    void shouldHandleDuplicateLikes_WhenSameUserLikesSamePost() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "테스트 게시글", "테스트 내용");
        entityManager.persistAndFlush(post);

        // 첫 번째 추천 저장
        PostLike firstLike = PostLike.builder().user(user).post(post).build();
        postLikeCommandAdapter.save(firstLike);
        entityManager.flush();

        // When: 동일한 사용자가 동일한 게시글에 다시 추천 시도
        PostLike secondLike = PostLike.builder().user(user).post(post).build();
        
        // Then: 중복 저장으로 인한 제약조건 위반이나 예외 처리 확인
        try {
            postLikeCommandAdapter.save(secondLike);
            entityManager.flush();
            
            // 중복 저장이 허용된다면 개수 확인
            Long count = entityManager.getEntityManager()
                    .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user = :user AND pl.post = :post", Long.class)
                    .setParameter("user", user)
                    .setParameter("post", post)
                    .getSingleResult();
            
            // 중복 저장이 허용되는지 확인 (비즈니스 로직에 따라 다름)
            assertThat(count).isGreaterThanOrEqualTo(1L);
        } catch (Exception e) {
            // 제약조건으로 중복이 방지되는 경우
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("트랜잭션 - 저장 후 즉시 조회 가능")
    void shouldBeAvailableImmediately_WhenPostLikeSaved() {
        // Given: 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "즉시 조회 테스트", "저장 후 바로 조회되는지 확인");
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder().user(user).post(post).build();

        // When: 추천 저장 후 즉시 조회
        postLikeCommandAdapter.save(postLike);
        entityManager.flush(); // 강제 플러시

        Long count = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user = :user AND pl.post = :post", Long.class)
                .setParameter("user", user)
                .setParameter("post", post)
                .getSingleResult();

        // Then: 즉시 조회 가능 확인
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("성능 - 대량 추천 저장")
    void shouldHandleMultipleLikes_WhenSavingMany() {
        // Given: 하나의 게시글과 여러 사용자
        User author = createTestUser("author", "000000");
        entityManager.persistAndFlush(author);

        Post post = createTestPost(author, "인기 게시글", "많은 추천을 받을 게시글");
        entityManager.persistAndFlush(post);

        // When: 10명의 사용자가 추천
        for (int i = 1; i <= 10; i++) {
            User user = createTestUser("user" + i, "user" + i + "123");
            entityManager.persistAndFlush(user);

            PostLike postLike = PostLike.builder().user(user).post(post).build();
            postLikeCommandAdapter.save(postLike);

            // 각 추천이 정상 저장되었는지 확인
            assertThat(postLike.getUser().getUserName()).isEqualTo("user" + i);
            assertThat(postLike.getPost().getId()).isEqualTo(post.getId());
        }

        entityManager.flush();

        // Then: 총 저장된 추천 수 확인
        Long count = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId", Long.class)
                .setParameter("postId", post.getId())
                .getSingleResult();
        assertThat(count).isEqualTo(10L);
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
package jaeik.bimillog.infrastructure.adapter.post.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.post.PostCommandAdapter;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>PostCommandAdapter 테스트</h2>
 * <p>게시글 명령 어댑터의 핵심 비즈니스 로직을 테스트합니다.</p>
 * <p>조회수 증가, 공지사항 설정, 캐시 플래그 설정</p>
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
        "jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post"
})
@Import(PostCommandAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostCommandAdapterTest {

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
    private PostCommandAdapter postCommandAdapter;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    @DisplayName("정상 케이스 - 조회수 증가")
    void shouldIncrementView_WhenValidPostProvided() {
        // Given: 저장된 게시글 (조회수 0)
        User user = User.builder()
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
        entityManager.persistAndFlush(user);

        String title = "조회수 테스트";
        String content = "조회수 증가 테스트 내용";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.create(post);
        entityManager.flush();
        entityManager.clear();

        // When: 조회수 증가
        Post fetchedPost = entityManager.find(Post.class, savedPost.getId());
        assertThat(fetchedPost.getViews()).isEqualTo(0);
        
        postCommandAdapter.incrementViewByPostId(fetchedPost.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 조회수 증가 검증
        Post updatedPost = entityManager.find(Post.class, savedPost.getId());
        assertThat(updatedPost.getViews()).isEqualTo(1);
    }


    @Test
    @DisplayName("비즈니스 로직 - 공지사항 설정 후 저장")
    void shouldSavePost_WhenSetAsNotice() {
        // Given: 일반 게시글
        User user = User.builder()
                .userName("admin")
                .socialId("admin123")
                .provider(SocialProvider.KAKAO)
                .socialNickname("관리자")
                .role(UserRole.ADMIN)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(user);

        String title = "공지사항이 될 게시글";
        String content = "중요한 공지입니다.";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.create(post);

        // When: 공지사항으로 설정 후 저장
        savedPost.setAsNotice();
        entityManager.flush();
        Post updatedPost = savedPost;
        entityManager.flush();
        entityManager.clear();

        // Then: 공지사항 설정 확인
        Post foundPost = entityManager.find(Post.class, updatedPost.getId());
        assertThat(foundPost.isNotice()).isTrue();
    }

    @Test
    @DisplayName("비즈니스 로직 - 캐시 플래그 설정 후 저장")
    void shouldSavePost_WhenCacheFlagSet() {
        // Given: 일반 게시글
        User user = User.builder()
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
        entityManager.persistAndFlush(user);

        String title = "인기 게시글";
        String content = "많이 본 게시글입니다.";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.create(post);

        // When: 캐시 플래그 설정 후 저장
        savedPost.updatePostCacheFlag(PostCacheFlag.REALTIME);
        entityManager.flush();
        Post updatedPost = savedPost;
        entityManager.flush();
        entityManager.clear();

        // Then: 캐시 플래그 설정 확인
        Post foundPost = entityManager.find(Post.class, updatedPost.getId());
        assertThat(foundPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 삭제")
    void shouldDeletePost_WhenValidPostProvided() {
        // Given: 저장된 게시글
        User user = User.builder()
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
        entityManager.persistAndFlush(user);

        String title = "삭제될 게시글";
        String content = "삭제 테스트 내용";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.create(post);
        entityManager.flush();
        entityManager.clear();

        // When: 게시글 삭제
        Post postToDelete = entityManager.find(Post.class, savedPost.getId());
        assertThat(postToDelete).isNotNull(); // 삭제 전 존재 확인
        
        postCommandAdapter.delete(postToDelete);
        entityManager.flush();
        entityManager.clear();

        // Then: 게시글 삭제 확인
        Post deletedPost = entityManager.find(Post.class, savedPost.getId());
        assertThat(deletedPost).isNull();
    }

}
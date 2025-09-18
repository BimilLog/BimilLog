package jaeik.bimillog.infrastructure.adapter.post.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.post.PostLikeCommandAdapter;
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
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post"
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
    @DisplayName("정상 케이스 - 게시글 추천 저장")
    void shouldSave_WhenValidPostLike() {
        // Given
        User user = User.builder()
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(user);

        Post post = Post.createPost(user, "테스트 게시글", "내용", 1234);
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();

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
        User user = User.builder()
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(user);

        Post post = Post.createPost(user, "테스트 게시글", "내용", 1234);
        entityManager.persistAndFlush(post);

        PostLike postLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();
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
        User user1 = User.builder()
                .userName("user1")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("유저1")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(user1);

        User user2 = User.builder()
                .userName("user2")
                .socialId("654321")
                .provider(SocialProvider.GOOGLE)
                .socialNickname("유저2")
                .role(UserRole.USER)
                .setting(Setting.builder().build())
                .build();
        entityManager.persistAndFlush(user2);

        Post post = Post.createPost(user1, "테스트 게시글", "내용", 1234);
        entityManager.persistAndFlush(post);

        PostLike postLike1 = PostLike.builder()
                .post(post)
                .user(user1)
                .build();
        PostLike postLike2 = PostLike.builder()
                .post(post)
                .user(user2)
                .build();
        
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
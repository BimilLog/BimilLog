package jaeik.bimillog.infrastructure.outadapter.post.post;

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
 * <p>게시글 명령 어댑터의 모든 기능을 테스트합니다.</p>
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
    @DisplayName("정상 케이스 - 새 게시글 저장")
    void shouldSavePost_WhenValidPostProvided() {
        // Given: 유효한 사용자와 게시글 생성
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

        String title = "테스트 제목";
        String content = "테스트 내용입니다.";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);

        // When: 게시글 저장
        Post savedPost = postCommandAdapter.save(post);

        // Then: 저장된 게시글 검증
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("테스트 제목");
        assertThat(savedPost.getContent()).isEqualTo("테스트 내용입니다.");
        assertThat(savedPost.getUser().getUserName()).isEqualTo("testUser");
        assertThat(savedPost.getViews()).isEqualTo(0);
        assertThat(savedPost.isNotice()).isFalse();

        // 데이터베이스에서 실제 저장 확인
        Post foundPost = entityManager.find(Post.class, savedPost.getId());
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getTitle()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("정상 케이스 - 기존 게시글 수정 저장")
    void shouldUpdatePost_WhenModifiedPostProvided() {
        // Given: 기존 게시글 저장
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

        String originalTitle = "원본 제목";
        String originalContent = "원본 내용";
        Integer originalPassword = 1234;

        Post originalPost = Post.createPost(user, originalTitle, originalContent, originalPassword);
        Post savedPost = postCommandAdapter.save(originalPost);
        entityManager.flush();

        // 수정할 내용
        String updateTitle = "수정된 제목";
        String updateContent = "수정된 내용입니다.";

        savedPost.updatePost(updateTitle, updateContent);

        // When: 수정된 게시글 저장
        Post updatedPost = postCommandAdapter.save(savedPost);

        // Then: 수정 내용 검증
        assertThat(updatedPost.getId()).isEqualTo(savedPost.getId());
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정된 내용입니다.");
        
        // 데이터베이스에서 수정 확인
        entityManager.flush();
        entityManager.clear();
        Post foundPost = entityManager.find(Post.class, updatedPost.getId());
        assertThat(foundPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(foundPost.getContent()).isEqualTo("수정된 내용입니다.");
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

        String title = "삭제할 제목";
        String content = "삭제할 내용";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.save(post);
        Long postId = savedPost.getId();
        entityManager.flush();

        // When: 게시글 삭제
        postCommandAdapter.delete(savedPost);
        entityManager.flush();

        // Then: 삭제 검증
        Post deletedPost = entityManager.find(Post.class, postId);
        assertThat(deletedPost).isNull();
    }

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
        Post savedPost = postCommandAdapter.save(post);
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
    @DisplayName("정상 케이스 - 조회수 여러번 증가")
    void shouldIncrementViewMultipleTimes_WhenCalledRepeatedly() {
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

        String title = "다중 조회수 테스트";
        String content = "다중 조회수 증가 테스트";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);
        Post savedPost = postCommandAdapter.save(post);
        entityManager.flush();

        // When: 조회수 3번 증가
        postCommandAdapter.incrementViewByPostId(savedPost.getId());
        postCommandAdapter.incrementViewByPostId(savedPost.getId());
        postCommandAdapter.incrementViewByPostId(savedPost.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 조회수가 3 증가했는지 검증
        Post updatedPost = entityManager.find(Post.class, savedPost.getId());
        assertThat(updatedPost.getViews()).isEqualTo(3);
    }

    @Test
    @DisplayName("경계값 - null 사용자로 게시글 생성")
    void shouldSavePost_WhenUserIsNull() {
        // Given: null 사용자와 게시글 (익명 게시글 시나리오)
        String title = "익명 게시글";
        String content = "익명으로 작성한 게시글입니다.";
        Integer password = 1234;

        Post post = Post.createPost(null, title, content, password);

        // When: null 사용자 게시글 저장
        Post savedPost = postCommandAdapter.save(post);

        // Then: 저장 성공 검증
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getUser()).isNull();
        assertThat(savedPost.getTitle()).isEqualTo("익명 게시글");
        assertThat(savedPost.getContent()).isEqualTo("익명으로 작성한 게시글입니다.");
    }

    @Test
    @DisplayName("경계값 - 긴 제목과 내용으로 게시글 저장")
    void shouldSavePost_WhenTitleAndContentAreLong() {
        // Given: 최대 길이 제목(30자)과 긴 내용
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

        String longTitle = "a".repeat(30); // 30자 제목
        String longContent = "긴 내용입니다. ".repeat(100); // 매우 긴 내용

        Integer password = 1234;

        Post post = Post.createPost(user, longTitle, longContent, password);

        // When: 긴 제목과 내용의 게시글 저장
        Post savedPost = postCommandAdapter.save(post);

        // Then: 저장 성공 검증
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getTitle()).hasSize(30);
        assertThat(savedPost.getContent()).isNotEmpty();
        assertThat(savedPost.getContent().length()).isEqualTo(longContent.length()); // 원본과 동일해야 함
        assertThat(savedPost.getContent().length()).isGreaterThan(500); // 충분히 긴 내용 검증
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
        Post savedPost = postCommandAdapter.save(post);

        // When: 공지사항으로 설정 후 저장
        savedPost.setAsNotice();
        Post updatedPost = postCommandAdapter.save(savedPost);
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
        Post savedPost = postCommandAdapter.save(post);

        // When: 캐시 플래그 설정 후 저장
        savedPost.setPostCacheFlag(PostCacheFlag.REALTIME);
        Post updatedPost = postCommandAdapter.save(savedPost);
        entityManager.flush();
        entityManager.clear();

        // Then: 캐시 플래그 설정 확인
        Post foundPost = entityManager.find(Post.class, updatedPost.getId());
        assertThat(foundPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("예외 케이스 - 제약조건 위반 (제목 null)")
    void shouldThrowException_WhenTitleIsNull() {
        // Given: 제목이 null인 게시글
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

        // 직접 빌더로 null 제목 설정
        Post post = Post.builder()
                .user(user)
                .title(null) // NULL 제목
                .content("내용은 있음")
                .views(0)
                .isNotice(false)
                .password(1234)
                .build();

        // When & Then: 제약조건 위반 예외 발생
        assertThatThrownBy(() -> {
            postCommandAdapter.save(post);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("예외 케이스 - 제약조건 위반 (내용 null)")
    void shouldThrowException_WhenContentIsNull() {
        // Given: 내용이 null인 게시글
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

        Post post = Post.builder()
                .user(user)
                .title("제목은 있음")
                .content(null) // NULL 내용
                .views(0)
                .isNotice(false)
                .password(1234)
                .build();

        // When & Then: 제약조건 위반 예외 발생
        assertThatThrownBy(() -> {
            postCommandAdapter.save(post);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("트랜잭션 - 저장 후 즉시 조회 가능")
    void shouldBeAvailableImmediately_WhenPostSaved() {
        // Given: 새 게시글
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

        String title = "즉시 조회 테스트";
        String content = "저장 후 바로 조회되는지 확인";
        Integer password = 1234;

        Post post = Post.createPost(user, title, content, password);

        // When: 저장 후 즉시 조회
        Post savedPost = postCommandAdapter.save(post);
        entityManager.flush(); // 강제 플러시

        Post foundPost = entityManager.find(Post.class, savedPost.getId());

        // Then: 즉시 조회 가능 확인
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isEqualTo(savedPost.getId());
        assertThat(foundPost.getTitle()).isEqualTo("즉시 조회 테스트");
    }

    @Test
    @DisplayName("성능 - 대량 게시글 저장")
    void shouldHandleMultiplePosts_WhenSavingMany() {
        // Given: 사용자와 여러 게시글
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

        // When: 10개 게시글 저장
        for (int i = 1; i <= 10; i++) {
            String title = "대량 테스트 게시글 " + i;
            String content = "대량 저장 테스트 내용 " + i;
            Integer password = 1234;

            Post post = Post.createPost(user, title, content, password);
            Post savedPost = postCommandAdapter.save(post);

            // Then: 각 게시글이 정상 저장되었는지 확인
            assertThat(savedPost.getId()).isNotNull();
            assertThat(savedPost.getTitle()).contains("대량 테스트 게시글 " + i);
        }

        entityManager.flush();
        
        // 총 저장된 게시글 수 확인
        Long count = entityManager.getEntityManager()
                .createQuery("SELECT COUNT(p) FROM Post p", Long.class)
                .getSingleResult();
        assertThat(count).isEqualTo(10L);
    }
}
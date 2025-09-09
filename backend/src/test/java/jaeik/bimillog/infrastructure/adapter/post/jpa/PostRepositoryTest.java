package jaeik.bimillog.infrastructure.adapter.post.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.jpa.PostRepository;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>PostRepository 테스트</h2>
 * <p>게시글 JPA Repository의 모든 기능을 테스트합니다.</p>
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
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostRepositoryTest {

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
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("정상 케이스 - 새 게시글 저장")
    void shouldSavePost_WhenValidPostProvided() {
        // Given: 유효한 사용자와 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "테스트 제목", "테스트 내용");

        // When: 게시글 저장
        Post savedPost = postRepository.save(post);

        // Then: 저장 성공 검증
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("테스트 제목");
        assertThat(savedPost.getContent()).isEqualTo("테스트 내용");
        assertThat(savedPost.getUser().getUserName()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("정상 케이스 - ID로 게시글 조회")
    void shouldFindPost_WhenValidIdProvided() {
        // Given: 저장된 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "조회 테스트 게시글", "조회 테스트 내용");
        Post savedPost = postRepository.save(post);
        entityManager.flush();

        // When: ID로 게시글 조회
        Optional<Post> foundPost = postRepository.findById(savedPost.getId());

        // Then: 조회 성공 검증
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getId()).isEqualTo(savedPost.getId());
        assertThat(foundPost.get().getTitle()).isEqualTo("조회 테스트 게시글");
        assertThat(foundPost.get().getContent()).isEqualTo("조회 테스트 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 모든 게시글 조회")
    void shouldFindAllPosts_WhenMultiplePostsExist() {
        // Given: 여러 개의 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        for (int i = 1; i <= 5; i++) {
            Post post = createTestPost(user, "제목 " + i, "내용 " + i);
            postRepository.save(post);
        }
        entityManager.flush();

        // When: 모든 게시글 조회
        List<Post> allPosts = postRepository.findAll();

        // Then: 모든 게시글 조회 성공
        assertThat(allPosts).hasSize(5);
        assertThat(allPosts.getFirst().getTitle()).contains("제목");
        assertThat(allPosts.getFirst().getContent()).contains("내용");
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 수정")
    void shouldUpdatePost_WhenValidModificationProvided() {
        // Given: 기존 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "원본 제목", "원본 내용");
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        // When: 게시글 수정
        Post foundPost = postRepository.findById(savedPost.getId()).get();
        String updateTitle = "수정된 제목";
        String updateContent = "수정된 내용";
        foundPost.updatePost(updateTitle, updateContent);
        
        Post updatedPost = postRepository.save(foundPost);
        entityManager.flush();

        // Then: 수정 성공 검증
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 삭제")
    void shouldDeletePost_WhenValidPostProvided() {
        // Given: 저장된 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "삭제될 게시글", "삭제될 내용");
        Post savedPost = postRepository.save(post);
        Long postId = savedPost.getId();
        entityManager.flush();

        // When: 게시글 삭제
        postRepository.delete(savedPost);
        entityManager.flush();

        // Then: 삭제 검증
        Optional<Post> deletedPost = postRepository.findById(postId);
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - ID로 게시글 삭제")
    void shouldDeletePostById_WhenValidIdProvided() {
        // Given: 저장된 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "ID로 삭제될 게시글", "ID로 삭제될 내용");
        Post savedPost = postRepository.save(post);
        Long postId = savedPost.getId();
        entityManager.flush();

        // When: ID로 게시글 삭제
        postRepository.deleteById(postId);
        entityManager.flush();

        // Then: 삭제 검증
        Optional<Post> deletedPost = postRepository.findById(postId);
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 존재 여부 확인")
    void shouldCheckExistence_WhenValidIdProvided() {
        // Given: 저장된 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "존재 확인 게시글", "존재 확인 내용");
        Post savedPost = postRepository.save(post);
        entityManager.flush();

        // When: 게시글 존재 여부 확인
        boolean exists = postRepository.existsById(savedPost.getId());
        boolean notExists = postRepository.existsById(99999L);

        // Then: 존재 여부 정확히 반환
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 개수 조회")
    void shouldCountPosts_WhenMultiplePostsExist() {
        // Given: 여러 개의 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        final int POST_COUNT = 7;
        for (int i = 1; i <= POST_COUNT; i++) {
            Post post = createTestPost(user, "카운트 테스트 " + i, "카운트 내용 " + i);
            postRepository.save(post);
        }
        entityManager.flush();

        // When: 게시글 개수 조회
        long count = postRepository.count();

        // Then: 정확한 개수 반환
        assertThat(count).isEqualTo(POST_COUNT);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 ID
        Long nonExistentId = 99999L;

        // When: 존재하지 않는 ID로 조회
        Optional<Post> result = postRepository.findById(nonExistentId);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 빈 데이터베이스에서 전체 조회")
    void shouldReturnEmptyList_WhenNoPostsExist() {
        // Given: 빈 데이터베이스

        // When: 모든 게시글 조회
        List<Post> allPosts = postRepository.findAll();
        long count = postRepository.count();

        // Then: 빈 리스트와 0 카운트 반환
        assertThat(allPosts).isEmpty();
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("경계값 - null 사용자 게시글 저장")
    void shouldSavePost_WhenUserIsNull() {
        // Given: null 사용자 게시글 (익명 게시글)
        Post anonymousPost = createTestPost(null, "익명 게시글", "익명 내용");

        // When: null 사용자 게시글 저장
        Post savedPost = postRepository.save(anonymousPost);

        // Then: 저장 성공 검증
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getUser()).isNull();
        assertThat(savedPost.getTitle()).isEqualTo("익명 게시글");
    }

    @Test
    @DisplayName("비즈니스 로직 - 공지사항 플래그 유지")
    void shouldMaintainNoticeFlag_WhenPostSaved() {
        // Given: 공지사항으로 설정된 게시글
        User admin = User.builder()
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
        entityManager.persistAndFlush(admin);

        Post notice = createTestPost(admin, "공지사항", "중요한 공지입니다");
        notice.setAsNotice();

        // When: 공지사항 저장
        Post savedNotice = postRepository.save(notice);
        entityManager.flush();
        entityManager.clear();

        Post foundNotice = postRepository.findById(savedNotice.getId()).get();

        // Then: 공지사항 플래그 유지
        assertThat(foundNotice.isNotice()).isTrue();
    }

    @Test
    @DisplayName("비즈니스 로직 - 캐시 플래그 유지")
    void shouldMaintainCacheFlag_WhenPostSaved() {
        // Given: 캐시 플래그가 설정된 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "실시간 캐시 게시글", "인기 게시글입니다");
        post.updatePostCacheFlag(PostCacheFlag.REALTIME);

        // When: 캐시 플래그 게시글 저장
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        Post foundPost = postRepository.findById(savedPost.getId()).get();

        // Then: 캐시 플래그 유지
        assertThat(foundPost.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
    }

    @Test
    @DisplayName("트랜잭션 - 저장 후 즉시 조회 가능")
    void shouldBeAvailableImmediately_WhenPostSaved() {
        // Given: 새 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "즉시 조회 테스트", "저장 후 바로 조회");

        // When: 저장 후 즉시 조회
        Post savedPost = postRepository.save(post);
        entityManager.flush();

        Optional<Post> foundPost = postRepository.findById(savedPost.getId());

        // Then: 즉시 조회 가능
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getTitle()).isEqualTo("즉시 조회 테스트");
    }

    @Test
    @DisplayName("성능 - 대량 게시글 처리")
    void shouldHandleMultiplePosts_WhenProcessingMany() {
        // Given: 사용자와 대량 게시글 데이터
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        final int POST_COUNT = 50;

        // When: 대량 게시글 저장
        for (int i = 1; i <= POST_COUNT; i++) {
            Post post = createTestPost(user, "대량 테스트 " + i, "대량 내용 " + i);
            postRepository.save(post);
            
            if (i % 10 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();

        // Then: 모든 게시글 정상 저장
        long totalCount = postRepository.count();
        assertThat(totalCount).isEqualTo(POST_COUNT);

        // 조회 성능 확인
        long startTime = System.currentTimeMillis();
        List<Post> allPosts = postRepository.findAll();
        long endTime = System.currentTimeMillis();

        assertThat(allPosts).hasSize(POST_COUNT);
        assertThat(endTime - startTime).isLessThan(1000L); // 1초 이내
    }

    @Test
    @DisplayName("일관성 - 동시 수정 시나리오")
    void shouldMaintainConsistency_WhenConcurrentModifications() {
        // Given: 기존 게시글
        User user = createTestUser("testUser", "123456");
        entityManager.persistAndFlush(user);

        Post post = createTestPost(user, "동시성 테스트", "원본 내용");
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        // When: 동일한 게시글을 두 번 조회해서 수정
        Post post1 = postRepository.findById(savedPost.getId()).get();
        Post post2 = postRepository.findById(savedPost.getId()).get();

        // 첫 번째 수정
        String update1Title = "첫 번째 수정";
        String update1Content = "첫 번째 내용";
        post1.updatePost(update1Title, update1Content);
        postRepository.save(post1);
        entityManager.flush();

        // 두 번째 수정 (최신 데이터를 다시 조회해서 수정)
        Post latestPost = postRepository.findById(savedPost.getId()).get();
        String update2Title = "두 번째 수정";
        String update2Content = "두 번째 내용";
        latestPost.updatePost(update2Title, update2Content);
        postRepository.save(latestPost);
        entityManager.flush();

        // Then: 최종 수정 사항이 반영됨
        Post finalPost = postRepository.findById(savedPost.getId()).get();
        assertThat(finalPost.getTitle()).isEqualTo("두 번째 수정");
        assertThat(finalPost.getContent()).isEqualTo("두 번째 내용");
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
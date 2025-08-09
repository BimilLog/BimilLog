package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.post.user.PostUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>PostUserRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 사용자별 게시글 조회 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    jaeik.growfarm.repository.post.user.PostUserRepositoryImpl.class,
    jaeik.growfarm.repository.comment.CommentRepository.class
})
@DisplayName("PostUserRepository 통합 테스트")
class PostUserRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.use_sql_comments", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostUserRepository postUserRepository;

    private Users testUser1;
    private Users testUser2;
    private Users testUser3;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;
    private Post noticePost;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("사용자1")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("사용자2")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        testUser3 = Users.builder()
                .userName("사용자3")
                .kakaoId(11111L)
                .build();
        entityManager.persistAndFlush(testUser3);

        // testUser1이 작성한 게시글
        testPost1 = Post.builder()
                .title("사용자1의 첫 번째 게시글")
                .content("사용자1이 작성한 첫 번째 내용입니다.")
                .views(100)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost1);

        testPost2 = Post.builder()
                .title("사용자1의 두 번째 게시글")
                .content("사용자1이 작성한 두 번째 내용입니다.")
                .views(50)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost2);

        // testUser2가 작성한 게시글
        testPost3 = Post.builder()
                .title("사용자2의 게시글")
                .content("사용자2가 작성한 내용입니다.")
                .views(200)
                .isNotice(false)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(testPost3);

        // 공지사항 (testUser1이 작성했지만 isNotice=true)
        noticePost = Post.builder()
                .title("공지사항")
                .content("중요한 공지사항입니다.")
                .views(300)
                .isNotice(true)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(noticePost);

        // 좋아요 생성
        // testUser2가 testPost1에 좋아요
        PostLike like1 = PostLike.builder()
                .post(testPost1)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(like1);

        // testUser2가 testPost3에 좋아요 (자신의 글에 좋아요)
        PostLike like2 = PostLike.builder()
                .post(testPost3)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(like2);

        // testUser1이 testPost3에 좋아요
        PostLike like3 = PostLike.builder()
                .post(testPost3)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(like3);

        entityManager.clear();
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 성공")
    void findPostsByUserId_Success() {
        // Given
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 공지사항 제외하고 일반 게시글 2개
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        // 모든 게시글이 testUser1이 작성한 것인지 확인
        result.getContent().forEach(post -> {
            assertThat(post.getUserName()).isEqualTo("사용자1");
            assertThat(post.getUserId()).isEqualTo(userId);
            assertThat(post.is_notice()).isFalse(); // 공지사항 제외 확인
        });

        // 최신순 정렬 확인
        if (result.getContent().size() > 1) {
            for (int i = 0; i < result.getContent().size() - 1; i++) {
                SimplePostDTO current = result.getContent().get(i);
                SimplePostDTO next = result.getContent().get(i + 1);
                assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
            }
        }
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 빈 결과")
    void findPostsByUserId_EmptyResult() {
        // Given
        Long userId = testUser3.getId(); // 게시글을 작성하지 않은 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자가 추천한 글 목록 조회 - 성공")
    void findLikedPostsByUserId_Success() {
        // Given
        Long userId = testUser2.getId(); // testPost1, testPost3에 좋아요를 누른 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 2개 게시글에 좋아요
        assertThat(result.getTotalElements()).isEqualTo(2);

        // 좋아요한 게시글들인지 확인
        boolean hasPost1 = result.getContent().stream()
                .anyMatch(post -> post.getTitle().equals("사용자1의 첫 번째 게시글"));
        boolean hasPost3 = result.getContent().stream()
                .anyMatch(post -> post.getTitle().equals("사용자2의 게시글"));
        
        assertThat(hasPost1).isTrue();
        assertThat(hasPost3).isTrue();
    }

    @Test
    @DisplayName("사용자가 추천한 글 목록 조회 - 추천한 글 없음")
    void findLikedPostsByUserId_NoLikedPosts() {
        // Given
        Long userId = testUser3.getId(); // 좋아요를 누르지 않은 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자 작성 글 목록 조회 - 페이징 확인")
    void findPostsByUserId_Paging() {
        // Given - testUser1에게 추가 게시글 생성
        for (int i = 3; i <= 7; i++) {
            Post additionalPost = Post.builder()
                    .title("사용자1의 " + i + "번째 게시글")
                    .content("추가 게시글 내용 " + i)
                    .views(10 * i)
                    .isNotice(false)
                    .user(testUser1)
                    .build();
            entityManager.persistAndFlush(additionalPost);
        }
        entityManager.clear();

        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 3); // 첫 페이지, 3개씩

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(7); // 총 7개 (공지사항 제외)
        assertThat(result.getTotalPages()).isEqualTo(3); // 7 ÷ 3 = 3페이지
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자가 추천한 글 조회 - 다른 사용자의 글만 포함")
    void findLikedPostsByUserId_ContainsOtherUsersPostsOnly() {
        // Given
        Long userId = testUser1.getId(); // testPost3에 좋아요를 누른 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // testPost3에만 좋아요

        SimplePostDTO likedPost = result.getContent().get(0);
        assertThat(likedPost.getTitle()).isEqualTo("사용자2의 게시글");
        assertThat(likedPost.getUserName()).isEqualTo("사용자2"); // 다른 사용자가 작성
        assertThat(likedPost.getUserId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("사용자 작성 글 조회 - 공지글 제외 확인")
    void findPostsByUserId_ExcludesNotices() {
        // Given
        Long userId = testUser1.getId(); // 공지사항도 작성한 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        
        // 공지사항이 결과에 포함되지 않았는지 확인
        boolean hasNoticePost = result.getContent().stream()
                .anyMatch(post -> post.getTitle().equals("공지사항"));
        assertThat(hasNoticePost).isFalse();

        // 모든 게시글이 일반 게시글인지 확인
        result.getContent().forEach(post -> 
            assertThat(post.is_notice()).isFalse()
        );
    }

    @Test
    @DisplayName("좋아요 수와 댓글 수 확인")
    void findPostsByUserId_WithLikeAndCommentCounts() {
        // Given
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        
        // testPost1에 대한 좋아요 수 확인 (testUser2가 좋아요)
        SimplePostDTO post1 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("사용자1의 첫 번째 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post1.getLikes()).isEqualTo(1);
        
        // testPost2에 대한 좋아요 수 확인 (좋아요 없음)
        SimplePostDTO post2 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("사용자1의 두 번째 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post2.getLikes()).isEqualTo(0);
    }
}
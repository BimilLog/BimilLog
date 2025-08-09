package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.post.read.PostReadRepository;
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
 * <h2>PostReadRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 게시글 조회 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    jaeik.growfarm.repository.post.read.PostReadRepositoryImpl.class,
    jaeik.growfarm.repository.comment.CommentRepository.class
})
@DisplayName("PostReadRepository 통합 테스트")
class PostReadRepositoryIntegrationTest {

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
    private PostReadRepository postReadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Users testUser1;
    private Users testUser2;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("testUser1")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("testUser2")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        // 테스트 게시글 생성
        testPost1 = Post.builder()
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글 내용입니다.")
                .views(100)
                .isNotice(false)
                .popularFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost1);

        testPost2 = Post.builder()
                .title("두 번째 테스트 게시글")
                .content("두 번째 게시글 내용입니다.")
                .views(200)
                .isNotice(false)
                .popularFlag(PopularFlag.WEEKLY)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(testPost2);

        testPost3 = Post.builder()
                .title("공지사항")
                .content("공지사항 내용입니다.")
                .views(50)
                .isNotice(true)
                .popularFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost3);

        // 좋아요 생성 (testPost1에 testUser2가 좋아요)
        PostLike postLike = PostLike.builder()
                .post(testPost1)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(postLike);

        entityManager.clear();
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void findPostsWithCommentAndLikeCounts_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postReadRepository.findPostsWithCommentAndLikeCounts(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 공지사항 포함 3개
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // 최신순 정렬 확인
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        
        // 좋아요 수 확인 (testPost1에 1개 좋아요)
        SimplePostDTO firstPost = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(firstPost.getLikes()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글 목록 조회 - 빈 결과")
    void findPostsWithCommentAndLikeCounts_EmptyResult() {
        // Given - 모든 게시글 삭제
        entityManager.getEntityManager().createQuery("DELETE FROM PostLike").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Post").executeUpdate();
        entityManager.flush();
        
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostDTO> result = postReadRepository.findPostsWithCommentAndLikeCounts(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (로그인 사용자)")
    void findPostById_Success_LoggedInUser() {
        // Given
        Long postId = testPost1.getId();
        Long userId = testUser2.getId(); // 좋아요를 누른 사용자

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("첫 번째 테스트 게시글");
        assertThat(result.getContent()).isEqualTo("첫 번째 게시글 내용입니다.");
        assertThat(result.getViews()).isEqualTo(100);
        assertThat(result.getUserName()).isEqualTo("testUser1");
        assertThat(result.isUserLike()).isTrue(); // testUser2가 좋아요를 누름
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (비로그인 사용자)")
    void findPostById_Success_NonLoggedInUser() {
        // Given
        Long postId = testPost2.getId();
        Long userId = null; // 비로그인

        // When
        PostDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("두 번째 테스트 게시글");
        assertThat(result.getUserName()).isEqualTo("testUser2");
        assertThat(result.isUserLike()).isFalse(); // 비로그인 사용자는 좋아요 없음
    }

    @Test
    @DisplayName("게시글 상세 조회 - 게시글 없음")
    void findPostById_PostNotFound() {
        // Given
        Long nonExistentPostId = 99999L;
        Long userId = testUser1.getId();

        // When
        PostDTO result = postReadRepository.findPostById(nonExistentPostId, userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 처리")
    void findPostById_AnonymousUser() {
        // Given - 익명 사용자 게시글 생성 (userName이 null인 경우)
        Users anonymousUser = Users.builder()
                .userName(null) // 익명
                .kakaoId(99999L)
                .build();
        entityManager.persistAndFlush(anonymousUser);

        Post anonymousPost = Post.builder()
                .title("익명 게시글")
                .content("익명 사용자의 게시글입니다.")
                .views(10)
                .isNotice(false)
                .user(anonymousUser)
                .build();
        entityManager.persistAndFlush(anonymousPost);
        entityManager.clear();

        // When
        PostDTO result = postReadRepository.findPostById(anonymousPost.getId(), testUser1.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("익명");
        assertThat(result.getTitle()).isEqualTo("익명 게시글");
    }

    @Test
    @DisplayName("페이징 테스트")
    void findPostsWithCommentAndLikeCounts_Paging() {
        // Given
        Pageable pageable = PageRequest.of(0, 2); // 첫 번째 페이지, 2개씩

        // When
        Page<SimplePostDTO> result = postReadRepository.findPostsWithCommentAndLikeCounts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2); // 3개 ÷ 2 = 2페이지
        assertThat(result.getNumber()).isEqualTo(0); // 첫 번째 페이지
        assertThat(result.hasNext()).isTrue();
    }
}
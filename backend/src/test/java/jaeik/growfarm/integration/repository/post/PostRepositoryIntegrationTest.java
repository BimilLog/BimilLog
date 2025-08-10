package jaeik.growfarm.integration.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.read.PostReadRepositoryImpl;
import jaeik.growfarm.repository.post.user.PostUserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>Post Repository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 게시글 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostRepositoryIntegrationTest.TestConfig.class)
@DisplayName("Post Repository 통합 테스트")
class PostRepositoryIntegrationTest {

    @Configuration
    static class TestConfig {
        
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
        
        @Bean
        public PostReadRepositoryImpl postReadRepository(JPAQueryFactory jpaQueryFactory, CommentRepository commentRepository) {
            return new PostReadRepositoryImpl(jpaQueryFactory, commentRepository);
        }
        
        @Bean 
        public PostUserRepositoryImpl postUserRepository(JPAQueryFactory jpaQueryFactory, CommentRepository commentRepository) {
            return new PostUserRepositoryImpl(jpaQueryFactory, commentRepository);
        }
    }

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
    private CommentRepository commentRepository;
    
    @Autowired
    private PostReadRepositoryImpl postReadRepository;
    
    @Autowired
    private PostUserRepositoryImpl postUserRepository;

    private Users testUser1;
    private Users testUser2;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("테스트사용자1")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("테스트사용자2")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        // 테스트 게시글 생성
        testPost1 = Post.builder()
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글 내용입니다.")
                .views(100)
                .isNotice(false)
                .postCacheFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost1);

        testPost2 = Post.builder()
                .title("두 번째 테스트 게시글")
                .content("두 번째 게시글 내용입니다.")
                .views(200)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(testPost2);

        testPost3 = Post.builder()
                .title("공지사항")
                .content("공지사항 내용입니다.")
                .views(50)
                .isNotice(true)
                .postCacheFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost3);

        // 좋아요 생성 (testPost1에 testUser2가 좋아요)
        PostLike postLike = PostLike.builder()
                .post(testPost1)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(postLike);

        // 댓글 생성 (testPost1에 1개 댓글)
        Comment comment = Comment.builder()
                .content("테스트 댓글입니다.")
                .post(testPost1)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(comment);

        entityManager.clear();
    }

    @Test
    @DisplayName("PostReadRepository - 게시글 목록 조회 성공")
    void postReadRepository_findSimplePost_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 공지사항 포함 3개
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // 좋아요 수 확인 (testPost1에 1개 좋아요)
        SimplePostResDTO firstPost = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(firstPost.getLikes()).isEqualTo(1);
        assertThat(firstPost.getCommentCount()).isEqualTo(1);
        
        System.out.println("=== 게시글 목록 조회 쿼리 실행 성공 ===");
        result.getContent().forEach(post -> 
            System.out.println(String.format("제목: %s, 좋아요: %d, 댓글: %d, 작성자: %s", 
                post.getTitle(), post.getLikes(), post.getCommentCount(), post.getUserName()))
        );
    }

    @Test
    @DisplayName("PostReadRepository - 게시글 상세 조회 성공")
    void postReadRepository_findPostById_Success() {
        // Given
        Long postId = testPost1.getId();
        Long userId = testUser2.getId(); // 좋아요를 누른 사용자

        // When
        FullPostResDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("첫 번째 테스트 게시글");
        assertThat(result.getContent()).isEqualTo("첫 번째 게시글 내용입니다.");
        assertThat(result.isUserLike()).isTrue(); // testUser2가 좋아요를 누름
        
        System.out.println("=== 게시글 상세 조회 쿼리 실행 성공 ===");
        System.out.println(String.format("제목: %s, 내용: %s, 사용자 좋아요: %b", 
            result.getTitle(), result.getContent(), result.isUserLike()));
    }

    @Test
    @DisplayName("PostUserRepository - 사용자 작성글 조회 성공")
    void postUserRepository_findPostsByUserId_Success() {
        // Given
        Long userId = testUser1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postUserRepository.findPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // 공지사항 제외하고 일반 게시글 1개
        assertThat(result.getTotalElements()).isEqualTo(1);
        
        SimplePostResDTO post = result.getContent().get(0);
        assertThat(post.getUserName()).isEqualTo("테스트사용자1");
        assertThat(post.isNotice()).isFalse(); // 공지사항 제외 확인
        
        System.out.println("=== 사용자 작성글 조회 쿼리 실행 성공 ===");
        System.out.println(String.format("사용자 %s의 작성글 %d개", post.getUserName(), result.getTotalElements()));
    }

    @Test
    @DisplayName("PostUserRepository - 사용자 좋아요한 글 조회 성공")
    void postUserRepository_findLikedPostsByUserId_Success() {
        // Given
        Long userId = testUser2.getId(); // testPost1에 좋아요를 누른 사용자
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postUserRepository.findLikedPostsByUserId(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // 1개 게시글에 좋아요
        assertThat(result.getTotalElements()).isEqualTo(1);

        SimplePostResDTO likedPost = result.getContent().get(0);
        assertThat(likedPost.getTitle()).isEqualTo("첫 번째 테스트 게시글");
        
        System.out.println("=== 사용자 좋아요한 글 조회 쿼리 실행 성공 ===");
        System.out.println(String.format("좋아요한 글: %s", likedPost.getTitle()));
    }

    @Test
    @DisplayName("복합 쿼리 테스트 - 좋아요와 댓글 수 집계")
    void complexQueryTest_LikeAndCommentCounts() {
        // Given - 추가 데이터 생성
        Users additionalUser = Users.builder()
                .userName("추가사용자")
                .kakaoId(99999L)
                .build();
        entityManager.persistAndFlush(additionalUser);

        // testPost1에 추가 좋아요와 댓글
        PostLike additionalLike = PostLike.builder()
                .post(testPost1)
                .user(additionalUser)
                .build();
        entityManager.persistAndFlush(additionalLike);

        Comment additionalComment = Comment.builder()
                .content("추가 댓글입니다.")
                .post(testPost1)
                .user(additionalUser)
                .build();
        entityManager.persistAndFlush(additionalComment);

        entityManager.clear();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        SimplePostResDTO targetPost = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();

        assertThat(targetPost.getLikes()).isEqualTo(2); // 2개 좋아요
        assertThat(targetPost.getCommentCount()).isEqualTo(2); // 2개 댓글
        
        System.out.println("=== 복합 쿼리 테스트 - 집계 기능 확인 ===");
        System.out.println(String.format("게시글: %s, 좋아요: %d개, 댓글: %d개", 
            targetPost.getTitle(), targetPost.getLikes(), targetPost.getCommentCount()));
        
        // 실제 쿼리 로그 확인을 위해 모든 게시글 출력
        System.out.println("=== 전체 게시글 목록과 집계 결과 ===");
        result.getContent().forEach(post ->
            System.out.println(String.format("[%d] %s - 좋아요: %d, 댓글: %d, 조회수: %d, 작성자: %s",
                post.getPostId(), post.getTitle(), post.getLikes(), 
                post.getCommentCount(), post.getViews(), post.getUserName()))
        );
    }

    @Test
    @DisplayName("쿼리 성능 및 N+1 문제 확인")
    void queryPerformanceTest() {
        // Given - 대량 데이터 생성
        Users[] users = new Users[3];
        for (int i = 0; i < 3; i++) {
            users[i] = Users.builder()
                    .userName("성능테스트사용자" + i)
                    .kakaoId(100000L + i)
                    .build();
            entityManager.persistAndFlush(users[i]);
        }

        for (int i = 0; i < 5; i++) {
            Post post = Post.builder()
                    .title("성능테스트 게시글 " + i)
                    .content("성능 테스트용 게시글 내용 " + i)
                    .views(i * 10)
                    .isNotice(false)
                    .user(users[i % 3])
                    .build();
            entityManager.persistAndFlush(post);
            
            // 각 게시글에 좋아요와 댓글 추가
            for (int j = 0; j < 2; j++) {
                PostLike like = PostLike.builder()
                        .post(post)
                        .user(users[j % 3])
                        .build();
                entityManager.persistAndFlush(like);
                
                Comment comment = Comment.builder()
                        .content("성능 테스트 댓글 " + j)
                        .post(post)
                        .user(users[j % 3])
                        .build();
                entityManager.persistAndFlush(comment);
            }
        }

        entityManager.clear();

        // When - 성능 측정
        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, 20);
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(5);
        
        System.out.println("=== 쿼리 성능 테스트 결과 ===");
        System.out.println(String.format("조회 시간: %d ms", endTime - startTime));
        System.out.println(String.format("조회된 게시글 수: %d개", result.getContent().size()));
        System.out.println("=== 쿼리 최적화 확인 (로그에서 실행된 쿼리 수 확인) ===");
        
        // 각 게시글의 집계 결과가 올바른지 확인
        result.getContent().forEach(post -> {
            if (post.getTitle().startsWith("성능테스트 게시글")) {
                assertThat(post.getLikes()).isEqualTo(2);
                assertThat(post.getCommentCount()).isEqualTo(2);
            }
        });
    }
}
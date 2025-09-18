package jaeik.bimillog.infrastructure.adapter.out.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentLikeRepository;
import jaeik.bimillog.testutil.TestUserFactory;
import jaeik.bimillog.testutil.TestSettingFactory;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.security.EncryptionUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>CommentQueryAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentQueryAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 조회 동작 검증</p>
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
        "jaeik.bimillog.domain.admin.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.paper.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.notification.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment",
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentlike",
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure"
})
@Import(CommentQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.properties.hibernate.listeners.auto-registration=true"
})
class CommentQueryAdapterIntegrationTest {

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
        
        @Bean
        public EncryptionUtil encryptionUtil() {
            // 테스트용 암호화 유틸리티 (Mock) - JPA Converter에서 필요
            return org.mockito.Mockito.mock(EncryptionUtil.class);
        }
        
        // CommentReadRepository는 더 이상 사용하지 않음 - CommentQueryAdapter가 직접 QueryDSL 사용
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentQueryAdapter commentQueryAdapter;

    private User testUser1;
    private User testUser2;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentLikeRepository.deleteAll();
        commentRepository.deleteAll();
        
        // 테스트용 사용자들 생성
        Setting setting1 = TestSettingFactory.createDefaultSetting();
        entityManager.persistAndFlush(setting1);

        testUser1 = TestUserFactory.builder()
                .withSocialId("kakao123")
                .withUserName("testUser1")
                .withSocialNickname("테스트유저1")
                .withSetting(setting1)
                .build();
        entityManager.persistAndFlush(testUser1);
        
        Setting setting2 = TestSettingFactory.createDefaultSetting();
        entityManager.persistAndFlush(setting2);

        testUser2 = TestUserFactory.builder()
                .withSocialId("kakao456")
                .withUserName("testUser2")
                .withSocialNickname("테스트유저2")
                .withSetting(setting2)
                .build();
        entityManager.persistAndFlush(testUser2);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser1)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);
    }

    @Test
    @DisplayName("정상 케이스 - ID로 댓글 조회")
    void shouldFindCommentById_WhenValidIdProvided() {
        // Given: 저장된 댓글
        Comment savedComment = Comment.createComment(testPost, testUser1, "테스트 댓글", null);
        savedComment = commentRepository.save(savedComment);

        // When: ID로 댓글 조회
        Comment foundComment = commentQueryAdapter.findById(savedComment.getId());

        // Then: 올바른 댓글이 조회되는지 검증
        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(savedComment.getId());
        assertThat(foundComment.getContent()).isEqualTo("테스트 댓글");
        assertThat(foundComment.getUser()).isEqualTo(testUser1);
        assertThat(foundComment.getPost()).isEqualTo(testPost);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 댓글 조회")
    void shouldThrowException_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentId = 999L;

        // When & Then: 존재하지 않는 ID로 댓글 조회 시 예외 발생
        assertThatThrownBy(() -> commentQueryAdapter.findById(nonExistentId))
                .isInstanceOf(CommentCustomException.class);
    }




    @Test
    @DisplayName("정상 케이스 - 사용자 작성 댓글 목록 조회")
    void shouldFindCommentsByUserId_WhenValidUserIdProvided() {
        // Given: 특정 사용자의 여러 댓글
        Comment comment1 = Comment.createComment(testPost, testUser1, "사용자1 댓글1", null);
        Comment comment2 = Comment.createComment(testPost, testUser1, "사용자1 댓글2", null);
        Comment comment3 = Comment.createComment(testPost, testUser2, "사용자2 댓글1", null);
        
        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 특정 사용자의 댓글 조회
        Page<SimpleCommentInfo> userComments = commentQueryAdapter
                .findCommentsByUserId(testUser1.getId(), pageable);

        // Then: 해당 사용자의 댓글만 조회되는지 검증
        assertThat(userComments).isNotNull();
        assertThat(userComments.getContent()).hasSize(2);
        assertThat(userComments.getContent().get(0).getContent()).contains("사용자1");
        assertThat(userComments.getContent().get(1).getContent()).contains("사용자1");
    }

    @Test  
    @DisplayName("정상 케이스 - 사용자 추천한 댓글 목록 조회")
    void shouldFindLikedCommentsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자가 추천한 댓글들
        Comment comment1 = Comment.createComment(testPost, testUser1, "댓글1", null);
        Comment comment2 = Comment.createComment(testPost, testUser1, "댓글2", null);
        
        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // testUser2가 추천
        CommentLike like1 = CommentLike.builder()
                .comment(comment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like1);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자가 추천한 댓글 조회
        Page<SimpleCommentInfo> likedComments = commentQueryAdapter
                .findLikedCommentsByUserId(testUser2.getId(), pageable);

        // Then: 추천한 댓글들이 조회되는지 검증
        assertThat(likedComments).isNotNull();
        assertThat(likedComments.getContent()).hasSize(1);
        assertThat(likedComments.getContent().getFirst().getContent()).isEqualTo("댓글1");
    }

    @Test
    @DisplayName("정상 케이스 - 인기 댓글 목록 조회")
    void shouldFindPopularComments_WhenValidPostIdProvided() {
        // Given: 게시글의 여러 댓글과 추천 (인기 댓글 조건: 5개 이상)
        Comment comment1 = Comment.createComment(testPost, testUser1, "인기댓글1", null);
        comment1 = commentRepository.save(comment1);

        // 5개 이상의 추천 생성 (인기 댓글 조건 충족)
        for (int i = 0; i < 6; i++) {
            Setting setting = TestSettingFactory.createDefaultSetting();
            entityManager.persistAndFlush(setting);

            User likeUser = TestUserFactory.builder()
                    .withSocialId("kakao" + (1000 + i))
                    .withUserName("likeUser" + i)
                    .withSocialNickname("추천유저" + i)
                    .withSetting(setting)
                    .build();
            entityManager.persistAndFlush(likeUser);
            
            CommentLike like = CommentLike.builder()
                    .comment(comment1)
                    .user(likeUser)
                    .build();
            commentLikeRepository.save(like);
        }

        // testUser2가 comment1에 추천 - 사용자 추천 여부 테스트용
        CommentLike userLike = CommentLike.builder()
                .comment(comment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(userLike);

        // When: 인기 댓글 조회 (testUser2 관점에서)
        List<CommentInfo> popularComments = commentQueryAdapter
                .findPopularComments(testPost.getId(), testUser2.getId());

        // Then: 인기 댓글들이 조회되는지 검증
        assertThat(popularComments).isNotNull();
        assertThat(popularComments).hasSize(1);
        
        CommentInfo popularComment = popularComments.getFirst();
        assertThat(popularComment.getContent()).isEqualTo("인기댓글1");
        assertThat(popularComment.isPopular()).isTrue();
        assertThat(popularComment.getLikeCount()).isEqualTo(7); // 6 + testUser2의 추천
        assertThat(popularComment.isUserLike()).isTrue(); // 단일 쿼리로 사용자 추천 여부 검증
    }

    @Test
    @DisplayName("정상 케이스 - 인기 댓글 조회 시 사용자 추천 여부 검증 (추천하지 않은 경우)")
    void shouldFindPopularComments_WithUserLikeFalse_WhenUserDidNotLike() {
        // Given: 인기 댓글과 추천하지 않은 사용자
        Comment comment1 = Comment.createComment(testPost, testUser1, "인기댓글1", null);
        comment1 = commentRepository.save(comment1);

        // 3개 이상의 추천 생성 (다른 사용자들이 추천)
        for (int i = 0; i < 4; i++) {
            Setting setting = TestSettingFactory.createDefaultSetting();
            entityManager.persistAndFlush(setting);

            User likeUser = TestUserFactory.builder()
                    .withSocialId("kakao" + (2000 + i))
                    .withUserName("likeUser" + i)
                    .withSocialNickname("추천유저" + i)
                    .withSetting(setting)
                    .build();
            entityManager.persistAndFlush(likeUser);
            
            CommentLike like = CommentLike.builder()
                    .comment(comment1)
                    .user(likeUser)
                    .build();
            commentLikeRepository.save(like);
        }

        // When: 인기 댓글 조회 (testUser2는 추천하지 않음)
        List<CommentInfo> popularComments = commentQueryAdapter
                .findPopularComments(testPost.getId(), testUser2.getId());

        // Then: 사용자 추천 여부가 false로 설정되는지 검증
        assertThat(popularComments).isNotNull();
        assertThat(popularComments).hasSize(1);
        
        CommentInfo popularComment = popularComments.getFirst();
        assertThat(popularComment.getContent()).isEqualTo("인기댓글1");
        assertThat(popularComment.isPopular()).isTrue();
        assertThat(popularComment.getLikeCount()).isEqualTo(4);
        assertThat(popularComment.isUserLike()).isFalse(); // 사용자가 추천하지 않은 경우
    }

    @Test
    @DisplayName("정상 케이스 - 과거순 댓글 목록 조회") 
    void shouldFindCommentsWithOldestOrder_WhenValidPostIdProvided() {
        // Given: 게시글의 여러 댓글들
        Comment comment1 = Comment.createComment(testPost, testUser1, "첫번째 댓글", null);
        Comment comment2 = Comment.createComment(testPost, testUser1, "두번째 댓글", null);
        Comment comment3 = Comment.createComment(testPost, testUser2, "세번째 댓글", null);


        commentRepository.save(comment1);
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        commentRepository.save(comment2);
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        commentRepository.save(comment3);

        // testUser2가 comment2에만 추천 - 사용자 추천 여부 테스트용
        CommentLike userLike = CommentLike.builder()
                .comment(comment2)
                .user(testUser2)
                .build();
        commentLikeRepository.save(userLike);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 과거순 댓글 조회 (testUser2 관점에서)
        Page<CommentInfo> oldestComments = commentQueryAdapter
                .findCommentsWithOldestOrder(testPost.getId(), pageable, testUser2.getId());

        // Then: 과거순으로 댓글들이 조회되고 사용자 추천 여부가 올바르게 설정되는지 검증
        assertThat(oldestComments).isNotNull();
        assertThat(oldestComments.getContent()).hasSize(3);
        
        List<CommentInfo> comments = oldestComments.getContent();
        // 과거순 정렬 검증 (첫번째 댓글이 가장 먼저)
        assertThat(comments.getFirst().getContent()).isEqualTo("첫번째 댓글");
        assertThat(comments.getFirst().isUserLike()).isFalse(); // testUser2가 추천하지 않음
        
        assertThat(comments.get(1).getContent()).isEqualTo("두번째 댓글");
        assertThat(comments.get(1).isUserLike()).isTrue(); // testUser2가 추천함
        assertThat(comments.get(1).getLikeCount()).isEqualTo(1);
        
        assertThat(comments.get(2).getContent()).isEqualTo("세번째 댓글");
        assertThat(comments.get(2).isUserLike()).isFalse(); // testUser2가 추천하지 않음
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID 목록에 대한 댓글 수 조회")
    void shouldFindCommentCountsByPostIds_WhenValidPostIdsProvided() {
        // Given: 여러 게시글과 각각의 댓글들
        Post post2 = Post.builder()
                .user(testUser1)
                .title("두 번째 게시글")
                .content("두 번째 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(post2);
        
        Post post3 = Post.builder()
                .user(testUser2)
                .title("세 번째 게시글")
                .content("세 번째 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(post3);

        // testPost에 댓글 3개 생성
        Comment comment1 = Comment.createComment(testPost, testUser1, "첫 번째 게시글 댓글1", null);
        Comment comment2 = Comment.createComment(testPost, testUser2, "첫 번째 게시글 댓글2", null);
        Comment comment3 = Comment.createComment(testPost, testUser1, "첫 번째 게시글 댓글3", null);
        
        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        // post2에 댓글 2개 생성
        Comment comment4 = Comment.createComment(post2, testUser1, "두 번째 게시글 댓글1", null);
        Comment comment5 = Comment.createComment(post2, testUser2, "두 번째 게시글 댓글2", null);
        
        commentRepository.save(comment4);
        commentRepository.save(comment5);

        // post3에는 댓글 없음
        
        List<Long> postIds = List.of(testPost.getId(), post2.getId(), post3.getId());

        // When: 게시글 ID 목록에 대한 댓글 수 조회
        Map<Long, Integer> commentCounts = commentQueryAdapter.findCommentCountsByPostIds(postIds);

        // Then: 각 게시글별 댓글 수가 올바르게 반환되는지 검증
        assertThat(commentCounts).isNotNull();
        assertThat(commentCounts).hasSize(2); // 댓글이 있는 게시글 2개만 포함
        assertThat(commentCounts.get(testPost.getId())).isEqualTo(3);
        assertThat(commentCounts.get(post2.getId())).isEqualTo(2);
        assertThat(commentCounts.get(post3.getId())).isNull(); // 댓글이 없는 게시글은 맵에 포함되지 않음
    }

    @Test
    @DisplayName("정상 케이스 - 빈 게시글 ID 목록으로 댓글 수 조회")
    void shouldReturnEmptyMap_WhenEmptyPostIdsProvided() {
        // Given: 빈 게시글 ID 목록
        List<Long> emptyPostIds = List.of();

        // When: 빈 목록으로 댓글 수 조회
        Map<Long, Integer> commentCounts = commentQueryAdapter.findCommentCountsByPostIds(emptyPostIds);

        // Then: 빈 맵이 반환되어야 함
        assertThat(commentCounts).isNotNull();
        assertThat(commentCounts).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 게시글 ID로 댓글 수 조회")
    void shouldReturnEmptyMap_WhenNonExistentPostIdsProvided() {
        // Given: 존재하지 않는 게시글 ID들
        List<Long> nonExistentPostIds = List.of(999L, 998L, 997L);

        // When: 존재하지 않는 게시글 ID로 댓글 수 조회
        Map<Long, Integer> commentCounts = commentQueryAdapter.findCommentCountsByPostIds(nonExistentPostIds);

        // Then: 빈 맵이 반환되어야 함
        assertThat(commentCounts).isNotNull();
        assertThat(commentCounts).isEmpty();
    }

    @Test
    @DisplayName("트랜잭션 - 복합 쿼리 테스트")
    void shouldHandleComplexQueries_WhenMultipleOperationsPerformed() {
        // Given: 복잡한 테스트 데이터 설정
        Comment comment1 = Comment.createComment(testPost, testUser1, "복합쿼리 댓글1", null);
        Comment comment2 = Comment.createComment(testPost, testUser2, "복합쿼리 댓글2", null);
        
        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // testUser1이 comment2에 추천
        CommentLike like = CommentLike.builder()
                .comment(comment2)
                .user(testUser1)
                .build();
        commentLikeRepository.save(like);

        // When & Then: 여러 쿼리 연속 실행
        
        // 1. ID로 댓글 조회
        Comment foundComment1 = commentQueryAdapter.findById(comment1.getId());
        assertThat(foundComment1).isNotNull();
        assertThat(foundComment1.getContent()).isEqualTo("복합쿼리 댓글1");

        // 2. 댓글 수 조회
        Map<Long, Integer> commentCounts = commentQueryAdapter.findCommentCountsByPostIds(List.of(testPost.getId()));
        assertThat(commentCounts.get(testPost.getId())).isEqualTo(2);

        // 3. 존재하지 않는 댓글 조회
        assertThatThrownBy(() -> commentQueryAdapter.findById(999L))
                .isInstanceOf(CommentCustomException.class);
    }
}
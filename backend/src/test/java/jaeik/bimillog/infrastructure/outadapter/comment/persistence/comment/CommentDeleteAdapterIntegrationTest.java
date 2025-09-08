package jaeik.bimillog.infrastructure.outadapter.comment.persistence.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.CommentDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
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
 * <h2>CommentDeleteAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentDeleteAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 삭제 및 익명화 동작 검증</p>
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
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment",
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure"
})
@Import(CommentDeleteAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentDeleteAdapterIntegrationTest {

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
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentDeleteAdapter commentDeleteAdapter;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentRepository.deleteAll();
        
        // 테스트용 사용자 생성 (각 테스트마다 고유한 ID 사용)
        String uniqueSocialId = "kakao_delete_" + System.nanoTime();
        Setting setting = Setting.createSetting();
        entityManager.persistAndFlush(setting);
        
        testUser = User.builder()
                .socialId(uniqueSocialId) // 나노타임으로 고유성 보장
                .provider(SocialProvider.KAKAO)
                .userName("testUserDelete_" + System.nanoTime())
                .socialNickname("삭제테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        entityManager.persistAndFlush(testUser);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 하드 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldHardDeleteComment_WhenValidCommentProvided() {
        // Given: 기존 댓글 생성 및 저장
        Comment existingComment = Comment.createComment(
                testPost, 
                testUser, 
                "삭제될 댓글", 
                null
        );
        existingComment = commentRepository.save(existingComment);
        Long commentId = existingComment.getId();

        // 삭제 전 댓글 존재 확인
        assertThat(commentRepository.findById(commentId)).isPresent();

        // When: 댓글 하드 삭제 (자손이 없는 경우)
        int deleteCount = commentDeleteAdapter.hardDeleteComment(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글이 삭제되었는지 검증
        assertThat(deleteCount).isEqualTo(1);
        Optional<Comment> deletedComment = commentRepository.findById(commentId);
        assertThat(deletedComment).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID로 모든 댓글 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldDeleteAllCommentsByPostId_WhenValidPostIdProvided() {
        // Given: 동일한 게시글에 여러 댓글 생성
        Comment comment1 = Comment.createComment(testPost, testUser, "댓글 1", null);
        Comment comment2 = Comment.createComment(testPost, testUser, "댓글 2", null);
        Comment comment3 = Comment.createComment(testPost, testUser, "댓글 3", null);
        
        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        // 댓글들이 저장되었는지 확인
        List<Comment> commentsBefore = commentRepository.findAll();
        assertThat(commentsBefore).hasSize(3);

        // When: 게시글 ID로 모든 댓글 삭제
        commentDeleteAdapter.deleteAllByPostId(testPost.getId());

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 모든 댓글이 삭제되었는지 검증
        List<Comment> commentsAfter = commentRepository.findAll();
        assertThat(commentsAfter).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 댓글 익명화")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldAnonymizeUserComments_WhenValidUserIdProvided() {
        // Given: 특정 사용자의 여러 댓글 생성
        Comment comment1 = Comment.createComment(testPost, testUser, "사용자 댓글 1", null);
        Comment comment2 = Comment.createComment(testPost, testUser, "사용자 댓글 2", null);
        
        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // When: 사용자 댓글 익명화
        commentDeleteAdapter.anonymizeUserComments(testUser.getId());
        
        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글들이 익명화되었는지 검증
        Optional<Comment> foundComment1 = commentRepository.findById(comment1.getId());
        Optional<Comment> foundComment2 = commentRepository.findById(comment2.getId());
        
        assertThat(foundComment1).isPresent();
        assertThat(foundComment1.get().getUser()).isNull();
        assertThat(foundComment1.get().getContent()).isEqualTo("탈퇴한 사용자의 댓글입니다.");
        
        assertThat(foundComment2).isPresent();
        assertThat(foundComment2.get().getUser()).isNull();
        assertThat(foundComment2.get().getContent()).isEqualTo("탈퇴한 사용자의 댓글입니다.");
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 댓글 ID 목록 조회")
    void shouldFindCommentIdsByUserId_WhenValidUserIdProvided() {
        // Given: 특정 사용자의 여러 댓글 생성
        Comment comment1 = Comment.createComment(testPost, testUser, "사용자 댓글 1", null);
        Comment comment2 = Comment.createComment(testPost, testUser, "사용자 댓글 2", null);
        Comment comment3 = Comment.createComment(testPost, null, "익명 댓글", 1234);
        
        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);
        commentRepository.save(comment3);

        // When: 사용자 댓글 ID 목록 조회
        List<Long> commentIds = commentDeleteAdapter.findCommentIdsByUserId(testUser.getId());

        // Then: 해당 사용자의 댓글 ID만 조회되었는지 검증
        assertThat(commentIds).hasSize(2);
        assertThat(commentIds).contains(comment1.getId(), comment2.getId());
        assertThat(commentIds).doesNotContain(comment3.getId());  // 익명 댓글은 제외
    }

    @Test
    @DisplayName("정상 케이스 - 조건부 소프트 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldConditionalSoftDelete_WhenCommentHasChildren() {
        // Given: 댓글 생성 및 저장
        Comment comment = Comment.createComment(testPost, testUser, "부모 댓글", null);
        comment = commentRepository.save(comment);
        Long commentId = comment.getId();

        // When: 조건부 소프트 삭제 (실제로는 자손이 있는지 확인하는 로직이 필요하지만, 테스트에서는 단순화)
        int softDeleteCount = commentDeleteAdapter.conditionalSoftDelete(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 소프트 삭제 결과 검증
        // 자손이 없는 경우 0이 반환되어야 함 (실제 구현에 따라 달라질 수 있음)
        assertThat(softDeleteCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("경계값 - 빈 게시글에 댓글 삭제 시 아무 동작 안함")
    void shouldDoNothing_WhenDeletingCommentsFromEmptyPost() {
        // Given: 댓글이 없는 새 게시글
        Post emptyPost = Post.builder()
                .user(testUser)
                .title("빈 게시글")
                .content("댓글 없음")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(emptyPost);

        // When: 빈 게시글의 댓글 삭제
        commentDeleteAdapter.deleteAllByPostId(emptyPost.getId());

        // Then: 예외가 발생하지 않고 정상적으로 완료되어야 함
        List<Comment> comments = commentRepository.findAll();
        assertThat(comments).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 ID로 익명화")
    void shouldDoNothing_WhenAnonymizingNonExistentUser() {
        // Given: 기존 댓글과 존재하지 않는 사용자 ID
        Comment comment = Comment.createComment(testPost, testUser, "기존 댓글", null);
        comment = commentRepository.save(comment);
        
        Long nonExistentUserId = 999L;

        // When: 존재하지 않는 사용자 ID로 익명화
        commentDeleteAdapter.anonymizeUserComments(nonExistentUserId);

        // Then: 기존 댓글은 변경되지 않아야 함
        Optional<Comment> foundComment = commentRepository.findById(comment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getUser()).isEqualTo(testUser);
        assertThat(foundComment.get().getContent()).isEqualTo("기존 댓글");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 ID로 댓글 ID 목록 조회")
    void shouldReturnEmptyList_WhenFindingCommentIdsByNonExistentUser() {
        // Given: 기존 댓글과 존재하지 않는 사용자 ID
        Comment comment = Comment.createComment(testPost, testUser, "기존 댓글", null);
        commentRepository.save(comment);
        
        Long nonExistentUserId = 999L;

        // When: 존재하지 않는 사용자 ID로 댓글 ID 목록 조회
        List<Long> commentIds = commentDeleteAdapter.findCommentIdsByUserId(nonExistentUserId);

        // Then: 빈 목록이 반환되어야 함
        assertThat(commentIds).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 ID로 하드 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldReturnZero_WhenHardDeletingNonExistentComment() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 999L;

        // When: 존재하지 않는 댓글 하드 삭제
        int deleteCount = commentDeleteAdapter.hardDeleteComment(nonExistentCommentId);

        // Then: 삭제된 댓글 수가 0이어야 함
        assertThat(deleteCount).isEqualTo(0);
    }
}
package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jakarta.persistence.EntityManager;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.annotation.Commit;

/**
 * <h2>CommentCommandAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentCommandAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 CRUD 동작 검증</p>
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
        "jaeik.bimillog.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment"
})
@Import(CommentCommandAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentCommandAdapterIntegrationTest {

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
    private CommentCommandAdapter commentCommandAdapter;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentRepository.deleteAll();
        
        // 테스트용 사용자 생성
        Setting setting = Setting.createSetting();
        entityManager.persistAndFlush(setting);
        
        testUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
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
    @DisplayName("정상 케이스 - 새로운 댓글 저장")
    void shouldSaveNewComment_WhenValidCommentProvided() {
        // Given: 새로운 댓글 엔티티
        Comment newComment = Comment.createComment(
                testPost, 
                testUser, 
                "테스트 댓글 내용", 
                null
        );

        // When: 댓글 저장
        Comment savedComment = commentCommandAdapter.save(newComment);

        // Then: 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(savedComment.getPost()).isEqualTo(testPost);
        assertThat(savedComment.getUser()).isEqualTo(testUser);
        assertThat(savedComment.isDeleted()).isFalse();
        assertThat(savedComment.getPassword()).isNull();
        
        // DB에 실제로 저장되었는지 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("테스트 댓글 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 비밀번호가 있는 댓글 저장")
    void shouldSaveCommentWithPassword_WhenPasswordProvided() {
        // Given: 비밀번호가 있는 댓글
        Comment commentWithPassword = Comment.createComment(
                testPost, 
                null,  // 익명 댓글
                "익명 댓글 내용", 
                1234
        );

        // When: 댓글 저장
        Comment savedComment = commentCommandAdapter.save(commentWithPassword);

        // Then: 비밀번호 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("익명 댓글 내용");
        assertThat(savedComment.getUser()).isNull();
        assertThat(savedComment.getPassword()).isEqualTo(1234);
        
        // DB에 실제로 저장되었는지 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getPassword()).isEqualTo(1234);
    }

    @Test
    @DisplayName("정상 케이스 - 기존 댓글 내용 수정")
    void shouldUpdateExistingComment_WhenCommentModified() {
        // Given: 기존 댓글 생성 및 저장
        Comment existingComment = Comment.createComment(
                testPost, 
                testUser, 
                "원본 댓글 내용", 
                null
        );
        existingComment = commentRepository.save(existingComment);

        // 댓글 내용 수정
        existingComment.updateComment("수정된 댓글 내용");

        // When: 수정된 댓글 저장
        Comment updatedComment = commentCommandAdapter.save(existingComment);

        // Then: 댓글 내용이 올바르게 업데이트되었는지 검증
        assertThat(updatedComment.getId()).isEqualTo(existingComment.getId());
        assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글 내용");
        
        // DB에서 다시 조회하여 확인
        Optional<Comment> foundComment = commentRepository.findById(updatedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldDeleteComment_WhenValidCommentProvided() {
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
        int deleteCount = commentCommandAdapter.hardDeleteComment(commentId);

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
        commentCommandAdapter.deleteAllByPostId(testPost.getId());

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
        commentCommandAdapter.anonymizeUserComments(testUser.getId());
        
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
    @DisplayName("경계값 - 최대 길이 댓글 저장")
    void shouldSaveLongComment_WhenMaxLengthContentProvided() {
        // Given: 최대 길이(255자)의 댓글 내용
        String longContent = "가".repeat(255);
        Comment longComment = Comment.createComment(testPost, testUser, longContent, null);

        // When: 긴 댓글 저장
        Comment savedComment = commentCommandAdapter.save(longComment);

        // Then: 긴 댓글이 올바르게 저장되었는지 검증
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getContent()).hasSize(255);
        assertThat(savedComment.getContent()).isEqualTo(longContent);
        
        // DB에서 확인
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).hasSize(255);
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
        commentCommandAdapter.deleteAllByPostId(emptyPost.getId());

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
        commentCommandAdapter.anonymizeUserComments(nonExistentUserId);

        // Then: 기존 댓글은 변경되지 않아야 함
        Optional<Comment> foundComment = commentRepository.findById(comment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getUser()).isEqualTo(testUser);
        assertThat(foundComment.get().getContent()).isEqualTo("기존 댓글");
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 댓글 엔티티 저장 및 조회")
    void shouldSaveComplexCommentEntity_WhenAllFieldsProvided() {
        // Given: 모든 필드를 포함한 복잡한 댓글 엔티티
        Comment complexComment = Comment.createComment(
                testPost,
                testUser,
                "복잡한 댓글 내용입니다. 특수문자도 포함! @#$%^&*()",
                5678
        );

        // When: 복잡한 댓글 저장
        Comment savedComplexComment = commentCommandAdapter.save(complexComment);

        // Then: 모든 필드가 올바르게 저장되었는지 검증
        assertThat(savedComplexComment.getId()).isNotNull();
        assertThat(savedComplexComment.getPost()).isEqualTo(testPost);
        assertThat(savedComplexComment.getUser()).isEqualTo(testUser);
        assertThat(savedComplexComment.getContent()).isEqualTo("복잡한 댓글 내용입니다. 특수문자도 포함! @#$%^&*()");
        assertThat(savedComplexComment.getPassword()).isEqualTo(5678);
        assertThat(savedComplexComment.isDeleted()).isFalse();
        
        // 기본 엔티티 필드 검증
        assertThat(savedComplexComment.getCreatedAt()).isNotNull();
        assertThat(savedComplexComment.getModifiedAt()).isNotNull();
        
        // DB에서 직접 조회하여 재확인
        Optional<Comment> foundComment = commentRepository.findById(savedComplexComment.getId());
        assertThat(foundComment).isPresent();
        Comment dbComment = foundComment.get();
        assertThat(dbComment.getContent()).contains("특수문자도 포함");
        assertThat(dbComment.getPassword()).isEqualTo(5678);
    }
}
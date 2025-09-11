package jaeik.bimillog.infrastructure.adapter.comment.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.comment.CommentDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
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
    @DisplayName("정상 케이스 - 댓글 삭제 (자손 없는 경우 하드 삭제)")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldHardDeleteComment_WhenValidCommentProvided() {
        // Given: 기존 댓글 생성 및 저장 (자손이 없는 댓글)
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

        // When: 댓글 삭제 (내부적으로 자손이 없어 하드 삭제 수행)
        commentDeleteAdapter.deleteComment(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글이 완전히 삭제되었는지 검증
        Optional<Comment> deletedComment = commentRepository.findById(commentId);
        assertThat(deletedComment).isEmpty();
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

        // When: 사용자 댓글 익명화 (processUserCommentsOnWithdrawal를 통해 간접 호출)
        commentDeleteAdapter.processUserCommentsOnWithdrawal(testUser.getId());
        
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

        // When: 사용자 댓글 ID 목록 조회 (Repository를 직접 호출)
        List<Long> commentIds = commentRepository.findCommentIdsByUserId(testUser.getId());

        // Then: 해당 사용자의 댓글 ID만 조회되었는지 검증
        assertThat(commentIds).hasSize(2);
        assertThat(commentIds).contains(comment1.getId(), comment2.getId());
        assertThat(commentIds).doesNotContain(comment3.getId());  // 익명 댓글은 제외
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 삭제 통합 메서드")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldDeleteComment_WhenValidCommentProvided() {
        // Given: 댓글 생성 및 저장
        Comment comment = Comment.createComment(testPost, testUser, "테스트 댓글", null);
        comment = commentRepository.save(comment);
        Long commentId = comment.getId();

        // 삭제 전 댓글 존재 확인
        assertThat(commentRepository.findById(commentId)).isPresent();

        // When: 댓글 삭제 (통합 메서드로 내부 로직 확인)
        commentDeleteAdapter.deleteComment(commentId);

        // EntityManager 초기화로 변경사항 반영
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글 삭제 결과 검증 (자손이 없으므로 하드 삭제)
        Optional<Comment> deletedComment = commentRepository.findById(commentId);
        assertThat(deletedComment).isEmpty(); // 하드 삭제로 완전히 제거
    }


    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자 ID로 익명화")
    void shouldDoNothing_WhenAnonymizingNonExistentUser() {
        // Given: 기존 댓글과 존재하지 않는 사용자 ID
        Comment comment = Comment.createComment(testPost, testUser, "기존 댓글", null);
        comment = commentRepository.save(comment);
        
        Long nonExistentUserId = 999L;

        // When: 존재하지 않는 사용자 ID로 익명화 (processUserCommentsOnWithdrawal를 통해 간접 호출)
        commentDeleteAdapter.processUserCommentsOnWithdrawal(nonExistentUserId);

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
        List<Long> commentIds = commentRepository.findCommentIdsByUserId(nonExistentUserId);

        // Then: 빈 목록이 반환되어야 함
        assertThat(commentIds).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 ID로 삭제")
    @Commit  // 트랜잭션 롤백 방지로 @Modifying 쿼리 결과 확인
    void shouldDoNothing_WhenDeletingNonExistentComment() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 999L;

        // When: 존재하지 않는 댓글 삭제 (예외가 발생하지 않아야 함)
        commentDeleteAdapter.deleteComment(nonExistentCommentId);

        // Then: 예외가 발생하지 않고 정상적으로 완료되어야 함
        // 통합 테스트에서는 예외 발생 여부만 확인
        List<Comment> allComments = commentRepository.findAll();
        // 기존 댓글들에 영향을 주지 않아야 함
        assertThat(allComments).isEmpty(); // setUp에서 초기화되므로 빈 상태
    }
}
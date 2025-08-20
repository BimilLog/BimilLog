package jaeik.growfarm.infrastructure.adapter.comment.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike.CommentLikeCommandAdapter;
import jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike.CommentLikeRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentLikeCommandAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentLikeCommandAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 추천 CRUD 동작 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.comment.entity",
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment",
        "jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike"
})
@Import(CommentLikeCommandAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentLikeCommandAdapterIntegrationTest {

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
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentLikeCommandAdapter commentLikeCommandAdapter;

    private User testUser1;
    private User testUser2;
    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentLikeRepository.deleteAll();
        commentRepository.deleteAll();
        
        // 테스트용 사용자들 생성
        Setting setting1 = Setting.createSetting();
        entityManager.persistAndFlush(setting1);
        
        testUser1 = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser1")
                .socialNickname("테스트유저1")
                .role(UserRole.USER)
                .setting(setting1)
                .build();
        entityManager.persistAndFlush(testUser1);
        
        Setting setting2 = Setting.createSetting();
        entityManager.persistAndFlush(setting2);
        
        testUser2 = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("testUser2")
                .socialNickname("테스트유저2")
                .role(UserRole.USER)
                .setting(setting2)
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
        
        // 테스트용 댓글 생성
        testComment = Comment.createComment(testPost, testUser1, "테스트 댓글", null);
        testComment = commentRepository.save(testComment);
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 추천 저장")
    void shouldSaveNewCommentLike_WhenValidCommentLikeProvided() {
        // Given: 새로운 댓글 추천 엔티티
        CommentLike newCommentLike = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();

        // When: 댓글 추천 저장
        CommentLike savedCommentLike = commentLikeCommandAdapter.save(newCommentLike);

        // Then: 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedCommentLike).isNotNull();
        assertThat(savedCommentLike.getId()).isNotNull();
        assertThat(savedCommentLike.getComment()).isEqualTo(testComment);
        assertThat(savedCommentLike.getUser()).isEqualTo(testUser2);
        
        // DB에 실제로 저장되었는지 확인
        Optional<CommentLike> foundCommentLike = commentLikeRepository.findById(savedCommentLike.getId());
        assertThat(foundCommentLike).isPresent();
        assertThat(foundCommentLike.get().getComment().getId()).isEqualTo(testComment.getId());
        assertThat(foundCommentLike.get().getUser().getId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 추천 삭제")
    void shouldDeleteCommentLike_WhenValidCommentAndUserProvided() {
        // Given: 기존 댓글 추천 생성 및 저장
        CommentLike existingCommentLike = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();
        existingCommentLike = commentLikeRepository.save(existingCommentLike);
        
        // 삭제 전 댓글 추천 존재 확인
        boolean existsBefore = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(existsBefore).isTrue();

        // When: 댓글 추천 삭제
        commentLikeCommandAdapter.deleteLike(testComment, testUser2);

        // Then: 댓글 추천이 삭제되었는지 검증
        boolean existsAfter = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(existsAfter).isFalse();
        
        // ID로도 확인
        Optional<CommentLike> foundCommentLike = commentLikeRepository.findById(existingCommentLike.getId());
        assertThat(foundCommentLike).isEmpty();
    }

    @Test
    @DisplayName("경계값 - 중복 댓글 추천 저장 시 예외")
    void shouldThrowException_WhenDuplicateCommentLikeProvided() {
        // Given: 이미 존재하는 댓글 추천
        CommentLike existingCommentLike = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();
        commentLikeRepository.save(existingCommentLike);

        // 동일한 댓글-사용자 조합의 추천
        CommentLike duplicateCommentLike = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();

        // When & Then: 중복 댓글 추천으로 저장 시 예외 발생
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> commentLikeCommandAdapter.save(duplicateCommentLike)
        );
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 추천 삭제")
    void shouldDoNothing_WhenDeletingNonExistentCommentLike() {
        // Given: 추천하지 않은 댓글과 사용자
        // testUser2가 testComment를 추천하지 않은 상태

        // When: 존재하지 않는 댓글 추천 삭제
        commentLikeCommandAdapter.deleteLike(testComment, testUser2);

        // Then: 예외가 발생하지 않고 정상적으로 완료되어야 함
        boolean exists = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 사용자의 동일 댓글 추천")
    void shouldSaveMultipleCommentLikes_WhenDifferentUsersLikeSameComment() {
        // Given: 동일한 댓글에 대한 여러 사용자의 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment)
                .user(testUser1)
                .build();
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();

        // When: 여러 사용자의 댓글 추천 저장
        CommentLike savedLike1 = commentLikeCommandAdapter.save(like1);
        CommentLike savedLike2 = commentLikeCommandAdapter.save(like2);

        // Then: 모든 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike1.getId()).isNotNull();
        assertThat(savedLike2).isNotNull();
        assertThat(savedLike2.getId()).isNotNull();
        
        // DB에서 확인
        boolean user1Liked = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser1.getId());
        boolean user2Liked = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        
        assertThat(user1Liked).isTrue();
        assertThat(user2Liked).isTrue();
        
        // 전체 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(2);
    }

    @Test
    @DisplayName("정상 케이스 - 한 사용자의 여러 댓글 추천")
    void shouldSaveMultipleCommentLikes_WhenOneUserLikesDifferentComments() {
        // Given: 추가 댓글 생성
        Comment additionalComment = Comment.createComment(testPost, testUser1, "추가 댓글", null);
        additionalComment = commentRepository.save(additionalComment);
        
        // 한 사용자가 여러 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();
        
        CommentLike like2 = CommentLike.builder()
                .comment(additionalComment)
                .user(testUser2)
                .build();

        // When: 여러 댓글 추천 저장
        CommentLike savedLike1 = commentLikeCommandAdapter.save(like1);
        CommentLike savedLike2 = commentLikeCommandAdapter.save(like2);

        // Then: 모든 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike2).isNotNull();
        
        // DB에서 확인
        boolean firstCommentLiked = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        boolean secondCommentLiked = commentLikeRepository.existsByCommentIdAndUserId(additionalComment.getId(), testUser2.getId());
        
        assertThat(firstCommentLiked).isTrue();
        assertThat(secondCommentLiked).isTrue();
    }

    @Test
    @DisplayName("트랜잭션 - 댓글 추천 저장 후 삭제")
    void shouldSaveAndDeleteCommentLike_WhenOperationsPerformedSequentially() {
        // Given: 빈 상태에서 시작
        boolean initialState = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(initialState).isFalse();

        // When: 댓글 추천 저장
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();
        CommentLike savedCommentLike = commentLikeCommandAdapter.save(commentLike);

        // Then: 저장 확인
        assertThat(savedCommentLike).isNotNull();
        boolean afterSave = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(afterSave).isTrue();

        // When: 댓글 추천 삭제
        commentLikeCommandAdapter.deleteLike(testComment, testUser2);

        // Then: 삭제 확인
        boolean afterDelete = commentLikeRepository.existsByCommentIdAndUserId(testComment.getId(), testUser2.getId());
        assertThat(afterDelete).isFalse();
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 댓글 추천 시나리오")
    void shouldHandleComplexCommentLikeScenario_WhenMultipleOperationsPerformed() {
        // Given: 복잡한 시나리오 설정
        Comment comment2 = Comment.createComment(testPost, testUser1, "두 번째 댓글", null);
        comment2 = commentRepository.save(comment2);

        // When: 복잡한 추천 동작 수행
        
        // 1. testUser2가 첫 번째 댓글 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment)
                .user(testUser2)
                .build();
        CommentLike savedLike1 = commentLikeCommandAdapter.save(like1);

        // 2. testUser2가 두 번째 댓글도 추천
        CommentLike like2 = CommentLike.builder()
                .comment(comment2)
                .user(testUser2)
                .build();
        CommentLike savedLike2 = commentLikeCommandAdapter.save(like2);

        // 3. testUser1이 자신의 댓글이 아닌 두 번째 댓글 추천
        CommentLike like3 = CommentLike.builder()
                .comment(comment2)
                .user(testUser1)
                .build();
        CommentLike savedLike3 = commentLikeCommandAdapter.save(like3);

        // Then: 모든 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike2).isNotNull();
        assertThat(savedLike3).isNotNull();

        // 각 댓글의 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(3);  // testUser2 2개 + testUser1 1개

        // When: 일부 추천 삭제
        commentLikeCommandAdapter.deleteLike(comment2, testUser2);

        // Then: 삭제 후 상태 확인
        long totalLikesAfterDelete = commentLikeRepository.count();
        assertThat(totalLikesAfterDelete).isEqualTo(2);  // testUser2의 comment2 추천이 삭제됨
        
        boolean user2LikesComment2 = commentLikeRepository.existsByCommentIdAndUserId(comment2.getId(), testUser2.getId());
        boolean user1LikesComment2 = commentLikeRepository.existsByCommentIdAndUserId(comment2.getId(), testUser1.getId());
        
        assertThat(user2LikesComment2).isFalse();
        assertThat(user1LikesComment2).isTrue();
    }
}
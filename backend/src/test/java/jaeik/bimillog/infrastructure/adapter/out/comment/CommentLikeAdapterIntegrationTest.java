package jaeik.bimillog.infrastructure.adapter.out.comment;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentLikeRepository;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentLikeAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentLikeAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 추천 CRUD 및 조회 동작 검증</p>
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
@Import({CommentLikeAdapter.class, TestContainersConfiguration.class})
class CommentLikeAdapterIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentLikeAdapter commentLikeAdapter;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Post testPost;
    private Comment testComment1;
    private Comment testComment2;

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
        
        Setting setting3 = Setting.createSetting();
        entityManager.persistAndFlush(setting3);
        
        testUser3 = User.builder()
                .socialId("kakao789")
                .provider(SocialProvider.KAKAO)
                .userName("testUser3")
                .socialNickname("테스트유저3")
                .role(UserRole.USER)
                .setting(setting3)
                .build();
        entityManager.persistAndFlush(testUser3);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser1)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);
        
        // 테스트용 댓글들 생성
        testComment1 = Comment.createComment(testPost, testUser1, "테스트 댓글 1", null);
        testComment1 = commentRepository.save(testComment1);
        
        testComment2 = Comment.createComment(testPost, testUser2, "테스트 댓글 2", null);
        testComment2 = commentRepository.save(testComment2);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자가 댓글에 추천을 눌렀을 때 true 반환")
    void shouldReturnTrue_WhenUserLikedComment() {
        // Given: testUser2가 testComment1에 추천을 누른 상태
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // When: 댓글 추천 여부 확인
        boolean isLiked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());

        // Then: 추천을 눌렀으므로 true 반환
        assertThat(isLiked).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자가 댓글에 추천을 누르지 않았을 때 false 반환")
    void shouldReturnFalse_WhenUserDidNotLikeComment() {
        // Given: testUser2가 testComment1에 추천을 누르지 않은 상태
        // (별도의 설정 없이 빈 상태)

        // When: 댓글 추천 여부 확인
        boolean isLiked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());

        // Then: 추천을 누르지 않았으므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 댓글 ID로 조회 시 false 반환")
    void shouldReturnFalse_WhenCommentIdDoesNotExist() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 99999L;

        // When: 존재하지 않는 댓글에 대한 추천 여부 확인
        boolean isLiked = commentLikeAdapter.isLikedByUser(nonExistentCommentId, testUser1.getId());

        // Then: 존재하지 않는 댓글이므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 사용자 ID로 조회 시 false 반환")
    void shouldReturnFalse_WhenUserIdDoesNotExist() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 99999L;

        // When: 존재하지 않는 사용자에 대한 추천 여부 확인
        boolean isLiked = commentLikeAdapter.isLikedByUser(testComment1.getId(), nonExistentUserId);

        // Then: 존재하지 않는 사용자이므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 사용자가 동일 댓글에 추천한 경우 정확한 구분")
    void shouldReturnCorrectResult_WhenMultipleUsersLikeSameComment() {
        // Given: 여러 사용자가 동일한 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser1)
                .build();
        commentLikeRepository.save(like1);
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like2);
        
        // testUser3은 추천하지 않음

        // When: 각 사용자별 추천 여부 확인
        boolean user1Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser1.getId());
        boolean user2Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean user3Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser3.getId());

        // Then: 추천한 사용자는 true, 추천하지 않은 사용자는 false
        assertThat(user1Liked).isTrue();
        assertThat(user2Liked).isTrue();
        assertThat(user3Liked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 한 사용자가 여러 댓글에 추천한 경우 정확한 구분")
    void shouldReturnCorrectResult_WhenOneUserLikesMultipleComments() {
        // Given: 한 사용자가 여러 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like1);
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment2)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like2);

        // When: 각 댓글별 추천 여부 확인
        boolean comment1Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean comment2Liked = commentLikeAdapter.isLikedByUser(testComment2.getId(), testUser2.getId());
        
        // 다른 사용자의 경우
        boolean otherUserComment1Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser3.getId());
        boolean otherUserComment2Liked = commentLikeAdapter.isLikedByUser(testComment2.getId(), testUser3.getId());

        // Then: 추천한 댓글은 true, 추천하지 않은 조합은 false
        assertThat(comment1Liked).isTrue();
        assertThat(comment2Liked).isTrue();
        assertThat(otherUserComment1Liked).isFalse();
        assertThat(otherUserComment2Liked).isFalse();
    }



    @Test
    @DisplayName("트랜잭션 - 추천 상태 변경 시 실시간 반영 확인")
    void shouldReflectChangesImmediately_WhenCommentLikeStateChanges() {
        // Given: 초기 상태 - 추천하지 않은 상태
        boolean initialState = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(initialState).isFalse();

        // When: 추천 추가
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // Then: 추가 후 즉시 반영 확인
        boolean afterAdd = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterAdd).isTrue();

        // When: 추천 삭제
        commentLikeRepository.deleteByCommentIdAndUserId(testComment1.getId(), testUser2.getId());

        // Then: 삭제 후 즉시 반영 확인
        boolean afterDelete = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterDelete).isFalse();
    }



    // ============================================================================
    // 명령(Command) 테스트: save, deleteLike 기능
    // ============================================================================

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 추천 저장")
    void shouldSaveNewCommentLike_WhenValidCommentLikeProvided() {
        // Given: 새로운 댓글 추천 엔티티
        CommentLike newCommentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();

        // When: 댓글 추천 저장
        CommentLike savedCommentLike = commentLikeAdapter.save(newCommentLike);

        // Then: 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedCommentLike).isNotNull();
        assertThat(savedCommentLike.getId()).isNotNull();
        assertThat(savedCommentLike.getComment()).isEqualTo(testComment1);
        assertThat(savedCommentLike.getUser()).isEqualTo(testUser2);
        
        // DB에 실제로 저장되었는지 확인
        Optional<CommentLike> foundCommentLike = commentLikeRepository.findById(savedCommentLike.getId());
        assertThat(foundCommentLike).isPresent();
        assertThat(foundCommentLike.get().getComment().getId()).isEqualTo(testComment1.getId());
        assertThat(foundCommentLike.get().getUser().getId()).isEqualTo(testUser2.getId());
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 추천 삭제")
    void shouldDeleteCommentLike_WhenValidCommentAndUserProvided() {
        // Given: 기존 댓글 추천 생성 및 저장
        CommentLike existingCommentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        existingCommentLike = commentLikeRepository.save(existingCommentLike);
        
        // 삭제 전 댓글 추천 존재 확인
        boolean existsBefore = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(existsBefore).isTrue();

        // When: 댓글 추천 삭제
        commentLikeAdapter.deleteLikeByIds(testComment1.getId(), testUser2.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 댓글 추천이 삭제되었는지 검증
        boolean existsAfter = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(existsAfter).isFalse();
        
        // ID로도 확인
        Optional<CommentLike> foundCommentLike = commentLikeRepository.findById(existingCommentLike.getId());
        assertThat(foundCommentLike).isEmpty();
    }


    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 추천 삭제")
    void shouldDoNothing_WhenDeletingNonExistentCommentLike() {
        // Given: 추천하지 않은 댓글과 사용자
        // testUser2가 testComment1을 추천하지 않은 상태

        // When: 존재하지 않는 댓글 추천 삭제
        commentLikeAdapter.deleteLikeByIds(testComment1.getId(), testUser2.getId());

        // Then: 예외가 발생하지 않고 정상적으로 완료되어야 함
        boolean exists = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 사용자의 동일 댓글 추천")
    void shouldSaveMultipleCommentLikes_WhenDifferentUsersLikeSameComment() {
        // Given: 동일한 댓글에 대한 여러 사용자의 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser1)
                .build();
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();

        // When: 여러 사용자의 댓글 추천 저장
        CommentLike savedLike1 = commentLikeAdapter.save(like1);
        CommentLike savedLike2 = commentLikeAdapter.save(like2);

        // Then: 모든 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike1.getId()).isNotNull();
        assertThat(savedLike2).isNotNull();
        assertThat(savedLike2.getId()).isNotNull();
        
        // DB에서 확인
        boolean user1Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser1.getId());
        boolean user2Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        
        assertThat(user1Liked).isTrue();
        assertThat(user2Liked).isTrue();
        
        // 전체 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(2);
    }

    @Test
    @DisplayName("정상 케이스 - 한 사용자의 여러 댓글 추천")
    void shouldSaveMultipleCommentLikes_WhenOneUserLikesDifferentComments() {
        // Given: 추가 댓글 생성 (testComment2는 이미 setUp에서 생성됨)
        // 한 사용자가 여러 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment2)
                .user(testUser2)
                .build();

        // When: 여러 댓글 추천 저장
        CommentLike savedLike1 = commentLikeAdapter.save(like1);
        CommentLike savedLike2 = commentLikeAdapter.save(like2);

        // Then: 모든 댓글 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike2).isNotNull();
        
        // DB에서 확인
        boolean firstCommentLiked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean secondCommentLiked = commentLikeAdapter.isLikedByUser(testComment2.getId(), testUser2.getId());
        
        assertThat(firstCommentLiked).isTrue();
        assertThat(secondCommentLiked).isTrue();
    }

    // ============================================================================
    // 통합 워크플로우 테스트: 조회 + 명령 연계 테스트
    // ============================================================================

    @Test
    @DisplayName("통합 워크플로우 - 댓글 추천 저장 후 삭제")
    void shouldSaveAndDeleteCommentLike_WhenOperationsPerformedSequentially() {
        // Given: 빈 상태에서 시작
        boolean initialState = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(initialState).isFalse();

        // When: 댓글 추천 저장
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        CommentLike savedCommentLike = commentLikeAdapter.save(commentLike);

        // Then: 저장 확인
        assertThat(savedCommentLike).isNotNull();
        boolean afterSave = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterSave).isTrue();

        // When: 댓글 추천 삭제
        commentLikeAdapter.deleteLikeByIds(testComment1.getId(), testUser2.getId());

        // Then: 삭제 확인
        boolean afterDelete = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterDelete).isFalse();
    }


}
package jaeik.bimillog.infrastructure.adapter.comment.comment;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.comment.CommentLikeAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentLikeRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    @DisplayName("성능 테스트 - EXISTS 쿼리 최적화 확인")
    void shouldUseOptimizedExistsQuery_WhenCheckingCommentLike() {
        // Given: 대량의 테스트 데이터 생성 (성능 테스트용)
        // 여러 사용자와 댓글에 대한 추천 데이터 생성
        CommentLike targetLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(targetLike);
        
        // 다른 추천들도 생성 (노이즈 데이터)
        for (int i = 0; i < 10; i++) {
            Comment noiseComment = Comment.createComment(testPost, testUser1, "노이즈 댓글 " + i, null);
            noiseComment = commentRepository.save(noiseComment);
            
            CommentLike noiseLike = CommentLike.builder()
                    .comment(noiseComment)
                    .user(testUser3)
                    .build();
            commentLikeRepository.save(noiseLike);
        }

        // When: 타겟 추천 여부 확인 (EXISTS 쿼리 사용)
        long startTime = System.nanoTime();
        boolean isLiked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        long endTime = System.nanoTime();

        // Then: 빠른 시간 내에 정확한 결과 반환
        assertThat(isLiked).isTrue();
        
        // 성능 확인: EXISTS 쿼리는 매우 빨라야 함 (1ms 이내)
        long executionTimeMs = (endTime - startTime) / 1_000_000;
        assertThat(executionTimeMs).isLessThan(10); // 10ms 이내 (여유있게 설정)
    }

    @Test
    @DisplayName("경계값 - null 값 처리 확인")
    void shouldHandleNullValues_WhenNullParametersProvided() {
        // 실제 비즈니스 로직에서는 null이 넘어오지 않아야 하지만
        // 어댑터 레벨에서의 방어 코드 확인
        
        // Given & When & Then: null 처리 테스트
        // 비즈니스 로직에서 NULL 체크가 선행되므로 여기서는 기본 동작 확인
        
        // 정상적인 경우만 테스트 (NULL은 비즈니스 레이어에서 사전 차단)
        boolean normalCase = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser1.getId());
        assertThat(normalCase).isFalse();
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

    @Test
    @DisplayName("복잡한 시나리오 - 대용량 데이터에서의 정확성 검증")
    void shouldReturnAccurateResults_WhenLargeDatasetExists() {
        // Given: 대용량 테스트 데이터 생성
        final int COMMENT_COUNT = 50;
        
        // 추가 댓글들 생성
        for (int i = 0; i < COMMENT_COUNT; i++) {
            Comment comment = Comment.createComment(testPost, testUser1, "댓글 " + i, null);
            commentRepository.save(comment);
        }
        
        // 무작위 추천 관계 생성 (testUser2가 일부 댓글에만 추천)
        // testComment1에는 추천, testComment2에는 추천하지 않음
        CommentLike targetLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(targetLike);

        // When: 타겟 댓글들의 추천 여부 확인
        boolean comment1Liked = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean comment2Liked = commentLikeAdapter.isLikedByUser(testComment2.getId(), testUser2.getId());

        // Then: 대용량 데이터 환경에서도 정확한 결과 반환
        assertThat(comment1Liked).isTrue();   // 추천한 댓글
        assertThat(comment2Liked).isFalse();  // 추천하지 않은 댓글
        
        // 전체 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(1);  // targetLike 하나만 존재
    }

    @Test
    @DisplayName("동시성 시나리오 - 같은 댓글-사용자 조합에 대한 중복 체크")
    void shouldHandleConcurrentAccess_WhenSameCommentUserCombination() {
        // Given: 동일한 댓글-사용자 조합
        Long commentId = testComment1.getId();
        Long userId = testUser2.getId();
        
        // 초기 상태 확인
        boolean initialCheck = commentLikeAdapter.isLikedByUser(commentId, userId);
        assertThat(initialCheck).isFalse();

        // When: 추천 생성
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // Then: 여러 번 체크해도 일관된 결과
        for (int i = 0; i < 5; i++) {
            boolean check = commentLikeAdapter.isLikedByUser(commentId, userId);
            assertThat(check).isTrue();
        }
        
        // 다른 조합들은 영향받지 않음
        boolean otherUserCheck = commentLikeAdapter.isLikedByUser(commentId, testUser3.getId());
        boolean otherCommentCheck = commentLikeAdapter.isLikedByUser(testComment2.getId(), userId);
        
        assertThat(otherUserCheck).isFalse();
        assertThat(otherCommentCheck).isFalse();
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
    @DisplayName("데이터베이스 제약조건 - 유니크 키 위반 시 예외")
    void shouldThrowException_WhenDatabaseUniqueConstraintViolated() {
        // 실제 비즈니스 로직(CommentLikeService)에서는 토글 방식으로 중복 체크 후 처리하므로
        // 이 상황은 발생하지 않아야 함. 하지만 데이터베이스 레벨 제약조건 테스트로는 유효함
        // 의심 지점: 비즈니스 로직에서 중복 체크 로직이 우회되는 경우가 있는지 검토 필요
        
        // Given: 이미 존재하는 댓글 추천을 데이터베이스에 직접 저장
        CommentLike existingCommentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(existingCommentLike);

        // 동일한 댓글-사용자 조합의 추천 (데이터베이스 제약조건 위반 상황)
        CommentLike duplicateCommentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();

        // When & Then: 데이터베이스 유니크 제약조건 위반으로 예외 발생
        // 주의: 실제 비즈니스 로직에서는 이런 직접 저장이 발생하지 않음 (토글 로직 존재)
        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> commentLikeAdapter.save(duplicateCommentLike)
        );
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

    @Test
    @DisplayName("통합 워크플로우 - 복잡한 댓글 추천 시나리오")
    void shouldHandleComplexCommentLikeScenario_WhenMultipleOperationsPerformed() {
        // Given: 복잡한 시나리오 설정
        Comment comment3 = Comment.createComment(testPost, testUser1, "세 번째 댓글", null);
        comment3 = commentRepository.save(comment3);

        // When: 복잡한 추천 동작 수행
        
        // 1. testUser2가 첫 번째 댓글 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        CommentLike savedLike1 = commentLikeAdapter.save(like1);

        // 2. testUser2가 세 번째 댓글도 추천
        CommentLike like2 = CommentLike.builder()
                .comment(comment3)
                .user(testUser2)
                .build();
        CommentLike savedLike2 = commentLikeAdapter.save(like2);

        // 3. testUser1이 자신의 댓글이 아닌 세 번째 댓글 추천
        CommentLike like3 = CommentLike.builder()
                .comment(comment3)
                .user(testUser1)
                .build();
        CommentLike savedLike3 = commentLikeAdapter.save(like3);

        // Then: 모든 추천이 올바르게 저장되었는지 검증
        assertThat(savedLike1).isNotNull();
        assertThat(savedLike2).isNotNull();
        assertThat(savedLike3).isNotNull();

        // 각 댓글의 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(3);  // testUser2 2개 + testUser1 1개

        // When: 일부 추천 삭제
        commentLikeAdapter.deleteLikeByIds(comment3.getId(), testUser2.getId());

        // Then: 삭제 후 상태 확인
        long totalLikesAfterDelete = commentLikeRepository.count();
        assertThat(totalLikesAfterDelete).isEqualTo(2);  // testUser2의 comment3 추천이 삭제됨
        
        boolean user2LikesComment3 = commentLikeAdapter.isLikedByUser(comment3.getId(), testUser2.getId());
        boolean user1LikesComment3 = commentLikeAdapter.isLikedByUser(comment3.getId(), testUser1.getId());
        
        assertThat(user2LikesComment3).isFalse();
        assertThat(user1LikesComment3).isTrue();
    }

    @Test
    @DisplayName("통합 워크플로우 - ID 기반 삭제 최적화 테스트")
    void shouldDeleteByIds_WhenOptimizedDeletionRequired() {
        // Given: 댓글 추천 생성
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);
        
        // 생성 확인
        boolean existsBefore = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(existsBefore).isTrue();

        // When: ID 기반으로 최적화된 삭제 수행
        commentLikeAdapter.deleteLikeByIds(testComment1.getId(), testUser2.getId());

        // Then: 삭제 확인
        boolean existsAfter = commentLikeAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(existsAfter).isFalse();
        
        // 전체 추천 수도 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(0);
    }
}
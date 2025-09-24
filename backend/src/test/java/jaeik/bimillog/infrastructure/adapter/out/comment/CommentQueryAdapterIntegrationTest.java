package jaeik.bimillog.infrastructure.adapter.out.comment;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.CommentLike;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.comment.exception.CommentCustomException;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentLikeRepository;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.out.post.jpa.PostRepository;
import jaeik.bimillog.testutil.BaseIntegrationTest;
import jaeik.bimillog.testutil.CommentTestDataBuilder;
import jaeik.bimillog.testutil.TestFixtures;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
@Import(CommentQueryAdapter.class)
class CommentQueryAdapterIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentQueryAdapter commentQueryAdapter;

    @Autowired
    private PostRepository postRepository;

    private Post testPost;

    @Override
    protected void setUpChild() {
        // 테스트 데이터 초기화
        commentLikeRepository.deleteAll();
        commentRepository.deleteAll();

        // 테스트용 게시글 생성 (testUser는 BaseIntegrationTest에서 이미 생성됨)
        testPost = TestFixtures.createPost(testUser, "테스트 게시글", "테스트 게시글 내용입니다.");
        postRepository.save(testPost);
    }

    @Test
    @DisplayName("정상 케이스 - ID로 댓글 조회")
    void shouldFindCommentById_WhenValidIdProvided() {
        // Given: 저장된 댓글
        Comment savedComment = CommentTestDataBuilder.createComment(testUser, testPost, "테스트 댓글");
        savedComment = commentRepository.save(savedComment);

        // When: ID로 댓글 조회
        Comment foundComment = commentQueryAdapter.findById(savedComment.getId());

        // Then: 올바른 댓글이 조회되는지 검증
        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getId()).isEqualTo(savedComment.getId());
        assertThat(foundComment.getContent()).isEqualTo("테스트 댓글");
        assertThat(foundComment.getUser()).isEqualTo(testUser);
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
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "사용자1 댓글1");
        Comment comment2 = CommentTestDataBuilder.createComment(testUser, testPost, "사용자1 댓글2");
        Comment comment3 = CommentTestDataBuilder.createComment(otherUser, testPost, "사용자2 댓글1");

        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 특정 사용자의 댓글 조회
        Page<SimpleCommentInfo> userComments = commentQueryAdapter
                .findCommentsByUserId(testUser.getId(), pageable);

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
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "댓글1");
        Comment comment2 = CommentTestDataBuilder.createComment(testUser, testPost, "댓글2");

        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // otherUser가 추천
        CommentLike like1 = CommentLike.builder()
                .comment(comment1)
                .user(otherUser)
                .build();
        commentLikeRepository.save(like1);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자가 추천한 댓글 조회
        Page<SimpleCommentInfo> likedComments = commentQueryAdapter
                .findLikedCommentsByUserId(otherUser.getId(), pageable);

        // Then: 추천한 댓글들이 조회되는지 검증
        assertThat(likedComments).isNotNull();
        assertThat(likedComments.getContent()).hasSize(1);
        assertThat(likedComments.getContent().getFirst().getContent()).isEqualTo("댓글1");
    }

    @Test
    @DisplayName("정상 케이스 - 인기 댓글 목록 조회")
    void shouldFindPopularComments_WhenValidPostIdProvided() {
        // Given: 게시글의 여러 댓글과 추천 (인기 댓글 조건: 5개 이상)
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "인기댓글1");
        comment1 = commentRepository.save(comment1);

        // 5개 이상의 추천 생성 (인기 댓글 조건 충족)
        for (int i = 0; i < 6; i++) {
            User likeUser = TestUsers.createUniqueWithPrefix("likeUser" + i);
            userRepository.save(likeUser);

            CommentLike like = CommentLike.builder()
                    .comment(comment1)
                    .user(likeUser)
                    .build();
            commentLikeRepository.save(like);
        }

        // otherUser가 comment1에 추천 - 사용자 추천 여부 테스트용
        CommentLike userLike = CommentLike.builder()
                .comment(comment1)
                .user(otherUser)
                .build();
        commentLikeRepository.save(userLike);

        // When: 인기 댓글 조회 (otherUser 관점에서)
        List<CommentInfo> popularComments = commentQueryAdapter
                .findPopularComments(testPost.getId(), otherUser.getId());

        // Then: 인기 댓글들이 조회되는지 검증
        assertThat(popularComments).isNotNull();
        assertThat(popularComments).hasSize(1);

        CommentInfo popularComment = popularComments.getFirst();
        assertThat(popularComment.getContent()).isEqualTo("인기댓글1");
        assertThat(popularComment.isPopular()).isTrue();
        assertThat(popularComment.getLikeCount()).isEqualTo(7); // 6 + otherUser의 추천
        assertThat(popularComment.isUserLike()).isTrue(); // 단일 쿼리로 사용자 추천 여부 검증
    }

    @Test
    @DisplayName("정상 케이스 - 인기 댓글 조회 시 사용자 추천 여부 검증 (추천하지 않은 경우)")
    void shouldFindPopularComments_WithUserLikeFalse_WhenUserDidNotLike() {
        // Given: 인기 댓글과 추천하지 않은 사용자
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "인기댓글1");
        comment1 = commentRepository.save(comment1);

        // 3개 이상의 추천 생성 (다른 사용자들이 추천)
        for (int i = 0; i < 4; i++) {
            User likeUser = TestUsers.createUniqueWithPrefix("notLikeUser" + i);
            userRepository.save(likeUser);

            CommentLike like = CommentLike.builder()
                    .comment(comment1)
                    .user(likeUser)
                    .build();
            commentLikeRepository.save(like);
        }

        // When: 인기 댓글 조회 (otherUser는 추천하지 않음)
        List<CommentInfo> popularComments = commentQueryAdapter
                .findPopularComments(testPost.getId(), otherUser.getId());

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
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "첫번째 댓글");
        Comment comment2 = CommentTestDataBuilder.createComment(testUser, testPost, "두번째 댓글");
        Comment comment3 = CommentTestDataBuilder.createComment(otherUser, testPost, "세번째 댓글");

        commentRepository.save(comment1);
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        commentRepository.save(comment2);
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        commentRepository.save(comment3);

        // otherUser가 comment2에만 추천 - 사용자 추천 여부 테스트용
        CommentLike userLike = CommentLike.builder()
                .comment(comment2)
                .user(otherUser)
                .build();
        commentLikeRepository.save(userLike);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 과거순 댓글 조회 (otherUser 관점에서)
        Page<CommentInfo> oldestComments = commentQueryAdapter
                .findCommentsWithOldestOrder(testPost.getId(), pageable, otherUser.getId());

        // Then: 과거순으로 댓글들이 조회되고 사용자 추천 여부가 올바르게 설정되는지 검증
        assertThat(oldestComments).isNotNull();
        assertThat(oldestComments.getContent()).hasSize(3);

        List<CommentInfo> comments = oldestComments.getContent();
        // 과거순 정렬 검증 (첫번째 댓글이 가장 먼저)
        assertThat(comments.getFirst().getContent()).isEqualTo("첫번째 댓글");
        assertThat(comments.getFirst().isUserLike()).isFalse(); // otherUser가 추천하지 않음

        assertThat(comments.get(1).getContent()).isEqualTo("두번째 댓글");
        assertThat(comments.get(1).isUserLike()).isTrue(); // otherUser가 추천함
        assertThat(comments.get(1).getLikeCount()).isEqualTo(1);

        assertThat(comments.get(2).getContent()).isEqualTo("세번째 댓글");
        assertThat(comments.get(2).isUserLike()).isFalse(); // otherUser가 추천하지 않음
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID 목록에 대한 댓글 수 조회")
    void shouldFindCommentCountsByPostIds_WhenValidPostIdsProvided() {
        // Given: 여러 게시글과 각각의 댓글들
        Post post2 = TestFixtures.createPost(testUser, "테스트 게시글", "테스트 게시글 내용입니다.");
        postRepository.save(post2);

        Post post3 = TestFixtures.createPost(otherUser, "테스트 게시글", "테스트 게시글 내용입니다.");
        postRepository.save(post3);

        // testPost에 댓글 3개 생성
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "첫 번째 게시글 댓글1");
        Comment comment2 = CommentTestDataBuilder.createComment(otherUser, testPost, "첫 번째 게시글 댓글2");
        Comment comment3 = CommentTestDataBuilder.createComment(testUser, testPost, "첫 번째 게시글 댓글3");
        
        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        // post2에 댓글 2개 생성
        Comment comment4 = CommentTestDataBuilder.createComment(testUser, post2, "두 번째 게시글 댓글1");
        Comment comment5 = CommentTestDataBuilder.createComment(otherUser, post2, "두 번째 게시글 댓글2");
        
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
    @DisplayName("null 파라미터 처리 - findCommentCountsByPostIds")
    void shouldHandleNullPostIds_WhenNullProvided() {
        // Given
        List<Long> nullList = null;

        // When & Then
        assertThatThrownBy(() -> commentQueryAdapter.findCommentCountsByPostIds(nullList))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("null 파라미터 처리 - findCommentsWithOldestOrder")
    void shouldHandleNullParameters_WhenNullProvided() {
        // Given
        Pageable pageable = null;

        // When & Then
        assertThatThrownBy(() -> commentQueryAdapter.findCommentsWithOldestOrder(testPost.getId(), pageable, testUser.getId()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("트랜잭션 - 복합 쿼리 테스트")
    void shouldHandleComplexQueries_WhenMultipleOperationsPerformed() {
        // Given: 복잡한 테스트 데이터 설정
        Comment comment1 = CommentTestDataBuilder.createComment(testUser, testPost, "복합쿼리 댓글1");
        Comment comment2 = CommentTestDataBuilder.createComment(otherUser, testPost, "복합쿼리 댓글2");

        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // testUser가 comment2에 추천
        CommentLike like = CommentLike.builder()
                .comment(comment2)
                .user(testUser)
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
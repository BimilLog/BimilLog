package jaeik.bimillog.infrastructure.adapter.user.external;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.infrastructure.adapter.user.out.external.UserActivityAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>UserActivityAdapter 테스트</h2>
 * <p>사용자 활동 정보 조회 어댑터의 핵심 기능 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserActivityAdapterTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Pageable TEST_PAGEABLE = PageRequest.of(0, 10);

    @Mock private CommentQueryUseCase commentQueryUseCase;
    @Mock private PostQueryUseCase postQueryUseCase;

    @InjectMocks
    private UserActivityAdapter userActivityAdapter;

    @Test
    @DisplayName("사용자 작성 게시글 목록 조회")
    void shouldFindPostsByUserId_WhenValidUserIdProvided() {
        // Given
        List<PostSearchResult> posts = Arrays.asList(
                createPostSearchResult(1L, "첫 번째 게시글", TEST_USER_ID),
                createPostSearchResult(2L, "두 번째 게시글", TEST_USER_ID)
        );
        Page<PostSearchResult> expectedPage = new PageImpl<>(posts, TEST_PAGEABLE, posts.size());
        given(postQueryUseCase.getUserPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = userActivityAdapter.findPostsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("두 번째 게시글");
        verify(postQueryUseCase).getUserPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    @Test
    @DisplayName("사용자 추천 게시글 목록 조회")
    void shouldFindLikedPostsByUserId_WhenValidUserIdProvided() {
        // Given
        List<PostSearchResult> likedPosts = Arrays.asList(
                createPostSearchResult(3L, "추천한 게시글 1", 101L),
                createPostSearchResult(4L, "추천한 게시글 2", 102L)
        );
        Page<PostSearchResult> expectedPage = new PageImpl<>(likedPosts, TEST_PAGEABLE, likedPosts.size());
        given(postQueryUseCase.getUserLikedPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(expectedPage);

        // When
        Page<PostSearchResult> result = userActivityAdapter.findLikedPostsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("추천한 게시글 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("추천한 게시글 2");
        verify(postQueryUseCase).getUserLikedPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    @Test
    @DisplayName("빈 게시글 목록 조회")
    void shouldHandleEmptyPosts_WhenNoPostsFound() {
        // Given
        Page<PostSearchResult> emptyPage = new PageImpl<>(Collections.emptyList(), TEST_PAGEABLE, 0);
        given(postQueryUseCase.getUserPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(emptyPage);

        // When
        Page<PostSearchResult> result = userActivityAdapter.findPostsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(postQueryUseCase).getUserPosts(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    @Test
    @DisplayName("사용자 작성 댓글 목록 조회")
    void shouldFindCommentsByUserId_WhenValidUserIdProvided() {
        // Given
        List<SimpleCommentInfo> comments = Arrays.asList(
                new SimpleCommentInfo(1L, 1L, "작성자1", "첫 번째 댓글", Instant.now(), 0, false),
                new SimpleCommentInfo(2L, 2L, "작성자2", "두 번째 댓글", Instant.now(), 0, false)
        );
        Page<SimpleCommentInfo> expectedPage = new PageImpl<>(comments, TEST_PAGEABLE, comments.size());
        given(commentQueryUseCase.getUserComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(expectedPage);

        // When
        Page<SimpleCommentInfo> result = userActivityAdapter.findCommentsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("첫 번째 댓글");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("두 번째 댓글");
        verify(commentQueryUseCase).getUserComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    @Test
    @DisplayName("사용자 추천 댓글 목록 조회")
    void shouldFindLikedCommentsByUserId_WhenValidUserIdProvided() {
        // Given
        List<SimpleCommentInfo> likedComments = Arrays.asList(
                new SimpleCommentInfo(3L, 3L, "다른작성자1", "추천한 댓글 1", Instant.now(), 5, true),
                new SimpleCommentInfo(4L, 4L, "다른작성자2", "추천한 댓글 2", Instant.now(), 8, true)
        );
        Page<SimpleCommentInfo> expectedPage = new PageImpl<>(likedComments, TEST_PAGEABLE, likedComments.size());
        given(commentQueryUseCase.getUserLikedComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(expectedPage);

        // When
        Page<SimpleCommentInfo> result = userActivityAdapter.findLikedCommentsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("추천한 댓글 1");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("추천한 댓글 2");
        verify(commentQueryUseCase).getUserLikedComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    @Test
    @DisplayName("빈 댓글 목록 조회")
    void shouldHandleEmptyComments_WhenNoCommentsFound() {
        // Given
        Page<SimpleCommentInfo> emptyPage = new PageImpl<>(Collections.emptyList(), TEST_PAGEABLE, 0);
        given(commentQueryUseCase.getUserComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE))).willReturn(emptyPage);

        // When
        Page<SimpleCommentInfo> result = userActivityAdapter.findCommentsByUserId(TEST_USER_ID, TEST_PAGEABLE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(commentQueryUseCase).getUserComments(eq(TEST_USER_ID), eq(TEST_PAGEABLE));
    }

    private PostSearchResult createPostSearchResult(Long id, String title, Long userId) {
        return PostSearchResult.builder()
                .id(id)
                .title(title)
                .content(title + " 내용")
                .userName("테스트유저")
                .userId(userId)
                .createdAt(Instant.now())
                .likeCount(1)
                .commentCount(0)
                .viewCount(5)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .isNotice(false)
                .build();
    }
}
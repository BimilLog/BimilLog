package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalMemberQueryPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.RedisPostDeletePort;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCommandService 테스트</h2>
 * <p>게시글 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 *
 */
@DisplayName("PostCommandService 테스트")
@Tag("unit")
class PostCommandServiceTest extends BaseUnitTest {

    @Mock
    private PostCommandPort postCommandPort;

    @Mock
    private GlobalPostQueryPort globalPostQueryPort;

    @Mock
    private GlobalMemberQueryPort globalMemberQueryPort;

    @Mock
    private RedisPostDeletePort redisPostDeletePort;

    @Mock
    private PostLikeCommandPort postLikeCommandPort;

    @Mock
    private PostQueryPort postQueryPort;

    @InjectMocks
    private PostCommandService postCommandService;

    @Test
    @DisplayName("게시글 작성 - 성공")
    void shouldWritePost_WhenValidInput() {
        // Given
        Long memberId = 1L;
        Long expectedPostId = 123L;
        String title = "테스트 제목";
        String content = "테스트 내용";
        Integer password = 1234;

        Post createdPost = PostTestDataBuilder.withId(expectedPostId, PostTestDataBuilder.createPost(getTestMember(), title, content));

        given(globalMemberQueryPort.getReferenceById(memberId)).willReturn(getTestMember());
        given(postCommandPort.create(any(Post.class))).willReturn(createdPost);

        // When
        Long result = postCommandService.writePost(memberId, title, content, password);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(globalMemberQueryPort, times(1)).getReferenceById(memberId);
        verify(postCommandPort, times(1)).create(any(Post.class));
        verifyNoMoreInteractions(globalMemberQueryPort, postCommandPort);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "기존 제목", "기존 내용")));

        given(globalPostQueryPort.findById(postId)).willReturn(existingPost);
        given(existingPost.isAuthor(memberId, null)).willReturn(true);

        // When
        postCommandService.updatePost(memberId, postId, "수정된 제목", "수정된 내용", null);

        // Then
        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(existingPost, times(1)).isAuthor(memberId, null);
        verify(existingPost, times(1)).updatePost("수정된 제목", "수정된 내용");
        verify(redisPostDeletePort, times(1)).deleteSinglePostCache(postId);
        verify(redisPostDeletePort, times(1)).removePostFromListCache(postId);
        verifyNoMoreInteractions(globalPostQueryPort, postCommandPort, redisPostDeletePort);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(redisPostDeletePort, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherMember(), "다른 사용자 게시글", "내용")));

        given(globalPostQueryPort.findById(postId)).willReturn(otherUserPost);
        given(otherUserPost.isAuthor(memberId, null)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(memberId, null);
        verify(otherUserPost, never()).updatePost(anyString(), anyString());
        verify(redisPostDeletePort, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        Post postToDelete = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), postTitle, "내용")));

        given(globalPostQueryPort.findById(postId)).willReturn(postToDelete);
        given(postToDelete.isAuthor(memberId, null)).willReturn(true);
        given(postToDelete.getTitle()).willReturn(postTitle);

        // When
        postCommandService.deletePost(memberId, postId, null);

        // Then
        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(postToDelete, times(1)).isAuthor(memberId, null);
        // CASCADE로 Comment와 PostLike 자동 삭제되므로 명시적 호출 없음
        verify(postCommandPort, times(1)).delete(postToDelete);
        verify(redisPostDeletePort, times(1)).deleteSinglePostCache(postId);
        verify(redisPostDeletePort, times(1)).removePostIdFromRealtimeScore(postId);
        verify(redisPostDeletePort, times(1)).removePostFromListCache(postId);
        verify(redisPostDeletePort, times(1)).removePostIdFromStorage(postId);
        verifyNoMoreInteractions(globalPostQueryPort, postCommandPort, redisPostDeletePort);
    }

    @Test
    @DisplayName("게시글 삭제 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForDelete() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostDeletePort, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherMember(), "다른 사용자 게시글", "내용")));

        given(globalPostQueryPort.findById(postId)).willReturn(otherUserPost);
        given(otherUserPost.isAuthor(memberId, null)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.FORBIDDEN);

        verify(globalPostQueryPort, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(memberId, null);
        verify(postCommandPort, never()).delete(any());
        verify(redisPostDeletePort, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("회원 작성 게시글 일괄 삭제 - 게시글 존재")
    void shouldDeleteAllPostsByMemberId_WhenPostsExist() {
        // Given
        Long memberId = 1L;
        Long postId1 = 10L;
        Long postId2 = 11L;

        given(postQueryPort.findPostIdsMemberId(memberId)).willReturn(List.of(postId1, postId2));
        // When
        postCommandService.deleteAllPostsByMemberId(memberId);

        // Then
        verify(postQueryPort, times(1)).findPostIdsMemberId(memberId);
        verify(redisPostDeletePort, times(1)).deleteSinglePostCache(postId1);
        verify(redisPostDeletePort, times(1)).deleteSinglePostCache(postId2);
        verify(postCommandPort, times(1)).deleteAllByMemberId(memberId);
        verifyNoMoreInteractions(postQueryPort, postCommandPort, redisPostDeletePort);
    }

    @Test
    @DisplayName("회원 작성 게시글 일괄 삭제 - 게시글 없음")
    void shouldSkipDeletingPosts_WhenNoPostsExist() {
        // Given
        Long memberId = 1L;
        given(postQueryPort.findPostIdsMemberId(memberId)).willReturn(List.of());

        // When
        postCommandService.deleteAllPostsByMemberId(memberId);

        // Then
        verify(postQueryPort, times(1)).findPostIdsMemberId(memberId);
        verify(postCommandPort, times(1)).deleteAllByMemberId(memberId);
        verify(redisPostDeletePort, never()).deleteSinglePostCache(any());
        verifyNoMoreInteractions(postQueryPort, postCommandPort, redisPostDeletePort);
    }


    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "제목", "내용")));

        given(globalPostQueryPort.findById(postId)).willReturn(existingPost);
        given(existingPost.isAuthor(memberId, null)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(redisPostDeletePort).deleteSinglePostCache(postId);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cache delete failed");

        // 게시글 수정은 완료되지만 캐시 삭제에서 실패
        verify(existingPost, times(1)).updatePost("title", "content");
        verify(redisPostDeletePort, times(1)).deleteSinglePostCache(postId);
    }
}
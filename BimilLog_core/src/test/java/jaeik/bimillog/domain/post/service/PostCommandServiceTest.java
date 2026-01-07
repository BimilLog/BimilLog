package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.out.PostQueryRepository;
import jaeik.bimillog.domain.post.out.PostRepository;
import jaeik.bimillog.domain.post.out.PostToMemberAdapter;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.post.RedisRealTimePostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisDetailPostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier1PostStoreAdapter;
import jaeik.bimillog.infrastructure.redis.post.RedisTier2PostStoreAdapter;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

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
    private PostToMemberAdapter postToMemberAdapter;

    @Mock
    private RedisDetailPostStoreAdapter redisDetailPostStoreAdapter;

    @Mock
    private RedisTier2PostStoreAdapter redisTier2PostStoreAdapter;

    @Mock
    private RedisRealTimePostStoreAdapter redisRealTimePostStoreAdapter;

    @Mock
    private RedisTier1PostStoreAdapter redisTier1PostStoreAdapter;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentCommandService commentCommandService;

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

        given(postToMemberAdapter.getReferenceById(memberId)).willReturn(getTestMember());
        given(postRepository.save(any(Post.class))).willReturn(createdPost);

        // When
        Long result = postCommandService.writePost(memberId, title, content, password);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(postToMemberAdapter, times(1)).getReferenceById(memberId);
        verify(postRepository, times(1)).save(any(Post.class));
        verifyNoMoreInteractions(postToMemberAdapter, postRepository);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "기존 제목", "기존 내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));
        given(existingPost.isAuthor(memberId, null)).willReturn(true);

        // When
        postCommandService.updatePost(memberId, postId, "수정된 제목", "수정된 내용", null);

        // Then
        verify(postRepository, times(1)).findById(postId);
        verify(existingPost, times(1)).isAuthor(memberId, null);
        verify(existingPost, times(1)).updatePost("수정된 제목", "수정된 내용");
        verify(redisDetailPostStoreAdapter, times(1)).deleteSinglePostCache(postId);
        verify(redisTier1PostStoreAdapter, times(1)).removePostFromListCache(postId);
        verifyNoMoreInteractions(postRepository, redisDetailPostStoreAdapter);
    }

    @Test
    @DisplayName("게시글 수정 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForUpdate() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postRepository, times(1)).findById(postId);
        verify(redisDetailPostStoreAdapter, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForUpdate() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherMember(), "다른 사용자 게시글", "내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(otherUserPost));
        given(otherUserPost.isAuthor(memberId, null)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_FORBIDDEN);

        verify(postRepository, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(memberId, null);
        verify(otherUserPost, never()).updatePost(anyString(), anyString());
        verify(redisDetailPostStoreAdapter, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void shouldDeletePost_WhenValidAuthor() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;
        String postTitle = "삭제될 게시글";

        Post postToDelete = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), postTitle, "내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(postToDelete));
        given(postToDelete.isAuthor(memberId, null)).willReturn(true);
        given(postToDelete.getTitle()).willReturn(postTitle);

        // When
        postCommandService.deletePost(memberId, postId, null);

        // Then
        verify(postRepository, times(1)).findById(postId);
        verify(postToDelete, times(1)).isAuthor(memberId, null);
        // CASCADE로 Comment와 PostLike 자동 삭제되므로 명시적 호출 없음
        verify(postRepository, times(1)).delete(postToDelete);
        verify(redisDetailPostStoreAdapter, times(1)).deleteSinglePostCache(postId);
        verify(redisRealTimePostStoreAdapter, times(1)).removePostIdFromRealtimeScore(postId);
        verify(redisTier1PostStoreAdapter, times(1)).removePostFromListCache(postId);
        verify(redisTier2PostStoreAdapter, times(1)).removePostIdFromStorage(postId);
        verifyNoMoreInteractions(postRepository, redisDetailPostStoreAdapter);
    }

    @Test
    @DisplayName("게시글 삭제 - 게시글 없음 예외")
    void shouldThrowException_WhenPostNotFoundForDelete() {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, never()).delete(any());
        verify(redisDetailPostStoreAdapter, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음 예외")
    void shouldThrowException_WhenNotAuthorForDelete() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherMember(), "다른 사용자 게시글", "내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(otherUserPost));
        given(otherUserPost.isAuthor(memberId, null)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_FORBIDDEN);

        verify(postRepository, times(1)).findById(postId);
        verify(otherUserPost, times(1)).isAuthor(memberId, null);
        verify(postRepository, never()).delete(any());
        verify(redisDetailPostStoreAdapter, never()).deleteSinglePostCache(any());
    }

    @Test
    @DisplayName("회원 작성 게시글 일괄 삭제 - 게시글 존재")
    void shouldDeleteAllPostsByMemberId_WhenPostsExist() {
        // Given
        Long memberId = 1L;
        Long postId1 = 10L;
        Long postId2 = 11L;

        given(postRepository.findIdsWithCacheFlagByMemberId(memberId)).willReturn(List.of(postId1, postId2));

        // When
        postCommandService.deleteAllPostsByMemberId(memberId);

        // Then
        verify(postRepository, times(1)).findIdsWithCacheFlagByMemberId(memberId);

        // 각 게시글의 댓글 삭제 확인 (FK 제약 조건 위반 방지)
        verify(commentCommandService, times(1)).deleteCommentsByPost(postId1);
        verify(commentCommandService, times(1)).deleteCommentsByPost(postId2);

        // 캐시 삭제 확인
        verify(redisDetailPostStoreAdapter, times(1)).deleteSinglePostCache(postId1);
        verify(redisDetailPostStoreAdapter, times(1)).deleteSinglePostCache(postId2);

        // 게시글 일괄 삭제 확인
        verify(postRepository, times(1)).deleteAllByMemberId(memberId);

        verifyNoMoreInteractions(postQueryRepository, postRepository, redisDetailPostStoreAdapter, commentCommandService);
    }

    @Test
    @DisplayName("회원 작성 게시글 일괄 삭제 - 게시글 없음")
    void shouldSkipDeletingPosts_WhenNoPostsExist() {
        // Given
        Long memberId = 1L;
        given(postRepository.findIdsWithCacheFlagByMemberId(memberId)).willReturn(List.of());

        // When
        postCommandService.deleteAllPostsByMemberId(memberId);

        // Then
        verify(postRepository, times(1)).findIdsWithCacheFlagByMemberId(memberId);
        verify(postRepository, times(1)).deleteAllByMemberId(memberId);

        // 게시글이 없으므로 댓글 삭제와 캐시 삭제가 호출되지 않아야 함
        verify(commentCommandService, never()).deleteCommentsByPost(any());
        verify(redisDetailPostStoreAdapter, never()).deleteSinglePostCache(any());

        verifyNoMoreInteractions(postQueryRepository, postRepository, redisDetailPostStoreAdapter, commentCommandService);
    }


    @Test
    @DisplayName("게시글 수정 - 캐시 삭제 실패해도 정상 완료")
    void shouldUpdatePostEvenWhenCacheDeleteFails() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "제목", "내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));
        given(existingPost.isAuthor(memberId, null)).willReturn(true);
        doThrow(new RuntimeException("Cache delete failed")).when(redisDetailPostStoreAdapter).deleteSinglePostCache(postId);

        // When - 예외가 발생하지 않고 정상 완료되어야 함
        postCommandService.updatePost(memberId, postId, "title", "content", null);

        // Then - 게시글 수정은 완료되고, 캐시 삭제도 시도됨
        verify(existingPost, times(1)).updatePost("title", "content");
        verify(redisDetailPostStoreAdapter, times(1)).deleteSinglePostCache(postId);
    }
}
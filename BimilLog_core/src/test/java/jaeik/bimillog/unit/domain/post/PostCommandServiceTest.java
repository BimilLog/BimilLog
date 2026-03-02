package jaeik.bimillog.unit.domain.post;

import jaeik.bimillog.domain.comment.service.CommentCommandService;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.domain.post.event.PostModifiedEvent;
import jaeik.bimillog.domain.post.event.PostRemovedEvent;
import jaeik.bimillog.domain.post.event.PostWrittenEvent;
import jaeik.bimillog.domain.post.repository.PostRepository;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.service.PostCommandService;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.PostTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostCommandService 테스트</h2>
 * <p>게시글 명령 서비스의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>Cache Invalidation 전략을 검증합니다.</p>
 *
 * @author Jaeik
 */
@DisplayName("PostCommandService 테스트")
@Tag("unit")
class PostCommandServiceTest extends BaseUnitTest {

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentCommandService commentCommandService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        Integer password = null;

        Post createdPost = PostTestDataBuilder.withId(expectedPostId, PostTestDataBuilder.createPost(getTestMember(), title, content));

        given(postToMemberAdapter.getMember(memberId)).willReturn(getTestMember());
        given(postRepository.save(any(Post.class))).willReturn(createdPost);

        // When
        Long result = postCommandService.writePost(memberId, title, content, password);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(postToMemberAdapter, times(1)).getMember(memberId);
        verify(postRepository, times(1)).save(any(Post.class));
        // 새 글 작성 후 캐시 이벤트 발행
        verify(eventPublisher, times(1)).publishEvent(any(PostWrittenEvent.class));
        verifyNoMoreInteractions(postToMemberAdapter, postRepository);
    }

    @Test
    @DisplayName("게시글 수정 - 성공 (Cache Invalidation)")
    void shouldUpdatePost_WhenValidAuthor() {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post existingPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getTestMember(), "기존 제목", "기존 내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));
        doNothing().when(existingPost).validateAuthor(memberId, null);

        // When
        postCommandService.updatePost(memberId, postId, "수정된 제목", "수정된 내용", null);

        // Then
        verify(postRepository, times(1)).findById(postId);
        verify(existingPost, times(1)).validateAuthor(memberId, null);
        verify(existingPost, times(1)).updatePost("수정된 제목", "수정된 내용");
        // Cache Invalidation: 캐시 이벤트 발행
        verify(eventPublisher, times(1)).publishEvent(any(PostModifiedEvent.class));
    }

    @ParameterizedTest(name = "게시글 {0} - 게시글 없음 예외")
    @MethodSource("provideOperationTypes")
    @DisplayName("게시글 없음 예외 - 수정/삭제 공통")
    void shouldThrowException_WhenPostNotFound(String operationType) {
        // Given
        Long memberId = 1L;
        Long postId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // When & Then
        if ("수정".equals(operationType)) {
            assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
        } else {
            assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
        }

        verify(postRepository, times(1)).findById(postId);
    }

    @ParameterizedTest(name = "게시글 {0} - 권한 없음 예외")
    @MethodSource("provideOperationTypes")
    @DisplayName("권한 없음 예외 - 수정/삭제 공통")
    void shouldThrowException_WhenNotAuthor(String operationType) {
        // Given
        Long memberId = 1L;
        Long postId = 123L;

        Post otherUserPost = spy(PostTestDataBuilder.withId(postId, PostTestDataBuilder.createPost(getOtherMember(), "다른 사용자 게시글", "내용")));

        given(postRepository.findById(postId)).willReturn(Optional.of(otherUserPost));
        doThrow(new CustomException(ErrorCode.POST_FORBIDDEN)).when(otherUserPost).validateAuthor(memberId, null);

        // When & Then
        if ("수정".equals(operationType)) {
            assertThatThrownBy(() -> postCommandService.updatePost(memberId, postId, "title", "content", null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_FORBIDDEN);
            verify(otherUserPost, never()).updatePost(anyString(), anyString());
        } else {
            assertThatThrownBy(() -> postCommandService.deletePost(memberId, postId, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_FORBIDDEN);
            verify(postRepository, never()).delete(any());
        }

        verify(postRepository, times(1)).findById(postId);
        verify(otherUserPost, times(1)).validateAuthor(memberId, null);
    }

    @Test
    @DisplayName("익명 게시글 작성 - 성공")
    void shouldWriteAnonymousPost_WhenValidInput() {
        // Given
        Long expectedPostId = 123L;
        String title = "익명 게시글 제목";
        String content = "익명 게시글 내용";
        Integer password = 1234;

        Post createdPost = PostTestDataBuilder.withId(expectedPostId, PostTestDataBuilder.createPost(null, title, content));

        given(postRepository.save(any(Post.class))).willReturn(createdPost);

        // When
        Long result = postCommandService.writePost(null, title, content, password);

        // Then
        assertThat(result).isEqualTo(expectedPostId);

        verify(postToMemberAdapter, never()).getMember(any());
        verify(postRepository, times(1)).save(any(Post.class));
        verify(eventPublisher, times(1)).publishEvent(any(PostWrittenEvent.class));
    }

    @Test
    @DisplayName("게시글 작성 - memberId와 password 둘 다 null이면 예외")
    void shouldThrowException_WhenBothMemberIdAndPasswordNull() {
        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(null, "제목", "내용", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_BLANK_PASSWORD);

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("게시글 작성 - memberId와 password 둘 다 있으면 예외")
    void shouldThrowException_WhenBothMemberIdAndPasswordPresent() {
        // When & Then
        assertThatThrownBy(() -> postCommandService.writePost(1L, "제목", "내용", 1234))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_BLANK_PASSWORD);

        verify(postRepository, never()).save(any());
    }

    private static Stream<Arguments> provideOperationTypes() {
        return Stream.of(
                Arguments.of("수정"),
                Arguments.of("삭제")
        );
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
        doNothing().when(postToDelete).validateAuthor(memberId, null);

        // When
        postCommandService.deletePost(memberId, postId, null);

        // Then
        verify(postRepository, times(1)).findById(postId);
        verify(postToDelete, times(1)).validateAuthor(memberId, null);
        // CASCADE로 Comment와 PostLike 자동 삭제되므로 명시적 호출 없음
        verify(postRepository, times(1)).delete(postToDelete);
        // 모든 캐시 이벤트 발행
        verify(eventPublisher, times(1)).publishEvent(any(PostRemovedEvent.class));
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

        // 게시글 일괄 삭제 확인
        verify(postRepository, times(1)).deleteAllByMemberId(memberId);

        // 각 게시글의 캐시 삭제 이벤트 발행 확인
        verify(eventPublisher, times(2)).publishEvent(any(PostRemovedEvent.class));
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

        // 게시글이 없으므로 댓글 삭제와 캐시 이벤트가 호출되지 않아야 함
        verify(commentCommandService, never()).deleteCommentsByPost(any());
        verify(eventPublisher, never()).publishEvent(any(PostRemovedEvent.class));
    }
}

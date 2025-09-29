package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.global.application.port.out.GlobalPostQueryPort;
import jaeik.bimillog.domain.post.application.service.PostAdminService;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * <h2>PostAdminService 테스트</h2>
 * <p>게시글 공지사항 서비스의 핵심 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>공지 설정/해제 DB 로직에만 집중하며, 캐시는 Controller에서 분리됨</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("PostAdminService 테스트")
@Tag("test")
class PostAdminServiceTest extends BaseUnitTest {

    @Mock
    private GlobalPostQueryPort globalPostQueryPort;

    @Mock
    private Post post;

    @InjectMocks
    private PostAdminService postAdminService;

    @Test
    @DisplayName("게시글 공지 토글 - 일반 게시글을 공지로 설정")
    void shouldTogglePostNotice_WhenNormalPostToNotice() {
        // Given
        Long postId = 123L;
        String postTitle = "중요한 공지사항";

        given(globalPostQueryPort.findById(postId)).willReturn(post);
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(false); // 현재 공지 아님

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(globalPostQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).setAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - 존재하지 않는 게시글")
    void shouldThrowException_WhenToggleNonExistentPost() {
        // Given
        Long postId = 999L;

        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(globalPostQueryPort).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - null postId")
    void shouldThrowException_WhenTogglePostWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThatThrownBy(() -> postAdminService.togglePostNotice(postId))
                .isInstanceOf(Exception.class);

        verify(globalPostQueryPort).findById(postId);
        verify(post, never()).isNotice();
        verify(post, never()).setAsNotice();
        verify(post, never()).unsetAsNotice();
    }

    @Test
    @DisplayName("게시글 공지 토글 - 공지 게시글을 일반 게시글로 해제")
    void shouldTogglePostNotice_WhenNoticePostToNormal() {
        // Given
        Long postId = 123L;
        String postTitle = "공지 해제될 게시글";

        given(globalPostQueryPort.findById(postId)).willReturn(post);
        given(post.getTitle()).willReturn(postTitle);
        given(post.isNotice()).willReturn(true); // 현재 공지임

        // When
        postAdminService.togglePostNotice(postId);

        // Then
        verify(globalPostQueryPort).findById(postId);
        verify(post, times(2)).isNotice(); // 상태 확인 (if문 + 로그)
        verify(post).unsetAsNotice();
    }


    @Test
    @DisplayName("게시글 공지 상태 확인 - 공지인 게시글")
    void shouldReturnTrue_WhenPostIsNotice() {
        // Given
        Long postId = 123L;
        
        given(globalPostQueryPort.findById(postId)).willReturn(post);
        given(post.isNotice()).willReturn(true);

        // When
        boolean result = postAdminService.isPostNotice(postId);

        // Then
        assertThat(result).isTrue();
        verify(globalPostQueryPort).findById(postId);
        verify(post).isNotice();
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - 일반 게시글")
    void shouldReturnFalse_WhenPostIsNotNotice() {
        // Given
        Long postId = 123L;
        
        given(globalPostQueryPort.findById(postId)).willReturn(post);
        given(post.isNotice()).willReturn(false);

        // When
        boolean result = postAdminService.isPostNotice(postId);

        // Then
        assertThat(result).isFalse();
        verify(globalPostQueryPort).findById(postId);
        verify(post).isNotice();
    }

    @Test
    @DisplayName("게시글 공지 상태 확인 - 존재하지 않는 게시글")
    void shouldThrowException_WhenCheckNonExistentPostNotice() {
        // Given
        Long postId = 999L;

        given(globalPostQueryPort.findById(postId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> postAdminService.isPostNotice(postId))
                .isInstanceOf(PostCustomException.class)
                .hasFieldOrPropertyWithValue("postErrorCode", PostErrorCode.POST_NOT_FOUND);

        verify(globalPostQueryPort).findById(postId);
        verify(post, never()).isNotice();
    }

}
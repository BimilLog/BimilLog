package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.auth.out.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * <h2>CommentQueryService 테스트</h2>
 * <p>사용자 정보 유무에 따라 다른 파라미터로 포트가 호출되는지 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentQueryService 테스트")
@Tag("unit")
class CommentQueryServiceTest {

    @Mock
    private CommentQueryPort commentQueryPort;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    @DisplayName("인기 댓글 조회 - 사용자 정보가 있으면 ID 전달")
    void shouldPassMemberIdForPopularComments() {
        // Given
        Long postId = 1L;
        Long memberId = 10L;
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getMemberId()).willReturn(memberId);
        given(commentQueryPort.findPopularComments(postId, memberId)).willReturn(List.of());

        // When
        List<CommentInfo> result = commentQueryService.getPopularComments(postId, userDetails);

        // Then
        assertThat(result).isEmpty();
        verify(commentQueryPort).findPopularComments(postId, memberId);
    }

    @Test
    @DisplayName("인기 댓글 조회 - 사용자 정보가 없으면 null 전달")
    void shouldPassNullForPopularCommentsWhenAnonymous() {
        // Given
        Long postId = 2L;
        given(commentQueryPort.findPopularComments(postId, null)).willReturn(List.of());

        // When
        List<CommentInfo> result = commentQueryService.getPopularComments(postId, null);

        // Then
        assertThat(result).isEmpty();
        verify(commentQueryPort).findPopularComments(postId, null);
    }

    @Test
    @DisplayName("댓글 목록 조회 - 사용자 정보에 따라 ID 전달")
    void shouldHandleOldestCommentsWithMemberId() {
        // Given
        Long postId = 3L;
        PageRequest pageable = PageRequest.of(0, 10);
        Long memberId = 20L;
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getMemberId()).willReturn(memberId);
        Page<CommentInfo> expected = new PageImpl<>(List.of());
        given(commentQueryPort.findCommentsWithOldestOrder(postId, pageable, memberId)).willReturn(expected);

        // When
        Page<CommentInfo> result = commentQueryService.getCommentsOldestOrder(postId, pageable, userDetails);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(commentQueryPort).findCommentsWithOldestOrder(postId, pageable, memberId);
    }

    @Test
    @DisplayName("댓글 목록 조회 - 사용자 정보 없으면 null 전달")
    void shouldHandleOldestCommentsWithoutMemberId() {
        // Given
        Long postId = 4L;
        PageRequest pageable = PageRequest.of(0, 5);
        Page<CommentInfo> expected = Page.empty();
        given(commentQueryPort.findCommentsWithOldestOrder(postId, pageable, null)).willReturn(expected);

        // When
        Page<CommentInfo> result = commentQueryService.getCommentsOldestOrder(postId, pageable, null);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(commentQueryPort).findCommentsWithOldestOrder(postId, pageable, null);
    }
}

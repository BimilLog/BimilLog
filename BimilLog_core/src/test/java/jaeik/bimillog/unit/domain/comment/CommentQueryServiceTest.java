package jaeik.bimillog.unit.domain.comment;

import jaeik.bimillog.application.comment.dto.CommentDTO;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.repository.CommentQueryRepository;
import jaeik.bimillog.domain.comment.service.CommentQueryService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
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
 * <p>BFF 방식으로 인기 댓글과 일반 댓글을 통합 조회하는 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentQueryService 테스트")
@Tag("unit")
class CommentQueryServiceTest {

    @Mock
    private CommentQueryRepository commentQueryRepository;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    @DisplayName("댓글 통합 조회 - 인기댓글 + 일반댓글 (로그인)")
    void shouldFindComments_WhenLoggedIn() {
        // Given
        Long postId = 3L;
        Long memberId = 20L;
        PageRequest pageable = PageRequest.of(0, 10);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getMemberId()).willReturn(memberId);

        Page<CommentInfo> expectedComments = new PageImpl<>(List.of());
        List<CommentInfo> expectedPopular = List.of();
        given(commentQueryRepository.findComments(postId, pageable, memberId)).willReturn(expectedComments);
        given(commentQueryRepository.findPopularComments(postId, memberId)).willReturn(expectedPopular);

        // When
        CommentDTO result = commentQueryService.findComments(postId, pageable, userDetails);

        // Then
        assertThat(result.getCommentInfoPage()).isEqualTo(expectedComments);
        assertThat(result.getPopularCommentList()).isEqualTo(expectedPopular);
        verify(commentQueryRepository).findComments(postId, pageable, memberId);
        verify(commentQueryRepository).findPopularComments(postId, memberId);
    }

    @Test
    @DisplayName("댓글 통합 조회 - 인기댓글 + 일반댓글 (비로그인)")
    void shouldFindComments_WhenNotLoggedIn() {
        // Given
        Long postId = 3L;
        PageRequest pageable = PageRequest.of(0, 10);

        Page<CommentInfo> expectedComments = new PageImpl<>(List.of());
        List<CommentInfo> expectedPopular = List.of();
        given(commentQueryRepository.findComments(postId, pageable, null)).willReturn(expectedComments);
        given(commentQueryRepository.findPopularComments(postId, null)).willReturn(expectedPopular);

        // When
        CommentDTO result = commentQueryService.findComments(postId, pageable, null);

        // Then
        assertThat(result.getCommentInfoPage()).isEqualTo(expectedComments);
        assertThat(result.getPopularCommentList()).isEqualTo(expectedPopular);
        verify(commentQueryRepository).findComments(postId, pageable, null);
        verify(commentQueryRepository).findPopularComments(postId, null);
    }
}

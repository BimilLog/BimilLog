package jaeik.bimillog.infrastructure.adapter.post.comment;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.infrastructure.adapter.out.post.PostToCommentAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostToCommentAdapter 단위 테스트</h2>
 * <p>Post 도메인에서 Comment 도메인으로의 어댑터 기능을 테스트합니다.</p>
 * <p>게시글 목록의 댓글 수 조회 기능 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class PostToCommentAdapterTest {

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    private PostToCommentAdapter postToCommentAdapter;

    @BeforeEach
    void setUp() {
        postToCommentAdapter = new PostToCommentAdapter(commentQueryUseCase);
    }

    @Test
    @DisplayName("정상 케이스 - 게시글 ID 목록으로 댓글 수 조회")
    void shouldReturnCommentCounts_WhenValidPostIdsProvided() {
        // given
        List<Long> postIds = Arrays.asList(1L, 2L, 3L);
        Map<Long, Integer> expectedCounts = Map.of(
                1L, 5,
                2L, 10,
                3L, 0
        );
        
        given(commentQueryUseCase.findCommentCountsByPostIds(postIds))
                .willReturn(expectedCounts);

        // when
        Map<Long, Integer> result = postToCommentAdapter.findCommentCountsByPostIds(postIds);

        // then
        assertThat(result)
                .hasSize(3)
                .containsEntry(1L, 5)
                .containsEntry(2L, 10)
                .containsEntry(3L, 0);
        
        verify(commentQueryUseCase).findCommentCountsByPostIds(postIds);
    }

    @Test
    @DisplayName("빈 목록 입력 시 빈 맵 반환")
    void shouldReturnEmptyMap_WhenEmptyPostIdsProvided() {
        // given
        List<Long> postIds = Collections.emptyList();
        Map<Long, Integer> expectedCounts = Collections.emptyMap();
        
        given(commentQueryUseCase.findCommentCountsByPostIds(postIds))
                .willReturn(expectedCounts);

        // when
        Map<Long, Integer> result = postToCommentAdapter.findCommentCountsByPostIds(postIds);

        // then
        assertThat(result).isEmpty();
        verify(commentQueryUseCase).findCommentCountsByPostIds(postIds);
    }

}
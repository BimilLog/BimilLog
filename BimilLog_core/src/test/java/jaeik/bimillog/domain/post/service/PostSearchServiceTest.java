package jaeik.bimillog.domain.post.service;

import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.repository.PostSearchRepository;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * <h2>PostSearchService 단위 테스트</h2>
 * <p>검색 전략 라우팅 로직(전문검색 / 접두사 / 부분검색)과 블랙리스트 필터링을 검증합니다.</p>
 */
@DisplayName("PostSearchService 단위 테스트")
@Tag("unit")
class PostSearchServiceTest extends BaseUnitTest {

    @Mock
    private PostSearchRepository postSearchRepository;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @InjectMocks
    private PostSearchService postSearchService;

    private final Pageable pageable = PageRequest.of(0, 10);

    // ==================== 전략 1: 전문검색 ====================

    @Test
    @DisplayName("3글자 이상 + TITLE → 전문검색 (findByFullTextSearch) 호출")
    void shouldCallFullTextSearch_WhenQuery3CharsAndTypeTitle() {
        // Given
        given(postSearchRepository.findByFullTextSearch(eq(PostQueryType.TITLE), eq("프로그래밍"), eq(pageable), isNull()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "프로그래밍", pageable, null);

        // Then
        verify(postSearchRepository).findByFullTextSearch(eq(PostQueryType.TITLE), eq("프로그래밍"), eq(pageable), isNull());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("3글자 이상 + TITLE_CONTENT → 전문검색 (findByFullTextSearch) 호출")
    void shouldCallFullTextSearch_WhenQuery3CharsAndTypeTitleContent() {
        // Given
        given(postSearchRepository.findByFullTextSearch(eq(PostQueryType.TITLE_CONTENT), eq("스프링"), eq(pageable), isNull()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE_CONTENT, "스프링", pageable, null);

        // Then
        verify(postSearchRepository).findByFullTextSearch(eq(PostQueryType.TITLE_CONTENT), eq("스프링"), eq(pageable), isNull());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
    }

    // ==================== 전략 2: 접두사 검색 ====================

    @Test
    @DisplayName("WRITER + 4글자 이상 → 접두사 검색 (findByPrefixMatch) 호출")
    void shouldCallPrefixMatch_WhenWriterTypeAndQuery4Chars() {
        // Given
        given(postSearchRepository.findByPrefixMatch(eq(PostQueryType.WRITER), eq("작성자닉"), eq(pageable), isNull()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.WRITER, "작성자닉", pageable, null);

        // Then
        verify(postSearchRepository).findByPrefixMatch(eq(PostQueryType.WRITER), eq("작성자닉"), eq(pageable), isNull());
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPartialMatch(any(), any(), any(), any());
    }

    // ==================== 전략 3: 부분검색 ====================

    @Test
    @DisplayName("2글자 이하 → 부분검색 (findByPartialMatch) 호출")
    void shouldCallPartialMatch_WhenQueryLessThan3Chars() {
        // Given
        given(postSearchRepository.findByPartialMatch(eq(PostQueryType.TITLE), eq("자바"), eq(pageable), isNull()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자바", pageable, null);

        // Then
        verify(postSearchRepository).findByPartialMatch(eq(PostQueryType.TITLE), eq("자바"), eq(pageable), isNull());
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("WRITER + 3글자 이하 → 부분검색 (findByPartialMatch) 호출")
    void shouldCallPartialMatch_WhenWriterTypeAndQueryLessThan4Chars() {
        // Given
        given(postSearchRepository.findByPartialMatch(eq(PostQueryType.WRITER), eq("닉네"), eq(pageable), isNull()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.WRITER, "닉네", pageable, null);

        // Then
        verify(postSearchRepository).findByPartialMatch(eq(PostQueryType.WRITER), eq("닉네"), eq(pageable), isNull());
        verify(postSearchRepository, never()).findByFullTextSearch(any(), any(), any(), any());
        verify(postSearchRepository, never()).findByPrefixMatch(any(), any(), any(), any());
    }

    // ==================== 블랙리스트 필터링 ====================

    @Test
    @DisplayName("memberId 없음 → 블랙리스트 조회 안 함")
    void shouldNotFilterBlacklist_WhenMemberIdIsNull() {
        // Given
        given(postSearchRepository.findByPartialMatch(any(), any(), any(), any()))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자", pageable, null);

        // Then
        verify(postToMemberAdapter, never()).getInterActionBlacklist(any());
    }

    @Test
    @DisplayName("memberId 있음 → 블랙리스트 조회 후 필터링")
    void shouldFilterBlacklist_WhenMemberIdProvided() {
        // Given
        Long memberId = 1L;
        given(postSearchRepository.findByPartialMatch(any(), any(), any(), eq(memberId)))
                .willReturn(Page.empty(pageable));
        given(postToMemberAdapter.getInterActionBlacklist(memberId)).willReturn(List.of());

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자", pageable, memberId);

        // Then
        verify(postToMemberAdapter).getInterActionBlacklist(memberId);
    }
}

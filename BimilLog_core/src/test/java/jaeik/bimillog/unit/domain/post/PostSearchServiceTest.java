package jaeik.bimillog.unit.domain.post;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.bimillog.domain.post.adapter.PostToMemberAdapter;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.repository.PostFulltextRepository;
import jaeik.bimillog.domain.post.repository.PostQueryRepository;
import jaeik.bimillog.domain.post.repository.PostQueryType;
import jaeik.bimillog.domain.post.service.PostSearchService;
import jaeik.bimillog.testutil.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
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
    private PostFulltextRepository postFulltextRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostToMemberAdapter postToMemberAdapter;

    @InjectMocks
    private PostSearchService postSearchService;

    private final Pageable pageable = PageRequest.of(0, 10);

    // ==================== 전략 1: 전문검색 (NATURAL LANGUAGE MODE, 와일드카드 없음) ====================

    @Test
    @DisplayName("2글자 이상 + TITLE → 전문검색 (findByTitleFullText) 호출, 와일드카드 미부착")
    void shouldCallFullTextSearch_WhenQuery2CharsAndTypeTitle() {
        // Given - F-BUG-5 라운드 6: ngram NATURAL LANGUAGE MODE 전환에 따라 query 그대로 전달 (이전: query+"*")
        given(postFulltextRepository.findByTitleFullText(eq("프로그래밍"), eq(pageable), isNull()))
                .willReturn(Collections.singletonList(new Object[]{1L, "프로그래밍 입문", 0, java.sql.Timestamp.from(java.time.Instant.now()), 1L, "writer", 0, 0, false, false, false}));
        given(postFulltextRepository.countByTitleFullText(eq("프로그래밍"), isNull()))
                .willReturn(1L);

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "프로그래밍", pageable, null);

        // Then
        verify(postFulltextRepository).findByTitleFullText(eq("프로그래밍"), eq(pageable), isNull());
        verify(postQueryRepository, never()).selectPostSimpleDetails(any(BooleanExpression.class), any(Pageable.class), any(OrderSpecifier[].class));
    }

    @Test
    @DisplayName("3글자 이상 + TITLE_CONTENT → 전문검색 (findByTitleContentFullText) 호출")
    void shouldCallFullTextSearch_WhenQuery3CharsAndTypeTitleContent() {
        // Given
        given(postFulltextRepository.findByTitleContentFullText(eq("스프링"), eq(pageable), isNull()))
                .willReturn(Collections.singletonList(new Object[]{1L, "스프링 부트", 0, java.sql.Timestamp.from(java.time.Instant.now()), 1L, "writer", 0, 0, false, false, false}));
        given(postFulltextRepository.countByTitleContentFullText(eq("스프링"), isNull()))
                .willReturn(1L);

        // When
        postSearchService.searchPost(PostQueryType.TITLE_CONTENT, "스프링", pageable, null);

        // Then
        verify(postFulltextRepository).findByTitleContentFullText(eq("스프링"), eq(pageable), isNull());
        verify(postQueryRepository, never()).selectPostSimpleDetails(any(BooleanExpression.class), any(Pageable.class), any(OrderSpecifier[].class));
    }

    @Test
    @DisplayName("FTS 결과 0건 → LIKE 부분검색 폴백")
    void shouldFallbackToPartialMatch_WhenFullTextReturnsEmpty() {
        // Given - 전문검색이 0건이면 LIKE %query% 로 폴백
        given(postFulltextRepository.findByTitleFullText(eq("자바"), eq(pageable), isNull()))
                .willReturn(List.of());
        given(postFulltextRepository.countByTitleFullText(eq("자바"), isNull()))
                .willReturn(0L);
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자바", pageable, null);

        // Then
        verify(postFulltextRepository).findByTitleFullText(eq("자바"), eq(pageable), isNull());
        verify(postQueryRepository).selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class));
    }

    // ==================== 전략 2: WRITER → 항상 부분검색 (Containing 통일) ====================

    @Test
    @DisplayName("F-BUG-5: WRITER + 4글자 이상도 부분검색 (Containing) 호출 - prefix 정책 폐기")
    void shouldCallPartialMatch_WhenWriterTypeAndQuery4Chars() {
        // Given - 라운드 5 멤버 검색 결정과 일관: WRITER 길이에 관계없이 partialCondition (LIKE %query%) 사용
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.WRITER, "작성자닉", pageable, null);

        // Then
        verify(postQueryRepository).selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class));
        verify(postFulltextRepository, never()).findByTitleFullText(anyString(), any(Pageable.class), any());
        verify(postFulltextRepository, never()).findByTitleContentFullText(anyString(), any(Pageable.class), any());
    }

    @Test
    @DisplayName("WRITER + 짧은 검색어도 부분검색 호출")
    void shouldCallPartialMatch_WhenWriterTypeAndShortQuery() {
        // Given
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.WRITER, "닉네", pageable, null);

        // Then
        verify(postQueryRepository).selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class));
        verify(postFulltextRepository, never()).findByTitleFullText(anyString(), any(Pageable.class), any());
    }

    // ==================== 전략 3: 1글자 → 부분검색 ====================

    @Test
    @DisplayName("1글자 검색어 → 부분검색 (selectPostSimpleDetails) 호출")
    void shouldCallPartialMatch_WhenQuerySingleChar() {
        // Given - ngram token_size=2 미만은 FTS 활용 불가
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
                .willReturn(Page.empty(pageable));

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자", pageable, null);

        // Then
        verify(postQueryRepository).selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class));
        verify(postFulltextRepository, never()).findByTitleFullText(anyString(), any(Pageable.class), any());
    }

    // ==================== 블랙리스트 필터링 ====================

    @Test
    @DisplayName("memberId 없음 → 블랙리스트 조회 안 함")
    void shouldNotFilterBlacklist_WhenMemberIdIsNull() {
        // Given
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
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
        PostSimpleDetail post = PostSimpleDetail.builder().id(1L).title("테스트").memberId(2L).build();
        given(postQueryRepository.selectPostSimpleDetails(any(BooleanExpression.class), eq(pageable), any(OrderSpecifier[].class)))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(postToMemberAdapter.getInterActionBlacklist(memberId)).willReturn(List.of());

        // When
        postSearchService.searchPost(PostQueryType.TITLE, "자", pageable, memberId);

        // Then
        verify(postToMemberAdapter).getInterActionBlacklist(memberId);
    }
}

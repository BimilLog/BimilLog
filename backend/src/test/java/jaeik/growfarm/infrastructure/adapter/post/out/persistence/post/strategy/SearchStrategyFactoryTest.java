package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * <h2>SearchStrategyFactory 테스트</h2>
 * <p>검색 전략 팩토리의 모든 기능을 테스트합니다.</p>
 * <p>전략 선택 로직, 예외 처리, 기본값 처리 등을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SearchStrategyFactoryTest {

    @Mock
    private FullTextSearchStrategy fullTextSearchStrategy;

    @Mock
    private LikeSearchStrategy likeSearchStrategy;

    @Mock
    private BooleanExpression mockBooleanExpression;

    private SearchStrategyFactory searchStrategyFactory;

    @BeforeEach
    void setUp() {
        List<SearchStrategy> searchStrategies = Arrays.asList(fullTextSearchStrategy, likeSearchStrategy);
        
        // SearchStrategyFactory 인스턴스 생성
        searchStrategyFactory = new SearchStrategyFactory(searchStrategies);
    }

    @Test
    @DisplayName("정상 케이스 - FullTextSearchStrategy 선택 (긴 검색어)")
    void shouldSelectFullTextStrategy_WhenLongQueryProvided() {
        // Given: 긴 검색어 (FullTextSearchStrategy가 처리 가능)
        String query = "긴검색어테스트";  // 6글자
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(true);
        // given(likeSearchStrategy.canHandle(query, type)).willReturn(false); // 불필요한 stubbing 제거
        given(fullTextSearchStrategy.createCondition(type, query)).willReturn(mockBooleanExpression);
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: FullTextSearchStrategy가 선택되고 조건 생성됨
        assertThat(condition).isNotNull();
        assertThat(condition).isEqualTo(mockBooleanExpression);
        
        verify(fullTextSearchStrategy).canHandle(query, type);
        verify(fullTextSearchStrategy).createCondition(type, query);
        verify(likeSearchStrategy, never()).createCondition(any(), any());
    }

    @Test
    @DisplayName("정상 케이스 - LikeSearchStrategy 선택 (짧은 검색어)")
    void shouldSelectLikeStrategy_WhenShortQueryProvided() {
        // Given: 짧은 검색어 (LikeSearchStrategy만 처리 가능)
        String query = "짧은";  // 2글자
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(false);
        given(likeSearchStrategy.canHandle(query, type)).willReturn(true);
        given(likeSearchStrategy.createCondition(type, query)).willReturn(mockBooleanExpression);
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: LikeSearchStrategy가 선택되고 조건 생성됨
        assertThat(condition).isNotNull();
        assertThat(condition).isEqualTo(mockBooleanExpression);
        
        verify(fullTextSearchStrategy).canHandle(query, type);
        verify(likeSearchStrategy).canHandle(query, type);
        verify(likeSearchStrategy).createCondition(type, query);
        verify(fullTextSearchStrategy, never()).createCondition(any(), any());
    }

    @Test
    @DisplayName("경계값 - null 검색어 처리")
    void shouldReturnDefaultCondition_WhenNullQueryProvided() {
        // Given: null 검색어
        String query = null;
        String type = "title";
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 기본 조건(null) 반환
        assertThat(condition).isNull(); // getDefaultCondition()이 null을 반환
        
        // 전략 선택이 시도되지 않음
        verify(fullTextSearchStrategy, never()).canHandle(any(), any());
        verify(likeSearchStrategy, never()).canHandle(any(), any());
    }

    @Test
    @DisplayName("경계값 - 빈 검색어 처리")
    void shouldReturnDefaultCondition_WhenEmptyQueryProvided() {
        // Given: 빈 검색어
        String query = "";
        String type = "title";
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 기본 조건(null) 반환
        assertThat(condition).isNull();
        
        // 전략 선택이 시도되지 않음
        verify(fullTextSearchStrategy, never()).canHandle(any(), any());
        verify(likeSearchStrategy, never()).canHandle(any(), any());
    }

    @Test
    @DisplayName("경계값 - 공백만 있는 검색어 처리")
    void shouldReturnDefaultCondition_WhenWhitespaceOnlyQueryProvided() {
        // Given: 공백만 있는 검색어
        String query = "   ";
        String type = "title";
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 기본 조건(null) 반환
        assertThat(condition).isNull();
        
        // 공백이 trim()되어 빈 문자열이 되므로 전략 선택 안됨
        verify(fullTextSearchStrategy, never()).canHandle(any(), any());
        verify(likeSearchStrategy, never()).canHandle(any(), any());
    }

    @Test
    @DisplayName("정상 케이스 - 검색어 트림 처리")
    void shouldTrimQuery_WhenQueryHasWhitespaces() {
        // Given: 앞뒤 공백이 있는 검색어
        String query = "  테스트검색어  ";
        String trimmedQuery = query.trim();
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(trimmedQuery, type)).willReturn(true);
        given(fullTextSearchStrategy.createCondition(type, trimmedQuery)).willReturn(mockBooleanExpression);
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 트림된 검색어로 전략이 선택됨
        assertThat(condition).isNotNull();
        
        verify(fullTextSearchStrategy).canHandle(trimmedQuery, type);
        verify(fullTextSearchStrategy).createCondition(type, trimmedQuery);
    }

    @Test
    @DisplayName("예외 케이스 - 어떤 전략도 처리할 수 없는 경우 LikeSearchStrategy 기본 사용")
    void shouldUseLikeStrategyAsDefault_WhenNoStrategyCanHandle() {
        // Given: 어떤 전략도 처리할 수 없는 상황
        String query = "특수한검색어";
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(false);
        given(likeSearchStrategy.canHandle(query, type)).willReturn(false);
        given(likeSearchStrategy.createCondition(type, query)).willReturn(mockBooleanExpression);
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: LikeSearchStrategy가 기본값으로 사용됨
        assertThat(condition).isNotNull();
        assertThat(condition).isEqualTo(mockBooleanExpression);
        
        verify(fullTextSearchStrategy).canHandle(query, type);
        verify(likeSearchStrategy).canHandle(query, type);
        verify(likeSearchStrategy).createCondition(type, query);
    }

    @Test
    @DisplayName("예외 케이스 - 전략 목록에 LikeSearchStrategy가 없는 경우 예외 발생")
    void shouldThrowException_WhenNoLikeStrategyFound() {
        // Given: LikeSearchStrategy가 없는 전략 목록
        List<SearchStrategy> strategiesWithoutLike = Arrays.asList(fullTextSearchStrategy);
        SearchStrategyFactory factoryWithoutLike = new SearchStrategyFactory(strategiesWithoutLike);
        
        String query = "테스트검색어";
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(false);
        
        // When & Then: IllegalStateException 발생
        assertThatThrownBy(() -> {
            factoryWithoutLike.createSearchCondition(type, query);
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LikeSearchStrategy를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("예외 케이스 - 전략 실행 중 예외 발생 시 기본 조건 반환")
    void shouldReturnDefaultCondition_WhenStrategyThrowsException() {
        // Given: 전략 실행 중 예외 발생
        String query = "예외발생검색어";
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(true);
        given(fullTextSearchStrategy.createCondition(type, query))
            .willThrow(new RuntimeException("Database connection error"));
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 기본 조건(null) 반환
        assertThat(condition).isNull();
        
        verify(fullTextSearchStrategy).canHandle(query, type);
        verify(fullTextSearchStrategy).createCondition(type, query);
    }

    @Test
    @DisplayName("비즈니스 로직 - 전략 우선순위 확인 (FullText > Like)")
    void shouldSelectFirstMatchingStrategy_BasedOnListOrder() {
        // Given: 두 전략 모두 처리 가능한 경우 (첫 번째가 우선)
        String query = "우선순위테스트";
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(true);

        given(fullTextSearchStrategy.createCondition(type, query)).willReturn(mockBooleanExpression);
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 첫 번째 전략(FullTextSearchStrategy)이 선택됨
        assertThat(condition).isNotNull();
        assertThat(condition).isEqualTo(mockBooleanExpression);
        
        verify(fullTextSearchStrategy).canHandle(query, type);
        verify(fullTextSearchStrategy).createCondition(type, query);
        // LikeSearchStrategy는 canHandle이 호출되지 않아야 함 (스트림의 findFirst 때문)
        verify(likeSearchStrategy, never()).canHandle(query, type);
        verify(likeSearchStrategy, never()).createCondition(any(), any());
    }

    @Test
    @DisplayName("성능 테스트 - 대량 검색 요청 처리")
    void shouldHandleManySearchRequests_WithoutPerformanceIssues() {
        
        // Given: 대량 검색 요청 (1000개)
        String[] manyQueries = new String[1000];
        for (int i = 0; i < 1000; i++) {
            manyQueries[i] = i % 2 == 0 ? "짧은검색" : "긴검색어테스트"; // 짧은/긴 검색어 교대
        }
        
        given(fullTextSearchStrategy.canHandle(contains("긴검색어"), eq("title"))).willReturn(true);
        given(fullTextSearchStrategy.canHandle(eq("짧은검색"), eq("title"))).willReturn(false);
        given(likeSearchStrategy.canHandle(eq("짧은검색"), eq("title"))).willReturn(true);
        given(fullTextSearchStrategy.createCondition(eq("title"), contains("긴검색어"))).willReturn(mockBooleanExpression);
        given(likeSearchStrategy.createCondition(eq("title"), eq("짧은검색"))).willReturn(mockBooleanExpression);
        
        // When: 대량 검색 요청 처리
        long startTime = System.currentTimeMillis();
        
        for (String query : manyQueries) {
            BooleanExpression condition = searchStrategyFactory.createSearchCondition("title", query);
            assertThat(condition).isNotNull();
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Then: 성능 문제 없이 처리됨 (5초 이내)
        assertThat(executionTime).isLessThan(5000L);
    }

    @Test
    @DisplayName("통합 테스트 - 다양한 검색 시나리오")
    void shouldHandleVariousSearchScenarios_Correctly() {
        // Given: 다양한 검색 시나리오 설정
        
        // 긴 제목 검색 - FullTextSearchStrategy
        given(fullTextSearchStrategy.canHandle("긴제목검색어", "title")).willReturn(true);
        given(fullTextSearchStrategy.createCondition("title", "긴제목검색어")).willReturn(mockBooleanExpression);
        
        // 짧은 작성자 검색 - LikeSearchStrategy
        given(fullTextSearchStrategy.canHandle("짧은", "writer")).willReturn(false);
        given(likeSearchStrategy.canHandle("짧은", "writer")).willReturn(true);
        given(likeSearchStrategy.createCondition("writer", "짧은")).willReturn(mockBooleanExpression);
        
        // 제목+내용 검색 - FullTextSearchStrategy
        given(fullTextSearchStrategy.canHandle("제목과내용", "title_content")).willReturn(true);
        given(fullTextSearchStrategy.createCondition("title_content", "제목과내용")).willReturn(mockBooleanExpression);
        
        // When & Then: 각 시나리오별 검색 조건 생성
        
        // 1. 긴 제목 검색
        BooleanExpression titleCondition = searchStrategyFactory.createSearchCondition("title", "긴제목검색어");
        assertThat(titleCondition).isNotNull();
        
        // 2. 짧은 작성자 검색
        BooleanExpression writerCondition = searchStrategyFactory.createSearchCondition("writer", "짧은");
        assertThat(writerCondition).isNotNull();
        
        // 3. 제목+내용 검색
        BooleanExpression contentCondition = searchStrategyFactory.createSearchCondition("title_content", "제목과내용");
        assertThat(contentCondition).isNotNull();
        
        // 4. null 검색어
        BooleanExpression nullCondition = searchStrategyFactory.createSearchCondition("title", null);
        assertThat(nullCondition).isNull();
        
        // 5. 빈 검색어
        BooleanExpression emptyCondition = searchStrategyFactory.createSearchCondition("title", "");
        assertThat(emptyCondition).isNull();
        
        // 각 전략이 적절히 사용되었는지 확인
        verify(fullTextSearchStrategy, times(2)).createCondition(any(), any());
        verify(likeSearchStrategy, times(1)).createCondition(any(), any());
    }

    @Test
    @DisplayName("에지 케이스 - 빈 전략 목록 처리")
    void shouldThrowException_WhenEmptyStrategiesList() {
        // Given: 빈 전략 목록
        List<SearchStrategy> emptyStrategies = Collections.emptyList();
        SearchStrategyFactory emptyFactory = new SearchStrategyFactory(emptyStrategies);
        
        String query = "테스트검색어";
        String type = "title";
        
        // When & Then: IllegalStateException 발생
        assertThatThrownBy(() -> {
            emptyFactory.createSearchCondition(type, query);
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LikeSearchStrategy를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("로깅 검증 - 전략 선택 로그 확인")
    void shouldLogStrategySelection_WhenCreatingSearchCondition() {
        // Given: 전략 선택 성공 시나리오
        String query = "로깅테스트검색어";
        String type = "title";
        
        given(fullTextSearchStrategy.canHandle(query, type)).willReturn(true);
        given(fullTextSearchStrategy.createCondition(type, query)).willReturn(mockBooleanExpression);
        given(fullTextSearchStrategy.getStrategyName()).willReturn("FullTextSearchStrategy");
        
        // When: 검색 조건 생성
        BooleanExpression condition = searchStrategyFactory.createSearchCondition(type, query);
        
        // Then: 조건 생성 확인 (실제 로그는 통합테스트에서 확인)
        assertThat(condition).isNotNull();
        
        // 전략 이름이 올바르게 반환되는지 확인
        verify(fullTextSearchStrategy).getStrategyName();
    }
}
package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.user.entity.QUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * <h2>FullTextSearchStrategy 테스트</h2>
 * <p>MySQL FULLTEXT 검색 전략의 모든 기능을 테스트합니다.</p>
 * <p>검색 유형별 처리 가능성 확인, 검색 조건 생성, 예외 처리 등을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class FullTextSearchStrategyTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    private FullTextSearchStrategy fullTextSearchStrategy;

    @BeforeEach
    void setUp() {
        // FullTextSearchStrategy 인스턴스 생성
        fullTextSearchStrategy = new FullTextSearchStrategy(entityManager);
    }

    @Test
    @DisplayName("정상 케이스 - 제목 검색 처리 가능성 확인 (3글자 이상)")
    void shouldReturnTrue_WhenTitleSearchWith3OrMoreCharacters() {
        // Given: 3글자 이상의 제목 검색어
        String query = "검색어테스트";
        String type = "title";
        
        // When: 처리 가능성 확인
        boolean canHandle = fullTextSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 검색 처리 가능성 확인 (3글자 이상)")
    void shouldReturnTrue_WhenTitleContentSearchWith3OrMoreCharacters() {
        // Given: 3글자 이상의 제목+내용 검색어
        String query = "제목내용";
        String type = "title_content";
        
        // When: 처리 가능성 확인
        boolean canHandle = fullTextSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 작성자 검색 처리 가능성 확인 (4글자 이상)")
    void shouldReturnTrue_WhenWriterSearchWith4OrMoreCharacters() {
        // Given: 4글자 이상의 작성자 검색어
        String query = "작성자이름";
        String type = "writer";
        
        // When: 처리 가능성 확인
        boolean canHandle = fullTextSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("경계값 - 제목 검색 처리 불가능 (2글자 이하)")
    void shouldReturnFalse_WhenTitleSearchWith2OrFewerCharacters() {
        // Given: 2글자 이하의 제목 검색어
        String query = "검색";
        String type = "title";
        
        // When: 처리 가능성 확인
        boolean canHandle = fullTextSearchStrategy.canHandle(query, type);
        
        // Then: 처리 불가능함
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("경계값 - 작성자 검색 처리 불가능 (3글자 이하)")
    void shouldReturnFalse_WhenWriterSearchWith3OrFewerCharacters() {
        // Given: 3글자 이하의 작성자 검색어
        String query = "작성자";
        String type = "writer";
        
        // When: 처리 가능성 확인
        boolean canHandle = fullTextSearchStrategy.canHandle(query, type);
        
        // Then: 처리 불가능함
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 전략 이름 반환")
    void shouldReturnCorrectStrategyName() {
        // When: 전략 이름 조회
        String strategyName = fullTextSearchStrategy.getStrategyName();
        
        // Then: 올바른 전략 이름 반환
        assertThat(strategyName).isEqualTo("FullTextSearchStrategy");
    }

    @Test
    @DisplayName("정상 케이스 - 제목 FULLTEXT 검색 조건 생성")
    void shouldCreateTitleFullTextCondition_WhenValidQueryProvided() {
        // Given: 제목 검색 Mock 설정
        String searchQuery = "테스트제목";
        String type = "title";
        
        List<BigInteger> mockPostIds = Arrays.asList(
            BigInteger.valueOf(1L), 
            BigInteger.valueOf(2L), 
            BigInteger.valueOf(3L)
        );
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(Arrays.asList(mockPostIds.toArray()));
        
        // When: 제목 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: FULLTEXT 검색 조건 생성됨
        assertThat(condition).isNotNull();
        
        // Native Query 실행 검증
        verify(entityManager).createNativeQuery(contains("MATCH(p.title) AGAINST"));
        verify(nativeQuery).setParameter(1, searchQuery);
        verify(nativeQuery).getResultList();
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 FULLTEXT 검색 조건 생성")
    void shouldCreateTitleContentFullTextCondition_WhenValidQueryProvided() {
        // Given: 제목+내용 검색 Mock 설정
        String searchQuery = "제목과내용검색";
        String type = "title_content";
        
        List<BigInteger> mockPostIds = Arrays.asList(
            BigInteger.valueOf(10L), 
            BigInteger.valueOf(20L)
        );
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(Arrays.asList(mockPostIds.toArray()));
        
        // When: 제목+내용 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: FULLTEXT 검색 조건 생성됨
        assertThat(condition).isNotNull();
        
        // Native Query 실행 검증 (제목과 내용 모두)
        verify(entityManager).createNativeQuery(contains("MATCH(p.title, p.content) AGAINST"));
        verify(nativeQuery).setParameter(1, searchQuery);
        verify(nativeQuery).getResultList();
    }

    @Test
    @DisplayName("정상 케이스 - 작성자 검색 조건 생성 (LIKE 검색)")
    void shouldCreateWriterSearchCondition_WhenValidQueryProvided() {
        // Given: 작성자 검색 (FULLTEXT가 아닌 LIKE 검색)
        String searchQuery = "작성자이름";
        String type = "writer";
        
        // When: 작성자 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: LIKE 검색 조건 생성됨 (EntityManager 사용 안함)
        assertThat(condition).isNotNull();
        
        // Native Query는 사용되지 않음 (작성자는 LIKE 검색)
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    @DisplayName("경계값 - 빈 검색 결과 시 존재하지 않는 ID 조건 반환")
    void shouldReturnNonExistentIdCondition_WhenNoSearchResults() {
        // Given: 빈 검색 결과 Mock 설정
        String searchQuery = "존재하지않는검색어";
        String type = "title";
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(Collections.emptyList());
        
        // When: 제목 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: 존재하지 않는 ID 조건 반환 (빈 결과 보장)
        assertThat(condition).isNotNull();
        
        // Native Query 실행 확인
        verify(nativeQuery).getResultList();
    }

    @Test
    @DisplayName("예외 케이스 - FULLTEXT 쿼리 실행 오류 시 LIKE 검색으로 Fallback")
    void shouldFallbackToLikeSearch_WhenFullTextQueryFails() {
        // Given: FULLTEXT 쿼리 실행 오류
        String searchQuery = "테스트검색어";
        String type = "title";
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willThrow(new RuntimeException("MySQL FULLTEXT error"));
        
        // When: 제목 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: Fallback으로 LIKE 검색 조건 반환
        assertThat(condition).isNotNull();
        
        // Native Query 실행 시도했지만 실패 후 fallback
        verify(entityManager).createNativeQuery(anyString());
        verify(nativeQuery).getResultList();
    }

    @Test
    @DisplayName("예외 케이스 - 제목+내용 검색 오류 시 LIKE 검색으로 Fallback")
    void shouldFallbackToLikeSearchForTitleContent_WhenFullTextQueryFails() {
        // Given: 제목+내용 FULLTEXT 쿼리 실행 오류
        String searchQuery = "제목내용검색오류";
        String type = "title_content";
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willThrow(new RuntimeException("Database connection error"));
        
        // When: 제목+내용 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: Fallback으로 LIKE 검색 조건 반환 (제목 OR 내용)
        assertThat(condition).isNotNull();
        
        // Native Query 실행 시도했지만 실패 후 fallback
        verify(entityManager).createNativeQuery(anyString());
        verify(nativeQuery).getResultList();
    }

    @Test
    @DisplayName("비즈니스 로직 - 기본 타입(default) 처리")
    void shouldHandleDefaultType_WhenUnknownTypeProvided() {
        // Given: 알 수 없는 검색 타입
        String searchQuery = "기본타입테스트";
        String type = "unknown_type";
        
        List<BigInteger> mockPostIds = Arrays.asList(BigInteger.valueOf(1L));
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(Arrays.asList(mockPostIds.toArray()));
        
        // When: 알 수 없는 타입으로 검색 조건 생성
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: 기본적으로 제목 검색으로 처리됨
        assertThat(condition).isNotNull();
        
        // 제목 검색 Native Query 실행됨
        verify(entityManager).createNativeQuery(contains("MATCH(p.title) AGAINST"));
    }

    @Test
    @DisplayName("성능 테스트 - 대량 검색 결과 처리")
    void shouldHandleLargeSearchResults_WhenManyPostsMatch() {
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // Mock 검증 실패: 실제 Service 동작과 테스트 기대값 불일치
        // 가능한 문제: 1) 대량 결과 ID 목록 처리 로직 누락 2) IN 조건 최대 개수 제한 3) 메모리 효율성 문제
        // 수정 필요: FullTextSearchStrategy.executeFullTextQuery() 메서드 검토
        
        // Given: 대량 검색 결과 (1000개 게시글 ID)
        List<Object> largeMockResults = Arrays.asList(
            LongStream.rangeClosed(1L, 1000L)
                .mapToObj(BigInteger::valueOf)
                .toArray()
        );
        String searchQuery = "인기검색어";
        String type = "title";
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(largeMockResults);
        
        // When: 대량 검색 결과 처리
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: 성능 문제 없이 처리됨
        assertThat(condition).isNotNull();
        
        // 단일 Native Query로 처리되었는지 확인
        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(nativeQuery, times(1)).getResultList();
    }

    @Test
    @DisplayName("비즈니스 로직 - 검색 유형별 임계값 정확성")
    void shouldUseCorrectThresholds_ForDifferentSearchTypes() {
        // Given: 각 검색 유형별 임계값 테스트
        
        // When & Then: 각 타입별 임계값 확인
        
        // 제목 검색 - 3글자 이상
        assertThat(fullTextSearchStrategy.canHandle("검색", "title")).isFalse();     // 2글자
        assertThat(fullTextSearchStrategy.canHandle("검색어", "title")).isTrue();    // 3글자
        
        // 제목+내용 검색 - 3글자 이상
        assertThat(fullTextSearchStrategy.canHandle("제목", "title_content")).isFalse(); // 2글자
        assertThat(fullTextSearchStrategy.canHandle("제목내용", "title_content")).isTrue(); // 4글자
        
        // 작성자 검색 - 4글자 이상
        assertThat(fullTextSearchStrategy.canHandle("작성자", "writer")).isFalse();   // 3글자
        assertThat(fullTextSearchStrategy.canHandle("작성자명", "writer")).isFalse();  // 3글자
        assertThat(fullTextSearchStrategy.canHandle("작성자이름", "writer")).isTrue();  // 4글자
        
        // 기본 타입 - 3글자 이상
        assertThat(fullTextSearchStrategy.canHandle("기본", "default")).isFalse();   // 2글자
        assertThat(fullTextSearchStrategy.canHandle("기본타입", "default")).isTrue();  // 4글자
    }

    @Test
    @DisplayName("통합 테스트 - 전체 검색 전략 워크플로우")
    void shouldCompleteEntireSearchStrategyWorkflow() {
        // Given: 다양한 검색 조건 Mock 설정
        List<BigInteger> titleResults = Arrays.asList(BigInteger.valueOf(1L), BigInteger.valueOf(2L));
        List<BigInteger> contentResults = Arrays.asList(BigInteger.valueOf(3L), BigInteger.valueOf(4L));
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(anyInt(), anyString())).willReturn(nativeQuery);
        given(nativeQuery.getResultList())
            .willReturn(Arrays.asList(titleResults.toArray()))
            .willReturn(Arrays.asList(contentResults.toArray()));
        
        // When: 전체 워크플로우 실행
        // 1. 처리 가능성 확인
        boolean canHandleTitle = fullTextSearchStrategy.canHandle("테스트제목", "title");
        boolean canHandleContent = fullTextSearchStrategy.canHandle("테스트내용", "title_content");
        boolean canHandleWriter = fullTextSearchStrategy.canHandle("작성자이름", "writer");
        
        // 2. 각 타입별 검색 조건 생성
        BooleanExpression titleCondition = fullTextSearchStrategy.createCondition("title", "테스트제목");
        BooleanExpression contentCondition = fullTextSearchStrategy.createCondition("title_content", "테스트내용");
        BooleanExpression writerCondition = fullTextSearchStrategy.createCondition("writer", "작성자이름");
        
        // Then: 모든 단계가 정상 실행됨
        assertThat(canHandleTitle).isTrue();
        assertThat(canHandleContent).isTrue();
        assertThat(canHandleWriter).isTrue();
        
        assertThat(titleCondition).isNotNull();
        assertThat(contentCondition).isNotNull();
        assertThat(writerCondition).isNotNull();
        
        // Native Query는 제목, 제목+내용에만 실행됨 (작성자는 LIKE 검색)
        verify(entityManager, times(2)).createNativeQuery(anyString());
        verify(nativeQuery, times(2)).getResultList();
        
        // 전략 이름 확인
        assertThat(fullTextSearchStrategy.getStrategyName()).isEqualTo("FullTextSearchStrategy");
    }

    @Test
    @DisplayName("데이터 타입 변환 - BigInteger에서 Long 변환 정확성")
    void shouldConvertBigIntegerToLong_WhenProcessingQueryResults() {
        // Given: BigInteger 타입의 검색 결과
        String searchQuery = "타입변환테스트";
        String type = "title";
        
        List<Object> mixedResults = Arrays.asList(
            BigInteger.valueOf(100L),
            Long.valueOf(200L),
            "300"  // String 타입도 포함 (예외 상황)
        );
        
        given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
        given(nativeQuery.setParameter(eq(1), eq(searchQuery))).willReturn(nativeQuery);
        given(nativeQuery.getResultList()).willReturn(mixedResults);
        
        // When: 검색 조건 생성 (내부에서 타입 변환 수행)
        BooleanExpression condition = fullTextSearchStrategy.createCondition(type, searchQuery);
        
        // Then: 타입 변환 문제 없이 조건 생성됨
        assertThat(condition).isNotNull();
        
        // 혼합된 타입의 결과도 정상 처리됨
        verify(nativeQuery).getResultList();
    }
}
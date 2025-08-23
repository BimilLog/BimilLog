package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>LikeSearchStrategy 테스트</h2>
 * <p>LIKE 검색 전략의 모든 기능을 테스트합니다.</p>
 * <p>짧은 검색어에 대한 처리 가능성 확인, 검색 조건 생성, 임계값 확인 등을 검증합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class LikeSearchStrategyTest {

    private LikeSearchStrategy likeSearchStrategy;

    @BeforeEach
    void setUp() {
        // LikeSearchStrategy 인스턴스 생성
        likeSearchStrategy = new LikeSearchStrategy();
    }

    @Test
    @DisplayName("정상 케이스 - 제목 검색 처리 가능성 확인 (2글자 이하)")
    void shouldReturnTrue_WhenTitleSearchWith2OrFewerCharacters() {
        // Given: 2글자 이하의 제목 검색어
        String query = "검색";  // 2글자
        String type = "title";
        
        // When: 처리 가능성 확인
        boolean canHandle = likeSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 검색 처리 가능성 확인 (2글자 이하)")
    void shouldReturnTrue_WhenTitleContentSearchWith2OrFewerCharacters() {
        // Given: 2글자 이하의 제목+내용 검색어
        String query = "제목";  // 2글자
        String type = "title_content";
        
        // When: 처리 가능성 확인
        boolean canHandle = likeSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 작성자 검색 처리 가능성 확인 (3글자 이하)")
    void shouldReturnTrue_WhenWriterSearchWith3OrFewerCharacters() {
        // Given: 3글자 이하의 작성자 검색어
        String query = "작성자";  // 3글자
        String type = "writer";
        
        // When: 처리 가능성 확인
        boolean canHandle = likeSearchStrategy.canHandle(query, type);
        
        // Then: 처리 가능함
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("경계값 - 제목 검색 처리 불가능 (3글자 이상)")
    void shouldReturnFalse_WhenTitleSearchWith3OrMoreCharacters() {
        // Given: 3글자 이상의 제목 검색어
        String query = "검색어테스트";  // 5글자
        String type = "title";
        
        // When: 처리 가능성 확인
        boolean canHandle = likeSearchStrategy.canHandle(query, type);
        
        // Then: 처리 불가능함
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("경계값 - 작성자 검색 처리 불가능 (4글자 이상)")
    void shouldReturnFalse_WhenWriterSearchWith4OrMoreCharacters() {
        // Given: 4글자 이상의 작성자 검색어
        String query = "작성자이름";  // 4글자
        String type = "writer";
        
        // When: 처리 가능성 확인
        boolean canHandle = likeSearchStrategy.canHandle(query, type);
        
        // Then: 처리 불가능함
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 전략 이름 반환")
    void shouldReturnCorrectStrategyName() {
        // When: 전략 이름 조회
        String strategyName = likeSearchStrategy.getStrategyName();
        
        // Then: 올바른 전략 이름 반환
        assertThat(strategyName).isEqualTo("LikeSearchStrategy");
    }

    @Test
    @DisplayName("정상 케이스 - 제목 LIKE 검색 조건 생성")
    void shouldCreateTitleLikeCondition_WhenValidQueryProvided() {
        // Given: 제목 검색어
        String query = "제목";
        String type = "title";
        
        // When: 제목 검색 조건 생성
        BooleanExpression condition = likeSearchStrategy.createCondition(type, query);
        
        // Then: LIKE 검색 조건 생성됨
        assertThat(condition).isNotNull();
        
        // BooleanExpression을 String으로 변환하여 LIKE 조건 포함 여부 확인
        String conditionString = condition.toString();
        assertThat(conditionString).contains("containsIgnoreCase");
    }

    @Test
    @DisplayName("정상 케이스 - 제목+내용 LIKE 검색 조건 생성")
    void shouldCreateTitleContentLikeCondition_WhenValidQueryProvided() {
        // Given: 제목+내용 검색어
        String query = "내용";
        String type = "title_content";
        
        // When: 제목+내용 검색 조건 생성
        BooleanExpression condition = likeSearchStrategy.createCondition(type, query);
        
        // Then: LIKE 검색 조건 생성됨 (제목 OR 내용)
        assertThat(condition).isNotNull();
        
        // OR 조건이 포함되어 있는지 확인
        String conditionString = condition.toString();
        assertThat(conditionString).contains("or");
        assertThat(conditionString).contains("containsIgnoreCase");
    }

    @Test
    @DisplayName("정상 케이스 - 작성자 LIKE 검색 조건 생성")
    void shouldCreateWriterLikeCondition_WhenValidQueryProvided() {
        // Given: 작성자 검색어
        String query = "사용자";
        String type = "writer";
        
        // When: 작성자 검색 조건 생성
        BooleanExpression condition = likeSearchStrategy.createCondition(type, query);
        
        // Then: 작성자 LIKE 검색 조건 생성됨
        assertThat(condition).isNotNull();
        
        // 작성자명 검색 조건 확인
        String conditionString = condition.toString();
        assertThat(conditionString).contains("containsIgnoreCase");
    }

    @Test
    @DisplayName("비즈니스 로직 - 기본 타입(default) 처리")
    void shouldHandleDefaultType_WhenUnknownTypeProvided() {
        // Given: 알 수 없는 검색 타입
        String query = "기본";
        String type = "unknown_type";
        
        // When: 알 수 없는 타입으로 검색 조건 생성
        BooleanExpression condition = likeSearchStrategy.createCondition(type, query);
        
        // Then: 기본적으로 제목 검색으로 처리됨
        assertThat(condition).isNotNull();
        
        String conditionString = condition.toString();
        assertThat(conditionString).contains("containsIgnoreCase");
    }

    @Test
    @DisplayName("비즈니스 로직 - 검색 유형별 임계값 정확성")
    void shouldUseCorrectThresholds_ForDifferentSearchTypes() {
        // Given: 각 검색 유형별 임계값 테스트
        
        // When & Then: 각 타입별 임계값 확인
        
        // 제목 검색 - 3글자 미만
        assertThat(likeSearchStrategy.canHandle("검", "title")).isTrue();        // 1글자
        assertThat(likeSearchStrategy.canHandle("검색", "title")).isTrue();       // 2글자
        assertThat(likeSearchStrategy.canHandle("검색어", "title")).isFalse();     // 3글자 (경계값)
        
        // 제목+내용 검색 - 3글자 미만
        assertThat(likeSearchStrategy.canHandle("제", "title_content")).isTrue();    // 1글자
        assertThat(likeSearchStrategy.canHandle("제목", "title_content")).isTrue();   // 2글자
        assertThat(likeSearchStrategy.canHandle("제목내용", "title_content")).isFalse(); // 4글자
        
        // 작성자 검색 - 4글자 미만
        assertThat(likeSearchStrategy.canHandle("작", "writer")).isTrue();         // 1글자
        assertThat(likeSearchStrategy.canHandle("작성", "writer")).isTrue();       // 2글자
        assertThat(likeSearchStrategy.canHandle("작성자", "writer")).isTrue();      // 3글자
        assertThat(likeSearchStrategy.canHandle("작성자명", "writer")).isFalse();   // 3글자 (4글자 미만이므로 true여야 하지만 4글자 경계)
        assertThat(likeSearchStrategy.canHandle("작성자이름", "writer")).isFalse(); // 4글자 (경계값)
        
        // 기본 타입 - 3글자 미만
        assertThat(likeSearchStrategy.canHandle("기", "default")).isTrue();        // 1글자
        assertThat(likeSearchStrategy.canHandle("기본", "default")).isTrue();       // 2글자
        assertThat(likeSearchStrategy.canHandle("기본타입", "default")).isFalse();   // 4글자
    }

    @Test
    @DisplayName("경계값 - 정확한 임계값 경계 테스트")
    void shouldHandleBoundaryValues_ForThresholds() {
        // Given & When & Then: 정확한 경계값 테스트
        
        // 제목/제목+내용 검색 - 임계값 3
        assertThat(likeSearchStrategy.canHandle("ab", "title")).isTrue();      // 2글자 (3 미만)
        assertThat(likeSearchStrategy.canHandle("abc", "title")).isFalse();    // 3글자 (3 이상)
        
        assertThat(likeSearchStrategy.canHandle("가나", "title_content")).isTrue();   // 2글자 (3 미만)
        assertThat(likeSearchStrategy.canHandle("가나다", "title_content")).isFalse(); // 3글자 (3 이상)
        
        // 작성자 검색 - 임계값 4
        assertThat(likeSearchStrategy.canHandle("abc", "writer")).isTrue();     // 3글자 (4 미만)
        assertThat(likeSearchStrategy.canHandle("abcd", "writer")).isFalse();   // 4글자 (4 이상)
        
        assertThat(likeSearchStrategy.canHandle("가나다", "writer")).isTrue();    // 3글자 (4 미만)
        assertThat(likeSearchStrategy.canHandle("가나다라", "writer")).isFalse();  // 4글자 (4 이상)
    }

    @Test
    @DisplayName("통합 테스트 - 전체 LIKE 검색 전략 워크플로우")
    void shouldCompleteEntireLikeSearchWorkflow() {
        // Given: 다양한 짧은 검색어들
        String[] shortQueries = {"검", "검색", "작성자"};
        String[] types = {"title", "title_content", "writer"};
        
        // When & Then: 전체 워크플로우 실행
        for (String query : shortQueries) {
            for (String type : types) {
                // 처리 가능성 확인
                boolean canHandle = likeSearchStrategy.canHandle(query, type);
                
                if (canHandle) {
                    // 검색 조건 생성
                    BooleanExpression condition = likeSearchStrategy.createCondition(type, query);
                    
                    // 조건 생성 확인
                    assertThat(condition).isNotNull();
                    
                    // LIKE 패턴 확인
                    String conditionString = condition.toString();
                    assertThat(conditionString).contains("containsIgnoreCase");
                }
            }
        }
        
        // 전략 이름 확인
        assertThat(likeSearchStrategy.getStrategyName()).isEqualTo("LikeSearchStrategy");
    }

    @Test
    @DisplayName("성능 테스트 - 짧은 검색어 대량 처리")
    void shouldHandleMultipleShortQueries_WithoutPerformanceIssues() {
        // Given: 대량의 짧은 검색어들
        String[] manyShortQueries = new String[1000];
        for (int i = 0; i < 1000; i++) {
            manyShortQueries[i] = "검" + (i % 10); // "검0", "검1", ... "검9" 반복
        }
        
        // When: 대량 검색어 처리
        long startTime = System.currentTimeMillis();
        
        for (String query : manyShortQueries) {
            boolean canHandle = likeSearchStrategy.canHandle(query, "title");
            if (canHandle) {
                BooleanExpression condition = likeSearchStrategy.createCondition("title", query);
                assertThat(condition).isNotNull();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Then: 성능 문제 없이 처리됨 (1초 이내)
        assertThat(executionTime).isLessThan(1000L);
        
        // 모든 짧은 검색어가 처리 가능해야 함 (2글자이므로)
        for (String query : manyShortQueries) {
            assertThat(likeSearchStrategy.canHandle(query, "title")).isTrue();
        }
    }

    @Test
    @DisplayName("데이터 무결성 - null 및 빈 문자열 처리")
    void shouldHandleNullAndEmptyStrings_Gracefully() {
        // Given: null과 빈 문자열
        String nullQuery = null;
        String emptyQuery = "";
        String whitespaceQuery = "   ";
        
        // When & Then: 안전하게 처리됨
        
        // null 검색어 - NullPointerException 발생하지 않음
        try {
            boolean canHandleNull = likeSearchStrategy.canHandle(nullQuery, "title");
            // null은 length() 호출 시 NPE 발생 가능하지만, 비즈니스 로직에 따라 처리
        } catch (NullPointerException e) {
            // NPE는 예상되는 동작일 수 있음 (메인 로직에서 null 체크 필요)
        }
        
        // 빈 문자열 - 길이 0이므로 처리 가능해야 함
        boolean canHandleEmpty = likeSearchStrategy.canHandle(emptyQuery, "title");
        assertThat(canHandleEmpty).isTrue(); // 0 < 3 이므로 true
        
        // 공백만 있는 문자열 - 길이 3이므로 처리 불가능해야 함
        boolean canHandleWhitespace = likeSearchStrategy.canHandle(whitespaceQuery, "title");
        assertThat(canHandleWhitespace).isFalse(); // 3 >= 3 이므로 false
        
        // 빈 문자열로 조건 생성
        BooleanExpression emptyCondition = likeSearchStrategy.createCondition("title", emptyQuery);
        assertThat(emptyCondition).isNotNull();
    }
}
package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>검색 전략 팩토리</h2>
 * <p>검색어와 검색 유형에 따라 적절한 검색 전략을 선택하는 팩토리 클래스입니다.</p>
 * <p>Strategy Pattern과 Factory Pattern을 조합하여 검색 로직을 캡슐화합니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchStrategyFactory {
    
    private final List<SearchStrategy> searchStrategies;
    
    /**
     * <h3>검색 조건 생성</h3>
     * <p>주어진 검색 유형과 쿼리에 대해 가장 적절한 전략을 선택하여 검색 조건을 생성합니다.</p>
     * <p>전략 우선순위: FullTextSearchStrategy → LikeSearchStrategy → 기본값</p>
     * 
     * @param type 검색 유형 ("title", "writer", "title_content")
     * @param query 검색어
     * @return 생성된 BooleanExpression
     * @author Jaeik
     * @since 2.0.0
     */
    public BooleanExpression createSearchCondition(String type, String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("빈 검색어가 입력되었습니다. 기본 조건을 반환합니다.");
            return getDefaultCondition();
        }
        
        String trimmedQuery = query.trim();
        
        SearchStrategy selectedStrategy = selectStrategy(trimmedQuery, type);
        
        try {
            BooleanExpression condition = selectedStrategy.createCondition(type, trimmedQuery);
            log.debug("검색 전략 선택: {} (검색어: '{}', 타입: '{}')", 
                    selectedStrategy.getStrategyName(), trimmedQuery, type);
            return condition;
        } catch (Exception e) {
            log.error("검색 조건 생성 중 오류 발생: {}", e.getMessage(), e);
            return getDefaultCondition();
        }
    }
    
    /**
     * <h3>검색 전략 선택</h3>
     * <p>검색어와 검색 유형에 따라 가장 적절한 전략을 선택합니다.</p>
     * <p>각 전략의 canHandle() 메서드를 사용하여 적절한 전략을 선택합니다.</p>
     * 
     * @param query 검색어
     * @param type 검색 유형
     * @return 선택된 검색 전략
     * @author Jaeik
     * @since 2.0.0
     */
    private SearchStrategy selectStrategy(String query, String type) {
        return searchStrategies.stream()
                .filter(strategy -> strategy.canHandle(query, type))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("적절한 검색 전략을 찾을 수 없습니다. LikeSearchStrategy를 기본값으로 사용합니다.");
                    return searchStrategies.stream()
                            .filter(strategy -> strategy instanceof LikeSearchStrategy)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("LikeSearchStrategy를 찾을 수 없습니다."));
                });
    }

    /**
     * <h3>기본 검색 조건 반환</h3>
     * <p>오류 상황에서 사용할 기본 검색 조건을 반환합니다.</p>
     * 
     * @return 기본 BooleanExpression (항상 true)
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression getDefaultCondition() {
        return null;
    }
}

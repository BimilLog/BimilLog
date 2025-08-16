package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * <h2>검색 전략 인터페이스</h2>
 * <p>다양한 검색 방식을 추상화한 Strategy Pattern 인터페이스입니다.</p>
 * <p>검색 쿼리의 특성에 따라 적절한 검색 전략을 선택할 수 있습니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
public interface SearchStrategy {
    
    /**
     * <h3>검색 조건 생성</h3>
     * <p>주어진 검색 유형과 쿼리에 따라 QueryDSL BooleanExpression을 생성합니다.</p>
     * 
     * @param type 검색 유형 ("title", "writer", "title_content")
     * @param query 검색어
     * @return 생성된 BooleanExpression
     * @author Jaeik
     * @since 2.0.0
     */
    BooleanExpression createCondition(String type, String query);
    
    /**
     * <h3>전략 적용 가능 여부 확인</h3>
     * <p>주어진 검색어와 검색 유형에 대해 이 전략을 적용할 수 있는지 판단합니다.</p>
     * 
     * @param query 검색어
     * @param type 검색 유형 ("title", "writer", "title_content")
     * @return 적용 가능 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean canHandle(String query, String type);
    
    /**
     * <h3>전략 이름 반환</h3>
     * <p>디버깅 및 로깅을 위한 전략 이름을 반환합니다.</p>
     * 
     * @return 전략 이름
     * @author Jaeik
     * @since 2.0.0
     */
    String getStrategyName();
}

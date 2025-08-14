package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.user.entity.QUser;
import org.springframework.stereotype.Component;

/**
 * <h2>LIKE 검색 전략</h2>
 * <p>짧은 검색어에 대해 LIKE 검색을 수행하는 전략입니다.</p>
 * <p>일반적으로 1-3글자의 짧은 검색어에 대해 사용됩니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class LikeSearchStrategy implements SearchStrategy {
    
    private static final QPost POST = QPost.post;
    private static final QUser USER = QUser.user;
    
    /**
     * <h3>LIKE 검색 조건 생성</h3>
     * <p>검색 유형에 따라 적절한 LIKE 검색 조건을 생성합니다.</p>
     * 
     * @param type 검색 유형
     * @param query 검색어
     * @return LIKE 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public BooleanExpression createCondition(String type, String query) {
        return switch (type) {
            case "title" -> createTitleLikeCondition(query);
            case "writer" -> createWriterLikeCondition(query);
            case "title_content" -> createTitleContentLikeCondition(query);
            default -> createTitleLikeCondition(query);
        };
    }
    
    /**
     * <h3>처리 가능 여부 확인</h3>
     * <p>짧은 검색어에 대해서만 처리 가능합니다.</p>
     * <p>제목/내용: 1-2글자, 작성자: 1-3글자</p>
     * 
     * @param query 검색어
     * @param type 검색 유형
     * @return 처리 가능 여부
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean canHandle(String query, String type) {
        int threshold = getThreshold(type);
        return query.length() < threshold;
    }
    
    /**
     * <h3>검색 유형별 임계값 반환</h3>
     * <p>각 검색 유형에 따른 LIKE vs FullText 구분 임계값을 반환합니다.</p>
     * 
     * @param type 검색 유형
     * @return 임계값 (이 값 미만이면 LIKE, 이상이면 FullText)
     * @author Jaeik
     * @since 2.0.0
     */
    private int getThreshold(String type) {
        return switch (type) {
            case "title", "title_content" -> 3;  // 1-2글자: LIKE, 3글자 이상: FullText
            case "writer" -> 4;                  // 1-3글자: LIKE, 4글자 이상: FullText
            default -> 3;
        };
    }
    
    /**
     * <h3>전략 이름 반환</h3>
     * 
     * @return 전략 이름
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String getStrategyName() {
        return "LikeSearchStrategy";
    }
    
    /**
     * <h3>제목 LIKE 검색 조건</h3>
     * <p>제목에 대한 LIKE 검색 조건을 생성합니다.</p>
     * 
     * @param query 검색어
     * @return 제목 LIKE 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createTitleLikeCondition(String query) {
        return POST.title.containsIgnoreCase(query);
    }
    
    /**
     * <h3>제목+내용 LIKE 검색 조건</h3>
     * <p>제목과 내용에 대한 LIKE 검색 조건을 생성합니다.</p>
     * 
     * @param query 검색어
     * @return 제목+내용 LIKE 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createTitleContentLikeCondition(String query) {
        return POST.title.containsIgnoreCase(query)
                .or(POST.content.containsIgnoreCase(query));
    }
    
    /**
     * <h3>작성자 LIKE 검색 조건</h3>
     * <p>작성자명에 대한 LIKE 검색 조건을 생성합니다.</p>
     * <p>LikeSearchStrategy는 1-3글자만 담당: %LIKE% 검색</p>
     * 
     * @param query 검색어
     * @return 작성자 LIKE 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createWriterLikeCondition(String query) {
        // LikeSearchStrategy는 1-3글자만 담당하므로 항상 contains 사용
        return USER.userName.containsIgnoreCase(query);
    }
}

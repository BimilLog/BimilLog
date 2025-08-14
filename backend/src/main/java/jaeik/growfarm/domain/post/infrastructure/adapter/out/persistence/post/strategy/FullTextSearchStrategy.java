package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.user.entity.QUser;
import org.springframework.stereotype.Component;

/**
 * <h2>풀텍스트 검색 전략</h2>
 * <p>긴 검색어에 대해 풀텍스트 검색을 수행하는 전략입니다.</p>
 * <p><strong>현재 구현:</strong> 임시로 LIKE 검색 사용</p>
 * <p><strong>TODO:</strong> MySQL FULLTEXT 또는 Elasticsearch로 교체 예정</p>
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@Component
public class FullTextSearchStrategy implements SearchStrategy {
    
    private static final QPost POST = QPost.post;
    private static final QUser USER = QUser.user;
    
    /**
     * <h3>풀텍스트 검색 조건 생성</h3>
     * <p>검색 유형에 따라 적절한 풀텍스트 검색 조건을 생성합니다.</p>
     * <p><strong>현재:</strong> LIKE 검색으로 임시 구현</p>
     * <p><strong>향후:</strong> 실제 풀텍스트 검색으로 교체</p>
     * 
     * @param type 검색 유형
     * @param query 검색어
     * @return 풀텍스트 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public BooleanExpression createCondition(String type, String query) {
        return switch (type) {
            case "title" -> createTitleFullTextCondition(query);
            case "writer" -> createWriterFullTextCondition(query);
            case "title_content" -> createTitleContentFullTextCondition(query);
            default -> createTitleFullTextCondition(query);
        };
    }
    
    /**
     * <h3>처리 가능 여부 확인</h3>
     * <p>긴 검색어(3글자 이상)에 대해서만 처리 가능합니다.</p>
     * 
     * @param query 검색어
     * @return 처리 가능 여부
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public boolean canHandle(String query) {
        // 3글자 이상에서 풀텍스트 검색 사용
        return query.length() >= 3;
    }
    
    /**
     * <h3>전략 이름 반환</h3>
     * 
     * @return 전략 이름
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public String getStrategyName() {
        return "FullTextSearchStrategy";
    }
    
    /**
     * <h3>제목 풀텍스트 검색 조건</h3>
     * <p><strong>TODO:</strong> MySQL의 MATCH AGAINST 또는 Elasticsearch로 교체</p>
     * 
     * @param query 검색어
     * @return 제목 풀텍스트 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    private BooleanExpression createTitleFullTextCondition(String query) {
        // TODO: 실제 풀텍스트 검색으로 교체 예정
        // 예시: SELECT * FROM posts WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE)
        // 현재는 임시로 LIKE 검색 사용
        return POST.title.containsIgnoreCase(query);
    }
    
    /**
     * <h3>제목+내용 풀텍스트 검색 조건</h3>
     * <p><strong>TODO:</strong> MySQL의 MATCH AGAINST 또는 Elasticsearch로 교체</p>
     * 
     * @param query 검색어
     * @return 제목+내용 풀텍스트 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    private BooleanExpression createTitleContentFullTextCondition(String query) {
        // TODO: 실제 풀텍스트 검색으로 교체 예정
        // 예시: SELECT * FROM posts WHERE MATCH(title, content) AGAINST(? IN BOOLEAN MODE)
        // 현재는 임시로 LIKE 검색 사용
        return POST.title.containsIgnoreCase(query)
                .or(POST.content.containsIgnoreCase(query));
    }
    
    /**
     * <h3>작성자 풀텍스트 검색 조건</h3>
     * <p>작성자명은 일반적으로 풀텍스트 검색 대상이 아니므로 LIKE 검색 유지</p>
     * <p>FullTextSearchStrategy는 4글자 이상만 담당: LIKE% 검색 (인덱스 효율성)</p>
     * 
     * @param query 검색어
     * @return 작성자 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    private BooleanExpression createWriterFullTextCondition(String query) {
        // FullTextSearchStrategy는 4글자 이상만 담당하므로 항상 startsWithIgnoreCase 사용
        // (인덱스 효율성을 위한 LIKE% 패턴)
        return USER.userName.startsWithIgnoreCase(query);
    }
}

package jaeik.growfarm.domain.post.infrastructure.adapter.out.persistence.post.strategy;

import com.querydsl.core.types.dsl.BooleanExpression;
import jaeik.growfarm.domain.post.entity.QPost;
import jaeik.growfarm.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;

/**
 * <h2>풀텍스트 검색 전략</h2>
 * <p>긴 검색어에 대해 MySQL FULLTEXT 검색을 수행하는 전략입니다.</p>
 * <p>MySQL의 MATCH() AGAINST() 구문을 사용하여 고성능 전문 검색을 제공합니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FullTextSearchStrategy implements SearchStrategy {
    
    private final EntityManager entityManager;
    
    private static final QPost POST = QPost.post;
    private static final QUser USER = QUser.user;
    
    /**
     * <h3>풀텍스트 검색 조건 생성</h3>
     * <p>검색 유형에 따라 적절한 풀텍스트 검색 조건을 생성합니다.</p>

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
     * <p>긴 검색어에 대해서만 처리 가능합니다.</p>
     * <p>제목/내용: 3글자 이상, 작성자: 4글자 이상</p>
     * 
     * @param query 검색어
     * @param type 검색 유형
     * @return 처리 가능 여부
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public boolean canHandle(String query, String type) {
        int threshold = getThreshold(type);
        return query.length() >= threshold;
    }
    
    /**
     * <h3>검색 유형별 임계값 반환</h3>
     * <p>각 검색 유형에 따른 LIKE vs FullText 구분 임계값을 반환합니다.</p>
     * 
     * @param type 검색 유형
     * @return 임계값 (이 값 이상이면 FullText, 미만이면 LIKE)
     * @author Jaeik
     * @since 1.0.0
     */
    private int getThreshold(String type) {
        return switch (type) {
            case "title", "title_content" -> 3;  // 3글자 이상: FullText, 1-2글자: LIKE
            case "writer" -> 4;                  // 4글자 이상: FullText, 1-3글자: LIKE
            default -> 3;
        };
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
     * <p>MySQL FULLTEXT 인덱스를 사용한 제목 검색을 수행합니다.</p>
     * 
     * @param query 검색어
     * @return 제목 풀텍스트 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createTitleFullTextCondition(String query) {
        try {
            // MySQL FULLTEXT 검색으로 게시글 ID 목록 조회
            List<Long> postIds = executeFullTextQuery(
                "SELECT p.post_id FROM post p WHERE p.is_notice = false AND MATCH(p.title) AGAINST(?1 IN BOOLEAN MODE)",
                query
            );
            
            // 결과가 없으면 결과 없음 조건 반환
            if (postIds.isEmpty()) {
                return POST.id.eq(-1L); // 존재하지 않는 ID로 빈 결과 반환
            }
            
            // 검색된 ID 목록을 QueryDSL 조건으로 변환
            return POST.id.in(postIds);
            
        } catch (Exception e) {
            log.error("제목 FULLTEXT 검색 중 오류 발생: {}", e.getMessage(), e);
            // 오류 시 기본 LIKE 검색으로 fallback
            return POST.title.containsIgnoreCase(query);
        }
    }
    
    /**
     * <h3>제목+내용 풀텍스트 검색 조건</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용한 제목과 내용 통합 검색을 수행합니다.</p>
     * 
     * @param query 검색어
     * @return 제목+내용 풀텍스트 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    private BooleanExpression createTitleContentFullTextCondition(String query) {
        try {
            // MySQL FULLTEXT 검색으로 게시글 ID 목록 조회 (제목과 내용 모두 검색)
            List<Long> postIds = executeFullTextQuery(
                "SELECT p.post_id FROM post p WHERE p.is_notice = false AND MATCH(p.title, p.content) AGAINST(?1 IN BOOLEAN MODE)",
                query
            );
            
            // 결과가 없으면 결과 없음 조건 반환
            if (postIds.isEmpty()) {
                return POST.id.eq(-1L); // 존재하지 않는 ID로 빈 결과 반환
            }
            
            // 검색된 ID 목록을 QueryDSL 조건으로 변환
            return POST.id.in(postIds);
            
        } catch (Exception e) {
            log.error("제목+내용 FULLTEXT 검색 중 오류 발생: {}", e.getMessage(), e);
            // 오류 시 기본 LIKE 검색으로 fallback
            return POST.title.containsIgnoreCase(query)
                    .or(POST.content.containsIgnoreCase(query));
        }
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
        return USER.userName.startsWithIgnoreCase(query);
    }
    
    /**
     * <h3>FULLTEXT 쿼리 실행</h3>
     * <p>주어진 네이티브 쿼리를 실행하여 게시글 ID 목록을 반환합니다.</p>
     * 
     * @param nativeQuery MySQL FULLTEXT 네이티브 쿼리
     * @param searchTerm 검색어
     * @return 검색된 게시글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @SuppressWarnings("unchecked")
    private List<Long> executeFullTextQuery(String nativeQuery, String searchTerm) {
        try {
            Query query = entityManager.createNativeQuery(nativeQuery);
            query.setParameter(1, searchTerm);
            
            // 결과를 BigInteger로 받아서 Long으로 변환
            List<Object> results = query.getResultList();
            return results.stream()
                    .map(result -> {
                        if (result instanceof Number) {
                            return ((Number) result).longValue();
                        }
                        return Long.valueOf(result.toString());
                    })
                    .toList();
                    
        } catch (Exception e) {
            log.error("FULLTEXT 쿼리 실행 중 오류 발생 - 쿼리: {}, 검색어: {}, 오류: {}", 
                     nativeQuery, searchTerm, e.getMessage(), e);
            throw new RuntimeException("FULLTEXT 검색 실행 실패", e);
        }
    }
}

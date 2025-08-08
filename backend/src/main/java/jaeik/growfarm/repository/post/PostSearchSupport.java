package jaeik.growfarm.repository.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.repository.comment.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>게시글 검색 지원 추상 클래스</h2>
 * <p>
 * 검색 관련 공통 유틸과 네이티브 결과 처리 로직을 제공한다.
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
public abstract class PostSearchSupport extends PostBaseRepository {

    protected PostSearchSupport(JPAQueryFactory jpaQueryFactory, CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }

    /**
     * <h3>네이티브 쿼리 결과를 SimplePostDTO로 변환</h3>
     *
     * @param results 네이티브 쿼리 결과 배열 목록
     * @return 변환된 {@link SimplePostDTO} 목록
     * @since 1.1.0
     */
    protected List<SimplePostDTO> convertNativeQueryResults(List<Object[]> results) {
        return results.stream()
                .map(row -> {
                    Long postId = convertToLong(row[0]);
                    String title = (String) row[1];
                    int views = row[2] != null ? (Integer) row[2] : 0;
                    Boolean isNotice = (Boolean) row[3];
                    PopularFlag popularFlag = row[4] != null ? PopularFlag.valueOf((String) row[4]) : null;
                    java.sql.Timestamp createdAt = (java.sql.Timestamp) row[5];
                    Long userId = row[6] != null ? convertToLong(row[6]) : null;
                    String userName = row[7] != null ? (String) row[7] : "익명";

                    return safeBuildSimplePostDTO(
                            postId,
                            userId,
                            userName,
                            title,
                            0,
                            0,
                            views,
                            createdAt.toInstant(),
                            Boolean.TRUE.equals(isNotice),
                            popularFlag);
                })
                .collect(Collectors.toList());
    }

    /**
     * <h3>네이티브 쿼리 결과 처리</h3>
     * <p>
     * 네이티브 쿼리 결과를 SimplePostDTO로 변환하고 댓글 수/좋아요 수를 통합 조회한다.
     * </p>
     *
     * @param results    네이티브 쿼리 결과 배열 목록
     * @param pageable   페이지 정보
     * @param totalCount 전체 결과 개수
     * @return 댓글/좋아요 수가 채워진 {@link SimplePostDTO} 페이지
     * @since 1.1.0
     * @author Jaeik
     */
    protected Page<SimplePostDTO> processNativeQueryResults(List<Object[]> results, Pageable pageable, long totalCount) {
        List<SimplePostDTO> posts = convertNativeQueryResults(results);
        if (posts.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
        }
        List<Long> postIds = posts.stream()
                .map(SimplePostDTO::getPostId)
                .collect(Collectors.toList());

        Map<Long, Integer> commentCounts = commentRepository.findCommentCountsByPostIds(postIds);
        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        posts.forEach(post -> {
            post.setCommentCount(commentCounts.getOrDefault(post.getPostId(), 0));
            post.setLikes(likeCounts.getOrDefault(post.getPostId(), 0));
        });

        return new PageImpl<>(posts, pageable, totalCount);
    }

    /**
     * <h3>검색 조건 유효성 검증</h3>
     *
     * @param keyword    검색어
     * @param searchType 검색 타입
     * @return 유효한 검색 조건이면 true, 아니면 false
     * @since 1.1.0
     * @author Jaeik
     */
    protected boolean hasValidSearchCondition(String keyword, String searchType) {
        return keyword != null && !keyword.trim().isEmpty() && searchType != null;
    }

    /**
     * <h3>검색어 정규화</h3>
     * <p>
     * 허용: 문자(한글 포함), 숫자, 공백. 그 외 특수문자는 공백으로 치환 후 공백 정규화
     * </p>
     *
     * @param keyword 원본 검색어
     * @return 정규화된 검색어 (null이면 빈 문자열)
     * @since 1.1.0
     * @author Jaeik
     */
    protected String safeNormalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String trimmed = keyword.trim();
        String cleaned = trimmed.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    /**
     * <h3>FULLTEXT 검색 비효율 판단</h3>
     *
     * @param keyword 정규화된 키워드
     * @return 너무 짧거나 전부 짧은 토큰으로만 구성된 경우 true
     * @since 1.1.0
     * @author Jaeik
     */
    protected boolean isFullTextLikelyIneffective(String keyword) {
        if (keyword == null) {
            return true;
        }
        String k = keyword.trim();
        if (k.length() < 3) {
            return true;
        }
        String[] tokens = k.split(" ");
        boolean allTooShort = true;
        for (String token : tokens) {
            if (token.length() >= 3) {
                allTooShort = false;
                break;
            }
        }
        return allTooShort;
    }

    /**
     * <h3>Object를 Long으로 안전하게 변환</h3>
     *
     * @param obj BigInteger, Long, Integer 등
     * @return 변환된 Long 값, 변환 불가 타입이면 예외
     * @since 1.1.0
     * @author Jaeik
     */
    private Long convertToLong(Object obj) {
        return switch (obj) {
            case null -> null;
            case BigInteger bigInteger -> bigInteger.longValue();
            case Long l -> l;
            case Integer i -> i.longValue();
            default -> throw new IllegalArgumentException("Unsupported type for Long conversion: " + obj.getClass());
        };
    }
}


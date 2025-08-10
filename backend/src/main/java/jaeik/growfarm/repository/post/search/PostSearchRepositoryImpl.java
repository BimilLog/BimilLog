package jaeik.growfarm.repository.post.search;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostBaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * <h2>게시글 검색 구현체</h2>
 * <p>
 * 게시글 검색과 조회 관련 기능을 담당한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Repository
public class PostSearchRepositoryImpl extends PostBaseRepository implements PostSearchRepository {


    public PostSearchRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                    CommentRepository commentRepository) {
        super(jpaQueryFactory, commentRepository);
    }


    /**
     * <h3>검색 방법 레코드 클래스</h3>
     * <p>
     * 검색 유형과 키워드를 기반으로 검색 조건을 생성하는 레코드 클래스이다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param isFullText 전체 텍스트 검색 여부
     * @author Jaeik
     * @version 1.0.0
     */
    private record SearchMethod(String searchType, String keyword, boolean isFullText) {

        /**
         * <h3>검색 방법 생성</h3>
         * <p>
         * 검색 유형과 키워드에 따라 적절한 검색 방법을 생성한다.
         * </p>
         *
         * @param searchType 검색 유형
         * @param keyword    검색어
         * @return 생성된 SearchMethod 인스턴스
         * @throws CustomException 잘못된 검색 형식인 경우 예외 발생
         * @author Jaeik
         * @since 2.0.0
         */
        public static SearchMethod from(String searchType, String keyword) {
            return switch (searchType) {
                case "TITLE" -> new SearchMethod("title", keyword, true);
                case "TITLE_CONTENT" -> new SearchMethod("titleAndContent", keyword, true);
                case "AUTHOR" -> {
                    boolean isAuthorStartsWith = keyword.length() >= 4;
                    if (isAuthorStartsWith) {
                        yield new SearchMethod("authorStartsWith", keyword, false);
                    } else {
                        yield new SearchMethod("authorContains", keyword, false);
                    }
                }
                default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
            };
        }

        /**
         * <h3>검색 유형이 전체 텍스트 검색인지 확인</h3>
         * <p>
         * 현재 검색 방법이 전체 텍스트 검색인지 여부를 반환한다.
         * </p>
         *
         * @return 전체 텍스트 검색 여부
         * @author Jaeik
         * @since 2.0.0
         */
        public boolean isFullTextSearch() {
            return isFullText;
        }
    }


    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색하며, 각 게시글의 총 댓글 수와 총 추천 수를 반환한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @throws CustomException 검색 조건이 유효하지 않거나 검색 중 오류가 발생한 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimplePostResDTO> searchPosts(String keyword, String searchType, Pageable pageable) {
        if (!hasValidSearchCondition(keyword, searchType)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        try {
            String trimmedKeyword = safeNormalizeKeyword(keyword);

            SearchMethod searchMethod = SearchMethod.from(searchType, trimmedKeyword);

            BooleanExpression condition = createSearchCondition(
                    searchMethod.searchType(),
                    searchMethod.keyword(),
                    searchMethod.isFullTextSearch());

            List<SimplePostResDTO> results = createPostListQuery(condition, pageable);
            long totalCount = createPostCountQuery(condition);

            return new PageImpl<>(results, pageable, totalCount);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생. keyword: {}, searchType: {}", keyword, searchType, e);
            throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT, e);
        }
    }

    /**
     * <h3>검색 조건 생성</h3>
     * <p>
     * 검색 유형과 키워드에 따라 적절한 검색 조건을 생성한다.
     * </p>
     *
     * @param searchType     검색 유형
     * @param keyword        검색어
     * @param fullTextSearch 전체 텍스트 검색 여부
     * @return 생성된 검색 조건
     * @author Jaeik
     * @since 2.0.0
     */
    protected BooleanExpression createSearchCondition(String searchType, String keyword, boolean fullTextSearch) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        if (fullTextSearch) {
            if ("title".equals(searchType)) {
                return Expressions.booleanTemplate("MATCH({0}) AGAINST({1} IN BOOLEAN MODE)", post.title, keyword);
            } else if ("titleAndContent".equals(searchType)) {
                return Expressions.booleanTemplate("MATCH({0}, {1}) AGAINST({2} IN BOOLEAN MODE)", post.title, post.content, keyword);
            }
        } else {
            String lowerKeyword = keyword.toLowerCase();
            if ("authorStartsWith".equals(searchType)) {
                return user.userName.toLowerCase().startsWith(lowerKeyword);
            } else if ("authorContains".equals(searchType)) {
                return user.userName.toLowerCase().contains(lowerKeyword);
            }
        }
        return null;
    }

    /**
     * <h3>검색 조건 유효성 검사</h3>
     * <p>
     * 검색어와 검색 유형이 유효한지 검사한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형
     * @return 유효한 검색 조건이면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    protected boolean hasValidSearchCondition(String keyword, String searchType) {
        return keyword != null && !keyword.trim().isEmpty() && searchType != null;
    }

    /**
     * <h3>안전한 키워드 정규화</h3>
     * <p>
     * 검색어를 안전하게 정규화하여 불필요한 특수문자를 제거하고, 공백을 정리한다.
     * </p>
     *
     * @param keyword 검색어
     * @return 정규화된 검색어
     * @author Jaeik
     * @since 2.0.0
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
}
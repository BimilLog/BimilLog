package jaeik.growfarm.repository.post.search;

import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * @version 1.1.0
 * @since 1.1.0
 */
@Slf4j
@Repository
public class PostSearchRepositoryImpl extends PostSearchSupport implements PostSearchRepository {

    private final PostCustomFullTextRepository postCustomFullTextRepository;

    public PostSearchRepositoryImpl(JPAQueryFactory jpaQueryFactory,
                                    CommentRepository commentRepository,
                                    PostCustomFullTextRepository postCustomFullTextRepository) {
        super(jpaQueryFactory, commentRepository);
        this.postCustomFullTextRepository = postCustomFullTextRepository;
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색한다. 검색 시에는 공지글을 제외한다.
     * 각 게시글의 총 댓글 수와 총 추천 수를 반환한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable) {
        if (hasValidSearchCondition(keyword, searchType)) {
            return switch (searchType) {
                case "TITLE" -> searchPostsWithNativeQuery(keyword, "TITLE", pageable);
                case "TITLE_CONTENT" -> searchPostsWithNativeQuery(keyword, "TITLE_CONTENT", pageable);
                case "AUTHOR" -> searchPostsWithNativeQuery(keyword, "AUTHOR", pageable);
                default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
            };
        }
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }


    /**
     * <h3>네이티브 쿼리를 이용한 게시글 검색</h3>
     *
     * <p>
     * 네이티브 쿼리를 사용하여 게시글을 검색한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.1.0
     */
    private Page<SimplePostDTO> searchPostsWithNativeQuery(String keyword, String searchType, Pageable pageable) {
        try {
            String trimmedKeyword = safeNormalizeKeyword(keyword);

            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "created_at"));

            List<Object[]> results;
            long totalCount;

            switch (searchType) {
                case "TITLE" -> {
                    if (isFullTextLikelyIneffective(trimmedKeyword)) {
                        results = postCustomFullTextRepository.findByTitleLike(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByTitleLike(trimmedKeyword);
                    } else {
                        results = postCustomFullTextRepository.findByTitleFullText(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByTitleFullText(trimmedKeyword);
                    }
                }
                case "TITLE_CONTENT" -> {
                    if (isFullTextLikelyIneffective(trimmedKeyword)) {
                        results = postCustomFullTextRepository.findByTitleContentLike(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByTitleContentLike(trimmedKeyword);
                    } else {
                        results = postCustomFullTextRepository.findByTitleContentFullText(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByTitleContentFullText(trimmedKeyword);
                    }
                }
                case "AUTHOR" -> {
                    if (trimmedKeyword.length() >= 4) {
                        results = postCustomFullTextRepository.findByAuthorStartsWith(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByAuthorStartsWith(trimmedKeyword);
                    } else {
                        results = postCustomFullTextRepository.findByAuthorContains(trimmedKeyword, sortedPageable.getPageSize(), (int) sortedPageable.getOffset());
                        totalCount = postCustomFullTextRepository.countByAuthorContains(trimmedKeyword);
                    }
                }
                default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
            }

            if (results.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
            }
            return processNativeQueryResults(results, pageable, totalCount);

        } catch (Exception e) {
            log.error("네이티브 쿼리 검색 중 오류 발생. keyword: {}, searchType: {}", keyword, searchType, e);
            throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT, e);
        }
    }
}
package jaeik.bimillog.domain.post.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import jaeik.bimillog.domain.member.entity.QMember;
import jaeik.bimillog.domain.post.entity.PostSearchType;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jaeik.bimillog.domain.post.entity.jpa.QPost;
import jaeik.bimillog.domain.post.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostSearchRepository {
    private final PostFulltextRepository postFullTextRepository;
    private final PostFulltextUtil postFullTextUtil;
    private final PostQueryRepository postQueryRepository;

    private static final QPost post = QPost.post;
    private static final QMember member = QMember.member;

    /**
     * <h3>MySQL FULLTEXT 전문 검색</h3>
     * <p>MySQL FULLTEXT 인덱스를 사용하여 게시글을 검색합니다.</p>
     * <p>검색 실패 시 빈 페이지를 반환하며, 에러는 로그로 기록됩니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형 (TITLE, TITLE_CONTENT)
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findByFullTextSearch(PostSearchType type, String query, Pageable pageable, Long viewerId) {
        String searchTerm = query + "*";
        try {
            List<Object[]> rows = switch (type) {
                case TITLE -> postFullTextRepository.findByTitleFullText(searchTerm, pageable, viewerId);
                case TITLE_CONTENT -> postFullTextRepository.findByTitleContentFullText(searchTerm, pageable, viewerId);
                case WRITER -> List.of();
            };

            long total = switch (type) {
                case TITLE -> postFullTextRepository.countByTitleFullText(searchTerm, viewerId);
                case TITLE_CONTENT -> postFullTextRepository.countByTitleContentFullText(searchTerm, viewerId);
                case WRITER -> 0L;
            };

            if (rows.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, total);
            }

            List<PostSimpleDetail> content = postFullTextUtil.mapFullTextRows(rows);
            return new PageImpl<>(content, pageable, total);
        } catch (DataAccessException e) {
            log.warn("FULLTEXT 검색 중 데이터베이스 오류 - type: {}, query: {}, error: {}", type, query, e.getMessage());
            return Page.empty(pageable);
        } catch (IllegalArgumentException e) {
            log.debug("FULLTEXT 검색 파라미터 오류 - type: {}, query: {}, error: {}", type, query, e.getMessage());
            return Page.empty(pageable);
        }
    }

    /**
     * <h3>접두사 검색 (인덱스 활용)</h3>
     * <p>LIKE 'query%' 조건으로 검색하여 인덱스를 활용합니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.3.0
     */
    public Page<PostSimpleDetail> findByPrefixMatch(PostSearchType type, String query, Pageable pageable, Long viewerId) {
        BooleanExpression condition = switch (type) {
            case WRITER -> member.memberName.startsWith(query);
            case TITLE -> post.title.startsWith(query);
            case TITLE_CONTENT -> post.title.startsWith(query).or(post.content.startsWith(query));
        };

        Consumer<JPAQuery<?>> customizer = q -> q.where(condition);
        return postQueryRepository.findPostsWithQuery(customizer, customizer, pageable);
    }

    /**
     * <h3>부분 문자열 검색 (인덱스 미활용)</h3>
     * <p>LIKE '%query%' 조건으로 부분 검색을 수행합니다.</p>
     * <p>{@link PostQueryService}에서 검색 전략에 따라 호출됩니다.</p>
     *
     * @param type     검색 유형
     * @param query    검색어
     * @param pageable 페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    public Page<PostSimpleDetail> findByPartialMatch(PostSearchType type, String query, Pageable pageable, Long viewerId) {
        BooleanExpression condition = switch (type) {
            case TITLE -> post.title.contains(query);
            case WRITER -> member.memberName.contains(query);
            case TITLE_CONTENT -> post.title.contains(query).or(post.content.contains(query));
        };

        Consumer<JPAQuery<?>> customizer = q -> q.where(condition);
        return postQueryRepository.findPostsWithQuery(customizer, customizer, pageable);
    }


}

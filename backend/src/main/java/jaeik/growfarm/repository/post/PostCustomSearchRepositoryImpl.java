package jaeik.growfarm.repository.post;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.post.PostDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.entity.post.QPost;
import jaeik.growfarm.entity.post.QPostLike;
import jaeik.growfarm.entity.user.QUsers;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
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
import java.util.Map;

/**
 * <h2>게시글 검색 및 조회 구현체</h2>
 * <p>
 * 게시글 검색, 조회 관련 기능을 담당하는 레포지터리
 * </p>
 *
 * @author Jaeik
 * @version 1.0
 */
@Slf4j
@Repository
public class PostCustomSearchRepositoryImpl extends PostCustomBaseRepository {

    private final PostCustomFullTextRepository postCustomFullTextRepository;

    public PostCustomSearchRepositoryImpl(JPAQueryFactory jpaQueryFactory,
            CommentRepository commentRepository,
            PostCustomFullTextRepository postCustomFullTextRepository) {
        super(jpaQueryFactory, commentRepository);
        this.postCustomFullTextRepository = postCustomFullTextRepository;
    }

    /**
     * <h3>게시글 목록 조회</h3>
     * <p>
     * 최신순으로 페이징하여 게시글 목록을 조회한다.
     * </p>
     * <p>
     * 게시글 당 댓글 수, 추천 수를 반환한다.
     * 공지글과 일반글 모두 포함한다.
     * </p>
     *
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> findPostsWithCommentAndLikeCounts(Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        // 일반 조회에서는 공지글도 포함 (조건 없음)
        List<Tuple> postTuples = fetchPosts(post, user, null, pageable);
        Long total = fetchTotalCount(post, user, null);

        return processPostTuples(postTuples, post, user, pageable, false, total);
    }

    /**
     * <h3>게시글 검색</h3>
     * <p>
     * 검색어와 검색 유형에 따라 게시글을 검색한다.
     * </p>
     * <p>
     * 게시글 마다의 총 댓글 수, 총 추천 수를 반환한다.
     * 검색 시에는 공지글을 제외한다.
     * </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형 (TITLE, TITLE_CONTENT, AUTHOR 등)
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public Page<SimplePostDTO> searchPosts(String keyword, String searchType, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty() && searchType != null) {
            return switch (searchType) {
                case "TITLE" -> searchPostsWithNativeQuery(keyword, "TITLE", pageable);
                case "TITLE_CONTENT" -> searchPostsWithNativeQuery(keyword, "TITLE_CONTENT", pageable);
                case "AUTHOR" -> searchPostsWithNativeQuery(keyword, "AUTHOR", pageable);
                default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
            };
        }
        // 검색어가 없으면 일반 조회로 처리 (공지글 포함)
        return findPostsWithCommentAndLikeCounts(pageable);
    }

    /**
     * <h3>게시글 상세 조회</h3>
     * <p>
     * 게시글 정보, 좋아요 수, 사용자 좋아요 여부를 조회한다.
     * </p>
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID (null 가능)
     * @return PostDTO 게시글 상세 정보
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    public PostDTO findPostById(Long postId, Long userId) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        Tuple postTuple = jpaQueryFactory
                .select(
                        post.id,
                        post.title,
                        post.content,
                        post.views,
                        post.isNotice,
                        post.popularFlag,
                        post.createdAt,
                        post.user.id,
                        user.userName)
                .from(post)
                .leftJoin(post.user, user)
                .where(post.id.eq(postId))
                .fetchOne();

        if (postTuple == null) {
            return null;
        }

        List<Long> postIds = Collections.singletonList(postId);

        Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

        boolean userLike = false;
        if (userId != null) {
            QPostLike userPostLike = QPostLike.postLike;
            Long likeCount = jpaQueryFactory
                    .select(userPostLike.count())
                    .from(userPostLike)
                    .where(userPostLike.post.id.eq(postId).and(userPostLike.user.id.eq(userId)))
                    .fetchOne();
            userLike = likeCount != null && likeCount > 0;
        }

        Integer views = postTuple.get(post.views);
        String userName = postTuple.get(user.userName);
        return PostDTO.existedPost(
                postTuple.get(post.id),
                postTuple.get(post.user.id),
                userName != null ? userName : "익명",
                postTuple.get(post.title),
                postTuple.get(post.content),
                views != null ? views : 0,
                likeCounts.getOrDefault(postId, 0),
                Boolean.TRUE.equals(postTuple.get(post.isNotice)),
                postTuple.get(post.popularFlag),
                postTuple.get(post.createdAt),
                userLike);
    }

    /**
     * <h3>게시글 조회 공통 로직</h3>
     * <p>
     * 게시글 목록 조회와 검색 기능의 공통 로직을 처리한다.
     * </p>
     *
     * @param keyword    검색어 (null 또는 빈 문자열이면 전체 조회)
     * @param searchType 검색 유형 (null이면 전체 조회)
     * @param pageable   페이지 정보
     * @return 게시글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    private Page<SimplePostDTO> findPostsInternal(String keyword, String searchType, Pageable pageable) {
        QPost post = QPost.post;
        QUsers user = QUsers.users;

        BooleanExpression searchCondition = createSearchCondition(post, user, keyword, searchType);

        // 검색 시에만 공지글 제외, 일반 조회 시에는 공지글도 포함
        BooleanExpression finalCondition = searchCondition;
        if (keyword != null && !keyword.trim().isEmpty() && searchType != null) {
            // 검색 시에는 공지글 제외
            BooleanExpression baseCondition = post.isNotice.eq(false);
            finalCondition = searchCondition != null
                    ? baseCondition.and(searchCondition)
                    : baseCondition;
        }

        List<Tuple> postTuples = fetchPosts(post, user, finalCondition, pageable);
        Long total = fetchTotalCount(post, user, finalCondition);

        return processPostTuples(postTuples, post, user, pageable, false, total);
    }

    /**
     * <h3>네이티브 쿼리를 이용한 게시글 검색</h3>
     * <p>
     * 
     * @Query(nativeQuery = true)를 사용하여 게시글을 검색한다.
     *                    </p>
     *
     * @param keyword    검색어
     * @param searchType 검색 유형
     * @param pageable   페이지 정보
     * @return 검색된 게시글 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    private Page<SimplePostDTO> searchPostsWithNativeQuery(String keyword, String searchType, Pageable pageable) {
        try {
            String trimmedKeyword = keyword.trim();

            // 네이티브 쿼리에 정렬 조건 추가 (최신순)
            Pageable sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "created_at"));

            List<Object[]> results;
            long totalCount;

            switch (searchType) {
                case "TITLE" -> {
                    results = postCustomFullTextRepository.findByTitleFullText(trimmedKeyword, sortedPageable);
                    totalCount = postCustomFullTextRepository.countByTitleFullText(trimmedKeyword);
                }
                case "TITLE_CONTENT" -> {
                    results = postCustomFullTextRepository.findByTitleContentFullText(trimmedKeyword, sortedPageable);
                    totalCount = postCustomFullTextRepository.countByTitleContentFullText(trimmedKeyword);
                }
                case "AUTHOR" -> {
                    if (trimmedKeyword.length() >= 4) {
                        results = postCustomFullTextRepository.findByAuthorStartsWith(trimmedKeyword, sortedPageable);
                        totalCount = postCustomFullTextRepository.countByAuthorStartsWith(trimmedKeyword);
                    } else {
                        results = postCustomFullTextRepository.findByAuthorContains(trimmedKeyword, sortedPageable);
                        totalCount = postCustomFullTextRepository.countByAuthorContains(trimmedKeyword);
                    }
                }
                default -> throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT);
            }

            if (results.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, totalCount);
            }

            // 결과 변환
            List<SimplePostDTO> posts = convertNativeQueryResults(results);

            // 댓글 수와 좋아요 수 조회
            List<Long> postIds = posts.stream()
                    .map(SimplePostDTO::getPostId)
                    .collect(java.util.stream.Collectors.toList());

            Map<Long, Integer> commentCounts = commentRepository.findCommentCountsByPostIds(postIds);
            Map<Long, Integer> likeCounts = fetchLikeCounts(postIds);

            // DTO에 댓글 수와 좋아요 수 설정
            posts.forEach(post -> {
                post.setCommentCount(commentCounts.getOrDefault(post.getPostId(), 0));
                post.setLikes(likeCounts.getOrDefault(post.getPostId(), 0));
            });

            return new PageImpl<>(posts, pageable, totalCount);

        } catch (Exception e) {
            log.error("네이티브 쿼리 검색 중 오류 발생. keyword: {}, searchType: {}", keyword, searchType, e);
            throw new CustomException(ErrorCode.INCORRECT_SEARCH_FORMAT, e);
        }
    }

    /**
     * <h3>검색 조건 생성</h3>
     * <p>
     * 검색어와 검색 유형에 따라 검색 조건을 생성한다.
     * </p>
     *
     * @param post       QPost 엔티티
     * @param user       QUsers 엔티티
     * @param keyword    검색어
     * @param searchType 검색 유형
     * @return BooleanExpression 검색 조건
     * @author Jaeik
     * @since 1.0.0
     */
    private BooleanExpression createSearchCondition(QPost post, QUsers user, String keyword, String searchType) {
        if (keyword == null || keyword.trim().isEmpty() || searchType == null) {
            return null;
        }

        // 네이티브 쿼리로 처리하지 않는 경우만 여기서 처리
        return switch (searchType) {
            case "AUTHOR" -> createAuthorSearchCondition(user, keyword);
            default -> null; // 다른 검색 유형은 네이티브 쿼리에서 처리
        };
    }
}
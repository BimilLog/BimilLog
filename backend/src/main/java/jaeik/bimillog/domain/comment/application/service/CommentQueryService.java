package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.application.port.out.CommentQueryPort;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentInfo;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h2>댓글 서비스</h2>
 * <p>
 * 댓글 관련 조회 요구사항을 구현하는 서비스 클래스
 * </p>
 * <p>
 * 댓글 조회, 인기 댓글 조회 등 다양한 댓글 조회 기능을 제공
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService implements CommentQueryUseCase {

    private final CommentQueryPort commentQueryPort;

    /**
     * <h3>인기 댓글 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다. 사용자 추천 여부를 한 번의 쿼리로 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<CommentInfo> 인기 댓글 정보 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentInfo> getPopularComments(Long postId, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return commentQueryPort.findPopularComments(postId, userId);
    }



    /**
     * <h3>과거순 댓글 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 과거순으로 페이지네이션하여 조회합니다. 사용자 추천 여부를 한 번의 쿼리로 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentInfo> 과거순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentInfo> getCommentsOldestOrder(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        return commentQueryPort.findCommentsWithOldestOrder(postId, pageable, userId);
    }

    /**
     * <h3>ID로 댓글 조회</h3>
     * <p>주어진 ID로 댓글을 조회합니다.</p>
     *
     * @param commentId 댓글 ID
     * @return Optional<Comment> 조회된 댓글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long commentId) {
        return commentQueryPort.findById(commentId);
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     * <p>특정 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>CommentQuery어댑터의 기존 구현체를 활용합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentInfo> getUserComments(Long userId, Pageable pageable) {
        return commentQueryPort.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>CommentQuery어댑터의 기존 구현체를 활용합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentInfo> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentInfo> getUserLikedComments(Long userId, Pageable pageable) {
        return commentQueryPort.findLikedCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>게시글 ID 목록에 대한 댓글 수 조회</h3>
     * <p>여러 게시글의 댓글 수를 배치로 조회하여 N+1 문제를 해결합니다.</p>
     * <p>CommentQuery어댑터의 기존 구현체를 활용합니다.</p>
     *
     * @param postIds 게시글 ID 목록
     * @return Map<Long, Integer> 게시글 ID를 키로, 댓글 수를 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> findCommentCountsByPostIds(List<Long> postIds) {
        return commentQueryPort.findCommentCountsByPostIds(postIds);
    }

    /**
     * <h3>단일 게시글의 댓글 수 조회</h3>
     * <p>단일 게시글의 댓글 수를 조회합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Integer 댓글 수
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Integer countByPostId(Long postId) {
        return commentQueryPort.countByPostId(postId);
    }
}

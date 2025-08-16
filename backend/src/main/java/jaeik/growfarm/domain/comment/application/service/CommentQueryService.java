package jaeik.growfarm.domain.comment.application.service;

import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.comment.application.port.out.CommentQueryPort;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>댓글 서비스</h2>
 * <p>
 * 댓글 관련 Query 유스케이스를 구현하는 서비스 클래스
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
     * <p>주어진 게시글 ID에 대한 인기 댓글 목록을 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<CommentDTO> 인기 댓글 DTO 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails) {
        List<Long> likedCommentIds = getUserLikedCommentIdsForPopular(postId, userDetails);

        return commentQueryPort.findPopularComments(postId, likedCommentIds);
    }

    /**
     * <h3>인기 댓글에 대한 사용자 추천 ID 조회</h3>
     * <p>주어진 게시글 ID에 대한 인기 댓글 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     * <p>성능 최적화: 인기 댓글 ID를 먼저 조회한 후, 해당 댓글들에 대해서만 추천 여부를 확인합니다.</p>
     *
     * @param postId      게시글 ID
     * @param userDetails 사용자 인증 정보
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIdsForPopular(Long postId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }

        List<CommentDTO> popularComments = commentQueryPort.findPopularComments(postId, Collections.emptyList());
        if (popularComments.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> popularCommentIds = popularComments.stream().map(CommentDTO::getId).collect(Collectors.toList());
        return commentQueryPort.findUserLikedCommentIds(popularCommentIds, userDetails.getUserId());
    }

    /**
     * <h3>페이지별 사용자 추천 ID 조회</h3>
     * <p>주어진 게시글 ID와 페이지 정보에 해당하는 댓글 중 사용자가 추천를 누른 댓글의 ID 목록을 조회합니다.</p>
     * <p>성능 최적화: 페이지의 댓글 ID를 먼저 조회한 후, 해당 댓글들에 대해서만 추천 여부를 확인합니다.</p>
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param userDetails 사용자 인증 정보
     * @return List<Long> 사용자가 추천를 누른 댓글 ID 목록
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIdsByPage(Long postId, Pageable pageable, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Page<CommentDTO> commentPage = commentQueryPort.findCommentsWithLatestOrder(postId, pageable, Collections.emptyList());
        if (!commentPage.hasContent()) {
            return Collections.emptyList();
        }
        
        List<Long> pageCommentIds = commentPage.getContent().stream().map(CommentDTO::getId).collect(Collectors.toList());
        return commentQueryPort.findUserLikedCommentIds(pageCommentIds, userDetails.getUserId());
    }

    /**
     * <h3>최신순 댓글 조회</h3>
     * <p>주어진 게시글 ID에 대한 댓글을 최신순으로 페이지네이션하여 조회합니다.</p>
     *
     * @param postId      게시글 ID
     * @param page        페이지 번호
     * @param userDetails 사용자 인증 정보
     * @return Page<CommentDTO> 최신순 댓글 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        Pageable pageable = Pageable.ofSize(20).withPage(page);
        List<Long> likedCommentIds = getUserLikedCommentIdsByPage(postId, pageable, userDetails);

        return commentQueryPort.findCommentsWithLatestOrder(postId, pageable, likedCommentIds);
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
     * <p>CommentQueryPort의 기존 구현체를 활용합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 작성한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentDTO> getUserComments(Long userId, Pageable pageable) {
        return commentQueryPort.findCommentsByUserId(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     * <p>특정 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     * <p>CommentQueryPort의 기존 구현체를 활용합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<SimpleCommentDTO> 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SimpleCommentDTO> getUserLikedComments(Long userId, Pageable pageable) {
        return commentQueryPort.findLikedCommentsByUserId(userId, pageable);
    }
}

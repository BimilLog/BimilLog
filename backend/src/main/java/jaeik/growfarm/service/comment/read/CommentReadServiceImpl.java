package jaeik.growfarm.service.comment.read;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.QComment;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.read.CommentReadRepository;
import jaeik.growfarm.repository.comment.user.CommentUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <h2>CommentReadService</h2>
 * <p>댓글 조회 관련 서비스를 담당하는 클래스입니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReadServiceImpl implements CommentReadService {

    private final CommentReadRepository commentReadRepository;
    private final CommentUserRepository commentUserRepository;

    /**
     * <h3>인기댓글 조회</h3>
     * <p>
     * 해당 게시글의 댓글 중에서 추천수 3개 이상인 상위 3개를 조회
     * </p>
     *
     * @param postId      게시글 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 인기댓글 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails) {

        try {
            List<Tuple> popularTuples = commentReadRepository.findPopularComments(postId);

            if (popularTuples.isEmpty()) {
                return List.of();
            }

            List<Long> popularCommentIds = popularTuples.stream()
                    .map(tuple -> {
                        Comment comment = tuple.get(QComment.comment);
                        return comment != null ? comment.getId() : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            List<Long> userLikedCommentIds = getUserLikedCommentIds(popularCommentIds, userDetails);

            List<CommentDTO> popularComments = new ArrayList<>();
            for (Tuple tuple : popularTuples) {
                Comment comment = tuple.get(QComment.comment);
                if (comment == null)
                    continue;

                try {
                    CommentDTO commentDTO = new CommentDTO(comment);

                    Long likeCount = tuple.get(1, Long.class);
                    commentDTO.setLikes(likeCount != null ? likeCount.intValue() : 0);

                    commentDTO.setUserLike(userLikedCommentIds.contains(comment.getId()));

                    // parentId 설정 (tuple의 3번째 요소)
                    Long parentId = tuple.get(2, Long.class);
                    commentDTO.setParentId(parentId);

                    commentDTO.setPopular(true);

                    popularComments.add(commentDTO);
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.POPULAR_COMMENT_FAILED, e);
                }
            }

            return popularComments;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * <h3>일반 댓글 조회</h3>
     * <p>
     * 루트댓글을 최신순으로 조회하고 자손댓글도 함께 반환
     * </p>
     * <p>
     * 인기댓글도 포함되어 중복 표시됨
     * </p>
     *
     * @param postId      게시글 ID
     * @param page        페이지 번호
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 댓글 목록 페이지
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        if (page < 0) {
            return Page.empty();
        }

        Pageable pageable = Pageable.ofSize(20).withPage(page);

        try {
            List<Tuple> commentTuples = commentReadRepository.findCommentsWithLatestOrder(postId, pageable);

            if (commentTuples.isEmpty()) {
                return Page.empty(pageable);
            }

            List<Long> commentIds = commentTuples.stream()
                    .map(tuple -> {
                        Comment comment = tuple.get(QComment.comment);
                        return comment != null ? comment.getId() : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<Long> userLikedCommentIds = getUserLikedCommentIds(commentIds, userDetails);

            Map<Long, CommentDTO> commentMap = new HashMap<>();
            List<CommentDTO> rootComments = new ArrayList<>();

            for (Tuple tuple : commentTuples) {
                Comment comment = tuple.get(QComment.comment);
                if (comment == null)
                    continue;

                Long commentId = comment.getId();
                if (commentMap.containsKey(commentId))
                    continue;

                try {
                    CommentDTO dto = new CommentDTO(comment);

                    Long likeCount = tuple.get(1, Long.class);
                    dto.setLikes(likeCount != null ? likeCount.intValue() : 0);

                    dto.setUserLike(userLikedCommentIds.contains(commentId));

                    // parentId 설정 (tuple의 5번째 요소)
                    Long parentId = tuple.get(4, Long.class);
                    dto.setParentId(parentId);

                    commentMap.put(commentId, dto);

                    Integer depth = tuple.get(3, Integer.class);
                    if (depth != null && depth == 0) {
                        rootComments.add(dto);
                    }
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.COMMENT_FAILED, e);
                }
            }

            Long totalCount = commentReadRepository.countRootCommentsByPostId(postId);

            return new PageImpl<>(rootComments, pageable, totalCount != null ? totalCount : 0L);

        } catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    /**
     * <h3>사용자가 추천한 댓글 ID 조회</h3>
     * <p>
     * 로그인한 사용자가 추천한 댓글들의 ID를 조회한다.
     * </p>
     *
     * @param commentIds  댓글 ID 리스트
     * @param userDetails 사용자 정보
     * @return 사용자가 추천한 댓글 ID 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    private List<Long> getUserLikedCommentIds(List<Long> commentIds, CustomUserDetails userDetails) {
        return (userDetails != null)
                ? commentUserRepository.findUserLikedCommentIds(commentIds, userDetails.getUserId())
                : List.of();
    }
}

package jaeik.growfarm.service.comment;

import com.querydsl.core.Tuple;
import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.*;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h2>CommentService</h2>
 * <p>
 * 댓글 작성
 * </p>
 * <p>
 * 댓글 수정
 * </p>
 * <p>
 * 댓글 삭제
 * </p>
 * <p>
 * 댓글 추천
 * </p>
 * <p>
 * 댓글 추천 취소
 * </p>
 * <p>
 * 인기 댓글 선정
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentClosureRepository commentClosureRepository;
    private final CommentUpdateService commentUpdateService;

    /**
     * <h3>댓글 작성</h3>
     *
     * <p>
     * 댓글을 DB에 저장하고 글 작성자에게 실시간 알림과 푸시 메시지를 발송한다.
     * </p>
     * <p>
     * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.
     * </p>
     * <p>
     * 클로저 테이블 패턴을 활용하여 계층형태로 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param commentDTO  댓글 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public void writeComment(CustomUserDetails userDetails, CommentDTO commentDTO) {
        Post post = postRepository.getReferenceById(commentDTO.getPostId());
        Users user = (userDetails != null) ? userRepository.getReferenceById(userDetails.getUserId()) : null;

        commentUpdateService.saveCommentWithClosure(
                post,
                user,
                commentDTO.getContent(),
                commentDTO.getPassword(),
                commentDTO.getParentId());

        if (post.getUser() != null) {
            eventPublisher.publishEvent(new CommentCreatedEvent(
                    post.getUser().getId(),
                    commentDTO.getUserName(),
                    commentDTO.getPostId()));
        }
    }

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
     * @since 1.0.0
     */
    public List<CommentDTO> getPopularComments(Long postId, CustomUserDetails userDetails) {

        try {
            List<Tuple> popularTuples = commentRepository.findPopularComments(postId);

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
     * @since 1.0.0
     */
    public Page<CommentDTO> getCommentsLatestOrder(Long postId, int page, CustomUserDetails userDetails) {
        if (page < 0) {
            return Page.empty();
        }

        Pageable pageable = Pageable.ofSize(20).withPage(page);

        try {
            List<Tuple> commentTuples = commentRepository.findCommentsWithLatestOrder(postId, pageable);

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

            Long totalCount = commentRepository.countRootCommentsByPostId(postId);

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
     * @since 1.0.0
     */
    private List<Long> getUserLikedCommentIds(List<Long> commentIds, CustomUserDetails userDetails) {
        return (userDetails != null)
                ? commentRepository.findUserLikedCommentIds(commentIds, userDetails.getUserId())
                : List.of();
    }

    /**
     * <h3>댓글 수정</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 수정할 수 있다.
     * </p>
     *
     * @param commentDTO  수정할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void updateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = ValidateComment(commentDTO, userDetails);
        commentUpdateService.commentUpdate(commentDTO, comment);
    }


    /**
     * <h3>댓글 삭제</h3>
     *
     * <p>
     * 댓글 작성자만 댓글을 삭제할 수 있다.
     * </p>
     * <p>
     * 자손 댓글이 있는 경우: Soft Delete (논리적 삭제) - "삭제된 메시지입니다" 표시
     * </p>
     * <p>
     * 자손 댓글이 없는 경우: Hard Delete (물리적 삭제) - 완전 삭제
     * </p>
     *
     * @param commentDTO  삭제할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = ValidateComment(commentDTO, userDetails);
        Long commentId = commentDTO.getId();

        try {
            boolean hasDescendants = commentClosureRepository.hasDescendants(commentId);
            commentUpdateService.commentDelete(hasDescendants, commentId, comment);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED, e);
        }
    }

    /**
     * <h3>댓글 추천</h3>
     *
     * <p>
     * 댓글을 추천하거나 추천 취소한다.
     * </p>
     * <p>
     * 이미 추천한 경우 추천을 취소하고, 추천하지 않은 경우 추천을 추가한다.
     * </p>
     *
     * @param commentDTO  추천할 댓글 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void likeComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Long commentId = commentDTO.getId();
        Long userId = userDetails.getUserId();

        Comment comment = commentRepository.getReferenceById(commentId);
        Users user = userRepository.getReferenceById(userId);

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        commentUpdateService.saveCommentLike(existingLike, comment, user);
    }

    /**
     * <h3>댓글 유효성 검사</h3>
     *
     * <p>
     * 댓글 수정 및 삭제 시 비밀번호 확인 및 작성자 확인을 수행한다.
     * </p>
     *
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 유효한 댓글 엔티티
     * @throws CustomException 댓글 비밀번호 불일치 또는 작성자 불일치 시 예외 발생
     */
    private Comment ValidateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = commentRepository.getReferenceById(commentDTO.getId());

        if (commentDTO.getPassword() != null && !Objects.equals(comment.getPassword(), commentDTO.getPassword())) {
            throw new CustomException(ErrorCode.COMMENT_PASSWORD_NOT_MATCH);
        }

        if (commentDTO.getPassword() == null
                && !comment.getUser().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new CustomException(ErrorCode.ONLY_COMMENT_OWNER_UPDATE);
        }
        return comment;
    }
}

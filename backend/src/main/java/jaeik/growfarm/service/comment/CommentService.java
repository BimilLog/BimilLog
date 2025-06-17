package jaeik.growfarm.service.comment;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.comment.CommentLike;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.event.CommentFeaturedEvent;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

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

        eventPublisher.publishEvent(new CommentCreatedEvent(
                post.getUser().getId(),
                commentDTO.getFarmName(),
                commentDTO.getPostId(),
                post.getUser()));
    }

    /**
     * <h3>댓글 조회</h3>
     *
     * <p>
     * 게시글에 달린 댓글을 페이지 단위로 조회한다.
     * </p>
     * <p>
     * 루트 댓글과 자손 댓글들을 클로저 테이블에서 조회하여 트리 구조로 구성한다.
     * </p>
     *
     * @param postId      게시글 ID
     * @param page        페이지 번호 (0부터 시작)
     * @param userDetails 현재 로그인 한 사용자 정보 (추천 여부 확인용)
     * @return 댓글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<CommentDTO> getComments(Long postId, int page, CustomUserDetails userDetails) {
        Pageable pageable = Pageable.ofSize(30).withPage(page);

        Page<Comment> rootCommentPage = commentRepository.findRootCommentsByPostId(postId, pageable);

        if (rootCommentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> rootCommentIds = rootCommentPage.getContent()
                .stream()
                .map(Comment::getId)
                .toList();

        List<Object[]> commentWithParentResults = commentRepository.findCommentsWithParentByRootIds(rootCommentIds);

        List<Long> allCommentIds = commentWithParentResults.stream()
                .map(row -> (Long) row[0])
                .distinct()
                .toList();

        Map<Long, Integer> likeCountMap = buildLikeCountMap(allCommentIds);

        List<Long> userLikedCommentIds = getUserLikedCommentIds(allCommentIds, userDetails);

        Map<Long, CommentDTO> commentDTOMap = buildCommentDTOMap(commentWithParentResults, likeCountMap,
                userLikedCommentIds);

        List<CommentDTO> rootCommentDTOs = rootCommentPage.getContent()
                .stream()
                .map(comment -> commentDTOMap.get(comment.getId()))
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(rootCommentDTOs, pageable, rootCommentPage.getTotalElements());
    }

    /**
     * <h3>추천 수 맵 생성</h3>
     * <p>
     * 댓글 ID와 추천 수의 매핑을 생성한다.
     * </p>
     *
     * @param commentIds 댓글 ID 리스트
     * @return 댓글 ID와 추천 수의 매핑
     * @author Jaeik
     * @since 1.0.0
     */
    private Map<Long, Integer> buildLikeCountMap(List<Long> commentIds) {
        return commentRepository.findLikeCountsByCommentIds(commentIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0], // commentId
                        row -> ((Number) row[1]).intValue() // likeCount
                ));
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
     * <h3>댓글 DTO 맵 생성</h3>
     * <p>
     * 쿼리 결과를 기반으로 CommentDTO 맵을 생성하고 관계를 설정한다.
     * </p>
     *
     * @param commentWithParentResults 댓글과 부모 관계 쿼리 결과
     * @param likeCountMap             추천 수 매핑
     * @param userLikedCommentIds      사용자가 추천한 댓글 ID 리스트
     * @return 댓글 ID와 DTO의 매핑
     * @author Jaeik
     * @since 1.0.0
     */
    private Map<Long, CommentDTO> buildCommentDTOMap(
            List<Object[]> commentWithParentResults,
            Map<Long, Integer> likeCountMap,
            List<Long> userLikedCommentIds) {

        Map<Long, CommentDTO> commentDTOMap = new HashMap<>();

        for (Object[] row : commentWithParentResults) {
            Long commentId = (Long) row[0];
            Long parentId = (Long) row[1];
            Comment comment = (Comment) row[3];

            if (comment == null) {
                continue;
            }

            CommentDTO dto = commentDTOMap.get(commentId);
            if (dto == null) {
                dto = new CommentDTO(comment);

                dto.setLikes(likeCountMap.getOrDefault(commentId, 0));
                dto.setUserLike(userLikedCommentIds.contains(commentId));

                commentDTOMap.put(commentId, dto);
            }

            if (parentId != null) {
                dto.setParentId(parentId);
            }
        }

        return commentDTOMap;
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
        comment.updateComment(commentDTO.getContent());
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

            if (hasDescendants) {
                comment.softDelete();
            } else {
                commentUpdateService.hardDelete(commentId, comment);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED);
        }
    }

    /**
     * <h3>댓글 추천</h3>
     *
     * <p>
     * 댓글에 추천을 추가하거나 제거한다.
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

        commentUpdateService.likeSaveComment(existingLike, comment, user);
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

    /**
     * <h3>인기 댓글 업데이트</h3>
     *
     * <p>10분마다 추천 수 3개 이상인 댓글을 인기댓글로 선정하고, 이벤트 발행을 통해 SSE와 FCM 알림을 비동기로 처리한다.</p>
     *
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    @Scheduled(fixedRate = 1000 * 60 * 10) // 10분마다 실행
    public void updateFeaturedComments() {
        // Step 1: 기존 인기 댓글 초기화
        commentRepository.resetAllCommentFeaturedFlags();

        // Step 2: 추천 수 3개 이상인 댓글 전부 불러오기
        List<Comment> popularComments = commentRepository.findPopularComments();

        // Step 3: 게시글별 상위 3개만 선정
        Map<Long, List<Comment>> topCommentsByPost = popularComments.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getPost().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().limit(3).toList())));

        // Step 4: 인기 댓글 지정 및 이벤트 발행 🚀
        topCommentsByPost.values().stream()
                .flatMap(List::stream)
                .forEach(comment -> {
                    comment.updatePopular(true); // 인기 댓글 지정

                    // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
                    eventPublisher.publishEvent(new CommentFeaturedEvent(
                            comment.getUser().getId(),
                            comment.getPost().getId(),
                            comment.getUser()));
                });
    }

}

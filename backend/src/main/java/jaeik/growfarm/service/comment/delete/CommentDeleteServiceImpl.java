package jaeik.growfarm.service.comment.delete;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentClosureRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <h2>CommentDeleteService</h2>
 * <p>댓글 삭제 관련 서비스를 담당하는 클래스입니다.</p>
 *
 * @author jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentDeleteServiceImpl implements CommentDeleteService {

    private final CommentRepository commentRepository;
    private final CommentClosureRepository commentClosureRepository;

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
    @Override
    public void deleteComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDTO, userDetails);
        Long commentId = commentDTO.getId();

        try {
            boolean hasDescendants = commentClosureRepository.hasDescendants(commentId);
            if (hasDescendants) {
                comment.softDelete();
            } else {
                commentClosureRepository.deleteByDescendantId(commentId);
                commentRepository.delete(comment);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FAILED, e);
        }
    }

    /**
     * <h3>댓글 유효성 검사</h3>
     *
     * <p>
     * 댓글 삭제 시 비밀번호 확인 및 작성자 확인을 수행한다.
     * </p>
     *
     * @param commentDTO  댓글 정보 DTO
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 유효한 댓글 엔티티
     * @throws CustomException 댓글 비밀번호 불일치 또는 작성자 불일치 시 예외 발생
     */
    private Comment validateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
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

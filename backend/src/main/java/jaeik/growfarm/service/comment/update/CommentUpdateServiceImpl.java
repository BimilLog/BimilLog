package jaeik.growfarm.service.comment.update;

import jaeik.growfarm.dto.comment.CommentDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <h2>댓글 업데이트 서비스</h2>
 * <p>
 * 댓글의 DB작업을 처리하는 서비스
 * </p>
 *
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class CommentUpdateServiceImpl implements CommentUpdateService {

    private final CommentRepository commentRepository;

    /**
     * <h3>댓글 수정</h3>
     * <p>댓글 내용을 수정한다.</p>
     *
     * @param commentDTO 댓글 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    @Override
    public void updateComment(CommentDTO commentDTO, CustomUserDetails userDetails) {
        Comment comment = validateComment(commentDTO, userDetails);
        comment.updateComment(commentDTO.getContent());
    }

    /**
     * <h3>댓글 유효성 검사</h3>
     *
     * <p>
     * 댓글 수정 시 비밀번호 확인 및 작성자 확인을 수행한다.
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

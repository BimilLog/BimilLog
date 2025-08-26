package jaeik.growfarm.infrastructure.adapter.comment.in.web;

import jaeik.growfarm.domain.comment.entity.SimpleCommentInfo;
import jaeik.growfarm.domain.comment.entity.CommentInfo;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import jaeik.growfarm.infrastructure.adapter.comment.in.web.dto.CommentDTO;
import org.springframework.stereotype.Component;

/**
 * <h3>Comment 응답 매퍼</h3>
 * <p>댓글 도메인 객체를 응답 DTO로 변환하는 매퍼 클래스</p>
 * <p>Controller의 변환 로직을 분리하여 단일 책임 원칙을 준수</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Component
public class CommentResponseMapper {

    /**
     * <h3>SimpleCommentInfo를 SimpleCommentDTO로 변환</h3>
     *
     * @param commentInfo 변환할 도메인 객체
     * @return SimpleCommentDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public SimpleCommentDTO convertToSimpleCommentDTO(SimpleCommentInfo commentInfo) {
        return new SimpleCommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getUserName(),
                commentInfo.getContent(),
                commentInfo.getCreatedAt(),
                commentInfo.getLikeCount(),
                commentInfo.isUserLike()
        );
    }

    /**
     * <h3>CommentInfo를 CommentDTO로 변환</h3>
     *
     * @param commentInfo 변환할 도메인 객체
     * @return CommentDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public CommentDTO convertToCommentDTO(CommentInfo commentInfo) {
        CommentDTO commentDTO = new CommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getUserId(),
                commentInfo.getUserName(),
                commentInfo.getContent(),
                commentInfo.isDeleted(),
                commentInfo.getCreatedAt(),
                commentInfo.getParentId(),
                commentInfo.getLikeCount()
        );
        commentDTO.setPopular(commentInfo.isPopular());
        commentDTO.setUserLike(commentInfo.isUserLike());
        return commentDTO;
    }
}
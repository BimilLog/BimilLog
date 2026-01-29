package jaeik.bimillog.application.comment.dto;

import jaeik.bimillog.domain.comment.entity.CommentInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * <h2>댓글 DTO</h2>
 * <p>인기 댓글과 일반 댓글을 모두 관리한다.</p>
 *
 * @version 2.7.0
 * @author Jaeik
 */
@Getter
@AllArgsConstructor
public class CommentDTO {
    private List<CommentInfo> popularCommentList;
    private Page<CommentInfo> commentInfoPage;

    public static CommentDTO from(List<CommentInfo> popularCommentList, Page<CommentInfo> commentInfoPage) {
        return new CommentDTO(popularCommentList, commentInfoPage);
    }
}

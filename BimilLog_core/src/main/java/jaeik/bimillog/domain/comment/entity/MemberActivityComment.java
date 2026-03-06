package jaeik.bimillog.domain.comment.entity;

import lombok.*;
import org.springframework.data.domain.Page;

/**
 * 사용자가 마이페이지에서 보는 댓글 객체
 */
@Getter
@AllArgsConstructor
public class MemberActivityComment extends CommentInfo {
    private Page<SimpleCommentInfo> writeComments;
    private Page<SimpleCommentInfo> likedComments;
}

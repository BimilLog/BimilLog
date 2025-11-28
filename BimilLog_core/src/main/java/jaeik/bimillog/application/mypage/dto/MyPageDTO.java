package jaeik.bimillog.application.mypage.dto;

import jaeik.bimillog.domain.comment.entity.MemberActivityComment;
import jaeik.bimillog.domain.post.entity.MemberActivityPost;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MyPageDTO {
    MemberActivityComment memberActivityComment;
    MemberActivityPost memberActivityPost;

    public static MyPageDTO from(MemberActivityComment memberActivityComment, MemberActivityPost memberActivityPost) {
        return new MyPageDTO(memberActivityComment, memberActivityPost);
    }
}

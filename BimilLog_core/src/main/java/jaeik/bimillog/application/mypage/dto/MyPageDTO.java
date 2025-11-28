package jaeik.bimillog.application.mypage.dto;

import jaeik.bimillog.domain.comment.entity.MemberActivityComment;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

@AllArgsConstructor
public class MyPageDTO {
    MemberActivityComment memberActivityComment;
    Page<PostSimpleDetail> SimplePostList;
    Page<PostSimpleDetail> likedPosts;

    public static MyPageDTO from(MemberActivityComment memberActivityComment,
                                 Page<PostSimpleDetail> SimplePostList, Page<PostSimpleDetail> likedPosts) {
        return new MyPageDTO(memberActivityComment, SimplePostList, likedPosts);
    }
}

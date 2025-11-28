package jaeik.bimillog.application.mypage.dto;

import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

@AllArgsConstructor
public class MyPageDTO {
    Page<SimpleCommentInfo> commentInfoList;
    Page<SimpleCommentInfo> likedCommentsInfo;
    Page<PostSimpleDetail> SimplePostList;
    Page<PostSimpleDetail> likedPosts;

    public static MyPageDTO from(Page<SimpleCommentInfo> commentInfoList, Page<SimpleCommentInfo> likedCommentsInfo,
                                 Page<PostSimpleDetail> SimplePostList, Page<PostSimpleDetail> likedPosts) {
        return new MyPageDTO(commentInfoList, likedCommentsInfo, SimplePostList, likedPosts);
    }
}

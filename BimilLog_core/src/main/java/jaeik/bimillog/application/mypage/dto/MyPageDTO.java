package jaeik.bimillog.application.mypage.dto;

import jaeik.bimillog.domain.comment.dto.SimpleCommentDTO;
import jaeik.bimillog.domain.post.dto.SimplePostDTO;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

@AllArgsConstructor
public class MyPageDTO {
    Page<SimpleCommentDTO> commentList;
    Page<SimpleCommentDTO> likedComments;
    Page<SimplePostDTO> postList;
    Page<SimplePostDTO> likePosts;

    public static MyPageDTO from(Page<SimpleCommentDTO> commentList, Page<SimpleCommentDTO> likedComments,
                                 Page<SimplePostDTO> PostList, Page<SimplePostDTO> likePosts) {
        return new MyPageDTO(commentList, likedComments, PostList, likePosts);
    }
}

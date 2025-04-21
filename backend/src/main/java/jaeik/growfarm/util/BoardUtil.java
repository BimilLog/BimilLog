package jaeik.growfarm.util;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.board.Comment;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardUtil {

    private final UserRepository userRepository;

    // 게시글 목록 보기 시 사용하는 변환 로직
    public SimplePostDTO postToSimpleDTO(Post post, int commentCount, int likes) {
        return new SimplePostDTO(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getFarmName(),
                post.getTitle(),
                commentCount,
                likes,
                post.getViews(),
                post.getCreatedAt(),
                post.isFeatured(),
                post.isNotice()
        );
    }

    public Post postReqDTOToPost(PostReqDTO postReqDTO) {
        return Post.builder()
                .user(getUserByPostReqDTO(postReqDTO))
                .title(postReqDTO.getTitle())
                .content(postReqDTO.getContent())
                .views(0)
                .isNotice(false)
                .isFeatured(false)
                .build();
    }

    public PostDTO postToDTO(Post post, int likes, List<CommentDTO> comments, boolean userLike) {
        return new PostDTO(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getFarmName(),
                post.getTitle(),
                post.getContent(),
                post.getViews(),
                likes,
                post.isNotice(),
                post.isFeatured(),
                post.getCreatedAt(),
                comments,
                userLike
        );
    }

    // 댓글 -> DTO 변환
    public CommentDTO commentToDTO(Comment comment, int likes, boolean userLike) {
        return new CommentDTO(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getFarmName(),
                comment.getContent(),
                likes,
                comment.getCreatedAt(),
                comment.isFeatured(),
                userLike
        );
    }

    public Comment commentDTOToComment(CommentDTO commentDTO, Post post, Users user) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(commentDTO.getContent())
                .isFeatured(commentDTO.is_featured())
                .build();
    }


    private Users getUserByPostReqDTO(PostReqDTO postReqDTO) {
        return userRepository.findById(postReqDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + postReqDTO.getUserId()));
    }
}

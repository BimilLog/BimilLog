package jaeik.growfarm.util;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.dto.board.PostReqDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.entity.board.Comment;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/*
 * BoardUtil 클래스
 * 게시판 관련 유틸리티 클래스
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class BoardUtil {

    /**
     * <h3>Post 엔티티를 SimplePostDTO로 변환</h3>
     *
     * <p>
     * 게시글 목록 보기 시 사용하는 변환 로직이다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param post         Post 엔티티
     * @param commentCount 댓글 수
     * @param likes        좋아요 수
     * @return 간단한 게시글 DTO
     */
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
                post.isNotice(),
                post.isRealtimePopular(),
                post.isWeeklyPopular(),
                post.isHallOfFame());
    }

    /**
     * <h3>PostReqDTO를 Post 엔티티로 변환</h3>
     *
     * <p>
     * 게시글 작성 요청 DTO를 Post 엔티티로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param user       작성자 정보
     * @param postReqDTO 게시글 작성 요청 DTO
     * @return Post 엔티티
     */
    public Post postReqDTOToPost(Users user, PostReqDTO postReqDTO) {
        return Post.builder()
                .user(user)
                .title(postReqDTO.getTitle())
                .content(postReqDTO.getContent())
                .views(0)
                .isNotice(false)
                .isRealtimePopular(false)
                .isWeeklyPopular(false)
                .isWeeklyPopular(false)
                .build();
    }

    /**
     * <h3>Post 엔티티를 PostDTO로 변환</h3>
     *
     * <p>
     * 게시글 상세 보기 시 사용하는 변환 로직이다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param post     Post 엔티티
     * @param likes    좋아요 수
     * @param comments 댓글 목록
     * @param userLike 사용자 좋아요 여부
     * @return 게시글 상세 DTO
     */
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
                post.isRealtimePopular(),
                post.isWeeklyPopular(),
                post.isHallOfFame(),
                post.getCreatedAt(),
                comments,
                userLike);
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
                userLike);
    }

    public Comment commentDTOToComment(CommentDTO commentDTO, Post post, Users user) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(commentDTO.getContent())
                .isFeatured(commentDTO.is_featured())
                .build();
    }
}

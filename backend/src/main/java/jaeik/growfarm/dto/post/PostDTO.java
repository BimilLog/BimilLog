package jaeik.growfarm.dto.post;

import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.post.Post;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h3>게시글 상세 정보 DTO</h3>
 * <p>
 * 게시글 상세 보기용 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
public class PostDTO {
    private Long postId;

    private Long userId;

    @Size(max = 8, message = "닉네임 은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    private int views;

    private int likes;

    private boolean isNotice;

    private PopularFlag popularFlag;

    private Instant createdAt;

    private boolean userLike;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다.")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다.")
    private Integer password;

    /**
     * <h3>새 게시글을 PostDTO로 변환</h3>
     * <p>
     * 게시글 상세 보기 시 사용하는 변환 로직이다.
     * </p>
     *
     * @param post 변환할 Post 엔티티
     * @return 변환된 PostDTO 객체
     * @since 1.0.0
     * @author Jaeik
     */
    public static PostDTO newPost(Post post) {
        PostDTO postDTO = new PostDTO();
        postDTO.setPostId(post.getId());

        Long userId = (post.getUser() != null) ? post.getUser().getId() : null;
        String userName = (post.getUser() != null) ? post.getUser().getUserName() : "익명";

        postDTO.setUserId(userId);
        postDTO.setUserName(userName);

        postDTO.setTitle(post.getTitle());
        postDTO.setContent(post.getContent());
        postDTO.setViews(0);
        postDTO.setLikes(0);
        postDTO.setNotice(false);
        postDTO.setPopularFlag(null);
        postDTO.setCreatedAt(post.getCreatedAt());
        return postDTO;
    }

    /**
     * <h3>기존 게시글을 PostDTO로 변환</h3>
     * <p>
     * Post 엔티티를 기반으로 PostDTO를 생성한다.
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     */
    public static PostDTO existedPost(Long postId, Long userId, String userName, String title, String content,
            int views, int likes, boolean isNotice, PopularFlag popularFlag, Instant createdAt, boolean userLike) {

        String nickName = userName != null ? userName : "익명";

        PostDTO postDTO = new PostDTO();
        postDTO.setPostId(postId);
        postDTO.setUserId(userId);
        postDTO.setUserName(nickName);
        postDTO.setTitle(title);
        postDTO.setContent(content);
        postDTO.setViews(views);
        postDTO.setLikes(likes);
        postDTO.setNotice(isNotice);
        postDTO.setPopularFlag(popularFlag);
        postDTO.setCreatedAt(createdAt);
        postDTO.setUserLike(userLike);
        return postDTO;
    }
}

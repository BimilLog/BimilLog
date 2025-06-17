package jaeik.growfarm.dto.board;

import jaeik.growfarm.entity.post.PopularFlag;
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

    private boolean is_notice;

    private PopularFlag popularFlag;

    private Instant createdAt;

    private boolean userLike;

    @Size(max = 8, message = "비밀번호는 최대 8글자 까지 입력 가능합니다.")
    private Integer password;

    public PostDTO(Long postId, Long userId, String userName, String title, String content, int views, int likes, boolean is_notice, PopularFlag popularFlag, Instant createdAt, boolean userLike) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.views = views;
        this.likes = likes;
        this.is_notice = is_notice;
        this.popularFlag = popularFlag;
        this.createdAt = createdAt;
        this.userLike = userLike;
    }
}

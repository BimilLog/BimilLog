package jaeik.growfarm.dto.board;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 댓글 DTO
@Getter @Setter
public class CommentDTO {

    private Long id;

    private Long postId;

    private Long userId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    @Size(max = 255, message = "댓글은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private int likes;

    private LocalDateTime createdAt;

    private boolean is_featured;

    private boolean userLike;

    public CommentDTO(Long id, Long postId, Long userId, String farmName, String content, int likes, LocalDateTime createdAt, boolean featured, boolean userLike) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.farmName = farmName;
        this.content = content;
        this.likes = likes;
        this.createdAt = createdAt;
        this.is_featured = featured;
        this.userLike = userLike;
    }
}

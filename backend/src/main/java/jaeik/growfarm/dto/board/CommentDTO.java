package jaeik.growfarm.dto.board;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CommentDTO {

    private Long id;

    private Long postId;

    private Long userId;

    private String farmName;

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

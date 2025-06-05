package jaeik.growfarm.dto.board;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// 댓글 DTO
@Getter @Setter
public class CommentDTO {

    private Long id;

    private Long parentId;

    @NotNull
    private Long postId;

    @NotNull
    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    @NotNull
    @Size(max = 255, message = "댓글은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private boolean popular;

    private Integer password;

    private int likes;

    private Instant createdAt;

    private boolean userLike;

    public CommentDTO(Long id, Long postId, String farmName, String content, int likes, Instant createdAt, Integer password, boolean featured, boolean userLike) {
        this.id = id;
        this.postId = postId;
        this.farmName = farmName;
        this.content = content;
        this.likes = likes;
        this.createdAt = createdAt;
        this.password = password;
        this.popular = featured;
        this.userLike = userLike;
    }
}

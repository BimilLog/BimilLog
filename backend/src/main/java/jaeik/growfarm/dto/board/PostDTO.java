package jaeik.growfarm.dto.board;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// 글 상세 보기용 DTO
@Getter
@Setter
public class PostDTO {
    private Long postId;

    private Long userId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    @Size(max = 30, message = "글 제목은 최대 30자 까지 입력 가능합니다.")
    private String title;

    @Size(max = 1000, message = "글 내용은 최대 1000자 까지 입력 가능합니다.")
    private String content;

    private int views;

    private int likes;

    private boolean is_notice;

    private boolean is_RealtimePopular;

    private boolean is_WeeklyPopular;

    private boolean is_HallOfFame;

    private LocalDateTime createdAt;

    private List<CommentDTO> comments;

    private boolean userLike;

    public PostDTO(Long postId, Long userId, String farmName, String title, String content, int views, int likes, boolean is_notice, boolean is_RealtimePopular, boolean is_WeeklyPopular, boolean is_HallOfFame, LocalDateTime createdAt, List<CommentDTO> comments, boolean userLike) {
        this.postId = postId;
        this.userId = userId;
        this.farmName = farmName;
        this.title = title;
        this.content = content;
        this.views = views;
        this.likes = likes;
        this.is_notice = is_notice;
        this.is_RealtimePopular = is_RealtimePopular;
        this.is_WeeklyPopular = is_WeeklyPopular;
        this.is_HallOfFame = is_HallOfFame;
        this.createdAt = createdAt;
        this.comments = comments;
        this.userLike = userLike;
    }
}

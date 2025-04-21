package jaeik.growfarm.dto.board;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 글 목록 보기 용 DTO
@Getter @Setter
public class SimplePostDTO {

    private Long postId;

    private Long userId;

    private String farmName;

    private String title;

    private int commentCount;

    private int likes;

    private int views;

    private LocalDateTime createdAt;

    private boolean is_featured;

    private boolean is_notice;


    public SimplePostDTO(Long postId, Long userId, String farmName, String title, int commentCount, int likes, int views, LocalDateTime createdAt, boolean is_featured, boolean is_notice) {
        this.postId = postId;
        this.userId = userId;
        this.farmName = farmName;
        this.title = title;
        this.commentCount = commentCount;
        this.likes = likes;
        this.views = views;
        this.createdAt = createdAt;
        this.is_featured = is_featured;
        this.is_notice = is_notice;
    }
}

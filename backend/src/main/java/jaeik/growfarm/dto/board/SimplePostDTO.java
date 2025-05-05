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

    private boolean is_notice;

    private boolean is_RealtimePopular;

    private boolean is_WeeklyPopular;

    private boolean is_HallOfFame;

    public SimplePostDTO(Long postId, Long userId, String farmName, String title, int commentCount, int likes, int views, LocalDateTime createdAt, boolean is_notice, boolean isRealtimePopular, boolean isWeeklyPopular, boolean isHallOfFame) {
        this.postId = postId;
        this.userId = userId;
        this.farmName = farmName;
        this.title = title;
        this.commentCount = commentCount;
        this.likes = likes;
        this.views = views;
        this.createdAt = createdAt;
        this.is_notice = is_notice;
        this.is_RealtimePopular = isRealtimePopular;
        this.is_WeeklyPopular = isWeeklyPopular;
        this.is_HallOfFame = isHallOfFame;
    }
}

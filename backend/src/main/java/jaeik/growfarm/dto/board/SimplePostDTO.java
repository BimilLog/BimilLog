package jaeik.growfarm.dto.board;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 글 목록 보기 용 DTO
@Getter @Setter
public class SimplePostDTO {

    private Long postId;

    private Long userId;

    @Size(max = 8, message = "농장 이름은 최대 8글자 까지 입력 가능합니다.")
    private String farmName;

    @Size(max = 30, message = "글 내용은 최대 30자 까지 입력 가능합니다.")
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

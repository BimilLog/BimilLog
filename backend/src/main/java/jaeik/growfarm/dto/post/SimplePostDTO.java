package jaeik.growfarm.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jaeik.growfarm.entity.post.PopularFlag;
import jaeik.growfarm.entity.user.Users;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h3>게시글 목록용 DTO</h3>
 * <p>
 * 게시글 목록 보기용 간단한 데이터 전송 객체
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
public class SimplePostDTO {

    private Long postId;

    private Long userId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 30, message = "글 내용은 최대 30자 까지 입력 가능합니다.")
    private String title;

    private int commentCount;

    private int likes;

    private int views;

    private Instant createdAt;

    private boolean is_notice;

    private PopularFlag popularFlag;

    // 인기글 선정 시 알림 이벤트 발행용 (JSON 직렬화에서 제외)
    @JsonIgnore
    private Users user;

    public SimplePostDTO(Long postId, Long userId, String userName, String title, int commentCount, int likes,
            int views, Instant createdAt, boolean is_notice) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.commentCount = commentCount;
        this.likes = likes;
        this.views = views;
        this.createdAt = createdAt;
        this.is_notice = is_notice;
    }

    public SimplePostDTO(Long postId, Long userId, String userName, String title, int commentCount, int likes,
            int views, Instant createdAt, boolean is_notice, Users user) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.commentCount = commentCount;
        this.likes = likes;
        this.views = views;
        this.createdAt = createdAt;
        this.is_notice = is_notice;
        this.user = user;
    }
}

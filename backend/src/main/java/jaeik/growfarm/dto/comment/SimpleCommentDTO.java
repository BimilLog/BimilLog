package jaeik.growfarm.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SimpleCommentDTO {

    private Long id;

    @NotNull
    private Long postId;

    @NotNull
    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @NotNull
    @Size(max = 255, message = "댓글은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private int likes;

    private boolean userLike;

    private Instant createdAt;

    /**
     * <h3>마이페이지 댓글 조회용 DTO</h3>
     *
     * @param id        댓글 ID
     * @param postId    게시글 ID
     * @param userName  사용자 이름
     * @param content   댓글 내용
     * @param createdAt 댓글 작성 시간
     * @param likes     댓글 추천 수
     * @param userLike  사용자가 추천를 눌렀는지 여부
     */
    public SimpleCommentDTO(Long id, Long postId, String userName, String content, Instant createdAt, int likes, boolean userLike) {
        this.id = id;
        this.postId = postId;
        this.userName = userName != null ? userName : "익명";
        this.content = content;
        this.createdAt = createdAt;
        this.likes = likes;
        this.userLike = userLike;
    }
}

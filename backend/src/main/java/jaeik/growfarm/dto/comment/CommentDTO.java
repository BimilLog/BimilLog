package jaeik.growfarm.dto.comment;

import jaeik.growfarm.domain.comment.domain.Comment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>댓글 정보 DTO</h2>
 * <p>
 * 게시글에 작성된 댓글의 정보를 담는 DTO 클래스
 * </p>
 *
 * @since 1.0.0
 * @author Jaeik
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentDTO {

    private Long id;

    private Long parentId;

    private Long postId;

    private Long userId;

    @Size(max = 8, message = "닉네임은 최대 8글자 까지 입력 가능합니다.")
    private String userName;

    @Size(max = 255, message = "댓글은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private boolean popular;

    private boolean deleted;

    @Min(value = 1000, message = "비밀번호는 4자리 숫자여야 합니다.")
    @Max(value = 9999, message = "비밀번호는 4자리 숫자여야 합니다.")
    private Integer password;

    private int likes;

    private Instant createdAt;

    private boolean userLike;

    public CommentDTO(Long id, Long postId, Long userId, String userName, String content, boolean deleted, Integer password, Instant createdAt, Long parentId) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.deleted = deleted;
        this.password = password;
        this.createdAt = createdAt;
        this.parentId = parentId;
        this.popular = false;
        this.likes = 0;
        this.userLike = false;
    }

    /**
     * <h3>엔티티를 DTO로 변환</h3>
     * <p>
     * likes와 userLike는 별도로 설정해야 함
     * </p>
     *
     * @param comment 변환할 댓글 엔티티
     * @since 1.0.0
     * @author Jaeik
     */
    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.postId = comment.getPost().getId();
        this.userName = comment.getUser() != null ? comment.getUser().getUserName() : "익명";
        this.userId = comment.getUser() != null ? comment.getUser().getId() : null;
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.password = comment.getPassword();
        this.deleted = comment.isDeleted();
        this.parentId = null;
        this.likes = 0;
        this.userLike = false;
    }
}

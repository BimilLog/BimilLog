package jaeik.growfarm.dto.board;

import jaeik.growfarm.entity.comment.Comment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
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

    private boolean deleted;

    private Integer password;

    private int likes;

    private Instant createdAt;

    private boolean userLike;

    /**
     * <h3>엔티티를 DTO로 변환</h3>
     * <p>
     * likes와 userLike는 별도로 설정해야 함
     * </p>
     *
     * @param comment 변환할 댓글 엔티티
     * @return 변환된 CommentDTO 객체
     * @since 1.0.0
     * @author Jaeik
     */
    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.postId = comment.getPost().getId();
        this.farmName = comment.getUser() != null ? comment.getUser().getUserName() : "비회원";
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.password = comment.getPassword();
        this.deleted = comment.isDeleted();
        this.parentId = null;
        this.likes = 0;
        this.userLike = false;
    }
}

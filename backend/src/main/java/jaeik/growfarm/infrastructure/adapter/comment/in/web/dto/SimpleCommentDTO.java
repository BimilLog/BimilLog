package jaeik.growfarm.infrastructure.adapter.comment.in.web.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>간편 댓글 조회용 DTO</h2>
 * <p>간편 댓글을 조회할 때 사용하는 간단한 댓글 DTO</p>
 * <p>글로 이동할 수 있는 postId가 포함되어있음</p>
 * <p>연관관계와 논리적 삭제를 표현할 필요는 없음</p>
 * <p>댓글의 인기 플래그를 표현할 필요는 없음</p>
 * <p>현재는 주로 마이페이지에서 댓글 조회 시 사용됨</p>
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class SimpleCommentDTO {

    private Long id;

    @NotNull
    private Long postId;

    @NotNull
    private String userName;

    @NotNull
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

    @QueryProjection
    public SimpleCommentDTO(
            Long id,
            Long postId,
            String userName,
            String content,
            Instant createdAt,
            Integer likes,
            boolean userLike) {
        this.id = id;
        this.postId = postId;
        this.userName = userName;
        this.content = content;
        this.createdAt = createdAt;
        this.likes = likes;
        this.userLike = userLike;
    }
}

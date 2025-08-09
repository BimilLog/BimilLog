package jaeik.growfarm.dto.post;

import jaeik.growfarm.entity.post.PostCacheFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * <h2>게시글 목록/상세 보기 시 공통으로 사용되는 DTO</h2>
 * <p>
 * 게시글의 ID, 조회수, 좋아요 수, 생성일 등 조회 관련 필드를 포함합니다.
 * </p>
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BasePostDisplayDTO {

    protected Long postId;
    protected Long userId;
    protected String userName;
    protected String title;
    protected int views;
    protected int likes;
    private int commentCount;
    protected Instant createdAt;
    protected boolean isNotice;
    protected PostCacheFlag postCacheFlag;

    /**
     * <h3>댓글 및 좋아요 수 설정</h3>
     * <p>
     * 댓글 수와 좋아요 수를 설정합니다.
     * </p>
     *
     * @param commentCount 댓글 수
     * @param likeCount    좋아요 수
     * @author Jaeik
     * @since 2.0.0
     */
    public void withCounts(int commentCount, int likeCount) {
        this.commentCount = commentCount;
        this.likes = likeCount;
    }
}
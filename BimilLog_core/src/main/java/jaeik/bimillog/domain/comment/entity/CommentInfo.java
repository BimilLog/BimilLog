package jaeik.bimillog.domain.comment.entity;

import lombok.*;

import java.time.Instant;

/**
 * <h3>댓글 정보 값 객체</h3>
 * <p>댓글 상세 조회 결과를 담는 객체</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentInfo {

    private Long id;
    private Long parentId;
    private Long postId;
    private Long memberId;
    private String memberName;
    private String content;
    private boolean popular;
    private boolean deleted;
    private Integer likeCount;
    private Instant createdAt;
    private boolean userLike;

    /**
     * <h3>일반댓글 생성자<h3>
     * @param id         댓글 ID
     * @param postId     게시글 ID
     * @param memberId   사용자 ID
     * @param memberName 사용자명
     * @param content    댓글 내용
     * @param deleted    삭제 여부
     * @param createdAt  생성 시간
     * @param parentId   부모 댓글 ID (closure.ancestor.id)
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentInfo(Long id, Long postId, Long memberId, String memberName, String content,
                       Boolean deleted, Instant createdAt, Long parentId) {
        this.id = id;
        this.parentId = parentId;
        this.postId = postId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.content = content;
        this.popular = false;
        this.deleted = deleted != null ? deleted : false;
        this.createdAt = createdAt;
    }

    /**
     * <h3>인기댓글 생성자<h3>
     *
     * @param id         댓글 ID
     * @param postId     게시글 ID
     * @param memberId   사용자 ID
     * @param memberName 사용자명
     * @param content    댓글 내용
     * @param deleted    삭제 여부
     * @param createdAt  생성 시간
     * @param parentId   부모 댓글 ID (closure.ancestor.id)
     * @param likeCount  좋아요 수
     * @param userLike   사용자 좋아요 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentInfo(Long id, Long postId, Long memberId, String memberName, String content,
                       Boolean deleted, Instant createdAt, Long parentId, Integer likeCount, Boolean userLike) {
        this.id = id;
        this.parentId = parentId;
        this.postId = postId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.content = content;
        this.popular = true;
        this.deleted = deleted != null ? deleted : false;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.createdAt = createdAt;
        this.userLike = userLike != null ? userLike : false;
    }
}



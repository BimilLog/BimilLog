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


    // 생성자
    public CommentInfo(Long id, Long postId, Long memberId, String memberName, String content,
                       Boolean deleted, Instant createdAt, Long parentId, Integer likeCount, Boolean userLike) {
        this.id = id;
        this.parentId = parentId;
        this.postId = postId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.content = content;
        this.popular = false; // 기본값
        this.deleted = deleted != null ? deleted : false;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.createdAt = createdAt;
        this.userLike = userLike != null ? userLike : false;
    }
}



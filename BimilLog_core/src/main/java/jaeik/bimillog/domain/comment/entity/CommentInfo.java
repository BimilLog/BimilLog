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
}

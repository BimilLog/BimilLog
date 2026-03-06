package jaeik.bimillog.domain.comment.entity;

import lombok.*;

import java.time.Instant;

/**
 * <h3>간편 댓글 정보 값 객체</h3>
 * <p>
 * 간편 댓글 조회 결과를 담는 도메인 순수 값 객체
 * </p>
 * <p>
 * 주로 마이페이지에서 사용자가 작성하거나 추천한 댓글 목록 조회 시 사용
 * </p>
 * <p>
 * 성능 최적화를 위해 mutable로 변경 - 추천수, 사용자 추천 여부를 나중에 설정 가능
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCommentInfo {
    private Long id;
    private Long postId;
    private String memberName;
    private String content;
    private Integer likeCount;
    private boolean userLike;
    private Instant createdAt;
}

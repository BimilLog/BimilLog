package jaeik.bimillog.domain.comment.entity;

import lombok.*;
import org.springframework.data.domain.Page;

import java.time.Instant;

/**
 * 사용자가 마이페이지에서 보는 댓글 객체
 */
@Getter
@AllArgsConstructor
public class MemberActivityComment extends CommentInfo {
    private Page<SimpleCommentInfo> writeComments;
    private Page<SimpleCommentInfo> likedComments;

    /**
     * <h3>간편 댓글 정보 값 객체</h3>
     * <p>
     * 간편 댓글 조회 결과를 담는 도메인 순수 값 객체
     * SimpleCommentDTO의 도메인 전용 대체
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
    public static class SimpleCommentInfo {
        private Long id;
        private Long postId;
        private String memberName;
        private String content;
        private Integer likeCount;
        private boolean userLike;
        private Instant createdAt;

        /**
         * <h3>생성자</h3>
         *
         * @param id 댓글 ID
         * @param postId 게시글 ID
         * @param memberName 사용자명
         * @param content 댓글 내용
         * @param createdAt 생성시각
         * @param likeCount 추천수
         * @param userLike 사용자 추천 여부
         * @author Jaeik
         * @since 2.0.0
         */
        public SimpleCommentInfo(Long id, Long postId, String memberName, String content,
                                Instant createdAt, Integer likeCount, boolean userLike) {
            this.id = id;
            this.postId = postId;
            this.memberName = memberName != null ? memberName : "익명";
            this.content = content;
            this.createdAt = createdAt;
            this.likeCount = likeCount != null ? likeCount : 0;
            this.userLike = userLike;
        }
    }
}

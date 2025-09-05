package jaeik.bimillog.domain.comment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * <h3>댓글 정보 값 객체</h3>
 * <p>
 * 댓글 상세 조회 결과를 담는 도메인 순수 값 객체
 * CommentDTO의 도메인 전용 대체
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
public class CommentInfo {
    
    private Long id;
    private Long parentId;
    private Long postId;
    private Long userId;
    private String userName;
    private String content;
    private boolean popular;
    private boolean deleted;
    private Integer likeCount;
    private Instant createdAt;
    private boolean userLike;

    /**
     * <h3>댓글 정보 생성</h3>
     * <p>댓글 엔티티와 메타 정보로부터 댓글 정보를 생성합니다.</p>
     *
     * @param comment 댓글 엔티티
     * @param parentId 부모 댓글 ID
     * @param likeCount 추천수
     * @param isUserLike 사용자 추천 여부
     * @return CommentInfo 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static CommentInfo of(Comment comment, Long parentId, Integer likeCount, boolean isUserLike) {
        return CommentInfo.builder()
                .id(comment.getId())
                .parentId(parentId)
                .postId(comment.getPost().getId())
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .userName(comment.getUser() != null ? comment.getUser().getUserName() : "익명")
                .content(comment.getContent())
                .popular(false)
                .deleted(comment.isDeleted())
                .likeCount(likeCount != null ? likeCount : 0)
                .createdAt(comment.getCreatedAt())
                .userLike(isUserLike)
                .build();
    }

    /**
     * <h3>추천 여부 없는 댓글 정보 생성</h3>
     * <p>로그인하지 않은 사용자용 댓글 정보를 생성합니다.</p>
     *
     * @param comment 댓글 엔티티
     * @param parentId 부모 댓글 ID
     * @param likeCount 추천수
     * @return CommentInfo 값 객체 (userLike = false)
     * @author Jaeik
     * @since 2.0.0
     */
    public static CommentInfo of(Comment comment, Long parentId, Integer likeCount) {
        return of(comment, parentId, likeCount, false);
    }

    /**
     * <h3>기본 댓글 정보 생성</h3>
     * <p>기본값으로 댓글 정보를 생성합니다.</p>
     *
     * @param comment 댓글 엔티티
     * @return CommentInfo 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static CommentInfo of(Comment comment) {
        return of(comment, null, 0, false);
    }
}
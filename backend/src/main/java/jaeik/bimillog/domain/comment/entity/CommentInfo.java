package jaeik.bimillog.domain.comment.entity;

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

    /**
     * <h3>Builder용 모든 필드 생성자</h3>
     * <p>Lombok @Builder에서 사용하는 생성자입니다.</p>
     * <p>필드 선언 순서대로 매개변수를 배치했습니다.</p>
     *
     * @param id 댓글 ID
     * @param parentId 부모 댓글 ID
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @param userName 사용자명
     * @param content 댓글 내용
     * @param popular 인기 댓글 여부
     * @param deleted 삭제 여부
     * @param likeCount 좋아요 수
     * @param createdAt 생성 시간
     * @param userLike 사용자 좋아요 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentInfo(Long id, Long parentId, Long postId, Long userId, String userName, String content,
                      boolean popular, boolean deleted, Integer likeCount, Instant createdAt, boolean userLike) {
        this.id = id;
        this.parentId = parentId;
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.popular = popular;
        this.deleted = deleted;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.userLike = userLike;
    }

    /**
     * <h3>QueryDSL Projection용 생성자</h3>
     * <p>QueryDSL Projection에서 사용하는 파라미터 순서에 맞는 생성자입니다.</p>
     * <p>CommentProjection.getCommentInfoProjectionWithUserLike에서 호출됩니다.</p>
     *
     * @param id 댓글 ID
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @param userName 사용자명
     * @param content 댓글 내용
     * @param deleted 삭제 여부
     * @param createdAt 생성 시간
     * @param parentId 부모 댓글 ID (closure.ancestor.id)
     * @param likeCount 좋아요 수
     * @param userLike 사용자 좋아요 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public CommentInfo(Long id, Long postId, Long userId, String userName, String content,
                      Boolean deleted, Instant createdAt, Long parentId, Integer likeCount, Boolean userLike) {
        this.id = id;
        this.parentId = parentId;
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.popular = false; // 기본값
        this.deleted = deleted != null ? deleted : false;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.createdAt = createdAt;
        this.userLike = userLike != null ? userLike : false;
    }
}
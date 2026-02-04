package jaeik.bimillog.domain.post.entity.jpa;

import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * <h2>게시글 조회 전용 모델</h2>
 * <p>CQRS 패턴에서 조회 성능 최적화를 위한 비정규화 테이블 엔티티입니다.</p>
 * <p>Post 삭제 시 DB CASCADE로 자동 삭제됩니다.</p>
 * <p>Post 엔티티와 JPA 관계를 맺지 않고 postId만 저장합니다.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Entity
@Table(name = "post_read_model",
        indexes = @Index(name = "idx_post_read_model_created", columnList = "created_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReadModel {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name", length = 50)
    private String memberName;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "modified_at", columnDefinition = "TIMESTAMP(6)")
    private Instant modifiedAt;

    @Builder
    private PostReadModel(Long postId, String title, Integer viewCount, Integer likeCount,
                          Integer commentCount, Long memberId, String memberName, Instant createdAt) {
        this.postId = postId;
        this.title = title;
        this.viewCount = viewCount != null ? viewCount : 0;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.commentCount = commentCount != null ? commentCount : 0;
        this.memberId = memberId;
        this.memberName = memberName != null ? memberName : "익명";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * PostSimpleDetail로부터 PostReadModel 생성
     */
    public static PostReadModel fromPostSimpleDetail(PostSimpleDetail detail) {
        return PostReadModel.builder()
                .postId(detail.getId())
                .title(detail.getTitle())
                .viewCount(detail.getViewCount())
                .likeCount(detail.getLikeCount())
                .commentCount(detail.getCommentCount())
                .memberId(detail.getMemberId())
                .memberName(detail.getMemberName())
                .createdAt(detail.getCreatedAt())
                .build();
    }

    /**
     * Post 엔티티로부터 PostReadModel 생성
     */
    public static PostReadModel fromPost(Post post, String memberName) {
        return PostReadModel.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .viewCount(post.getViews())
                .likeCount(0)
                .commentCount(0)
                .memberId(post.getMember() != null ? post.getMember().getId() : null)
                .memberName(memberName != null ? memberName : "익명")
                .createdAt(post.getCreatedAt())
                .build();
    }

    /**
     * 제목 업데이트
     */
    public void updateTitle(String newTitle) {
        this.title = newTitle;
        this.modifiedAt = Instant.now();
    }

    /**
     * PostSimpleDetail로 변환
     */
    public PostSimpleDetail toPostSimpleDetail() {
        return PostSimpleDetail.builder()
                .id(this.postId)
                .title(this.title)
                .viewCount(this.viewCount)
                .likeCount(this.likeCount)
                .commentCount(this.commentCount)
                .memberId(this.memberId)
                .memberName(this.memberName)
                .createdAt(this.createdAt)
                .build();
    }
}

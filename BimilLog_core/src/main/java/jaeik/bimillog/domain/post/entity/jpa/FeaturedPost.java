package jaeik.bimillog.domain.post.entity.jpa;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.post.entity.PostSimpleDetail;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * <h2>특집 게시글 엔티티</h2>
 * <p>주간 레전드, 레전드, 공지사항을 통합 관리하는 엔티티입니다.</p>
 * <p>스케줄러에서 WEEKLY/LEGEND 선정 시 DB에 저장되고, 공지 토글 시에도 사용됩니다.</p>
 * <p>Redis 캐시와 함께 사용하여 영속성을 보장합니다.</p>
 * <p>PostCacheFlag Enum을 DB 타입으로도 재사용합니다 (REALTIME은 DB에 저장하지 않음).</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@Table(
    name = "featured_post", uniqueConstraints = {
        @UniqueConstraint(name = "uk_featured_post_type", columnNames = {"post_id", "type"})
    },
    indexes = {
        @Index(name = "idx_featured_post_type_featured", columnList = "type, featured_at DESC")
    }
)
public class FeaturedPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "featured_post_id")
    private Long id;

    @NotNull
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Nullable
    @Column(name = "author_name") // 익명 글쓴이는 NULL
    private String author;

    @NotNull
    @Column(nullable = false, length = 30) // 제목 30자 허용
    private String title;

    @NotNull
    @Column(nullable = false)
    private Integer viewCount;

    @NotNull
    @Column(nullable = false)
    private Integer likeCount;

    @NotNull
    @Column(nullable = false)
    private Integer commentCount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PostCacheFlag type;

    @NotNull
    @Column(name = "featured_at", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant featuredAt;

    public static FeaturedPost createFeaturedPost(PostSimpleDetail detail, PostCacheFlag type) {
        return FeaturedPost.builder()
                .postId(detail.getId())
                .author(detail.getMemberName())
                .title(detail.getTitle())
                .viewCount(detail.getViewCount() != null ? detail.getViewCount() : 0)
                .likeCount(detail.getLikeCount() != null ? detail.getLikeCount() : 0)
                .commentCount(detail.getCommentCount() != null ? detail.getCommentCount() : 0)
                .type(type)
                .featuredAt(Instant.now())
                .build();
    }
}

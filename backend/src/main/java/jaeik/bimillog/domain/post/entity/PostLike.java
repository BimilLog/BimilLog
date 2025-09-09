package jaeik.bimillog.domain.post.entity;

import jaeik.bimillog.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>게시글 추천 엔티티</h2>
 * <p>
 * 사용자와 게시글 간의 추천 관계를 나타내는 연결 엔티티
 * </p>
 * <p>게시글 추천/추천 취소 시 PostLikeCommandController에서 생성/삭제됩니다.</p>
 * <p>PostQueryService에서 사용자의 게시글 추천 상태 확인 시 조회됩니다.</p>
 * <p>중복 추천 방지를 위해 user_id + post_id 묶음 인덱스를 사용합니다.</p>
 * <p>CASCADE 옵션으로 사용자나 게시글 삭제 시 자동 삭제됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_postlike_user_post", columnList = "user_id, post_id")
})
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postLike_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id")
    private Post post;
}

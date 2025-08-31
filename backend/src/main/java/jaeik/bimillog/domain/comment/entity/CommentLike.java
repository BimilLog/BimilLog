package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>댓글 추천 엔티티</h2>
 * <p>댓글에 대한 추천 정보를 저장하는 엔티티</p>
 * <p>사용자와 댓글 간의 관계를 나타냄</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        indexes = {
                @Index(name = "idx_comment_like_user_comment", columnList = "comment_id, user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comment_like_user_comment", columnNames = {"comment_id", "user_id"})
        }
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentLike_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "comment_id")
    private Comment comment;
}


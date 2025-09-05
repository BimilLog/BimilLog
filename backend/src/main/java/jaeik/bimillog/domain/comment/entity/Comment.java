package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.common.entity.BaseEntity;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>댓글 엔티티</h2>
 * <p>
 * 게시글에 대한 댓글 정보를 저장하는 엔티티
 * </p>
 * <p>
 * 댓글 내용은 255자 까지 허용한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_comment_post_deleted", columnList = "post_id, deleted"),
        @Index(name = "idx_comment_post_created", columnList = "post_id, created_at DESC")
})
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(nullable = false) // 255자 허용
    private String content;

    @NotNull
    @Column(nullable = false)
    private boolean deleted;

    private Integer password;

    /**
     * <h3>댓글 생성</h3>
     *
     * <p>
     * 새로운 댓글을 생성한다.
     * </p>
     *
     * @param post     댓글이 달릴 게시글
     * @param user     댓글 작성자
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (선택적)
     * @return 생성된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Comment createComment(Post post, User user, String content, Integer password) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .deleted(false)
                .password(password)
                .build();
    }

    /**
     * <h3>댓글 수정</h3>
     *
     * <p>
     * 댓글 내용을 수정한다.
     * </p>
     *
     * @param content 수정할 댓글 내용
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateComment(String content) {
        this.content = content;
    }
}


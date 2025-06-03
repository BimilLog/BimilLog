package jaeik.growfarm.entity.board;

import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>댓글 엔티티</h2>
 * <p>게시글에 대한 댓글 정보를 저장하는 엔티티</p>
 * <p>댓글 내용, 작성자, 게시글 정보 등을 포함</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private Users user;

    @NotNull
    @Column(nullable = false) // 255자 허용
    private String content;

    @Enumerated(EnumType.STRING)
    private PopularFlag popularFlag;

    // 댓글 수정
    public void updateComment(String content) {
        this.content = content;
    }

    public void updatePopular(CommentDTO commentDTO) {
        this.popularFlag = PopularFlag.POPULAR;
    }
}

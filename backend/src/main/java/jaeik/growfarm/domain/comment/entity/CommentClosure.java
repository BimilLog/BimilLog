package jaeik.growfarm.domain.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>댓글 클로저 엔티티</h2>
 * <p>
 * 댓글의 계층 구조를 관리하기 위한 클로저 테이블 엔티티
 * </p>
 * <p>
 * 각 댓글과 그 조상 댓글 간의 관계 및 깊이를 저장한다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_comment_closure_ancestor_depth", columnList = "descendant_id, depth")
})
public class CommentClosure {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ancestor_id")
    private Comment ancestor;

    @ManyToOne
    @JoinColumn(name = "descendant_id")
    private Comment descendant;

    @Column(nullable = false)
    private int depth;

    /**
     * <h3>댓글 클로저 엔티티 생성</h3>
     * <p>조상 댓글, 자손 댓글, 그리고 깊이를 기반으로 새로운 댓글 클로저 엔티티를 생성합니다.</p>
     *
     * @param ancestor   조상 댓글 엔티티
     * @param descendant 자손 댓글 엔티티
     * @param depth      조상 댓글로부터 자손 댓글까지의 깊이
     * @return CommentClosure 생성된 댓글 클로저 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static CommentClosure createCommentClosure(Comment ancestor, Comment descendant, int depth) {
        return CommentClosure.builder()
                .ancestor(ancestor)
                .descendant(descendant)
                .depth(depth)
                .build();
    }
}


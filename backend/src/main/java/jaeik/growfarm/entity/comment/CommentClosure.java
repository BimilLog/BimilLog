package jaeik.growfarm.entity.comment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static CommentClosure createCommentClosure(Comment ancestor, Comment descendant, int depth) {
        return CommentClosure.builder()
                .ancestor(ancestor)
                .descendant(descendant)
                .depth(depth)
                .build();
    }
}

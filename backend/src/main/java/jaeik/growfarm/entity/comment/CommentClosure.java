package jaeik.growfarm.entity.comment;

import jakarta.persistence.*;

@Entity
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
}

package jaeik.growfarm.entity.board;

import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// 게시글 엔티티
@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @NotNull
    @Column(nullable = false)
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @NotNull
    @Column(nullable = false)
    private int views;

    @NotNull
    @Column(nullable = false)
    private boolean isFeatured;

    @NotNull
    @Column(nullable = false)
    private boolean isNotice;

    // 게시글 수정
    public void updatePost(PostDTO postDTO) {
        this.title = postDTO.getTitle();
        this.content = postDTO.getContent();
    }
}

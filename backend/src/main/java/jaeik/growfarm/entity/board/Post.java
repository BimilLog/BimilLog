package jaeik.growfarm.entity.board;

import jaeik.growfarm.dto.board.PostDTO;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private Users user;

    @NotNull
    @Column(nullable = false, length = 30) // 제목 30자 허용
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false) // 내용 1000자 허용
    private String content;

    @NotNull
    @Column(nullable = false)
    private int views;

    @NotNull
    @Column(nullable = false)
    private boolean isNotice;

    @NotNull
    @Column(nullable = false)
    private boolean isRealtimePopular;

    @NotNull
    @Column(nullable = false)
    private boolean isWeeklyPopular;

    @NotNull
    @Column(nullable = false)
    private boolean isHallOfFame;


    // 게시글 수정
    public void updatePost(PostDTO postDTO) {
        this.title = postDTO.getTitle();
        this.content = postDTO.getContent();
    }
}

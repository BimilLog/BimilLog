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

/**
 * <h3>게시글 엔티티</h3>
 * <p>
 * 게시판의 게시글 정보를 저장하는 엔티티
 * </p>
 * 
 * @since 1.0.0
 * @author Jaeik
 */
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

    /**
     * <h3>게시글 정보 업데이트</h3>
     *
     * <p>
     * 게시글의 제목과 내용을 업데이트한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param postDTO 업데이트할 게시글 정보
     */
    public void updatePost(PostDTO postDTO) {
        this.title = postDTO.getTitle();
        this.content = postDTO.getContent();
    }

    /**
     * <h3>실시간 인기글 설정</h3>
     *
     * <p>
     * 게시글을 실시간 인기글로 설정한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param isRealtimePopular 실시간 인기글 여부
     */
    public void setRealtimePopular(boolean isRealtimePopular) {
        this.isRealtimePopular = isRealtimePopular;
    }

    /**
     * <h3>주간 인기글 설정</h3>
     *
     * <p>
     * 게시글을 주간 인기글로 설정한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param isWeeklyPopular 주간 인기글 여부
     */
    public void setWeeklyPopular(boolean isWeeklyPopular) {
        this.isWeeklyPopular = isWeeklyPopular;
    }

    /**
     * <h3>명예의 전당 설정</h3>
     *
     * <p>
     * 게시글을 명예의 전당으로 설정한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param isHallOfFame 명예의 전당 여부
     */
    public void setHallOfFame(boolean isHallOfFame) {
        this.isHallOfFame = isHallOfFame;
    }
}
